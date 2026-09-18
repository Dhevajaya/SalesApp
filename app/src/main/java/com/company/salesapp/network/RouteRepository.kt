package com.company.salesapp.network

import android.content.Context
import com.company.salesapp.database.AppDatabase
import com.company.salesapp.database.RouteCacheEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Section 22 & 25 — sumber tunggal data rute untuk seluruh app.
 *
 * Strategi: coba network dulu, kalau gagal jatuh ke cache Room (offline-safe).
 *
 * Dipakai oleh:
 *  - MapActivity        -> menggambar rute + stop di peta
 *  - RerouteManager     -> baca geometry untuk deteksi deviasi tanpa network call
 */
class RouteRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val dao = AppDatabase.getInstance(appContext).routeCacheDao()
    private val api = ApiClient.getApiService(appContext)

    data class RouteResult(
        val route: RouteResponse?,
        val fromCache: Boolean
    )

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /**
     * Ambil rute hari ini. Network dulu; bila gagal/kosong -> cache lokal.
     * @return RouteResult(route = null) bila network gagal DAN cache juga kosong.
     */
    suspend fun getTodayRoute(): RouteResult {
        try {
            val response = api.getTodayRoute()
            val body = response.body()
            if (response.isSuccessful && body != null && body.success) {
                saveToCache(body)
                return RouteResult(body, fromCache = false)
            }
        } catch (_: Exception) {
            // Offline / server mati / timeout -> lanjut baca cache di bawah.
        }

        return RouteResult(loadFromCache(), fromCache = true)
    }

    /**
     * Minta rute baru karena Sales menyimpang (Section 22).
     * Hasil yang sukses langsung disimpan ke cache supaya jadi acuan deviasi berikutnya.
     */
    suspend fun requestReroute(
        latitude: Double,
        longitude: Double,
        trackingSessionId: Long?
    ): RouteResponse? {
        return try {
            val response = api.postReroute(
                RerouteRequest(latitude, longitude, trackingSessionId)
            )
            val body = response.body()
            if (response.isSuccessful && body != null && body.success) {
                saveToCache(body)
                body
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveToCache(route: RouteResponse) {
        val key = todayKey()
        dao.upsert(
            RouteCacheEntity(
                routeDate = key,
                source = route.source,
                routeId = route.routeId,
                distanceMeters = route.distanceMeters,
                durationSeconds = route.durationSeconds,
                geometryJson = gson.toJson(route.geometry ?: emptyList<GeoPointDto>()),
                stopsJson = gson.toJson(route.stops ?: emptyList<RouteStopDto>()),
                savedAtEpochMs = System.currentTimeMillis()
            )
        )
        dao.clearOtherThan(key)
    }

    suspend fun loadFromCache(): RouteResponse? {
        val entity = dao.getByDate(todayKey()) ?: dao.getLatest() ?: return null

        val geometryType = object : TypeToken<List<GeoPointDto>>() {}.type
        val stopsType = object : TypeToken<List<RouteStopDto>>() {}.type

        return RouteResponse(
            success = true,
            source = entity.source,
            routeId = entity.routeId,
            distanceMeters = entity.distanceMeters,
            durationSeconds = entity.durationSeconds,
            geometry = gson.fromJson(entity.geometryJson, geometryType) ?: emptyList(),
            stops = gson.fromJson(entity.stopsJson, stopsType) ?: emptyList()
        )
    }

    /** Hanya geometry, tanpa network — dipakai deviation detector di LocationService. */
    suspend fun getCachedGeometryOnly(): List<GeoPointDto> {
        val entity = dao.getByDate(todayKey()) ?: dao.getLatest() ?: return emptyList()
        val geometryType = object : TypeToken<List<GeoPointDto>>() {}.type
        return gson.fromJson(entity.geometryJson, geometryType) ?: emptyList()
    }
}
