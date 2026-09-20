package com.company.salesapp.config

/**
 * Konfigurasi terpusat untuk nilai-nilai yang perlu disetel manual
 * (threshold, interval, style peta).
 *
 * CATATAN: BASE_URL Laravel TIDAK ditaruh di sini — tetap di
 * `app/build.gradle` sebagai `buildConfigField "String", "BASE_URL"` supaya
 * bisa beda antara build debug dan release. Lihat BuildConfig.BASE_URL.
 */
object AppConfig {

    // ---- Location Processor (Section 11) ----
    // PERBAIKAN AUDIT #10: sebelumnya MIN_INTERVAL_MS=7s membuat Sales yang
    // diam tetap upload tiap ~7 detik, lebih agresif dari baseline blueprint
    // (heartbeat 30-60s). GPS acquisition sendiri tetap sering (5-10s) supaya
    // fix cepat didapat, tapi heartbeat upload minimum dinaikkan ke 45s.
    const val LOCATION_UPDATE_INTERVAL_MS = 8_000L
    const val MAX_ACCEPTABLE_ACCURACY_METERS = 50f
    const val MIN_MOVEMENT_METERS = 30f
    const val MIN_INTERVAL_MS = 45_000L

    // PERBAIKAN AUDIT #9: watchdog SIGNAL_LOST - kalau tidak ada satu pun
    // callback GPS mentah (bahkan yang difilter processor) dalam rentang ini,
    // service dianggap kehilangan sinyal. ~5x LOCATION_UPDATE_INTERVAL_MS
    // supaya toleran terhadap GPS yang sesekali telat, tapi tetap responsif.
    const val SIGNAL_LOST_TIMEOUT_MS = 45_000L

    // ---- Sync Manager (Section 12) ----
    const val SYNC_BATCH_SIZE = 50
    const val SYNC_INTERVAL_MINUTES = 15L

    // ---- Re-route / GPS Deviation Detector (Section 22) ----
    // HARUS selaras dengan config/routing.php di Laravel
    // (reroute_deviation_threshold_meters & reroute_cooldown_seconds).
    // PERBAIKAN AUDIT #20: diselaraskan ke 200m (batas atas rentang contoh
    // blueprint 100-200m "kandidat reroute"), dari sebelumnya 300m yang
    // tidak sinkron dengan default Laravel.
    const val REROUTE_DEVIATION_THRESHOLD_METERS = 200.0
    const val REROUTE_COOLDOWN_MS = 5 * 60 * 1000L // 5 menit

    // Seberapa sering geometry rute dibaca ulang dari cache Room oleh service
    // (bukan dari network). Mencegah query DB di tiap tick GPS.
    const val ROUTE_GEOMETRY_CACHE_TTL_MS = 5 * 60 * 1000L

    // PERBAIKAN AUDIT #16 (Turn-by-turn): radius dianggap "sampai" di titik
    // maneuver/stop berikutnya, dipakai NavigationManager untuk memajukan step.
    const val MANEUVER_ARRIVAL_RADIUS_METERS = 40.0

    // ---- MapLibre (Section 25) ----
    // OpenFreeMap: vector tile gratis, tanpa API key, cocok untuk development.
    // UNTUK PRODUKSI: tinjau ulang kebijakan & kapasitas provider tile yang dipakai
    // (self-host, MapTiler, Stadia Maps, dsb). Jangan asumsikan tile publik
    // siap menanggung traffic produksi tanpa dicek dulu.
    const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    const val MAP_FOLLOW_UPDATE_INTERVAL_MS = 5_000L
}
