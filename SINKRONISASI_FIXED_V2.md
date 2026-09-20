# Sinkronisasi dari `android_project_fixed_v2.zip`

Zip ini ternyata adalah project kamu sendiri yang sudah melewati **audit internal**
(`Live_Sales_Field_Operations_Audit_2026-09-19`), ditandai komentar `PERBAIKAN AUDIT`
di kode. Saya membandingkan **setiap file** satu per satu terhadap project di E: dan
hanya menerapkan bagian yang benar-benar berbeda. Tidak ada yang ditimpa membabi buta.

## 9 perbaikan yang diterapkan

1. **`config/AppConfig.kt`** — 4 perubahan nilai:
   - `MIN_INTERVAL_MS`: 7 detik → 45 detik. Sebelumnya Sales yang diam tetap upload tiap ~7 detik, lebih agresif dari baseline blueprint (30-60 detik).
   - `LOCATION_UPDATE_INTERVAL_MS`: 5 detik → 8 detik.
   - `REROUTE_DEVIATION_THRESHOLD_METERS`: 300m → 200m, supaya selaras dengan default Laravel (`config/routing.php`).
   - Tambah `SIGNAL_LOST_TIMEOUT_MS` (45 detik) dan `MANEUVER_ARRIVAL_RADIUS_METERS` (40m) untuk 2 fitur baru di bawah.

2. **`location/LocationService.kt` — watchdog SIGNAL_LOST.** Sebelumnya begitu `startUpdates()` dipanggil, status langsung diklaim `ACTIVE` walau belum ada satu fix GPS pun yang masuk. Sekarang status tetap `STARTING` sampai GPS pertama benar-benar diterima, dan ada watchdog timer: kalau 45 detik tidak ada callback GPS sama sekali, status otomatis pindah ke `PAUSED_SIGNAL_LOST` dan notifikasi berubah jadi "Sinyal GPS hilang…".

3. **`webview/WebViewBridge.kt` — `startTracking()` sekarang wajib cek izin background location dulu.** Sebelumnya hanya cek izin foreground; tracking bisa dinyatakan `ACTIVE` padahal nanti diam-diam berhenti begitu layar mati atau app di-background (Android 10+ mencabut akses lokasi tanpa izin background). Ditambah method baru `hasBackgroundLocationPermission()` yang bisa dipanggil dari JS.

4. **`database/AppDatabase.kt` — `fallbackToDestructiveMigration()` dihapus.** Ini yang paling penting: sebelumnya kalau versi database naik, SELURUH antrian lokasi offline Sales yang belum ter-sync akan **terhapus**. Sekarang pakai `Migration` resmi (`MIGRATION_1_2`) yang hanya menambah tabel `route_cache`, tidak menyentuh data yang sudah ada.

5. **`database/LocationEventEntity.kt` + `SyncWorker.kt` + `LocationEventDao.kt` — status `REJECTED` baru.** Sebelumnya semua kegagalan kirim lokasi (401 token invalid, 403 tidak berhak, 400/422 data tidak valid, ATAU 5xx server error) ditandai sama-sama `FAILED` dan di-retry terus oleh WorkManager — padahal 401/403/400/422 adalah kegagalan permanen yang pasti gagal lagi walau diulang. Sekarang dipisah: error permanen → `REJECTED` (tidak di-retry), error sementara (network/5xx) → tetap `FAILED` (di-retry). `purgeSynced()` sekarang juga membersihkan baris `REJECTED` lama, bukan cuma `SYNCED`.

6. **Turn-by-turn dasar** (`network/ApiModels.kt`, `navigation/NavigationManager.kt`, `map/MapActivity.kt`) — fitur baru. Kalau Laravel mengirim field `steps` di response rute (lihat kontrak baru di bawah), Android sekarang menampilkan instruksi arah yang berganti otomatis saat Sales mendekati titik belok (radius 40m), bukan cuma jarak garis lurus ke stop berikutnya. `NavigationManager` berubah dari `object` (statis) menjadi `class` per-sesi supaya progres instruksi tidak nyangkut antar kunjungan. **Batasan jujur:** tetap tanpa voice guidance dan tanpa snap-to-road — itu di luar cakupan tanpa provider navigation SDK penuh.

## Kontrak API yang berubah — perlu disesuaikan di Laravel

`GET /api/sales/routes/today` dan `POST /api/sales/routes/reroute` sekarang boleh
menyertakan field baru `steps` (opsional — kalau tidak dikirim, Android otomatis
fallback ke guidance garis lurus seperti sebelumnya, tidak error):

```json
{
  "success": true,
  "source": "provider",
  "stops": [...],
  "geometry": [...],
  "steps": [
    {
      "message": "Belok kiri ke Jl. Merdeka",
      "maneuver": "turn_left",
      "latitude": -7.281,
      "longitude": 112.741,
      "route_offset_meters": 850
    }
  ]
}
```

## Yang TIDAK saya ambil dari zip

- **Tidak ada.** Setelah dibandingkan file-per-file, semua bagian lain (MainActivity, WebViewManager, AppLocationManager, database RouteCache, ikon, layout, build config, dll.) sudah identik antara zip dan project kamu — kemungkinan besar itu perbaikan yang sudah kamu terapkan sendiri sebelumnya di sesi lain. `local.properties` sengaja tidak disentuh karena isinya path SDK khusus mesin kamu.

## Yang perlu kamu lakukan

1. **Build ulang:**
   ```
   gradlew testDebugUnitTest
   gradlew assembleDebug
   ```
2. **Uninstall APK lama dari HP sebelum install yang baru.** Karena skema database berubah dari "destructive migration" ke migration resmi, kombinasi APK lama yang masih terpasang + logic migration baru berpotensi konflik. Uninstall bersih lebih aman untuk saat ini (project masih development, belum ada data produksi yang perlu dijaga).
3. **Kalau Laravel sudah mengirim `steps`,** cukup jalankan seperti biasa — fitur turn-by-turn otomatis aktif. Kalau belum, tidak perlu diubah apa-apa, app tetap jalan seperti sebelumnya (fallback).
