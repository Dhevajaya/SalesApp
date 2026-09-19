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
import com.google.android.gms.tasks.CancellationTokenSource

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

    /**
     * Ambil SATU titik lokasi (bukan stream terus-menerus). Dipakai untuk
     * fitur yang butuh GPS sesaat di luar sesi tracking aktif -- mis.
     * Tagging Toko -- yang TIDAK bisa mengandalkan LocationService.lastKnownLocation()
     * karena service itu hanya terisi selama tracking berjalan.
     *
     * Tidak memakai WebView navigator.geolocation sama sekali karena Chromium
     * (mesin WebView) menolak Geolocation API di origin non-HTTPS selain
     * localhost -- BASE_URL dev (http://IP-LAN:8000) selalu dianggap origin
     * tidak aman, jadi permintaan lokasi lewat browser API akan SELALU gagal
     * di WebView modern walau izin Android sudah diberikan.
     */
    @SuppressLint("MissingPermission") // permission divalidasi sebelum method ini dipanggil
    fun requestSingleLocation(onResult: (Location?) -> Unit) {
        val cancellationTokenSource = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location -> onResult(location) }
            .addOnFailureListener { onResult(null) }
    }
}
