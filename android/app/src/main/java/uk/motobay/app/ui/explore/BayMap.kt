package uk.motobay.app.ui.explore

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.PointF
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.not
import org.maplibre.android.style.expressions.Expression.step
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.toColor
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circlePitchAlignment
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconColor
import org.maplibre.android.style.layers.PropertyFactory.iconHaloColor
import org.maplibre.android.style.layers.PropertyFactory.iconHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.iconSize
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
import uk.motobay.app.BuildConfig
import uk.motobay.app.R
import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import kotlin.math.floor
import kotlin.math.roundToInt

/** Camera moves requested by the screen (search result, my location, opening a bay). */
sealed interface MapCommand {
    /** [bottomPaddingPx] keeps the target above an overlay such as the bay sheet. */
    data class Fly(val lat: Double, val lon: Double, val zoom: Double? = null, val bottomPaddingPx: Int = 0) : MapCommand
    data class Fit(val box: BBox) : MapCommand
}

data class MapViewport(val box: BBox, val zoom: Double, val center: LatLon)

/** Pin and cluster colours, taken from the current M3 colour scheme. */
data class PinColors(
    val free: Color, val onFree: Color,
    val paid: Color, val onPaid: Color,
    val unknown: Color, val onUnknown: Color,
    /** Ring drawn round the selected pin. */
    val selectedRing: Color,
    val cluster: Color, val onCluster: Color,
    val stroke: Color,
    val user: Color,
)

private const val BAY_SOURCE = "bays"
private const val CLUSTERS = "bay-clusters"
private const val CLUSTER_COUNT = "bay-cluster-count"
private const val BAY_PINS = "bay-pins"
private const val BAY_GLYPHS = "bay-glyphs"
private const val SELECTED_SOURCE = "selected"
private const val SELECTED_PULSE = "selected-pulse"
private const val SELECTED_PIN = "selected-pin"
private const val SELECTED_GLYPH = "selected-glyph"
private const val USER_SOURCE = "user"
private const val USER_LAYER = "user-dot"

/**
 * Map styles (OpenStreetMap data, no API key). Light uses OpenFreeMap's
 * Liberty; dark uses our Graphite style, a lightened copy of their dark style.
 */
private fun styleUrl(dark: Boolean) =
    if (dark) BuildConfig.DARK_MAP_STYLE_URL else "https://tiles.openfreemap.org/styles/liberty"

/** Used if the Graphite style can't be fetched. */
private const val FALLBACK_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

private const val MOTORCYCLE_ICON = "motorcycle"
private const val PIN_ICON = "pin"
/** Pin image height in dp at icon-size 1; the icon is drawn with its tip on the coordinate. */
private const val PIN_PX = 72
private const val GLYPH_PX = 48
private const val PIN_SIZE = 0.6f
private const val GLYPH_SIZE = 0.45f
private const val SELECTED_PIN_SIZE = 0.85f
private const val SELECTED_GLYPH_SIZE = 0.64f

/** M3 "emphasized decelerate" easing, for things entering the screen. */
private val EMPHASIZED_DECELERATE = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
/** M3 "standard" easing, for things moving within the screen. */
private val STANDARD = PathInterpolator(0.2f, 0f, 0f, 1f)

/** How long pins take to fly out of a cluster / gather back into one. */
private const val SPLIT_MS = 380L
private const val GATHER_MS = 320L

/** Runs [onFrame] with 0→1 (eased) for [durationMs]; cancelled with the coroutine. */
private suspend fun animate(durationMs: Long, interpolator: TimeInterpolator, onFrame: (Float) -> Unit) {
    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = durationMs
        this.interpolator = interpolator
        addUpdateListener { onFrame(it.animatedValue as Float) }
    }
    try {
        animator.start()
        delay(durationMs)
    } finally {
        animator.cancel()
    }
}

/**
 * Animates a pin layer pair (teardrop + glyph) from [from] × target size to
 * full size. Scaling both by the same factor keeps the glyph in the pin head,
 * because icon-offset is expressed in the glyph's own size units.
 */
private suspend fun animatePinScale(
    style: Style,
    pinLayer: String,
    glyphLayer: String,
    pinSize: Float,
    glyphSize: Float,
    from: Float,
    durationMs: Long,
    interpolator: TimeInterpolator,
) {
    val pin = style.getLayerAs<SymbolLayer>(pinLayer) ?: return
    val glyph = style.getLayerAs<SymbolLayer>(glyphLayer) ?: return
    try {
        animate(durationMs, interpolator) { t ->
            val k = from + (1f - from) * t
            pin.setProperties(iconSize(pinSize * k))
            glyph.setProperties(iconSize(glyphSize * k))
        }
    } finally {
        // Cancelled (new selection, style reload) or finished: land on the exact size.
        runCatching {
            pin.setProperties(iconSize(pinSize))
            glyph.setProperties(iconSize(glyphSize))
        }
    }
}

/** The pin head centre sits 15/24 of the shape's height above its tip. */
private const val PIN_HEAD_FROM_TIP = 15f / 24f

/**
 * icon-offset for the glyph layer so it lands in the pin head. MapLibre scales
 * the offset by the glyph layer's own icon-size, so divide that back out.
 */
private fun glyphOffset(pinSize: Float, glyphSize: Float): Array<Float> =
    arrayOf(0f, -(PIN_PX * pinSize * PIN_HEAD_FROM_TIP) / glyphSize)

/** What the bay source currently shows, so the next zoom can animate from it. */
private class Displayed(val zoom: Int, val nodes: List<MapNode>)

/**
 * MapLibre map of every bay. Nearby bays are grouped into numbered clusters
 * when zoomed out. Zooming in makes the pins fly out of their cluster, and
 * zooming out gathers them back; tapping a cluster zooms in until it splits.
 */
@Composable
fun BayMap(
    bays: List<Bay>,
    selected: Bay?,
    userPosition: LatLon?,
    command: MapCommand?,
    colors: PinColors,
    dark: Boolean,
    initialCamera: CameraSnapshot?,
    onBayClick: (String) -> Unit,
    onViewportChange: (MapViewport) -> Unit,
    onCommandHandled: () -> Unit,
    onLongPress: (LatLon) -> Unit,
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
    var viewport by remember { mutableStateOf<MapViewport?>(null) }
    val displayed = remember { mutableStateOf<Displayed?>(null) }
    val latestBays by rememberUpdatedState(bays)
    val latestBayClick by rememberUpdatedState(onBayClick)
    val latestViewport by rememberUpdatedState(onViewportChange)
    val latestLongPress by rememberUpdatedState(onLongPress)
    val latestDark by rememberUpdatedState(dark)

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
            val start = initialCamera ?: CameraSnapshot(54.5, -3.0, 4.5)
            m.cameraPosition = CameraPosition.Builder().target(LatLng(start.lat, start.lon)).zoom(start.zoom).build()
            fun readViewport(): MapViewport? {
                val b = m.projection.visibleRegion.latLngBounds
                val c = m.cameraPosition.target ?: return null
                return MapViewport(
                    BBox(b.latitudeSouth, b.longitudeWest, b.latitudeNorth, b.longitudeEast),
                    m.cameraPosition.zoom,
                    LatLon(c.latitude, c.longitude),
                )
            }
            m.addOnCameraIdleListener {
                val vp = readViewport() ?: return@addOnCameraIdleListener
                viewport = vp
                latestViewport(vp)
            }
            m.addOnMapClickListener { point ->
                val screen: PointF = m.projection.toScreenLocation(point)
                val hit = m.queryRenderedFeatures(screen, SELECTED_PIN, BAY_PINS, BAY_GLYPHS, CLUSTERS, CLUSTER_COUNT).firstOrNull()
                    ?: return@addOnMapClickListener false
                val id = hit.getStringProperty("id") ?: return@addOnMapClickListener false
                val node = displayed.value?.nodes?.firstOrNull { it.id == id }
                if (node != null && node.isCluster) {
                    // Zoom in until the cluster splits, centred on it.
                    val byId = latestBays.associateBy { it.id }
                    val members = node.members.mapNotNull { byId[it] }
                    val vp = viewport ?: readViewport()
                    val zoom = if (vp != null) {
                        expansionZoom(members, floor(vp.zoom).toInt(), vp.box.expanded(0.5)).toDouble()
                    } else {
                        m.cameraPosition.zoom + 2
                    }
                    m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(node.lat, node.lon), zoom), 500)
                } else {
                    latestBayClick(id)
                }
                true
            }
            // If the Graphite style can't be fetched, fall back to OpenFreeMap's dark style once.
            var triedFallback = false
            mapView.addOnDidFailLoadingMapListener { _: String ->
                if (latestDark && !triedFallback) {
                    triedFallback = true
                    m.setStyle(Style.Builder().fromUri(FALLBACK_DARK_STYLE))
                }
            }
            m.addOnMapLongClickListener { point ->
                latestLongPress(LatLon(point.latitude, point.longitude))
                true
            }
            viewport = readViewport()
            map = m
        }
    }

    // (Re)load the style when the theme flips between light and dark.
    LaunchedEffect(map, dark) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromUri(styleUrl(dark))) { loaded ->
            loaded.addSource(GeoJsonSource(BAY_SOURCE))
            loaded.addSource(GeoJsonSource(SELECTED_SOURCE))
            loaded.addSource(GeoJsonSource(USER_SOURCE))
            // Tintable (SDF) images: the teardrop pin and the motorcycle glyph on it.
            // MapLibre reads the bitmap's density as its pixel ratio, so render each at
            // density × logical size to get a sharp image that measures PIN_PX in dp.
            val density = context.resources.displayMetrics.density
            fun image(id: Int, logicalPx: Int) = ContextCompat.getDrawable(context, id)!!
                .toBitmap((logicalPx * density).roundToInt(), (logicalPx * density).roundToInt())
                .apply { this.density = context.resources.displayMetrics.densityDpi }
            loaded.addImage(MOTORCYCLE_ICON, image(R.drawable.ic_motorcycle, GLYPH_PX), true)
            loaded.addImage(PIN_ICON, image(R.drawable.ic_pin, PIN_PX), true)

            loaded.addLayer(
                CircleLayer(CLUSTERS, BAY_SOURCE).withFilter(has("point_count")).withProperties(
                    circleRadius(step(get("point_count"), literal(16f), stop(10, 20f), stop(100, 26f), stop(1000, 32f))),
                ),
            )
            loaded.addLayer(
                SymbolLayer(CLUSTER_COUNT, BAY_SOURCE).withFilter(has("point_count")).withProperties(
                    textField(get("point_count_abbreviated")),
                    textFont(arrayOf("Noto Sans Bold")),
                    textSize(13f),
                    textAllowOverlap(true),
                    textIgnorePlacement(true),
                ),
            )
            // Teardrop pin with its tip on the bay, then the motorcycle in the pin head.
            loaded.addLayer(
                SymbolLayer(BAY_PINS, BAY_SOURCE).withFilter(not(has("point_count"))).withProperties(
                    iconImage(PIN_ICON),
                    iconSize(PIN_SIZE),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconColor(toColor(get("fill"))),
                    iconHaloWidth(1.5f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            loaded.addLayer(
                SymbolLayer(BAY_GLYPHS, BAY_SOURCE).withFilter(not(has("point_count"))).withProperties(
                    iconImage(MOTORCYCLE_ICON),
                    iconSize(GLYPH_SIZE),
                    iconOffset(glyphOffset(pinSize = PIN_SIZE, glyphSize = GLYPH_SIZE)),
                    iconColor(toColor(get("ink"))),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            loaded.addLayer(CircleLayer(USER_LAYER, USER_SOURCE).withProperties(circleRadius(8f), circleStrokeWidth(3f)))
            // Selected bay: a soft ripple on the ground, then the same pin, larger, with a bright halo.
            loaded.addLayer(
                CircleLayer(SELECTED_PULSE, SELECTED_SOURCE).withProperties(
                    circleColor(toColor(get("fill"))),
                    circleRadius(0f),
                    circleOpacity(0f),
                    circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                ),
            )
            loaded.addLayer(
                SymbolLayer(SELECTED_PIN, SELECTED_SOURCE).withProperties(
                    iconImage(PIN_ICON),
                    iconSize(SELECTED_PIN_SIZE),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconColor(toColor(get("fill"))),
                    iconHaloWidth(3f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            loaded.addLayer(
                SymbolLayer(SELECTED_GLYPH, SELECTED_SOURCE).withProperties(
                    iconImage(MOTORCYCLE_ICON),
                    iconSize(SELECTED_GLYPH_SIZE),
                    iconOffset(glyphOffset(pinSize = SELECTED_PIN_SIZE, glyphSize = SELECTED_GLYPH_SIZE)),
                    iconColor(toColor(get("ink"))),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            displayed.value = null
            style = loaded
        }
    }

    // Theme colours for clusters, the selected pin and the user's dot.
    LaunchedEffect(style, colors) {
        val s = style ?: return@LaunchedEffect
        s.getLayerAs<CircleLayer>(CLUSTERS)?.setProperties(
            circleColor(colors.cluster.toArgb()), circleStrokeColor(colors.stroke.toArgb()), circleStrokeWidth(2f),
        )
        s.getLayerAs<SymbolLayer>(CLUSTER_COUNT)?.setProperties(textColor(colors.onCluster.toArgb()))
        s.getLayerAs<SymbolLayer>(BAY_PINS)?.setProperties(iconHaloColor(colors.stroke.toArgb()))
        s.getLayerAs<SymbolLayer>(SELECTED_PIN)?.setProperties(iconHaloColor(colors.selectedRing.toArgb()))
        s.getLayerAs<CircleLayer>(USER_LAYER)?.setProperties(
            circleColor(colors.user.toArgb()), circleStrokeColor(colors.stroke.toArgb()),
        )
    }

    // Cluster the bays for the current zoom and animate between zoom levels.
    var pinsShown by remember { mutableStateOf(false) }
    LaunchedEffect(style) { pinsShown = false }
    LaunchedEffect(style, bays, viewport, colors) {
        val s = style ?: return@LaunchedEffect
        val vp = viewport ?: return@LaunchedEffect
        val source = s.getSourceAs<GeoJsonSource>(BAY_SOURCE) ?: return@LaunchedEffect
        val zoom = floor(vp.zoom).toInt().coerceAtMost(CLUSTER_MAX_ZOOM)
        // Cluster a margin around the screen too, so panning doesn't pop pins at the edge.
        val target = clusterBays(bays, zoom, vp.box.expanded(0.5))
        val previous = displayed.value
        displayed.value = Displayed(zoom, target)

        fun show(nodes: List<MapNode>, at: Map<String, LatLon>? = null) {
            source.setGeoJson(FeatureCollection.fromFeatures(nodes.map { it.toFeature(colors, at?.get(it.id)) }))
        }
        try {
            when {
                previous == null || previous.zoom == zoom || previous.nodes.isEmpty() || target.isEmpty() -> show(target)
                zoom > previous.zoom -> {
                    // Zoomed in: each new pin or cluster flies out of the cluster it was in.
                    val origin = HashMap<String, LatLon>()
                    previous.nodes.forEach { n -> n.members.forEach { origin[it] = LatLon(n.lat, n.lon) } }
                    val starts = target.associate { it.id to (origin[it.members.first()] ?: LatLon(it.lat, it.lon)) }
                    animate(SPLIT_MS, EMPHASIZED_DECELERATE) { t ->
                        show(target, target.associate { it.id to starts.getValue(it.id).lerp(LatLon(it.lat, it.lon), t) })
                    }
                }
                else -> {
                    // Zoomed out: the pins on screen slide into the cluster they now belong to.
                    val dest = HashMap<String, LatLon>()
                    target.forEach { n -> n.members.forEach { dest[it] = LatLon(n.lat, n.lon) } }
                    val ends = previous.nodes.associate { it.id to (dest[it.members.first()] ?: LatLon(it.lat, it.lon)) }
                    animate(GATHER_MS, STANDARD) { t ->
                        show(previous.nodes, previous.nodes.associate { it.id to LatLon(it.lat, it.lon).lerp(ends.getValue(it.id), t) })
                    }
                }
            }
        } finally {
            // Whether finished or interrupted by the next camera move, land on the real layout.
            runCatching { show(target) }
        }
        if (target.isNotEmpty() && !pinsShown) {
            // Pins scale in the first time they appear on a freshly loaded style.
            pinsShown = true
            animatePinScale(s, BAY_PINS, BAY_GLYPHS, PIN_SIZE, GLYPH_SIZE, from = 0f, durationMs = 450, interpolator = EMPHASIZED_DECELERATE)
        }
    }

    LaunchedEffect(style, selected, colors) {
        val s = style ?: return@LaunchedEffect
        val features = selected?.let { listOf(it.toNode().toFeature(colors)) }.orEmpty()
        s.getSourceAs<GeoJsonSource>(SELECTED_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
        val pulse = s.getLayerAs<CircleLayer>(SELECTED_PULSE)
        if (selected == null) {
            pulse?.setProperties(circleRadius(0f), circleOpacity(0f))
            return@LaunchedEffect
        }
        try {
            // Drops in with a bounce, like an M3 FAB appearing...
            animatePinScale(
                s, SELECTED_PIN, SELECTED_GLYPH, SELECTED_PIN_SIZE, SELECTED_GLYPH_SIZE,
                from = 0.2f, durationMs = 420, interpolator = OvershootInterpolator(2f),
            )
            // ...then a ripple spreads from the tip while the bay stays selected.
            while (true) {
                animate(1600, LinearInterpolator()) { t ->
                    pulse?.setProperties(circleRadius(6f + 26f * t), circleOpacity(0.45f * (1f - t)))
                }
                delay(400)
            }
        } finally {
            runCatching { pulse?.setProperties(circleRadius(0f), circleOpacity(0f)) }
        }
    }

    LaunchedEffect(style, userPosition) {
        val s = style ?: return@LaunchedEffect
        val features = userPosition?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) }.orEmpty()
        s.getSourceAs<GeoJsonSource>(USER_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(map, command) {
        val m = map ?: return@LaunchedEffect
        when (val c = command ?: return@LaunchedEffect) {
            is MapCommand.Fly -> {
                val zoom = c.zoom ?: maxOf(m.cameraPosition.zoom, 16.0)
                val position = CameraPosition.Builder()
                    .target(LatLng(c.lat, c.lon))
                    .zoom(zoom)
                    .padding(0.0, 0.0, 0.0, c.bottomPaddingPx.toDouble())
                    .build()
                m.animateCamera(CameraUpdateFactory.newCameraPosition(position), 800)
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

private fun Bay.toNode() = MapNode(id, lat, lon, listOf(id), this)

private fun LatLon.lerp(to: LatLon, t: Float): LatLon =
    LatLon(lat + (to.lat - lat) * t, lon + (to.lon - lon) * t)

/** The box grown by [fraction] of its size on every side. */
private fun BBox.expanded(fraction: Double): BBox {
    val dLat = (north - south) * fraction
    val dLon = (east - west) * fraction
    return BBox(south - dLat, west - dLon, north + dLat, east + dLon)
}

private fun abbreviate(n: Int): String = if (n >= 1_000) "${n / 1_000}k" else n.toString()

/** GeoJSON for a node, optionally drawn at an interpolated position [at] mid-animation. */
private fun MapNode.toFeature(colors: PinColors, at: LatLon? = null): Feature {
    val p = at ?: LatLon(lat, lon)
    return Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).apply {
        addStringProperty("id", id)
        val b = bay
        if (b == null) {
            addNumberProperty("point_count", count)
            addStringProperty("point_count_abbreviated", abbreviate(count))
        } else {
            val (fill, ink) = when (b.fee) {
                Fee.Free -> colors.free to colors.onFree
                Fee.Paid -> colors.paid to colors.onPaid
                Fee.Unknown -> colors.unknown to colors.onUnknown
            }
            addStringProperty("fill", fill.toHex())
            addStringProperty("ink", ink.toHex())
        }
    }
}

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
