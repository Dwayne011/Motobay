package uk.motobay.app.ui.explore

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.expressions.Expression.toColor
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon

/** Camera moves requested by the screen (search result, my location, opening a bay). */
sealed interface MapCommand {
    data class Fly(val lat: Double, val lon: Double, val zoom: Double? = null) : MapCommand
    data class Fit(val box: BBox) : MapCommand
}

data class MapViewport(val box: BBox, val zoom: Double, val center: LatLon)

/** Pin colours, taken from the current M3 colour scheme. */
data class PinColors(
    val free: Color, val onFree: Color,
    val paid: Color, val onPaid: Color,
    val unknown: Color, val onUnknown: Color,
    val selected: Color, val onSelected: Color,
    val stroke: Color,
    val user: Color,
)

private const val BAY_SOURCE = "bays"
private const val BAY_CIRCLES = "bays-circles"
private const val BAY_LABELS = "bays-labels"
private const val USER_SOURCE = "user"
private const val USER_LAYER = "user-dot"

/** OpenFreeMap vector styles: free, no API key, OpenStreetMap data. */
private fun styleUrl(dark: Boolean) =
    if (dark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/liberty"

/**
 * MapLibre map showing bays as pins coloured by price. Uses the native
 * MapLibre SDK inside Compose.
 */
@Composable
fun BayMap(
    bays: List<Bay>,
    selectedId: String?,
    userPosition: LatLon?,
    command: MapCommand?,
    colors: PinColors,
    dark: Boolean,
    initialCamera: CameraSnapshot?,
    onBayClick: (String) -> Unit,
    onViewportChange: (MapViewport) -> Unit,
    onCommandHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Previews and JVM screenshot tests can't load MapLibre's native code.
    if (LocalInspectionMode.current) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
        return
    }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val latestBayClick by rememberUpdatedState(onBayClick)
    val latestViewport by rememberUpdatedState(onViewportChange)

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isCompassEnabled = false
            val start = initialCamera ?: CameraSnapshot(54.5, -3.0, 5.0)
            m.cameraPosition = CameraPosition.Builder().target(LatLng(start.lat, start.lon)).zoom(start.zoom).build()
            m.addOnCameraIdleListener {
                val b = m.projection.visibleRegion.latLngBounds
                val c = m.cameraPosition.target ?: return@addOnCameraIdleListener
                latestViewport(
                    MapViewport(
                        BBox(b.latitudeSouth, b.longitudeWest, b.latitudeNorth, b.longitudeEast),
                        m.cameraPosition.zoom,
                        LatLon(c.latitude, c.longitude),
                    ),
                )
            }
            m.addOnMapClickListener { point ->
                val screen: PointF = m.projection.toScreenLocation(point)
                val hit = m.queryRenderedFeatures(screen, BAY_CIRCLES, BAY_LABELS).firstOrNull()
                hit?.getStringProperty("id")?.let { latestBayClick(it); true } ?: false
            }
            map = m
        }
    }

    // (Re)load the style when the theme flips between light and dark.
    LaunchedEffect(map, dark) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromUri(styleUrl(dark))) { loaded ->
            loaded.addSource(GeoJsonSource(BAY_SOURCE))
            loaded.addSource(GeoJsonSource(USER_SOURCE))
            loaded.addLayer(
                CircleLayer(USER_LAYER, USER_SOURCE).withProperties(
                    circleRadius(8f),
                    circleColor(colors.user.toArgb()),
                    circleStrokeColor(colors.stroke.toArgb()),
                    circleStrokeWidth(3f),
                ),
            )
            loaded.addLayer(
                CircleLayer(BAY_CIRCLES, BAY_SOURCE).withProperties(
                    circleRadius(switchCase(get("selected"), literal(18f), literal(14f))),
                    circleColor(toColor(get("fill"))),
                    circleStrokeColor(colors.stroke.toArgb()),
                    circleStrokeWidth(2f),
                ),
            )
            loaded.addLayer(
                SymbolLayer(BAY_LABELS, BAY_SOURCE).withProperties(
                    textField(get("glyph")),
                    textFont(arrayOf("Noto Sans Bold")),
                    textSize(14f),
                    textColor(toColor(get("ink"))),
                    textAllowOverlap(true),
                    textIgnorePlacement(true),
                ),
            )
            style = loaded
        }
    }

    // Push bay data whenever it, the selection or the colours change.
    LaunchedEffect(style, bays, selectedId, colors) {
        val s = style ?: return@LaunchedEffect
        val features = bays.map { bay ->
            val selected = bay.id == selectedId
            val (fill, ink) = when {
                selected -> colors.selected to colors.onSelected
                bay.fee == Fee.Free -> colors.free to colors.onFree
                bay.fee == Fee.Paid -> colors.paid to colors.onPaid
                else -> colors.unknown to colors.onUnknown
            }
            Feature.fromGeometry(Point.fromLngLat(bay.lon, bay.lat)).apply {
                addStringProperty("id", bay.id)
                addBooleanProperty("selected", selected)
                addStringProperty("fill", fill.toHex())
                addStringProperty("ink", ink.toHex())
                addStringProperty("glyph", when (bay.fee) { Fee.Free -> "P"; Fee.Paid -> "£"; Fee.Unknown -> "?" })
            }
        }
        // Draw the selected pin last so it sits on top.
        val ordered = features.sortedBy { it.getBooleanProperty("selected") }
        s.getSourceAs<GeoJsonSource>(BAY_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(ordered))
    }

    LaunchedEffect(style, userPosition, colors) {
        val s = style ?: return@LaunchedEffect
        s.getLayerAs<CircleLayer>(USER_LAYER)?.setProperties(
            circleColor(colors.user.toArgb()),
            circleStrokeColor(colors.stroke.toArgb()),
        )
        s.getLayerAs<CircleLayer>(BAY_CIRCLES)?.setProperties(circleStrokeColor(colors.stroke.toArgb()))
        val features = userPosition?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) }.orEmpty()
        s.getSourceAs<GeoJsonSource>(USER_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(map, command) {
        val m = map ?: return@LaunchedEffect
        when (val c = command ?: return@LaunchedEffect) {
            is MapCommand.Fly -> {
                val zoom = c.zoom ?: maxOf(m.cameraPosition.zoom, 16.0)
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(c.lat, c.lon), zoom), 800)
            }
            is MapCommand.Fit -> {
                val bounds = LatLngBounds.from(c.box.north, c.box.east, c.box.south, c.box.west)
                m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 48), 800)
            }
        }
        // Run each command once, so a recreated map doesn't repeat an old move.
        onCommandHandled()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
