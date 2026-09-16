package com.company.salesapp.map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * TODO Phase D (section 32) — jangan dikerjakan sebelum Phase A-C stabil (section 39 checkpoint).
 *
 * Rencana implementasi:
 * 1. Tambahkan MapView (MapLibre Android) di layout activity_map.xml.
 * 2. MapManager.kt -> load style OpenStreetMap-based, render marker Sales (posisi live)
 *    dan marker Customer (dari Laravel API), update posisi tanpa reload total (section 25).
 * 3. RouteManager.kt -> panggil endpoint routing Laravel (proxy ke TomTom Orbis v3),
 *    render polyline + ETA/distance, JANGAN panggil TomTom setiap GPS update (section 19 & 36).
 * 4. NavigationManager.kt (di package navigation/) -> mode navigasi in-app, next maneuver,
 *    deviation detection -> RerouteManager.kt untuk re-route saat deviation signifikan (section 22).
 *
 * Activity ini dibuka dari WebViewBridge.openNavigation() setelah Phase D-F siap.
 */
class MapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: setContentView(R.layout.activity_map) setelah layout dibuat
    }
}
