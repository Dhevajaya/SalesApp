package com.company.salesapp.utils

import android.content.Context

/**
 * Penyimpanan state ringan (SharedPreferences biasa) yang perlu bertahan
 * walau proses app dimatikan OS atau device di-restart.
 *
 * TIDAK berisi data sensitif — auth token tetap di TokenStore
 * (EncryptedSharedPreferences). Di sini hanya flag/timestamp operasional.
 */
class TrackingPrefs private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * true = user memang sedang dalam sesi tracking saat terakhir kali app hidup.
     * Dipakai BootReceiver untuk memutuskan apakah tracking perlu dilanjutkan
     * otomatis setelah device restart (Section 9).
     */
    var trackingActive: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, value).apply()

    /** tracking_session_id yang sedang berjalan (dari Laravel). null bila tidak ada. */
    var trackingSessionId: Long?
        get() = prefs.getLong(KEY_TRACKING_SESSION_ID, -1L).let { if (it == -1L) null else it }
        set(value) = prefs.edit().putLong(KEY_TRACKING_SESSION_ID, value ?: -1L).apply()

    /** Section 22: cooldown guard sisi client sebelum minta reroute lagi. */
    var lastRerouteAtMs: Long
        get() = prefs.getLong(KEY_LAST_REROUTE_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_REROUTE_AT, value).apply()

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_TRACKING_ACTIVE, false)
            .putLong(KEY_TRACKING_SESSION_ID, -1L)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "salesapp_tracking_prefs"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
        private const val KEY_TRACKING_SESSION_ID = "tracking_session_id"
        private const val KEY_LAST_REROUTE_AT = "last_reroute_at_ms"

        @Volatile
        private var INSTANCE: TrackingPrefs? = null

        fun getInstance(context: Context): TrackingPrefs {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackingPrefs(context).also { INSTANCE = it }
            }
        }
    }
}
