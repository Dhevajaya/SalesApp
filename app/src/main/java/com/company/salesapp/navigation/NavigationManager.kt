package com.company.salesapp.navigation

import com.company.salesapp.config.AppConfig
import com.company.salesapp.network.RouteStepDto
import com.company.salesapp.network.RouteStopDto
import kotlin.math.roundToInt

/**
 * Section 21 — Navigation Mode (in-app).
 *
 * PERBAIKAN AUDIT #16/#34 (turn-by-turn dasar): Laravel sekarang mengirim
 * `steps` (instruksi + titik maneuver + jarak kumulatif dari awal rute,
 * lihat TomTomRoutingAdapter::extractGuidanceMessages()). NavigationManager
 * memajukan instruksi aktif berdasarkan JARAK GARIS LURUS dari posisi GPS
 * Sales SEKARANG ke titik maneuver berikutnya — BUKAN dengan memanggil
 * TomTom lagi (tetap dilarang Section 36). ini turn-by-turn "cukup jujur":
 * instruksi berganti otomatis saat Sales mendekati titik belok, tapi TIDAK
 * ada voice guidance dan tidak ada re-snap ke jalan (map-matching) — itu di
 * luar scope tanpa provider navigation SDK penuh.
 *
 * Instance per sesi navigasi dipegang MapActivity (state currentStepIndex
 * disimpan per objek, bukan object singleton, supaya tidak nyangkut antar
 * kunjungan berbeda).
 */
class NavigationManager(private val steps: List<RouteStepDto>) {

    private var currentStepIndex = 0

    /** Instruksi yang sedang aktif, atau null kalau tidak ada guidance sama sekali. */
    fun currentStep(): RouteStepDto? = steps.getOrNull(currentStepIndex)

    /**
     * Panggil setiap ada update GPS baru (dari LocationService, lewat
     * broadcast lokal / StateFlow yang sudah ada) untuk memajukan instruksi
     * bila Sales sudah cukup dekat dengan titik maneuver aktif.
     * @return true bila instruksi aktif BERGANTI (berguna untuk trigger getaran/suara ringan).
     */
    fun onLocationUpdate(current: GeoPoint): Boolean {
        val step = currentStep() ?: return false
        val stepLat = step.latitude
        val stepLng = step.longitude
        if (stepLat == null || stepLng == null) {
            // Instruksi tanpa titik (mis. "Anda telah sampai") -> lewati saja ke berikutnya.
            currentStepIndex = (currentStepIndex + 1).coerceAtMost(steps.size)
            return true
        }

        val distance = RouteDeviationChecker.distanceMeters(current, GeoPoint(stepLat, stepLng))
        if (distance <= AppConfig.MANEUVER_ARRIVAL_RADIUS_METERS && currentStepIndex < steps.size) {
            currentStepIndex++
            return true
        }
        return false
    }

    /** Teks instruksi aktif untuk ditampilkan di panel bawah MapActivity. */
    fun instructionText(current: GeoPoint?): String {
        val step = currentStep() ?: return "Instruksi arah belum tersedia untuk rute ini."
        val message = step.message ?: "Lanjutkan perjalanan"

        if (current == null || step.latitude == null || step.longitude == null) return message

        val distance = RouteDeviationChecker.distanceMeters(current, GeoPoint(step.latitude, step.longitude))
        return "$message • ${formatDistance(distance)} lagi"
    }

    fun isFinished(): Boolean = currentStepIndex >= steps.size

    companion object {
        /** Stop pending pertama berdasarkan urutan sequence, atau null bila semua selesai. */
        fun nextPendingStop(stops: List<RouteStopDto>): RouteStopDto? {
            return stops
                .filter { it.status == null || it.status.equals("pending", ignoreCase = true) }
                .minByOrNull { it.sequence }
        }

        /**
         * Teks guidance fallback (dipakai kalau `steps` kosong - mis. mode
         * fallback tanpa TomTom) — jarak garis lurus ke stop berikutnya.
         */
        fun guidanceText(stops: List<RouteStopDto>, current: GeoPoint?): String {
            val next = nextPendingStop(stops)
                ?: return "Semua stop hari ini sudah dikunjungi."

            if (current == null) return "Menuju: ${next.name}"

            val distance = RouteDeviationChecker.distanceMeters(
                current,
                GeoPoint(next.latitude, next.longitude)
            )
            return "Menuju: ${next.name} • ${formatDistance(distance)} (garis lurus)"
        }

        fun formatDistance(meters: Double): String {
            return if (meters >= 1000) {
                String.format("%.1f km", meters / 1000)
            } else {
                "${meters.roundToInt()} m"
            }
        }

        fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            return if (minutes >= 60) {
                "${minutes / 60} jam ${minutes % 60} mnt"
            } else {
                "$minutes mnt"
            }
        }
    }
}
