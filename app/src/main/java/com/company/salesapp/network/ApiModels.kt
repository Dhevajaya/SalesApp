package com.company.salesapp.network

import com.google.gson.annotations.SerializedName

/**
 * Request body POST /api/sales/location
 * WAJIB mengikuti kontrak Laravel apa adanya (Section 14).
 * Jangan menambah/mengubah field tanpa koordinasi ke backend.
 */
data class LocationEventRequest(
    @SerializedName("location_event_id") val locationEventId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("tracking_session_id") val trackingSessionId: Long?
)

data class LocationEventResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("server_received_at") val serverReceivedAt: String?
)

data class StartWorkResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("ready_to_work") val readyToWork: Boolean,
    @SerializedName("message") val message: String?
)

data class TrackingStartResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("tracking_session_id") val trackingSessionId: Long?
)

data class TrackingStopResponse(
    @SerializedName("success") val success: Boolean
)

data class TrackingStatusResponse(
    @SerializedName("status") val status: String, // ACTIVE, IDLE, AT_CUSTOMER, SIGNAL_LOST, OFFLINE, OFF_DUTY
    @SerializedName("tracking_session_id") val trackingSessionId: Long?
)

data class CurrentTaskResponse(
    @SerializedName("task_id") val taskId: Long?,
    @SerializedName("is_applied") val isApplied: Boolean,
    @SerializedName("status") val status: String?
)

data class GenericSuccessResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

// =====================================================================
// ROUTE (Section 19, 22, 25)
// Android TIDAK memanggil TomTom langsung — semua lewat proxy Laravel.
// Bentuk response di bawah ini harus disepakati dengan backend.
// =====================================================================

/** Satu titik geometry polyline rute. */
data class GeoPointDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

/** Satu stop customer di rute harian. */
data class RouteStopDto(
    @SerializedName("customer_id") val customerId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("sequence") val sequence: Int,
    @SerializedName("status") val status: String? = null // pending | arrived | skipped
)

/**
 * Response GET /api/sales/routes/today dan POST /api/sales/routes/reroute.
 *
 * `source` menandai asal data menurut server: cache | provider | fallback.
 * `geometry` boleh null/kosong -> Android jatuh ke garis lurus antar stop
 * (ditandai abu-abu di peta supaya tidak dikira rute jalan sungguhan).
 */
data class RouteResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("source") val source: String?,
    @SerializedName("route_id") val routeId: Long?,
    @SerializedName("distance_meters") val distanceMeters: Int?,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
    @SerializedName("stops") val stops: List<RouteStopDto>?,
    @SerializedName("geometry") val geometry: List<GeoPointDto>?
)

/** Body POST /api/sales/routes/reroute — posisi Sales saat menyimpang. */
data class RerouteRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("tracking_session_id") val trackingSessionId: Long? = null
)
