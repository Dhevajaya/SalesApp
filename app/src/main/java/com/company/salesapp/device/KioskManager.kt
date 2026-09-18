package com.company.salesapp.device

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Section 27 — Kiosk / Dedicated Device.
 *
 * ===================== BACA INI SEBELUM DIPAKAI =====================
 * Yang diimplementasikan di sini adalah **Screen Pinning** bawaan Android
 * (startLockTask), BUKAN kiosk Device Owner/MDM.
 *
 * Konsekuensinya jujur:
 * - User MASIH BISA keluar dari app dengan menahan tombol Back + Overview
 *   bersamaan (atau gesture setara di device tanpa tombol fisik). Itu perilaku
 *   sistem operasi, bukan bug di kode ini.
 * - Kiosk yang benar-benar terkunci (perlu PIN admin IT untuk keluar) mengharuskan
 *   device didaftarkan sebagai **Device Owner** lewat proses provisioning MDM
 *   saat device masih dalam kondisi factory reset. Itu proses enrollment per-device
 *   di luar cakupan kode APK biasa.
 *
 * Bila device SUDAH Device Owner, startLockTask() akan langsung mengunci tanpa
 * dialog konfirmasi dan tidak bisa dikeluarkan user — tanpa perlu ubah kode ini.
 * ====================================================================
 */
object KioskManager {

    private const val TAG = "KioskManager"

    /** @return true bila permintaan lock task berhasil dikirim ke sistem. */
    fun enable(activity: Activity): Boolean {
        return try {
            activity.startLockTask()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengaktifkan kiosk mode: ${e.message}")
            false
        }
    }

    fun disable(activity: Activity): Boolean {
        return try {
            activity.stopLockTask()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menonaktifkan kiosk mode: ${e.message}")
            false
        }
    }

    fun isActive(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}
