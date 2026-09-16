package com.company.salesapp.location

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.company.salesapp.MainActivity
import com.company.salesapp.R
import com.company.salesapp.SalesApp
import com.company.salesapp.database.AppDatabase
import com.company.salesapp.database.LocationEventEntity
import com.company.salesapp.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Foreground Location Service (Section 10 blueprint).
 * Lifecycle: STOPPED -> STARTING -> ACTIVE -> PAUSED/SIGNAL_LOST -> ACTIVE -> STOPPING -> STOPPED
 *
 * Tanggung jawab:
 * - Membaca GPS fisik lewat AppLocationManager
 * - Memfilter update lewat LocationProcessor (accuracy & minimum movement)
 * - Menyimpan hasil ke Room Local Queue (offline-safe, Section 12)
 * - Memicu SyncManager untuk mengirim ke Laravel API
 *
 * TIDAK menaruh business logic Laravel di sini (Section 3 & 28).
 */
class LocationService : LifecycleService() {

    private lateinit var locationManager: AppLocationManager
    private lateinit var processor: LocationProcessor
    private lateinit var trackingSessionIdProvider: () -> Long?

    override fun onCreate() {
        super.onCreate()
        locationManager = AppLocationManager(this)
        processor = LocationProcessor()
        trackingSessionIdProvider = { currentTrackingSessionId }
        _lifecycleState.value = ServiceLifecycleState.STOPPED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                currentTrackingSessionId = intent.getLongExtra(EXTRA_TRACKING_SESSION_ID, -1L)
                    .let { if (it == -1L) null else it }
                startTracking()
            }
            ACTION_STOP -> stopTracking()
        }
        // START_STICKY: sistem akan mencoba restart service bila dimatikan paksa oleh OS,
        // namun ini tidak menjamin tracking 24 jam absolut (lihat catatan section 9).
        return START_STICKY
    }

    private fun startTracking() {
        _lifecycleState.value = ServiceLifecycleState.STARTING
        processor.reset()

        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_tracking_text)))

        locationManager.startUpdates { location ->
            val snapshot = processor.process(location)
            if (snapshot != null) {
                enqueueLocation(snapshot)
                _lifecycleState.value = ServiceLifecycleState.ACTIVE
            }
            // Bila snapshot null (difilter accuracy/jarak), lifecycle tetap ACTIVE
            // selama GPS masih memberi update; SIGNAL_LOST ditangani terpisah
            // lewat timeout listener (TODO: tambahkan watchdog timer bila diperlukan).
        }

        _lifecycleState.value = ServiceLifecycleState.ACTIVE
    }

    private fun stopTracking() {
        _lifecycleState.value = ServiceLifecycleState.STOPPING
        locationManager.stopUpdates()
        currentTrackingSessionId = null
        _lifecycleState.value = ServiceLifecycleState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enqueueLocation(snapshot: LocationSnapshot) {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).locationEventDao()
            val entity = LocationEventEntity(
                locationEventId = UUID.randomUUID().toString(),
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
                accuracy = snapshot.accuracy,
                recordedAt = isoFormat(snapshot.recordedAtEpochMillis),
                trackingSessionId = trackingSessionIdProvider()
            )
            dao.insert(entity)
            // Minta sync segera bila online; SyncManager sendiri yang menentukan
            // apakah langsung kirim atau menunggu WorkManager periodik (Section 12).
            SyncManager.requestImmediateSync(applicationContext)
        }
    }

    private fun isoFormat(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epochMillis))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SalesApp.TRACKING_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_tracking_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        locationManager.stopUpdates()
        _lifecycleState.value = ServiceLifecycleState.STOPPED
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.company.salesapp.action.START_TRACKING"
        const val ACTION_STOP = "com.company.salesapp.action.STOP_TRACKING"
        const val EXTRA_TRACKING_SESSION_ID = "extra_tracking_session_id"

        @Volatile
        private var currentTrackingSessionId: Long? = null

        private val _lifecycleState = MutableStateFlow(ServiceLifecycleState.STOPPED)
        val lifecycleState: StateFlow<ServiceLifecycleState> = _lifecycleState.asStateFlow()

        fun start(context: Context, trackingSessionId: Long?) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_START
                trackingSessionId?.let { putExtra(EXTRA_TRACKING_SESSION_ID, it) }
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun isActive(): Boolean = lifecycleState.value == ServiceLifecycleState.ACTIVE
    }
}
