package com.company.salesapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Application entry point.
 * - Menyiapkan notification channel untuk foreground location service.
 * - AppDatabase (Room) diinisialisasi lazy melalui singleton di AppDatabase.getInstance().
 */
class SalesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                getString(R.string.notif_channel_tracking),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status tracking lokasi sales"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TRACKING_CHANNEL_ID = "tracking_channel"
    }
}
