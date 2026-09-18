# Penyesuaian dari `01-android-app-SalesApp.zip`

Dokumen ini mencatat apa yang diambil dari zip, apa yang **tidak** diambil, dan alasannya.

## Keputusan utama: fitur diporting, build system TIDAK diganti

Zip dan project ini sebenarnya dua project berbeda dengan package name yang sama:

| | Project kamu (ini) | Zip |
|---|---|---|
| Gradle DSL | Groovy (`build.gradle`) | Kotlin DSL (`build.gradle.kts`) + version catalog |
| AGP / Gradle | 8.5.2 / 8.7 | 9.3.2 |
| compileSdk | 34 | 37 |
| UI | AppCompat + XML layout + viewBinding | Jetpack Compose |
| Struktur | `database/`, `network/`, `webview/`, `utils/` | `data/local/`, `data/remote/`, `bridge/` |
| Auth token | `TokenStore` (EncryptedSharedPreferences) | `SessionManager` (SharedPreferences biasa) |
| BASE_URL | `BuildConfig.BASE_URL` (beda debug/release) | konstanta hardcoded di `AppConfig` |

Menimpa project ini dengan isi zip akan **merusak build kamu** (AGP 9.3.2 butuh
Android Studio & JDK yang lebih baru, dan semua kode Compose tidak akan kompilasi
di setup sekarang). Jadi yang dilakukan: **fitur dari zip diporting** ke arsitektur
project ini. Beberapa hal justru tetap memakai versi project kamu karena lebih baik —
terutama `TokenStore` terenkripsi (zip menyimpan token sebagai plaintext) dan
`BuildConfig.BASE_URL` (zip mengharuskan edit kode untuk ganti server).

## File BARU

| File | Fungsi | Asal di zip |
|---|---|---|
| `config/AppConfig.kt` | Threshold & interval terpusat (reroute, GPS, style peta) | `config/AppConfig.kt` |
| `utils/TrackingPrefs.kt` | Flag sesi tracking + timestamp reroute terakhir | `data/remote/SessionManager.kt` (sebagian) |
| `database/RouteCacheEntity.kt` | Tabel cache rute offline | `data/local/RouteCacheEntity.kt` |
| `database/RouteCacheDao.kt` | DAO cache rute | `data/local/RouteCacheDao.kt` |
| `network/RouteRepository.kt` | Ambil rute: network dulu, fallback cache | `data/remote/RouteRepository.kt` |
| `navigation/RouteDeviationChecker.kt` | Hitung jarak posisi ke polyline (+ cooldown) | `location/RouteDeviationChecker.kt` |
| `location/BootReceiver.kt` | Lanjutkan tracking setelah device reboot | `location/BootReceiver.kt` |
| `res/layout/activity_map.xml` | Layout layar peta | (zip pakai layout programatik) |
| `app/src/test/.../RouteDeviationCheckerTest.kt` | Unit test deviasi rute | test yang sama di zip |

## File yang DIUBAH

| File | Perubahan |
|---|---|
| `database/AppDatabase.kt` | versi 1 → 2, daftarkan `RouteCacheEntity` + `routeCacheDao()` |
| `network/ApiModels.kt` | tambah `RouteResponse`, `RouteStopDto`, `GeoPointDto`, `RerouteRequest` |
| `network/ApiService.kt` | tambah `GET /api/sales/routes/today`, `POST /api/sales/routes/reroute` |
| `map/MapActivity.kt` | dari stub TODO → peta MapLibre penuh (rute, stop, follow-me, guidance) |
| `map/RouteManager.kt` | dari stub TODO → menyiapkan data rute untuk digambar, termasuk fallback garis lurus |
| `navigation/RerouteManager.kt` | dari stub TODO → deteksi deviasi + panggil reroute dengan cooldown |
| `navigation/NavigationManager.kt` | dari stub TODO → stop berikutnya + format jarak/durasi |
| `device/KioskManager.kt` | dari stub TODO → screen pinning (`startLockTask`) |
| `location/LocationService.kt` | hook deviasi rute, simpan lokasi terakhir, tulis `TrackingPrefs`, pakai `AppConfig` |
| `webview/WebViewBridge.kt` | tambah `openRouteMap`, `getCurrentLocation` (terisi), kiosk, `hasLocationPermission`, `logout` |
| `webview/WebViewManager.kt` | bridge didaftarkan juga sebagai `window.SalesNative` (alias) |
| `AndroidManifest.xml` | daftarkan `BootReceiver`, judul `MapActivity` |
| `res/values/strings.xml` | string untuk layar peta |

## Yang SENGAJA tidak diambil dari zip

- **Compose + AGP 9 + compileSdk 37** — akan memecah build kamu.
- **`SessionManager`** — token disimpan plaintext. `TokenStore` kamu lebih aman.
- **`AppConfig.LARAVEL_BASE_URL` hardcoded** — `BuildConfig.BASE_URL` lebih baik karena
  debug/release bisa beda tanpa ubah kode.
- **Endpoint `api/sales/locations/batch`, `login`, `visits/check-in`** dari zip —
  kontrak API project kamu (Section 14–15) berbeda dan sudah dipakai `SyncWorker`.
  Kalau backend Laravel kamu justru memakai kontrak versi zip, beri tahu saya,
  nanti `ApiService` + `SyncWorker` saya sesuaikan.

## Cara pakai dari halaman Laravel (JS)

```javascript
const N = window.Android || window.SalesNative; // dua-duanya tersedia

// setelah login sukses:
N.setAuthToken(tokenSanctum);

// mulai / berhenti tracking:
N.startTracking(trackingSessionId);   // Number
N.stopTracking();
JSON.parse(N.getTrackingStatus());    // {status:"ACTIVE", tracking_session_id:123}

// lokasi native (akurasi GPS asli, untuk validasi jarak check-in):
const loc = JSON.parse(N.getCurrentLocation());
// { available:true, latitude, longitude, accuracy, recorded_at_epoch_ms }

// buka peta rute harian:
N.openRouteMap();

// kiosk (screen pinning):
N.enableKioskMode();  N.disableKioskMode();  N.isKioskModeActive();

// terima event balik dari native:
window.onNativeTrackingStatus = function (status) {
  // ACTIVE | STOPPED | PERMISSION_DENIED
};
```

## Yang HARUS disiapkan di Laravel sebelum peta bisa jalan

Dua endpoint baru, keduanya butuh `Authorization: Bearer <token>`:

```
GET  /api/sales/routes/today
POST /api/sales/routes/reroute    body: {latitude, longitude, tracking_session_id}
```

Bentuk response yang diharapkan Android (keduanya sama):

```json
{
  "success": true,
  "source": "provider",            // cache | provider | fallback
  "route_id": 12,
  "distance_meters": 18450,
  "duration_seconds": 2700,
  "stops": [
    {"customer_id": 1, "name": "Toko A", "latitude": -7.28, "longitude": 112.74,
     "sequence": 1, "status": "pending"}
  ],
  "geometry": [{"lat": -7.28, "lng": 112.74}, {"lat": -7.29, "lng": 112.75}]
}
```

`geometry` boleh kosong/null — Android akan menggambar garis lurus antar stop
berwarna **abu-abu** dan menulis "Mode fallback" di layar, supaya Sales tidak
mengira itu rute jalan sungguhan.

Threshold reroute di `AppConfig.REROUTE_DEVIATION_THRESHOLD_METERS` (300 m) dan
`REROUTE_COOLDOWN_MS` (5 menit) **harus diselaraskan** dengan `config/routing.php`
di Laravel. Nilai di Android hanya penyaring awal; cooldown yang sesungguhnya
tetap wajib dijaga server supaya quota TomTom tidak jebol.

## Batasan yang perlu kamu tahu (bukan bug)

1. **Bukan turn-by-turn navigation.** Belum ada instruksi belok atau voice guidance —
   itu butuh maneuver steps dari TomTom yang belum ada di endpoint Laravel.
   Yang ada: follow-me + jarak garis lurus ke stop berikutnya.
2. **Kiosk = Screen Pinning, bukan MDM.** User masih bisa keluar dengan menahan
   Back + Overview. Kiosk terkunci penuh butuh device didaftarkan sebagai Device Owner
   lewat provisioning MDM saat device masih factory reset.
3. **Auto-resume setelah reboot tidak dijamin 100%.** Xiaomi/Oppo/Vivo memblokir
   autostart kecuali diizinkan manual di pengaturan device.
4. **Tile peta OpenFreeMap** gratis tanpa API key, cocok untuk development.
   Untuk produksi, tinjau kapasitas/kebijakan provider (self-host, MapTiler, Stadia).
5. **DB naik ke versi 2 dengan `fallbackToDestructiveMigration()`** — antrian lokasi
   yang belum ter-sync di device yang sudah terpasang app versi lama akan terhapus
   saat update. Aman selama belum dipakai Sales di lapangan.

## Cara verifikasi

```
gradlew testDebugUnitTest      # unit test deviasi rute, tanpa device
gradlew assembleDebug          # pastikan semua kompilasi
```

Lalu di emulator: jalankan Laravel (`php artisan serve --host=0.0.0.0 --port=8000`),
buka app, izinkan lokasi, panggil `window.Android.openRouteMap()` dari halaman Laravel
(atau lewat Chrome DevTools remote debugging).
