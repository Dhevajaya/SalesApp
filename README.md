# Sales App (Android) — Hasil Integrasi Blueprint

Project Android Studio (Kotlin) ini dibangun mengikuti **Android_Sales_App_Development_Blueprint.md**
kamu, dengan prioritas urutan sesuai *Section 39 — Prioritas Saat Mulai Coding*:

```
[1] WebView          ✅ SELESAI (skeleton siap pakai)
[2] Bridge            ✅ SELESAI
[3] Permission        ✅ SELESAI
[4] Foreground GPS    ✅ SELESAI
[5] GPS → API         ✅ SELESAI
[6] Room Queue        ✅ SELESAI
[7] Offline Sync      ✅ SELESAI
[8] Background Test   ⚠️  perlu kamu jalankan manual di device (lihat Section 33)
[9] MapLibre          🟡 stub/TODO (map/MapActivity.kt)
[10] TomTom Route     🟡 stub/TODO (map/RouteManager.kt)
[11] Navigation       🟡 stub/TODO (navigation/)
[12] Visit            ⬜ belum — nanti nempel di WebView Laravel + bridge tambahan
[13] Device            🟡 stub/TODO (device/DeviceManager.kt)
[14] Kiosk             🟡 stub/TODO (device/KioskManager.kt)
```

Checkpoint blueprint **"jangan lanjut ke MapLibre/TomTom sebelum Native GPS + queue + sync
sudah benar"** sudah saya ikuti — nomor 1–7 dibuat penuh/berfungsi, 9 ke atas sengaja
distub dengan komentar TODO supaya kamu isi bertahap.

---

## 1. Apa saja yang sudah jadi

| Bagian | File | Fungsi |
|---|---|---|
| WebView container | `webview/WebViewManager.kt` | load URL Laravel, progress bar, error handling |
| Bridge JS↔Native | `webview/WebViewBridge.kt` | `START_TRACKING`, `STOP_TRACKING`, `GET_TRACKING_STATUS`, `GET_CURRENT_LOCATION`, `SET_AUTH_TOKEN` |
| Permission flow | `MainActivity.kt` | rationale dialog → request fine location → background location (Section 8) |
| Foreground GPS | `location/LocationService.kt` | lifecycle STOPPED→STARTING→ACTIVE→STOPPING (Section 10) |
| Filter GPS | `location/LocationProcessor.kt` | accuracy + minimum movement 30m / interval 7dtk (Section 11, bisa disetel) |
| Offline queue | `database/*` (Room) | simpan location event, idempotency via `location_event_id` UUID |
| Sync ke Laravel | `sync/SyncManager.kt`, `sync/SyncWorker.kt` | WorkManager, retry otomatis, TIDAK hapus data saat gagal |
| Kontrak API | `network/ApiService.kt`, `ApiModels.kt` | sesuai daftar endpoint Section 15 apa adanya |
| Auth token native | `network/TokenStore.kt` | EncryptedSharedPreferences, diisi dari WebView via `Android.setAuthToken(token)` |

---

## 2. Integrasi dengan Laragon (dev lokal)

### a. Kalau pakai Android EMULATOR
Tidak perlu ubah apa-apa. `10.0.2.2` adalah alamat khusus emulator untuk mengakses
`localhost` di PC kamu. Selama Laragon jalan di `http://localhost:8000` (atau port
default Laragon lainnya), project ini sudah otomatis mengarah ke situ lewat:

```
app/build.gradle  →  buildConfigField "String", "BASE_URL", "\"http://10.0.2.2:8000/\""
```

Kalau port Laravel kamu bukan 8000, ganti angkanya di baris itu.

### b. Kalau pakai HP FISIK (via WiFi yang sama dengan PC)
1. Cek IP lokal PC kamu: buka CMD → `ipconfig` → lihat "IPv4 Address", misal `192.168.1.100`.
2. Ganti `BASE_URL` di `app/build.gradle`:
   ```
   buildConfigField "String", "BASE_URL", "\"http://192.168.1.100:8000/\""
   ```
3. Tambahkan IP yang sama di `app/src/main/res/xml/network_security_config.xml`
   (sudah ada contoh `192.168.1.100`, sesuaikan dengan IP kamu).
4. Pastikan Laravel di Laragon diakses lewat `php artisan serve --host=0.0.0.0 --port=8000`
   (bukan cuma `php artisan serve` biasa), supaya Laragon menerima koneksi dari luar
   `localhost`. Atau gunakan Laragon virtual host + tambahkan hostname itu ke sini.
5. Matikan Windows Firewall untuk port itu (atau buat exception) kalau koneksi masih gagal.

### c. Nanti pindah ke server produksi
Cukup ganti isi `BASE_URL` menjadi domain HTTPS asli:
```
buildConfigField "String", "BASE_URL", "\"https://domain-perusahaan.com/\""
```
Lalu **kosongkan/hapus** domain-domain dev di `network_security_config.xml` supaya HTTP
cleartext tidak diizinkan lagi di build release (sudah ada penanda `usesCleartextTraffic`
otomatis `false` di build type `release`).

---

## 3. Endpoint Laravel yang WAJIB kamu siapkan

Android sudah siap memanggil endpoint-endpoint ini persis sesuai Section 14–15 blueprint.
**Kalau route ini belum ada di Laravel kamu, buat dulu di sisi Laravel** (Android tidak
bisa jalan tanpa ini):

```
GET  /api/sales/tasks/current
GET  /api/sales/tasks/{id}/documents
GET  /api/sales/tasks/{id}/stock
POST /api/sales/tasks/{id}/verify-stock
POST /api/sales/tasks/{id}/start-work
POST /api/sales/tracking/start
POST /api/sales/tracking/stop
POST /api/sales/location
GET  /api/sales/tracking/status
```

Request/response `POST /api/sales/location` harus persis seperti ini (lihat `ApiModels.kt`):
```json
// Request
{
  "location_event_id": "uuid",
  "latitude": -7.982000,
  "longitude": 110.616000,
  "accuracy": 12,
  "recorded_at": "2026-09-14T14:20:00+07:00",
  "tracking_session_id": 123
}
// Response
{ "success": true, "server_received_at": "2026-09-14T14:20:01+07:00" }
```

Identity Sales **wajib** ditentukan Laravel dari token/session (Bearer token yang
dikirim `Authorization: Bearer <token>` oleh `ApiClient.kt`), **bukan** dari field
`sales_id` di body request (Section 28 — Security, sudah diikuti: tidak ada field
`sales_id` di request body).

### Auth token dari WebView ke Native
Setelah Sales berhasil login di halaman Laravel (di dalam WebView), halaman itu perlu
memanggil JS berikut agar Native (foreground service) bisa kirim lokasi walau WebView
tidak sedang aktif dibuka:
```js
// dipanggil dari halaman Laravel setelah login sukses
if (window.Android) {
  window.Android.setAuthToken("TOKEN_SANCTUM_ATAU_JWT_DARI_LARAVEL");
}
```
Kamu perlu menyediakan token ini di sisi Laravel (mis. Sanctum personal access token)
dan meng-inject/mengirimkannya ke halaman setelah login.

### Memicu tracking dari halaman Laravel
```js
window.Android.startTracking(trackingSessionId); // Number
window.Android.stopTracking();
window.Android.getTrackingStatus(); // -> string JSON: {"status":"ACTIVE"}
```

---

## 4. Cara membuka & menjalankan

1. Buka **Android Studio** → *Open* → pilih folder `SalesApp/` ini.
2. Tunggu Gradle Sync (Android Studio otomatis melengkapi Gradle wrapper jar bila belum ada;
   kalau diminta, klik "OK"/"Sync Now").
3. Jalankan Laragon, pastikan Laravel bisa diakses dari browser PC di `http://localhost:8000`.
4. Jalankan project ke **Emulator** dulu (paling gampang, tidak perlu setting IP).
5. APK akan membuka WebView yang memuat `http://10.0.2.2:8000/`.
6. Kalau muncul banner merah "Gagal memuat halaman", cek: Laragon aktif? Port benar?
   Firewall memblokir?

---

## 5. Lanjutan yang perlu kamu isi sendiri (sesuai urutan blueprint)

- **Phase D (MapLibre)** — `map/MapActivity.kt`, `map/RouteManager.kt`: butuh API key/style
  OpenStreetMap-based sesuai provider yang kamu pakai.
- **Phase E (TomTom Orbis v3)** — proxy routing **harus** lewat Laravel (Android tidak boleh
  panggil TomTom langsung, Section 19 & 36). Buat dulu endpoint routing di Laravel, baru
  isi `RouteManager.kt`.
- **Phase F (Navigation)** — `navigation/NavigationManager.kt`, `navigation/RerouteManager.kt`.
- **Phase G (Visit)** — Check In/Out sebaiknya tetap di halaman Laravel (WebView) yang
  memanggil `GET_CURRENT_LOCATION` / lokasi native lewat bridge untuk validasi jarak.
- **Phase H (Device & Kiosk)** — `device/DeviceManager.kt`, `device/KioskManager.kt`,
  diimplementasikan setelah fitur utama stabil sesuai catatan blueprint.

Setiap file stub sudah saya beri komentar `TODO` yang merujuk langsung ke nomor section
blueprint terkait, supaya kamu tinggal ikuti urutannya.

---

## 6. Testing wajib sebelum lanjut ke fase berikutnya (Section 33)

Sebelum masuk ke MapLibre/TomTom, uji dulu:
- GPS ON/OFF, permission granted/denied/revoked, akurasi buruk
- Screen ON/OFF, lock screen, WebView di background, app di background lalu dibuka lagi
- WiFi ON, Mobile Data ON, Internet OFF lalu kembali (cek antrian Room ikut terkirim)
- Battery saver aktif, restart device, restart app

Semua ini menguji `LocationService` + `SyncWorker` yang sudah dibangun di project ini.
