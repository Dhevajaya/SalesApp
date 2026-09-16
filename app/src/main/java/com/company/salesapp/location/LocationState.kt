package com.company.salesapp.location

/**
 * Lifecycle status Foreground Location Service (Section 10 blueprint).
 */
enum class ServiceLifecycleState {
    STOPPED,
    STARTING,
    ACTIVE,
    PAUSED_SIGNAL_LOST,
    STOPPING
}

/**
 * Status tracking yang perlu diketahui WebView/Laravel (Section 16 blueprint).
 * Perhitungan akhir ACTIVE/IDLE/AT_CUSTOMER tetap ditentukan server;
 * nilai di client ini adalah representasi lokal untuk UI & bridge.
 */
enum class TrackingStatus {
    ACTIVE,
    IDLE,
    AT_CUSTOMER,
    SIGNAL_LOST,
    OFFLINE,
    OFF_DUTY
}

enum class BridgeCommandStatus {
    ACTIVE,
    STOPPED,
    PERMISSION_DENIED,
    SIGNAL_LOST
}

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val recordedAtEpochMillis: Long
)
