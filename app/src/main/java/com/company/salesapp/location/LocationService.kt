package com.company.salesapp.location

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.company.salesapp.MainActivity
import com.company.salesapp.R
import com.company.salesapp.SalesApp
import com.company.salesapp.config.AppConfig
import com.company.salesapp.database.AppDatabase
import com.company.salesapp.database.LocationEventEntity
import com.company.salesapp.navigation.RerouteManager
import com.company.salesapp.network.RouteRepository
import com.company.salesapp.sync.SyncManager
import com.company.salesapp.utils.TrackingPrefs
import kotlinx.coroutines.Dispatchers
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
 * - Mengecek deviasi rute -> minta reroute bila perlu (Section 22, lewat RerouteManager)
 *
 * TIDAK menaruh business logic Laravel di sini (Section 3 & 28).
 */
class LocationService : LifecycleService() {

    private lateinit var locationManager: AppLocationManager
    private lateinit var processor: LocationProcessor
    private lateinit var trackingPrefs: TrackingPrefs
    private lateinit var rerouteManager: RerouteManager

    override fun onCreate() {
        super.onCreate()
        locationManager = AppLocationManager(this)
        processor = LocationProcessor(
            maxAcceptableAccuracyMeters = AppConfig.MAX_ACCEPTABLE_ACCURACY_METERS,
            minMovementMeters = AppConfig.MIN_MOVEMENT_METERS,
            minIntervalMillis = AppConfig.MIN_INTERVAL_MS
        )
        trackingPrefs = TrackingPrefs.getInstance(this)
        rerouteManager = RerouteManager(
            routeRepository = RouteRepository(this),
            trackingPrefs = trackingPrefs
        )
        _lifecycleState.value = ServiceLifecycleState.STOPPED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getLongExtra(EXTRA_TRACKING_SESSION_ID, -1L)
                    .let { if (it == -1L) null else it }
                // Bila dipanggil BootReceiver tanpa extra, pakai session terakhir yang tersimpan.
                currentTrackingSessionId = sessionId ?: trackingPrefs.trackingSessionId
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

        try {
            startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_tracking_text)))
        } catch (e: Exception) {
            // Android 14+ bisa menolak startForeground bila izin lokasi dicabut tepat
            // sebelum service dimulai. Jangan crash — berhenti dengan bersih.
            _lifecycleState.value = ServiceLifecycleState.STOPPED
            stopSelf()
            return
        }

        // Simpan state supaya BootReceiver tahu tracking perlu dilanjutkan setelah reboot.
        trackingPrefs.trackingActive = true
        trackingPrefs.trackingSessionId = currentTrackingSessionId

        locationManager.startUpdates(AppConfig.LOCATION_UPDATE_INTERVAL_MS) { location ->
            lastKnownLocation = location

            val snapshot = processor.process(location)
            if (snapshot != null) {
                enqueueLocation(snapshot)
                checkRouteDeviation(location)
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
        lastKnownLocation = null
        trackingPrefs.clearSession()
        _lifecycleState.value = ServiceLifecycleState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enqueueLocation(snapshot: LocationSnapshot) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(applicationContext).locationEventDao()
            val entity = LocationEventEntity(
                locationEventId = UUID.randomUUID().toString(),
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
                accuracy = snapshot.accuracy,
                recordedAt = isoFormat(snapshot.recordedAtEpochMillis),
                trackingSessionId = currentTrackingSessionId
            )
            dao.insert(entity)
            // Minta sync segera bila online; SyncManager sendiri yang menentukan
            // apakah langsung kirim atau menunggu WorkManager periodik (Section 12).
            SyncManager.requestImmediateSync(applicationContext)
        }
    }

    /**
     * Section 22: cek deviasi rute. Sengaja dijalankan di IO dan sepenuhnya
     * "best effort" — kegagalannya tidak boleh mengganggu perekaman lokasi,
     * karena tracking jauh lebih penting daripada rute yang up-to-date.
     */
    private fun checkRouteDeviation(location: Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            rerouteManager.onNewLocation(location)
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

        /**
         * Lokasi mentah terakhir yang diterima (belum tentu lolos filter processor).
         * Dipakai WebViewBridge.getCurrentLocation() supaya halaman Laravel bisa
         * memvalidasi jarak saat check-in tanpa membuka GPS sendiri lewat browser.
         */
        @Volatile
        private var lastKnownLocation: Location? = null

        fun lastKnownLocation(): Location? = lastKnownLocation

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
