package com.company.salesapp.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.company.salesapp.network.TokenStore
import com.company.salesapp.sync.SyncManager
import com.company.salesapp.utils.PermissionUtils
import com.company.salesapp.utils.TrackingPrefs

/**
 * Section 9 — auto-resume setelah device restart.
 *
 * Tanpa receiver ini, kalau HP Sales mati (baterai habis / restart manual) di tengah
 * sesi kerja, tracking TIDAK akan jalan lagi sampai Sales membuka app secara manual.
 *
 * Syarat yang dicek sebelum melanjutkan tracking (semua harus terpenuhi):
 *   1. Sesi tracking memang sedang aktif sebelum device mati (TrackingPrefs).
 *   2. Auth token masih tersimpan (Sales belum logout).
 *   3. Izin lokasi masih diberikan (bisa saja dicabut user via Settings).
 *
 * CATATAN JUJUR: ini tetap tidak menjamin tracking 24/7. Sebagian vendor
 * (Xiaomi, Oppo, Vivo, dsb) memblokir autostart aplikasi setelah boot kecuali
 * diizinkan manual di pengaturan device — itu batasan OEM, bukan bug kode.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val prefs = TrackingPrefs.getInstance(context)
        if (!prefs.trackingActive) {
            Log.d(TAG, "Tidak ada sesi tracking aktif sebelum reboot — tidak melanjutkan.")
            return
        }

        val token = TokenStore.getInstance(context).getToken()
        if (token.isNullOrBlank()) {
            Log.d(TAG, "Tidak ada auth token (sudah logout) — tidak melanjutkan tracking.")
            prefs.clearSession()
            return
        }

        if (!PermissionUtils.hasFineLocationPermission(context)) {
            Log.w(TAG, "Izin lokasi sudah dicabut — tidak bisa melanjutkan tracking otomatis.")
            return
        }

        Log.d(TAG, "Melanjutkan tracking setelah reboot, session=${prefs.trackingSessionId}")
        LocationService.start(context, prefs.trackingSessionId)
        SyncManager.schedulePeriodicSync(context.applicationContext)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
