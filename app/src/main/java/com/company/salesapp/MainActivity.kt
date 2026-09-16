package com.company.salesapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import com.company.salesapp.sync.SyncManager
import com.company.salesapp.utils.NetworkUtils
import com.company.salesapp.utils.PermissionUtils
import com.company.salesapp.webview.WebViewBridge
import com.company.salesapp.webview.WebViewManager

/**
 * Section 4 & 8 blueprint.
 * MainActivity hanya bertugas sebagai container WebView + orkestrasi permission lokasi.
 * Business logic (login, dashboard, task, dsb.) sepenuhnya berjalan di halaman Laravel
 * yang dimuat WebView.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineBanner: LinearLayout
    private lateinit var webViewManager: WebViewManager

    private val foregroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        if (granted) {
            requestBackgroundLocationIfNeeded()
        } else {
            Toast.makeText(this, "Izin lokasi ditolak. Tracking tidak akan berjalan.", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Hasil background permission tidak menghentikan alur; foreground permission
        // saja sudah cukup untuk tracking saat app di foreground (Section 8).
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op, notifikasi hanya untuk tampilan status tracking */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        offlineBanner = findViewById(R.id.offlineBanner)

        webViewManager = WebViewManager(this, webView)
        val bridge = WebViewBridge(this, webView)

        webViewManager.callback = object : WebViewManager.Callback {
            override fun onPageStarted(url: String?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(url: String?) {
                progressBar.visibility = View.GONE
            }

            override fun onProgressChanged(progress: Int) {
                progressBar.progress = progress
            }

            override fun onLoadError(errorDescription: String?) {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    "Gagal memuat halaman: pastikan Laragon aktif dan URL benar ($errorDescription)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        webViewManager.setup(bridge)
        webViewManager.loadBaseUrl()

        updateOfflineBanner()
        requestNotificationPermissionIfNeeded()

        SyncManager.schedulePeriodicSync(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        updateOfflineBanner()
    }

    private fun updateOfflineBanner() {
        offlineBanner.visibility = if (NetworkUtils.isOnline(this)) View.GONE else View.VISIBLE
    }

    /**
     * Dipanggil dari halaman "Tracking" Laravel via bridge, ATAU bisa dipanggil di sini
     * langsung sebelum start tracking pertama kali. Mengikuti flow Section 8:
     * Login -> Open Tracking -> Explain -> Request Permission -> granted?
     */
    fun requestLocationPermissionFlow() {
        if (PermissionUtils.hasFineLocationPermission(this)) {
            requestBackgroundLocationIfNeeded()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_rationale_title))
            .setMessage(getString(R.string.permission_rationale_message))
            .setPositiveButton("Lanjutkan") { _, _ ->
                foregroundLocationLauncher.launch(PermissionUtils.foregroundLocationPermissions())
            }
            .setNegativeButton("Nanti") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !PermissionUtils.hasBackgroundLocationPermission(this)
        ) {
            backgroundLocationLauncher.launch(PermissionUtils.backgroundLocationPermission())
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.hasNotificationPermission(this)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onBackPressed() {
        if (!webViewManager.handleBackPressed()) {
            super.onBackPressed()
        }
    }
}
