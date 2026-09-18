package com.company.salesapp.navigation

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sqrt

/** Titik geografis sederhana, sengaja lepas dari Android Location API agar bisa di-unit-test. */
data class GeoPoint(val lat: Double, val lng: Double)

/**
 * Section 22 — GPS Deviation Detector.
 *
 * Murni perhitungan, tanpa dependensi Android/MapLibre, supaya bisa diuji
 * tanpa device (lihat app/src/test/.../RouteDeviationCheckerTest.kt).
 *
 * Memakai pendekatan equirectangular: cukup akurat untuk jarak pendek
 * (puluhan–ratusan meter, sesuai skala threshold) dan jauh lebih murah
 * dibanding haversine penuh karena dipanggil di tiap update GPS.
 */
object RouteDeviationChecker {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)

        val x = dLng * cos((lat1 + lat2) / 2)
        val y = dLat
        return sqrt(x * x + y * y) * EARTH_RADIUS_METERS
    }

    /** Jarak titik `p` ke segmen garis a–b, dalam meter. */
    private fun distanceToSegmentMeters(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val latRef = Math.toRadians((a.lat + b.lat) / 2)
        val scaleLng = cos(latRef)

        val ax = a.lng * scaleLng
        val ay = a.lat
        val bx = b.lng * scaleLng
        val by = b.lat
        val px = p.lng * scaleLng
        val py = p.lat

        val dx = bx - ax
        val dy = by - ay
        val lengthSq = dx * dx + dy * dy

        val t = if (lengthSq == 0.0) {
            0.0
        } else {
            (((px - ax) * dx + (py - ay) * dy) / lengthSq).coerceIn(0.0, 1.0)
        }

        val closestX = ax + t * dx
        val closestY = ay + t * dy
        val closest = GeoPoint(closestY, closestX / scaleLng)

        return distanceMeters(p, closest)
    }

    /**
     * @return jarak terpendek (meter) dari `current` ke polyline rute.
     *   Double.MAX_VALUE bila polyline kurang dari 2 titik (tidak bisa dinilai).
     */
    fun distanceToRouteMeters(current: GeoPoint, routeGeometry: List<GeoPoint>): Double {
        if (routeGeometry.size < 2) return Double.MAX_VALUE

        var minDistance = Double.MAX_VALUE
        for (i in 0 until routeGeometry.size - 1) {
            val d = distanceToSegmentMeters(current, routeGeometry[i], routeGeometry[i + 1])
            minDistance = min(minDistance, d)
        }
        return minDistance
    }

    /**
     * Section 22: re-route HANYA saat deviasi signifikan, dan tidak boleh spam.
     *
     * @param lastRerouteAtMs 0L bila belum pernah reroute sama sekali.
     */
    fun shouldReroute(
        current: GeoPoint,
        routeGeometry: List<GeoPoint>,
        thresholdMeters: Double,
        cooldownMs: Long,
        lastRerouteAtMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (routeGeometry.size < 2) return false          // belum ada rute pembanding
        if (nowMs - lastRerouteAtMs < cooldownMs) return false // cooldown guard

        return distanceToRouteMeters(current, routeGeometry) > thresholdMeters
    }
}
