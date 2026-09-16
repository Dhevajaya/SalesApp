package com.company.salesapp.location

import android.location.Location

/**
 * Section 11 blueprint - Location Processor.
 * Setiap GPS update TIDAK langsung dikirim ke server:
 *   Accuracy valid? -> tidak, filter/tandai
 *   Moved enough?    -> tidak, skip
 *   ya -> masuk Local Queue
 *
 * Baseline: interval 5-10 detik ATAU perpindahan minimum 20-50 meter.
 * Nilai default di bawah adalah baseline; sesuaikan lebih lanjut dengan
 * kebutuhan navigation/visit/speed/battery/network sesuai catatan blueprint.
 */
class LocationProcessor(
    private val maxAcceptableAccuracyMeters: Float = 50f,
    private val minMovementMeters: Float = 30f,
    private val minIntervalMillis: Long = 7_000L
) {

    private var lastAcceptedLocation: Location? = null
    private var lastAcceptedAtMillis: Long = 0L

    /**
     * @return LocationSnapshot bila update layak dikirim ke Local Queue, atau null bila di-skip.
     */
    fun process(location: Location): LocationSnapshot? {
        if (!isAccuracyValid(location)) {
            return null
        }

        val now = System.currentTimeMillis()
        val last = lastAcceptedLocation

        if (last != null) {
            val elapsed = now - lastAcceptedAtMillis
            val distance = last.distanceTo(location)

            val movedEnough = distance >= minMovementMeters
            val enoughTimePassed = elapsed >= minIntervalMillis

            // Diterima jika sudah berpindah cukup jauh ATAU sudah cukup lama sejak update terakhir.
            if (!movedEnough && !enoughTimePassed) {
                return null
            }
        }

        lastAcceptedLocation = location
        lastAcceptedAtMillis = now

        return LocationSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            recordedAtEpochMillis = now
        )
    }

    private fun isAccuracyValid(location: Location): Boolean {
        if (!location.hasAccuracy()) return false
        return location.accuracy <= maxAcceptableAccuracyMeters
    }

    fun reset() {
        lastAcceptedLocation = null
        lastAcceptedAtMillis = 0L
    }
}
