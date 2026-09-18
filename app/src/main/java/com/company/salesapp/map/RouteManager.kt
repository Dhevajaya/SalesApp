package com.company.salesapp.map

import android.content.Context
import com.company.salesapp.network.GeoPointDto
import com.company.salesapp.network.RouteRepository
import com.company.salesapp.network.RouteResponse

/**
 * Section 19 & 36 — Routing.
 *
 * Arsitektur wajib:
 *   Android -> Laravel Route Endpoint (proxy) -> TomTom Orbis v3 -> Normalized Route -> Android -> MapLibre
 *
 * Kelas ini SENGAJA tipis: semua network + cache dikerjakan RouteRepository.
 * Tugasnya hanya menyiapkan data agar siap digambar MapActivity, termasuk
 * menentukan apakah polyline yang digambar itu geometry asli dari provider
 * atau sekadar garis lurus antar stop (fallback).
 *
 * Larangan yang tetap dipatuhi:
 * - TIDAK memanggil TomTom langsung dari Android.
 * - TIDAK memanggil routing provider di setiap GPS update (pakai cache).
 * - TIDAK memakai Google Maps App eksternal / OSRM self-hosted.
 */
class RouteManager(context: Context) {

    private val repository = RouteRepository(context.applicationContext)

    data class RenderableRoute(
        val route: RouteResponse,
        /** true bila data berasal dari cache lokal (device sedang offline). */
        val fromCache: Boolean,
        /** Titik-titik yang akan digambar sebagai polyline. */
        val line: List<GeoPointDto>,
        /** true bila `line` hanya garis lurus antar stop, bukan geometry jalan asli. */
        val isStraightLineFallback: Boolean
    )

    suspend fun loadTodayRoute(): RenderableRoute? {
        val result = repository.getTodayRoute()
        val route = result.route ?: return null

        val geometry = route.geometry.orEmpty()
        val useFallback = geometry.size < 2

        val line = if (useFallback) {
            route.stops.orEmpty()
                .sortedBy { it.sequence }
                .map { GeoPointDto(it.latitude, it.longitude) }
        } else {
            geometry
        }

        return RenderableRoute(
            route = route,
            fromCache = result.fromCache,
            line = line,
            isStraightLineFallback = useFallback
        )
    }

    /** Label singkat asal data, untuk ditampilkan ke Sales agar tidak salah paham. */
    fun sourceLabel(renderable: RenderableRoute): String {
        return when {
            renderable.fromCache -> "Rute tersimpan (offline)"
            renderable.isStraightLineFallback -> "Mode fallback — garis lurus, bukan rute jalan"
            renderable.route.source == "cache" -> "Rute dari cache server"
            renderable.route.source == "provider" -> "Rute baru dari provider"
            renderable.route.source == "fallback" -> "Mode fallback server (tanpa jarak presisi)"
            else -> "Rute hari ini"
        }
    }
}
