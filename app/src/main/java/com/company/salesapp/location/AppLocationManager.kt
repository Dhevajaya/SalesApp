package com.company.salesapp.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Wrapper physical GPS (Fused Location Provider).
 * Sumber utama background tracking (Section 9 & 29 blueprint) —
 * BUKAN WebView navigator.geolocation.
 */
class AppLocationManager(context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission") // permission divalidasi sebelum method ini dipanggil
    fun startUpdates(intervalMillis: Long = 5_000L, onLocation: (Location) -> Unit) {
        stopUpdates()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocation(it) }
            }
        }
        callback = locationCallback

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stopUpdates() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
    }
}
