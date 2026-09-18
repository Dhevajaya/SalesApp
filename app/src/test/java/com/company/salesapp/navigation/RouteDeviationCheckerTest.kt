package com.company.salesapp.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test murni (tanpa device/emulator) untuk logika deteksi deviasi rute —
 * bagian Testing Checklist (Section 33) yang bisa diotomasi penuh.
 *
 * Jalankan: gradlew testDebugUnitTest
 */
class RouteDeviationCheckerTest {

    // Rute lurus dari (-6.200, 106.800) ke (-6.210, 106.800), sekitar 1.1 km ke selatan.
    private val straightRoute = listOf(
        GeoPoint(-6.200, 106.800),
        GeoPoint(-6.210, 106.800)
    )

    @Test
    fun `titik tepat di atas garis rute dianggap tidak menyimpang`() {
        val onRoute = GeoPoint(-6.205, 106.800)
        val distance = RouteDeviationChecker.distanceToRouteMeters(onRoute, straightRoute)
        assertTrue("Jarak ke rute seharusnya sangat kecil, malah $distance meter", distance < 5.0)
    }

    @Test
    fun `titik jauh dari rute terdeteksi melebihi threshold`() {
        // Geser ~0.01 derajat longitude (~1.1 km) ke timur.
        val farFromRoute = GeoPoint(-6.205, 106.810)
        val distance = RouteDeviationChecker.distanceToRouteMeters(farFromRoute, straightRoute)
        assertTrue("Jarak seharusnya >300m, malah $distance meter", distance > 300.0)
    }

    @Test
    fun `shouldReroute false kalau masih dalam cooldown walau sudah menyimpang jauh`() {
        val now = 1_000_000L
        val result = RouteDeviationChecker.shouldReroute(
            current = GeoPoint(-6.205, 106.810),
            routeGeometry = straightRoute,
            thresholdMeters = 300.0,
            cooldownMs = 300_000L,          // 5 menit
            lastRerouteAtMs = now - 60_000L, // baru 1 menit lalu
            nowMs = now
        )
        assertFalse("Harusnya ditahan cooldown", result)
    }

    @Test
    fun `shouldReroute true kalau menyimpang jauh dan cooldown sudah lewat`() {
        val now = 1_000_000L
        val result = RouteDeviationChecker.shouldReroute(
            current = GeoPoint(-6.205, 106.810),
            routeGeometry = straightRoute,
            thresholdMeters = 300.0,
            cooldownMs = 300_000L,
            lastRerouteAtMs = now - 400_000L, // 6.6 menit lalu
            nowMs = now
        )
        assertTrue("Harusnya memicu reroute", result)
    }

    @Test
    fun `shouldReroute false kalau belum ada geometry rute`() {
        val result = RouteDeviationChecker.shouldReroute(
            current = GeoPoint(-6.205, 106.810),
            routeGeometry = emptyList(),
            thresholdMeters = 300.0,
            cooldownMs = 300_000L,
            lastRerouteAtMs = 0L,
            nowMs = 1_000_000L
        )
        assertFalse(result)
    }

    @Test
    fun `distanceMeters menghitung jarak dua titik dengan wajar`() {
        // 0.001 derajat latitude kurang lebih 111 meter.
        val d = RouteDeviationChecker.distanceMeters(
            GeoPoint(-6.200, 106.800),
            GeoPoint(-6.201, 106.800)
        )
        assertTrue("Harusnya sekitar 111 m, malah $d", d in 100.0..125.0)
    }
}
