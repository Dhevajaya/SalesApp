package com.company.salesapp.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Section 12 blueprint - Sync Manager.
 * Mengatur pengiriman Local Queue (Room) ke Laravel API, dengan:
 * - retry otomatis (exponential backoff) via WorkManager
 * - hanya berjalan saat ada koneksi internet (Constraints)
 * - idempotency dijamin oleh location_event_id (Section 13), bukan oleh SyncManager
 */
object SyncManager {

    private const val PERIODIC_WORK_NAME = "location_sync_periodic"
    private const val IMMEDIATE_WORK_NAME = "location_sync_immediate"

    /** Dipanggil setelah setiap insert lokasi baru; mencoba kirim segera bila online. */
    fun requestImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP, // jangan numpuk job identik bila sudah ada yang antre
            request
        )
    }

    /**
     * Jadwal periodik sebagai jaring pengaman (fallback) bila immediate sync gagal terus,
     * dipanggil sekali saat START WORK / app startup.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
