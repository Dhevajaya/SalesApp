package com.company.salesapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Location Queue (Section 12 blueprint).
 * Field minimal sesuai dokumen:
 * location_event_id, sales_id, latitude, longitude, accuracy,
 * recorded_at, tracking_session_id, sync_status
 *
 * location_event_id dipakai sebagai idempotency key (Section 13).
 */
@Entity(tableName = "location_events")
data class LocationEventEntity(
    @PrimaryKey
    val locationEventId: String, // UUID, dibuat di client, idempotency key

    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val recordedAt: String,      // ISO-8601, contoh: 2026-09-14T14:20:00+07:00
    val trackingSessionId: Long?,

    // sales_id TIDAK dikirim dari client sebagai authority (Section 28 - Security).
    // Disimpan lokal hanya untuk keperluan debug/multi-akun device, bukan untuk otorisasi.
    val salesIdLocalRef: String? = null,

    val syncStatus: String = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val createdAtLocal: Long = System.currentTimeMillis()
)

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}
