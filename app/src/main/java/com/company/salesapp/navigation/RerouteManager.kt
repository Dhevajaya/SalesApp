package com.company.salesapp.navigation

import android.location.Location
import android.util.Log
import com.company.salesapp.config.AppConfig
import com.company.salesapp.network.RouteRepository
import com.company.salesapp.utils.TrackingPrefs

/**
 * Section 22 — Re-routing.
 *
 * Dipanggil LocationService setiap ada titik GPS yang lolos filter.
 * Alur:
 *   1. Ambil geometry rute dari cache Room (bukan network), di-cache lagi di memory
 *      selama ROUTE_GEOMETRY_CACHE_TTL_MS supaya tidak query DB tiap tick GPS.
 *   2. Hitung jarak posisi sekarang ke polyline (RouteDeviationChecker).
 *   3. Bila > threshold DAN cooldown client sudah lewat -> minta rute baru ke Laravel.
 *
 * Server tetap punya cooldown-nya sendiri sebagai lapis kedua (defense in depth
 * terhadap quota TomTom). Nilai di sini HARUS selaras dengan config/routing.php.
 *
 * GPS update biasa TIDAK memicu pemanggilan routing provider (Section 35 & 36).
 */
class RerouteManager(
    private val routeRepository: RouteRepository,
    private val trackingPrefs: TrackingPrefs
) {

    private var cachedGeometry: List<GeoPoint> = emptyList()
    private var geometryLoadedAtMs: Long = 0L

    /**
     * @return true bila rute baru berhasil didapat (pemanggil boleh refresh tampilan).
     */
    suspend fun onNewLocation(location: Location): Boolean {
        try {
            refreshGeometryIfStale()
            if (cachedGeometry.size < 2) return false

            val current = GeoPoint(location.latitude, location.longitude)
            val now = System.currentTimeMillis()

            val shouldReroute = RouteDeviationChecker.shouldReroute(
                current = current,
                routeGeometry = cachedGeometry,
                thresholdMeters = AppConfig.REROUTE_DEVIATION_THRESHOLD_METERS,
                cooldownMs = AppConfig.REROUTE_COOLDOWN_MS,
                lastRerouteAtMs = trackingPrefs.lastRerouteAtMs,
                nowMs = now
            )
            if (!shouldReroute) return false

            Log.d(TAG, "Deviasi terdeteksi -> minta reroute ke Laravel")

            // Set timestamp DULU (optimistic) supaya tidak menembak berkali-kali
            // kalau network lambat dan beberapa titik GPS masuk beruntun.
            trackingPrefs.lastRerouteAtMs = now

            val newRoute = routeRepository.requestReroute(
                latitude = location.latitude,
                longitude = location.longitude,
                trackingSessionId = trackingPrefs.trackingSessionId
            ) ?: return false

            cachedGeometry = newRoute.geometry.orEmpty().map { GeoPoint(it.lat, it.lng) }
            geometryLoadedAtMs = System.currentTimeMillis()
            Log.d(TAG, "Reroute sukses, source=${newRoute.source}")
            return true
        } catch (e: Exception) {
            // Kegagalan reroute TIDAK boleh menghentikan tracking — tracking jauh
            // lebih penting daripada rute yang up-to-date.
            Log.w(TAG, "Reroute check gagal (diabaikan): ${e.message}")
            return false
        }
    }

    /** Paksa baca ulang geometry dari cache (mis. setelah rute baru dimuat di peta). */
    fun invalidate() {
        geometryLoadedAtMs = 0L
    }

    private suspend fun refreshGeometryIfStale() {
        val now = System.currentTimeMillis()
        if (now - geometryLoadedAtMs <= AppConfig.ROUTE_GEOMETRY_CACHE_TTL_MS) return

        cachedGeometry = routeRepository.getCachedGeometryOnly()
            .map { GeoPoint(it.lat, it.lng) }
        geometryLoadedAtMs = now
    }

    companion object {
        private const val TAG = "RerouteManager"
    }
}
