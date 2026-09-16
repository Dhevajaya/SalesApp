package com.company.salesapp.map

/**
 * TODO Phase E (section 32) — Routing.
 *
 * Arsitektur wajib (section 19 & 36):
 *   Android -> Laravel Route Endpoint (proxy) -> TomTom Orbis v3 -> Normalized Route -> Android -> MapLibre
 *
 * Larangan (section 35 & 36):
 * - JANGAN memanggil TomTom langsung dari Android.
 * - JANGAN memanggil TomTom setiap GPS update; gunakan route cache.
 * - JANGAN menggunakan Google Maps App eksternal / OSRM self-hosted.
 *
 * Output minimal yang perlu ditangani dari response Laravel:
 *   distance, duration/ETA, polyline, route steps, destination
 */
class RouteManager {
    // TODO: implementasikan pemanggilan ApiService.getRoute(...) (tambahkan endpoint di ApiService)
    // dan konversi polyline hasil Laravel ke format yang bisa digambar MapLibre.
}
