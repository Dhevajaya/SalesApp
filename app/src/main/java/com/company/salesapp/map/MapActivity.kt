package com.company.salesapp.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.company.salesapp.R
import com.company.salesapp.config.AppConfig
import com.company.salesapp.navigation.GeoPoint
import com.company.salesapp.navigation.NavigationManager
import com.company.salesapp.network.RouteStopDto
import com.company.salesapp.utils.PermissionUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Section 25 — Map & Route rendering (Phase D/F).
 *
 * MapLibre TIDAK menghitung rute. Ia hanya MENGGAMBAR hasil yang sudah dihitung
 * TomTom di sisi Laravel (atau garis lurus antar stop sebagai fallback, digambar
 * abu-abu supaya Sales tidak mengira itu rute jalan sungguhan).
 *
 * "Guidance" di layar ini disederhanakan: kamera mengikuti posisi Sales (follow-me)
 * + instruksi arah turn-by-turn dasar (teks + jarak ke maneuver berikutnya)
 * bila server mengirim `steps`; fallback ke garis lurus ke stop berikutnya
 * bila tidak. Lihat batasan jujur di NavigationManager.kt (tanpa voice
 * guidance, tanpa map-matching/snap-to-road).
 *
 * Dibuka dari WebView lewat: window.Android.openRouteMap()
 */
class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var progressBar: ProgressBar
    private lateinit var followMeButton: Button
    private lateinit var guidanceText: TextView
    private lateinit var routeSummaryText: TextView
    private lateinit var statusText: TextView

    private lateinit var routeManager: RouteManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var maplibreMap: MapLibreMap? = null
    private var currentStops: List<RouteStopDto> = emptyList()
    private var followMeEnabled = false
    private var isRequestingUpdates = false

    // PERBAIKAN AUDIT #16/#34: instance turn-by-turn untuk sesi navigasi ini,
    // null kalau Laravel tidak mengirim `steps` (mis. mode fallback tanpa TomTom) -
    // di kondisi itu tetap fallback ke guidance garis lurus seperti sebelumnya.
    private var navigationManager: NavigationManager? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onMyLocationUpdated(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // WAJIB dipanggil sebelum MapView di-inflate, kalau tidak MapLibre crash.
        MapLibre.getInstance(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        progressBar = findViewById(R.id.mapProgress)
        followMeButton = findViewById(R.id.followMeButton)
        guidanceText = findViewById(R.id.guidanceText)
        routeSummaryText = findViewById(R.id.routeSummaryText)
        statusText = findViewById(R.id.statusText)

        routeManager = RouteManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        followMeButton.setOnClickListener { toggleFollowMe() }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            maplibreMap = map
            map.setStyle(Style.Builder().fromUri(AppConfig.MAP_STYLE_URL)) {
                loadRoute()
            }
        }
    }

    private fun loadRoute() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val renderable = routeManager.loadTodayRoute()
            progressBar.visibility = View.GONE

            if (renderable == null) {
                showStatus(getString(R.string.map_route_unavailable))
                return@launch
            }

            val stops = renderable.route.stops.orEmpty()
            if (stops.isEmpty()) {
                showStatus(getString(R.string.map_no_assignment))
                return@launch
            }

            currentStops = stops
            val steps = renderable.route.steps.orEmpty()
            navigationManager = if (steps.isNotEmpty()) NavigationManager(steps) else null
            renderRoute(renderable)
            showStatus(routeManager.sourceLabel(renderable))
            showRouteSummary(renderable)
            updateGuidance(null)
            showMyLocationOnceIfPermitted()
        }
    }

    private fun renderRoute(renderable: RouteManager.RenderableRoute) {
        val style = maplibreMap?.style ?: return
        val stops = renderable.route.stops.orEmpty()

        // --- Polyline rute ---
        val linePoints = renderable.line.map { Point.fromLngLat(it.lng, it.lat) }
        if (linePoints.size >= 2) {
            style.addSource(
                GeoJsonSource(
                    SOURCE_ROUTE_LINE,
                    Feature.fromGeometry(LineString.fromLngLats(linePoints))
                )
            )
            style.addLayer(
                LineLayer(LAYER_ROUTE_LINE, SOURCE_ROUTE_LINE).withProperties(
                    // Abu-abu = fallback garis lurus, biru = geometry jalan asli.
                    lineColor(
                        if (renderable.isStraightLineFallback) Color.GRAY
                        else Color.parseColor("#1565C0")
                    ),
                    lineWidth(4f),
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
        }

        // --- Marker stop customer ---
        val stopFeatures = stops.map { stop ->
            Feature.fromGeometry(Point.fromLngLat(stop.longitude, stop.latitude)).apply {
                addNumberProperty("sequence", stop.sequence)
                addStringProperty("name", stop.name)
                addStringProperty("status", stop.status ?: "pending")
            }
        }
        style.addSource(GeoJsonSource(SOURCE_STOPS, FeatureCollection.fromFeatures(stopFeatures)))
        style.addLayer(
            CircleLayer(LAYER_STOPS, SOURCE_STOPS).withProperties(
                circleRadius(9f),
                circleColor(Color.parseColor("#E53935")),
                circleStrokeColor(Color.WHITE),
                circleStrokeWidth(2f)
            )
        )

        // --- Kamera awal: rangkum semua stop ---
        val boundsBuilder = LatLngBounds.Builder()
        stops.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
        try {
            maplibreMap?.moveCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), CAMERA_PADDING_PX)
            )
        } catch (e: Exception) {
            // Bisa gagal bila semua stop berada di titik yang sama persis.
            stops.firstOrNull()?.let {
                maplibreMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 14.0)
                )
            }
        }
    }

    private fun showRouteSummary(renderable: RouteManager.RenderableRoute) {
        val distance = renderable.route.distanceMeters
        val duration = renderable.route.durationSeconds
        if (distance == null && duration == null) return

        val parts = mutableListOf<String>()
        distance?.let { parts.add(NavigationManager.formatDistance(it.toDouble())) }
        duration?.let { parts.add(NavigationManager.formatDuration(it)) }
        parts.add("${renderable.route.stops.orEmpty().size} stop")

        routeSummaryText.text = parts.joinToString(" • ")
        routeSummaryText.visibility = View.VISIBLE
    }

    // ---------------- Follow me ----------------

    private fun toggleFollowMe() {
        followMeEnabled = !followMeEnabled
        followMeButton.setText(
            if (followMeEnabled) R.string.map_follow_on else R.string.map_follow_off
        )
        if (followMeEnabled) startFollowingLocation() else stopFollowingLocation()
    }

    @SuppressLint("MissingPermission") // permission dicek tepat di baris di bawah
    private fun startFollowingLocation() {
        if (!PermissionUtils.hasFineLocationPermission(this)) {
            showStatus(getString(R.string.map_permission_missing))
            followMeEnabled = false
            followMeButton.setText(R.string.map_follow_off)
            return
        }
        if (isRequestingUpdates) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            AppConfig.MAP_FOLLOW_UPDATE_INTERVAL_MS
        ).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper()
            )
            isRequestingUpdates = true
        } catch (e: SecurityException) {
            showStatus(getString(R.string.map_permission_missing))
        }
    }

    private fun stopFollowingLocation() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isRequestingUpdates = false
    }

    private fun onMyLocationUpdated(location: Location) {
        updateMyLocationMarker(location)
        updateGuidance(location)

        if (followMeEnabled) {
            maplibreMap?.easeCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(maplibreMap?.cameraPosition?.zoom?.coerceAtLeast(15.0) ?: 16.0)
                        .bearing(if (location.hasBearing()) location.bearing.toDouble() else 0.0)
                        .build()
                ),
                CAMERA_EASE_MS
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun showMyLocationOnceIfPermitted() {
        if (!PermissionUtils.hasFineLocationPermission(this)) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateMyLocationMarker(it)
                    updateGuidance(it)
                }
            }
        } catch (e: SecurityException) {
            // Bukan error fatal untuk layar peta — rute tetap tampil.
        }
    }

    private fun updateMyLocationMarker(location: Location) {
        val style = maplibreMap?.style ?: return
        val feature = Feature.fromGeometry(
            Point.fromLngLat(location.longitude, location.latitude)
        )

        val existing = style.getSourceAs<GeoJsonSource>(SOURCE_ME)
        if (existing == null) {
            style.addSource(GeoJsonSource(SOURCE_ME, feature))
            style.addLayer(
                CircleLayer(LAYER_ME, SOURCE_ME).withProperties(
                    circleRadius(8f),
                    circleColor(Color.parseColor("#2196F3")),
                    circleStrokeColor(Color.WHITE),
                    circleStrokeWidth(3f)
                )
            )
        } else {
            existing.setGeoJson(feature)
        }
    }

    private fun updateGuidance(location: Location?) {
        val current = location?.let { GeoPoint(it.latitude, it.longitude) }
        val nav = navigationManager

        guidanceText.text = if (nav != null) {
            current?.let { nav.onLocationUpdate(it) }
            nav.instructionText(current)
        } else {
            // Fallback: tidak ada `steps` dari server -> garis lurus ke stop berikutnya.
            NavigationManager.guidanceText(currentStops, current)
        }
        guidanceText.visibility = View.VISIBLE
    }

    private fun showStatus(text: String) {
        statusText.text = text
        statusText.visibility = View.VISIBLE
    }

    // ---- Forward lifecycle ke MapView (pola wajib MapLibre Android) ----

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (followMeEnabled) startFollowingLocation()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        stopFollowingLocation()
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val SOURCE_ROUTE_LINE = "route-line-source"
        private const val LAYER_ROUTE_LINE = "route-line-layer"
        private const val SOURCE_STOPS = "route-stops-source"
        private const val LAYER_STOPS = "route-stops-layer"
        private const val SOURCE_ME = "my-location-source"
        private const val LAYER_ME = "my-location-layer"

        private const val CAMERA_PADDING_PX = 120
        private const val CAMERA_EASE_MS = 1200
    }
}
