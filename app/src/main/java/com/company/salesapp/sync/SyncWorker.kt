package com.company.salesapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.company.salesapp.database.AppDatabase
import com.company.salesapp.database.SyncStatus
import com.company.salesapp.network.ApiClient
import com.company.salesapp.network.LocationEventRequest

/**
 * Worker yang benar-benar mengirim isi Local Queue ke:
 *   POST /api/sales/location
 *
 * - location_event_id dipakai sebagai idempotency key (Section 13):
 *   bila request dikirim ulang (mis. retry setelah timeout tapi server
 *   sebenarnya sudah menerima), Laravel yang bertanggung jawab menolak duplikat.
 * - Kegagalan per-item ditandai FAILED + retryCount++, TIDAK menghapus data (Section 35 no.11).
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).locationEventDao()
        val api = ApiClient.getApiService(applicationContext)

        val pending = dao.getPendingBatch(limit = 50)
        if (pending.isEmpty()) return Result.success()

        var hadFailure = false

        for (event in pending) {
            try {
                val response = api.postLocation(
                    LocationEventRequest(
                        locationEventId = event.locationEventId,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        accuracy = event.accuracy,
                        recordedAt = event.recordedAt,
                        trackingSessionId = event.trackingSessionId
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    dao.markAttempt(event.locationEventId, SyncStatus.SYNCED, System.currentTimeMillis())
                } else {
                    dao.markAttempt(event.locationEventId, SyncStatus.FAILED, System.currentTimeMillis())
                    hadFailure = true
                }
            } catch (e: Exception) {
                // Network error / timeout -> tetap FAILED, akan di-retry oleh WorkManager
                // atau immediate sync berikutnya. Data TIDAK dihapus.
                dao.markAttempt(event.locationEventId, SyncStatus.FAILED, System.currentTimeMillis())
                hadFailure = true
            }
        }

        // Bersihkan data lama yang sudah SYNCED (>7 hari) agar Room tidak membengkak.
        dao.purgeSynced(System.currentTimeMillis() - SEVEN_DAYS_MILLIS)

        return if (hadFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
