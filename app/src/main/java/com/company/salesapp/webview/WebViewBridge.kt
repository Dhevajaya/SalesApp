package com.company.salesapp.webview

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.company.salesapp.location.LocationService
import com.company.salesapp.network.TokenStore
import com.company.salesapp.utils.PermissionUtils
import com.google.gson.Gson

/**
 * Section 5 blueprint - WebView <-> Native Bridge.
 *
 * Command yang didukung (dipanggil dari JS Laravel via Android.xxx(...)):
 *   START_TRACKING(trackingSessionId)
 *   STOP_TRACKING()
 *   OPEN_NAVIGATION(lat, lng, customerName)   -> TODO diisi saat Phase F (Navigation)
 *   STOP_NAVIGATION()                          -> TODO diisi saat Phase F
 *   GET_TRACKING_STATUS() -> return String JSON
 *   GET_CURRENT_LOCATION() -> return String JSON (TODO: last known location cache)
 *   SET_AUTH_TOKEN(token) -> simpan token API untuk request native
 *
 * PENTING (Section 5): bridge tidak boleh membuka kemampuan native berbahaya
 * secara bebas. Semua command di bawah ini sengaja dibatasi hanya pada
 * operasi tracking/status yang sudah didefinisikan blueprint — tidak ada
 * command generik "run native code" / "exec" yang diekspos ke WebView.
 */
class WebViewBridge(
    private val context: Context,
    private val webView: WebView
) {

    private val gson = Gson()

    @JavascriptInterface
    fun startTracking(trackingSessionId: Long) {
        if (!hasLocationPermission()) {
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
        val status = mapOf("status" to state.name)
        return gson.toJson(status)
    }

    @JavascriptInterface
    fun getCurrentLocation(): String {
        // TODO (Phase B lanjutan): simpan last known LocationSnapshot di memory/DataStore
        // dan kembalikan di sini. Untuk sekarang kembalikan status kosong yang aman.
        return gson.toJson(mapOf("available" to false))
    }

    @JavascriptInterface
    fun openNavigation(latitude: Double, longitude: Double, customerName: String?) {
        // TODO (Phase F): buka MapActivity / NavigationManager.
        // Sengaja belum diaktifkan penuh agar prioritas Phase A-C selesai dulu (Section 39).
    }

    @JavascriptInterface
    fun stopNavigation() {
        // TODO (Phase F)
    }

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

    private fun hasLocationPermission(): Boolean {
        return PermissionUtils.hasFineLocationPermission(context)
    }

    /** Kirim event balik ke halaman Laravel lewat evaluateJavascript. */
    private fun notifyWeb(status: String) {
        webView.post {
            val js = "if (window.onNativeTrackingStatus) { window.onNativeTrackingStatus('$status'); }"
            webView.evaluateJavascript(js, null)
        }
    }

    companion object {
        /** Nama object JS: window.Android.startTracking(...) dst. */
        const val JS_INTERFACE_NAME = "Android"
    }
}
