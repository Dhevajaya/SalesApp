package com.company.salesapp.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.company.salesapp.BuildConfig

/**
 * Section 4 blueprint - WebView.
 * WebView hanya menjadi container UI Laravel; business logic tetap di Laravel.
 *
 * DEV MODE (Laragon lokal):
 *   BASE_URL default = http://10.0.2.2:8000/  (lihat app/build.gradle & network_security_config.xml)
 * PRODUKSI:
 *   Ganti BuildConfig.BASE_URL ke https://domain-perusahaan.com/ lalu build release.
 */
class WebViewManager(
    private val context: Context,
    private val webView: WebView
) {

    interface Callback {
        fun onPageStarted(url: String?)
        fun onPageFinished(url: String?)
        fun onProgressChanged(progress: Int)
        fun onLoadError(errorDescription: String?)
    }

    var callback: Callback? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(bridge: WebViewBridge) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
        }

        // Cookie login Laravel (session) tetap disimpan agar WebView tidak perlu login ulang
        // setiap kali dibuka.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(bridge, WebViewBridge.JS_INTERFACE_NAME)

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                callback?.onPageStarted(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                callback?.onPageFinished(url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // Hanya laporkan error untuk main frame, bukan untuk sub-resource
                // (mis. favicon/analytics gagal load tidak perlu menampilkan error page).
                if (request?.isForMainFrame == true) {
                    callback?.onLoadError(error?.description?.toString())
                }
            }

            // Batasi navigasi tetap di dalam domain Laravel yang dikonfigurasi;
            // tautan eksternal (WA, telepon, dsb.) tetap bisa ditangani terpisah bila diperlukan.
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false // biarkan WebView menangani semua navigasi http/https secara default
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                callback?.onProgressChanged(newProgress)
            }
        }
    }

    fun loadBaseUrl() {
        webView.loadUrl(BuildConfig.BASE_URL)
    }

    fun reload() {
        webView.reload()
    }

    /** @return true bila WebView menangani tombol back (masih ada history), false bila harus keluar app. */
    fun handleBackPressed(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }
}
