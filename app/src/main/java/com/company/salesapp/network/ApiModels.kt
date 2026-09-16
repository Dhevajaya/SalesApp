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
