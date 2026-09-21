package com.company.salesapp.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.company.salesapp.device.KioskManager
import com.company.salesapp.location.AppLocationManager
import com.company.salesapp.MainActivity
import com.company.salesapp.location.LocationService
import com.company.salesapp.map.MapActivity
import com.company.salesapp.network.TokenStore
import com.company.salesapp.utils.PermissionUtils
import com.company.salesapp.utils.TrackingPrefs
import com.google.gson.Gson

/**
 * Section 5 blueprint - WebView <-> Native Bridge.
 *
 * Dipanggil dari JS halaman Laravel lewat `window.Android.xxx(...)`
 * (alias `window.SalesNative.xxx(...)` juga tersedia, lihat WebViewManager).
 *
 * Command yang didukung:
 *   setAuthToken(token)                    simpan token Sanctum ke TokenStore terenkripsi
 *   clearAuthToken()
 *   hasLocationPermission()   -> Boolean
 *   startTracking(sessionId)               mulai foreground GPS service
 *   stopTracking()
 *   getTrackingStatus()       -> String JSON
 *   getCurrentLocation()      -> String JSON  lokasi native terakhir dari tracking (untuk validasi check-in)
 *   requestCurrentLocation()                 ambil 1 titik GPS one-shot (async), hasil lewat window.onNativeLocationResult(json)
 *   openRouteMap()                         buka layar peta rute harian (MapLibre)
 *   openNavigation(lat,lng,name)           buka peta (turn-by-turn belum tersedia)
 *   enableKioskMode() / disableKioskMode() / isKioskModeActive()
 *   logout()
 *
 * PENTING (Section 5): bridge tidak boleh membuka kemampuan native berbahaya
 * secara bebas. Semua command di bawah ini sengaja dibatasi hanya pada operasi
 * yang sudah didefinisikan blueprint — tidak ada command generik "run native
 * code" / "exec" yang diekspos ke WebView.
 */
class WebViewBridge(
    private val context: Context,
    private val webView: WebView
) {

    private val gson = Gson()
    private val oneShotLocationManager by lazy { AppLocationManager(context) }

    // ---------------- Auth ----------------

    @JavascriptInterface
    fun setAuthToken(token: String) {
        // Dipanggil Laravel setelah login sukses, agar Native (background service)
        // bisa memanggil API secara independen dari WebView. Lihat TokenStore.kt.
        TokenStore.getInstance(context).saveToken(token)
    }

    @JavascriptInterface
    fun clearAuthToken() {
        TokenStore.getInstance(context).clear()
    }

    @JavascriptInterface
    fun logout() {
        LocationService.stop(context)
        TrackingPrefs.getInstance(context).clearSession()
        TokenStore.getInstance(context).clear()
        notifyWeb("STOPPED")
    }

    // ---------------- Tracking ----------------

    @JavascriptInterface
    fun hasLocationPermission(): Boolean {
        return PermissionUtils.hasFineLocationPermission(context)
    }

    @JavascriptInterface
    fun hasBackgroundLocationPermission(): Boolean {
        return PermissionUtils.hasBackgroundLocationPermission(context)
    }

    /**
     * Minta izin lokasi dari halaman Laravel. Ini penting bila user sebelumnya
     * menekan "Nanti" pada dialog izin saat startup. Hasil permission dikirim
     * kembali ke WebView lewat window.onNativeLocationPermissionResult().
     */
    @JavascriptInterface
    fun requestLocationPermission() {
        val activity = context as? MainActivity ?: return
        activity.requestLocationPermissionFlow()
    }

    @JavascriptInterface
    fun startTracking(trackingSessionId: Long) {
        // Untuk Foreground Location Service, izin foreground location (fine)
        // adalah prasyarat utama. ACCESS_BACKGROUND_LOCATION tidak boleh dijadikan
        // blocker untuk memulai service dari halaman Tracking saat app sedang
        // foreground. Pada Android 11+ permission background punya alur Settings
        // tersendiri dan memblokir start di sini membuat server sudah ACTIVE tetapi
        // native service tidak pernah berjalan.
        if (!PermissionUtils.hasFineLocationPermission(context)) {
            notifyWeb("PERMISSION_DENIED")
            return
        }

        LocationService.start(context, trackingSessionId)
        notifyWeb("ACTIVE")
    }

    @JavascriptInterface
    fun stopTracking() {
        LocationService.stop(context)
        notifyWeb("STOPPED")
    }

    @JavascriptInterface
    fun getTrackingStatus(): String {
        val state = LocationService.lifecycleState.value
        return gson.toJson(
            mapOf(
                "status" to state.name,
                "tracking_session_id" to TrackingPrefs.getInstance(context).trackingSessionId
            )
        )
    }

    /**
     * Lokasi native terakhir. Dipakai halaman Laravel untuk validasi jarak
     * saat Check In/Out, supaya tidak perlu memanggil navigator.geolocation
     * di WebView (yang akurasinya lebih buruk dan bisa dimanipulasi).
     *
     * `available: false` berarti service tracking belum pernah menerima fix GPS
     * sejak app dijalankan — halaman web harus menangani kondisi ini.
     */
    @JavascriptInterface
    fun getCurrentLocation(): String {
        val location = LocationService.lastKnownLocation()
            ?: return gson.toJson(mapOf("available" to false))

        return gson.toJson(
            mapOf(
                "available" to true,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to location.accuracy,
                "recorded_at_epoch_ms" to location.time
            )
        )
    }

    /**
     * Versi ASYNC/one-shot: dipakai halaman yang butuh GPS di luar sesi
     * tracking aktif (mis. form Tagging Toko), TIDAK bergantung pada
     * LocationService yang sedang berjalan. Hasil dikirim balik lewat
     * window.onNativeLocationResult(json) karena JavascriptInterface tidak
     * bisa langsung me-return nilai dari operasi async.
     *
     * Ini sengaja dipisah dari getCurrentLocation() (yang tetap dipertahankan
     * untuk validasi jarak check-in memakai posisi tracking yang sudah ada)
     * supaya tidak mengubah kontrak JS yang sudah dipakai fitur lain.
     */
    @JavascriptInterface
    fun requestCurrentLocation() {
        if (!hasLocationPermission()) {
            notifyLocationResult(gson.toJson(mapOf("available" to false, "reason" to "PERMISSION_DENIED")))
            return
        }

        oneShotLocationManager.requestSingleLocation { location ->
            val json = if (location != null) {
                gson.toJson(
                    mapOf(
                        "available" to true,
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "accuracy" to location.accuracy,
                        "recorded_at_epoch_ms" to location.time
                    )
                )
            } else {
                gson.toJson(mapOf("available" to false, "reason" to "NO_FIX"))
            }
            notifyLocationResult(json)
        }
    }

    // ---------------- Peta & navigasi ----------------

    /** Buka layar peta rute harian (Section 25). */
    @JavascriptInterface
    fun openRouteMap() {
        context.startActivity(
            Intent(context, MapActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /**
     * Section 21. Saat ini membuka layar peta yang sama — belum ada mode
     * turn-by-turn per-customer (butuh maneuver steps dari endpoint routing
     * Laravel yang belum tersedia). Parameter tetap diterima agar kontrak JS
     * di halaman Laravel tidak perlu diubah nanti.
     */
    @JavascriptInterface
    fun openNavigation(latitude: Double, longitude: Double, customerName: String?) {
        openRouteMap()
    }

    @JavascriptInterface
    fun stopNavigation() {
        // Tidak ada sesi navigasi persisten untuk dihentikan saat ini.
        // Sales cukup menutup layar peta.
    }

    // ---------------- Kiosk (Section 27) ----------------
    // Ini Screen Pinning, BUKAN kiosk Device Owner/MDM. Lihat KioskManager.kt.

    @JavascriptInterface
    fun enableKioskMode(): Boolean {
        val activity = context as? Activity ?: return false
        return KioskManager.enable(activity)
    }

    @JavascriptInterface
    fun disableKioskMode(): Boolean {
        val activity = context as? Activity ?: return false
        return KioskManager.disable(activity)
    }

    @JavascriptInterface
    fun isKioskModeActive(): Boolean = KioskManager.isActive(context)

    // ---------------- Internal ----------------

    /** Kirim event balik ke halaman Laravel lewat evaluateJavascript. */
    private fun notifyWeb(status: String) {
        webView.post {
            val js = "if (window.onNativeTrackingStatus) { window.onNativeTrackingStatus('$status'); }"
            webView.evaluateJavascript(js, null)
        }
    }

    /** Kirim hasil requestCurrentLocation() balik ke halaman Laravel. */
    private fun notifyLocationResult(json: String) {
        webView.post {
            val js = "if (window.onNativeLocationResult) { window.onNativeLocationResult($json); }"
            webView.evaluateJavascript(js, null)
        }
    }

    companion object {
        /** Nama object JS utama: window.Android.startTracking(...) dst. */
        const val JS_INTERFACE_NAME = "Android"

        /** Alias, supaya halaman yang sudah pakai window.SalesNative tetap jalan. */
        const val JS_INTERFACE_ALIAS = "SalesNative"
    }
}
