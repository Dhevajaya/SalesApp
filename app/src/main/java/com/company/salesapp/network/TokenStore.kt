package com.company.salesapp.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Menyimpan auth token Laravel (mis. Sanctum Personal Access Token) yang dipakai
 * oleh Native Android untuk memanggil API secara independen dari WebView
 * (contoh: background location POST saat WebView sedang tidak aktif).
 *
 * ALUR TOKEN (perlu disepakati dengan backend Laravel):
 * 1. Sales login di dalam WebView (session/cookie Laravel seperti biasa).
 * 2. Laravel mengembalikan/menyediakan API token (mis. lewat endpoint khusus
 *    atau di-inject ke halaman setelah login berhasil).
 * 3. Halaman Laravel memanggil JS bridge: Android.setAuthToken("xxx")
 *    -> WebViewBridge menyimpannya lewat TokenStore ini.
 * 4. Semua request native (ApiClient) memakai token ini via AuthInterceptor.
 *
 * Jangan menyimpan token di SharedPreferences biasa (plaintext).
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "salesapp_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"

        @Volatile
        private var INSTANCE: TokenStore? = null

        fun getInstance(context: Context): TokenStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
