package com.company.salesapp.navigation

import com.company.salesapp.network.RouteStopDto
import kotlin.math.roundToInt

/**
 * Section 21 — Navigation Mode (in-app).
 *
 * BATAS JUJUR dari implementasi saat ini: ini BUKAN turn-by-turn navigation.
 * Belum ada instruksi manuver ("belok kiri 200 m lagi") maupun voice guidance,
 * karena itu butuh parsing maneuver/steps dari response TomTom yang belum
 * disediakan endpoint Laravel. Yang sudah jalan:
 *   - menentukan stop berikutnya (sequence terkecil yang masih pending)
 *   - jarak garis lurus ke stop tersebut, untuk orientasi Sales di peta
 *
 * Jarak garis lurus SENGAJA dipakai, bukan jarak jalan, karena menghitung jarak
 * jalan real-time berarti memanggil TomTom tiap detik — dilarang Section 36.
 */
object NavigationManager {

    /** Stop pending pertama berdasarkan urutan sequence, atau null bila semua selesai. */
    fun nextPendingStop(stops: List<RouteStopDto>): RouteStopDto? {
        return stops
            .filter { it.status == null || it.status.equals("pending", ignoreCase = true) }
            .minByOrNull { it.sequence }
    }

    /**
     * Teks guidance yang ditampilkan di panel bawah MapActivity.
     * @param current posisi Sales sekarang; null bila lokasi belum tersedia.
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
