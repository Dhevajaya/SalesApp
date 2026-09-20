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

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        dao.markAttempt(event.locationEventId, SyncStatus.SYNCED, System.currentTimeMillis())
                    }
                    // PERBAIKAN AUDIT #11/#35: 401 (token invalid/habis), 403 (tidak
                    // berhak), 400/422 (data tidak valid, mis. tracking_session_id
                    // sudah tidak aktif) adalah kegagalan PERMANEN - mengulang
                    // dengan payload yang sama pasti gagal lagi. Blueprint melarang
                    // auto-retry untuk kasus ini, jadi ditandai REJECTED (bukan
                    // FAILED) supaya keluar dari antrian getPendingBatch().
                    response.code() == 401 || response.code() == 403 ||
                        response.code() == 400 || response.code() == 422 -> {
                        dao.markAttempt(event.locationEventId, SyncStatus.REJECTED, System.currentTimeMillis())
                        // TIDAK di-set hadFailure = true: kegagalan permanen tidak
                        // boleh memicu WorkManager backoff-retry Result.retry().
                    }
                    else -> {
                        // 5xx / kode lain yang tidak terduga -> transient, boleh retry.
                        dao.markAttempt(event.locationEventId, SyncStatus.FAILED, System.currentTimeMillis())
                        hadFailure = true
                    }
                }
            } catch (e: Exception) {
                // Network error / timeout -> transient, tetap FAILED supaya di-retry.
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
