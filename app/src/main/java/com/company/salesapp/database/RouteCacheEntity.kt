package com.company.salesapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline route cache (Section 22 & 25).
 *
 * Menyimpan hasil terakhir dari GET /api/sales/routes/today supaya:
 *  1. MapActivity tetap bisa menggambar rute terakhir walau device sedang offline.
 *  2. LocationService (deviation detector) punya geometry rute untuk dibandingkan
 *     dengan posisi GPS, TANPA memanggil network di setiap update GPS
 *     (Section 36: jangan panggil routing provider tiap GPS update).
 *
 * Satu baris per tanggal (routeDate = "yyyy-MM-dd"), selalu di-replace dengan data terbaru.
 */
@Entity(tableName = "route_cache")
data class RouteCacheEntity(
    @PrimaryKey
    val routeDate: String,
    val source: String?,
    val routeId: Long?,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val geometryJson: String, // List<GeoPointDto> di-serialize Gson
    val stopsJson: String,    // List<RouteStopDto> di-serialize Gson
    val savedAtEpochMs: Long
)
