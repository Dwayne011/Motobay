package uk.motobay.app.ui.explore

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.PointF
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.not
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
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import uk.motobay.app.BuildConfig
import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import kotlin.coroutines.resume
import kotlin.math.floor

/** Camera moves requested by the screen (search result, my location, opening a bay). */
sealed interface MapCommand {
    /** [bottomPaddingPx] keeps the target above an overlay such as the bay sheet. */
    data class Fly(val lat: Double, val lon: Double, val zoom: Double? = null, val bottomPaddingPx: Int = 0) : MapCommand
    data class Fit(val box: BBox) : MapCommand
}

data class MapViewport(val box: BBox, val zoom: Double, val center: LatLon)

/** Marker colours: the semantic status dots plus the pill surfaces from the Aurora palette. */
data class PinColors(
    val free: Color,
    val paid: Color,
    val unknown: Color,
    val pillFill: Color,
    val pillBorder: Color,
    val pillText: Color,
    val selectedFill: Color,
    val stroke: Color,
    val user: Color,
)

private const val BAY_SOURCE = "bays"
private const val CLUSTERS = "bay-clusters"
private const val BAY_PINS = "bay-pins"
private const val SELECTED_SOURCE = "selected"
private const val SELECTED_PULSE = "selected-pulse"
private const val SELECTED_PIN = "selected-pin"
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

/** M3 "emphasized decelerate" easing, for things entering the screen. */
private val EMPHASIZED_DECELERATE = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
/** M3 "standard" easing, for things moving within the screen. */
private val STANDARD = PathInterpolator(0.2f, 0f, 0f, 1f)

/** The selected pill's scale, and the ring that pulses under it. */
private const val SELECTED_SCALE = 1.12f
private const val PULSE_RADIUS_DP = 24f
private const val PULSE_MS = 1400L

/** How long pins take to fly out of a cluster / gather back into one. */
private const val SPLIT_MS = 400L
private const val GATHER_MS = 340L

/** Drives [onFrame] with eased 0→1 progress from the display's frame clock, so it runs at the screen's refresh rate. */
private suspend fun animateFrames(durationMs: Long, interpolator: TimeInterpolator, onFrame: (Float) -> Unit) {
    val start = withFrameNanos { it }
    while (true) {
        val now = withFrameNanos { it }
        val raw = ((now - start) / 1_000_000.0 / durationMs).coerceAtMost(1.0).toFloat()
        onFrame(interpolator.getInterpolation(raw))
        if (raw >= 1f) return
    }
}

/** Runs [onFrame] with 0→1 (eased) for [durationMs] via a ValueAnimator; cancelled with the coroutine. */
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

/** Suspends until MapLibre reports a fully rendered frame (all sources loaded), or [timeoutMs] passes. */
private suspend fun MapView.awaitFullyRendered(timeoutMs: Long) {
    withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            val listener = object : MapView.OnDidFinishRenderingFrameListener {
                override fun onDidFinishRenderingFrame(fully: Boolean, frameEncodingTime: Double, frameRenderingTime: Double) {
                    if (fully && cont.isActive) {
                        removeOnDidFinishRenderingFrameListener(this)
                        cont.resume(Unit)
                    }
                }
            }
            addOnDidFinishRenderingFrameListener(listener)
            cont.invokeOnCancellation { removeOnDidFinishRenderingFrameListener(listener) }
        }
    }
}

/** What the bay source currently shows, so the next zoom can animate from it. */
private class Displayed(val zoom: Int, val nodes: List<MapNode>)

private enum class Fade { None, In, Out }

/** One marker moving across the screen during a zoom transition, in view pixels. */
private class Sprite(val image: MarkerImage, val start: Offset, val end: Offset, val fade: Fade)

private class Transition(val sprites: List<Sprite>, val t: Float)

/**
 * MapLibre map of every bay. Nearby bays are grouped into numbered clusters
 * when zoomed out. Zooming in makes the pins fly out of their cluster, and
 * zooming out gathers them back; tapping a cluster zooms in until it splits.
 *
 * The zoom transitions are drawn on a Compose canvas over the map rather than
 * by moving GeoJSON features: rewriting a source every frame makes MapLibre
 * re-tile and re-lay out the symbols, which stutters. The map's own pins are
 * hidden for the duration and the exact same bitmaps are blitted, so the
 * hand-over is seamless.
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
    onMovingChange: (Boolean) -> Unit = {},
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
    val holder = LocalMapHolder.current
    val mapView = remember { holder?.mapView ?: MapHolder.createMapView(context) }
    var map by remember { mutableStateOf(holder?.map) }
    var style by remember { mutableStateOf<Style?>(null) }
    var viewport by remember { mutableStateOf<MapViewport?>(null) }
    /** Bumped whenever the camera starts moving, which cancels any zoom transition in flight. */
    var moveGen by remember { mutableIntStateOf(0) }
    var transition by remember { mutableStateOf<Transition?>(null) }
    val displayed = remember { mutableStateOf<Displayed?>(null) }
    val images = remember(context, colors) { PinImages(context, colors) }
    val latestBays by rememberUpdatedState(bays)
    val latestBayClick by rememberUpdatedState(onBayClick)
    val latestViewport by rememberUpdatedState(onViewportChange)
    val latestMoving by rememberUpdatedState(onMovingChange)
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
            // A shared map only pauses here; the activity destroys it.
            if (holder == null) mapView.onDestroy()
        }
    }

    // What the map's listeners call while this screen is on it; the holder's listeners outlive the screen.
    val callbacks = remember {
        object : MapCallbacks {
            override fun onMoveStarted() { moveGen++; latestMoving(true) }
            override fun onMoveCancelled() { latestMoving(false) }
            override fun onIdle() {
                latestMoving(false)
                val vp = map?.readViewport() ?: return
                viewport = vp
                latestViewport(vp)
            }
            override fun onClick(point: LatLng): Boolean {
                val m = map ?: return false
                val screen: PointF = m.projection.toScreenLocation(point)
                val hit = m.queryRenderedFeatures(screen, SELECTED_PIN, BAY_PINS, CLUSTERS).firstOrNull() ?: return false
                val id = hit.getStringProperty("id") ?: return false
                val node = displayed.value?.nodes?.firstOrNull { it.id == id }
                if (node != null && node.isCluster) {
                    // Zoom in until the cluster splits, centred on it.
                    val byId = latestBays.associateBy { it.id }
                    val members = node.members.mapNotNull { byId[it] }
                    val vp = viewport ?: m.readViewport()
                    val zoom = if (vp != null) {
                        expansionZoom(members, floor(vp.zoom).toInt(), vp.box.expanded(0.5)).toDouble()
                    } else {
                        m.cameraPosition.zoom + 2
                    }
                    m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(node.lat, node.lon), zoom), 500)
                } else {
                    latestBayClick(id)
                }
                return true
            }
            override fun onLongClick(point: LatLng) { latestLongPress(LatLon(point.latitude, point.longitude)) }
            // If the Graphite style can't be fetched, fall back to OpenFreeMap's dark style once.
            private var triedFallback = false
            override fun onFailLoading() {
                if (latestDark && !triedFallback) {
                    triedFallback = true
                    map?.setStyle(Style.Builder().fromUri(FALLBACK_DARK_STYLE))
                }
            }
        }
    }
    val localCallbacks = remember { mutableStateOf<MapCallbacks?>(null) }
    DisposableEffect(callbacks) {
        if (holder != null) holder.callbacks = callbacks else localCallbacks.value = callbacks
        onDispose { if (holder != null && holder.callbacks === callbacks) holder.callbacks = null }
    }

    LaunchedEffect(mapView) {
        val existing = map
        if (existing != null) {
            // Coming back to a live map: pick up where it is.
            viewport = existing.readViewport()
            return@LaunchedEffect
        }
        mapView.getMapAsync { m ->
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isCompassEnabled = false
            // Keep the MapLibre logo and (i) attribution clear of the Map/List toggle that sits bottom-centre.
            val density = mapView.resources.displayMetrics.density
            val clearance = (ATTRIBUTION_BOTTOM_DP * density).toInt()
            m.uiSettings.setLogoMargins((8 * density).toInt(), 0, 0, clearance)
            m.uiSettings.setAttributionMargins((8 * density).toInt(), 0, 0, clearance)
            // First launch, or no saved position: central London, where most mapped bays are.
            val start = initialCamera ?: DEFAULT_CAMERA
            m.cameraPosition = CameraPosition.Builder().target(LatLng(start.lat, start.lon)).zoom(start.zoom).build()
            val current: () -> MapCallbacks? = { holder?.callbacks ?: localCallbacks.value }
            if (holder == null || !holder.listenersInstalled) {
                holder?.listenersInstalled = true
                m.addOnCameraMoveStartedListener { current()?.onMoveStarted() }
                m.addOnCameraMoveCancelListener { current()?.onMoveCancelled() }
                m.addOnCameraIdleListener { current()?.onIdle() }
                m.addOnMapClickListener { point -> current()?.onClick(point) ?: false }
                m.addOnMapLongClickListener { point -> current()?.onLongClick(point); true }
                mapView.addOnDidFailLoadingMapListener { _: String -> current()?.onFailLoading() }
            }
            viewport = m.readViewport()
            holder?.map = m
            map = m
        }
    }

    // (Re)load the style when the theme flips between light and dark.
    LaunchedEffect(map, dark) {
        val m = map ?: return@LaunchedEffect
        val kept = holder?.style
        if (kept != null && holder.styleDark == dark && m.style === kept) {
            style = kept
            return@LaunchedEffect
        }
        style = null
        holder?.style = null
        m.setStyle(Style.Builder().fromUri(styleUrl(dark))) { loaded ->
            loaded.addSource(GeoJsonSource(BAY_SOURCE))
            loaded.addSource(GeoJsonSource(SELECTED_SOURCE))
            loaded.addSource(GeoJsonSource(USER_SOURCE))
            // Every marker is a pre-rendered bitmap (see PinImages), chosen per feature by its "icon" key.
            loaded.addLayer(
                SymbolLayer(CLUSTERS, BAY_SOURCE).withFilter(has("point_count")).withProperties(
                    iconImage(get("icon")),
                    iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            // A capsule pill centred on the bay.
            loaded.addLayer(
                SymbolLayer(BAY_PINS, BAY_SOURCE).withFilter(not(has("point_count"))).withProperties(
                    iconImage(get("icon")),
                    iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            loaded.addLayer(CircleLayer(USER_LAYER, USER_SOURCE).withProperties(circleRadius(8f), circleStrokeWidth(3f)))
            // Selected bay: a pulsing ring on the ground, then the same pill, sprung up to 1.12x, with its status colour as border.
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
                    iconImage(get("icon")),
                    iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
            )
            displayed.value = null
            holder?.style = loaded
            holder?.styleDark = dark
            style = loaded
        }
    }

    LaunchedEffect(style, colors) {
        val s = style ?: return@LaunchedEffect
        s.getLayerAs<CircleLayer>(USER_LAYER)?.setProperties(
            circleColor(colors.user.toArgb()), circleStrokeColor(colors.stroke.toArgb()),
        )
    }

    // Cluster the bays for the current zoom and animate between zoom levels.
    var pinsShown by remember { mutableStateOf(false) }
    LaunchedEffect(style) { pinsShown = false }
    LaunchedEffect(style, bays, viewport, colors, moveGen) {
        val s = style ?: return@LaunchedEffect
        val m = map ?: return@LaunchedEffect
        val vp = viewport ?: return@LaunchedEffect
        val source = s.getSourceAs<GeoJsonSource>(BAY_SOURCE) ?: return@LaunchedEffect
        val zoom = floor(vp.zoom).toInt().coerceAtMost(CLUSTER_MAX_ZOOM)
        // Cluster a margin around the screen too, so panning doesn't pop pins at the edge.
        val target = clusterBays(bays, zoom, vp.box.expanded(0.5))
        val previous = displayed.value
        displayed.value = Displayed(zoom, target)

        fun show(nodes: List<MapNode>) {
            images.install(s, nodes.map(images::key))
            source.setGeoJson(FeatureCollection.fromFeatures(nodes.map { it.toFeature(images) }))
        }
        val animated = previous != null && previous.zoom != zoom && previous.nodes.isNotEmpty() && target.isNotEmpty()
        if (!animated) {
            transition = null
            show(target)
        } else {
            val proj = m.projection
            fun screen(lat: Double, lon: Double) = proj.toScreenLocation(LatLng(lat, lon)).let { Offset(it.x, it.y) }
            fun at(n: MapNode) = screen(n.lat, n.lon)
            val sprites = ArrayList<Sprite>()
            val duration: Long
            val easing: TimeInterpolator
            if (zoom > previous!!.zoom) {
                // Zoomed in: each new pin or cluster flies out of the cluster it was in, which fades away.
                duration = SPLIT_MS; easing = EMPHASIZED_DECELERATE
                val origin = HashMap<String, Offset>()
                previous.nodes.forEach { n -> val p = at(n); n.members.forEach { origin[it] = p } }
                previous.nodes.filter { it.isCluster }.forEach { sprites += Sprite(images[images.key(it)], at(it), at(it), Fade.Out) }
                target.forEach { n -> val end = at(n); sprites += Sprite(images[images.key(n)], origin[n.members.first()] ?: end, end, Fade.None) }
            } else {
                // Zoomed out: the pins on screen slide into the cluster they now belong to, which fades in.
                duration = GATHER_MS; easing = STANDARD
                val dest = HashMap<String, Offset>()
                target.forEach { n -> val p = at(n); n.members.forEach { dest[it] = p } }
                target.filter { it.isCluster }.forEach { sprites += Sprite(images[images.key(it)], at(it), at(it), Fade.In) }
                previous.nodes.forEach { n -> val start = at(n); sprites += Sprite(images[images.key(n)], start, dest[n.members.first()] ?: start, Fade.None) }
            }
            try {
                // Take over drawing: hide the map's markers and animate the same bitmaps on the canvas.
                transition = Transition(sprites, 0f)
                source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                animateFrames(duration, easing) { t -> transition = Transition(sprites, t) }
                // Hand back: load the real layout and keep the finished frame up until the map has drawn it.
                show(target)
                delay(32)
                mapView.awaitFullyRendered(timeoutMs = 400)
            } finally {
                transition = null
                runCatching { show(target) }
            }
        }
        if (target.isNotEmpty() && !pinsShown) {
            // Markers fade in the first time they appear on a freshly loaded style.
            pinsShown = true
            val pins = s.getLayerAs<SymbolLayer>(BAY_PINS)
            val clusters = s.getLayerAs<SymbolLayer>(CLUSTERS)
            try {
                animate(350, EMPHASIZED_DECELERATE) { t ->
                    pins?.setProperties(iconOpacity(t)); clusters?.setProperties(iconOpacity(t))
                }
            } finally {
                runCatching { pins?.setProperties(iconOpacity(1f)); clusters?.setProperties(iconOpacity(1f)) }
            }
        }
    }

    LaunchedEffect(style, selected, colors) {
        val s = style ?: return@LaunchedEffect
        val pulse = s.getLayerAs<CircleLayer>(SELECTED_PULSE)
        val pin = s.getLayerAs<SymbolLayer>(SELECTED_PIN)
        val source = s.getSourceAs<GeoJsonSource>(SELECTED_SOURCE)
        if (selected == null) {
            source?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            pulse?.setProperties(circleRadius(0f), circleOpacity(0f))
            return@LaunchedEffect
        }
        val key = images.pinKey(selected.fee, selected = true)
        images.install(s, listOf(key))
        source?.setGeoJson(selected.toNode().toFeature(images, iconKey = key))
        try {
            // Springs up to 1.12x (spec: dampingRatio 0.65, stiffness 400)...
            Animatable(1f).animateTo(SELECTED_SCALE, spring(dampingRatio = 0.65f, stiffness = 400f)) {
                pin?.setProperties(iconSize(value))
            }
            pin?.setProperties(iconSize(SELECTED_SCALE))
            // ...while a 48dp ring pulses out to 1.65x and fades, every 1400ms, for as long as the bay stays selected.
            while (true) {
                animate(PULSE_MS, LinearInterpolator()) { t ->
                    pulse?.setProperties(circleRadius(PULSE_RADIUS_DP * (1f + 0.65f * t)), circleOpacity(0.35f * (1f - t)))
                }
            }
        } finally {
            runCatching {
                pin?.setProperties(iconSize(1f))
                pulse?.setProperties(circleRadius(0f), circleOpacity(0f))
            }
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

    Box(modifier) {
        AndroidView(
            factory = { mapView.also { v -> (v.parent as? ViewGroup)?.removeView(v) } },
            modifier = Modifier.fillMaxSize(),
        )
        // Zoom transition overlay; draws nothing (and takes no touches) when idle.
        Canvas(Modifier.fillMaxSize()) {
            val tr = transition ?: return@Canvas
            for (sp in tr.sprites) {
                val alpha = when (sp.fade) {
                    Fade.None -> 1f
                    Fade.Out -> (1f - tr.t / 0.4f).coerceIn(0f, 1f)
                    Fade.In -> ((tr.t - 0.6f) / 0.4f).coerceIn(0f, 1f)
                }
                if (alpha <= 0f) continue
                val p = lerp(sp.start, sp.end, tr.t)
                drawImage(sp.image.image, topLeft = Offset(p.x - sp.image.anchorX, p.y - sp.image.anchorY), alpha = alpha)
            }
        }
    }
}

private fun MapLibreMap.readViewport(): MapViewport? {
    val b = projection.visibleRegion.latLngBounds
    val c = cameraPosition.target ?: return null
    return MapViewport(
        BBox(b.latitudeSouth, b.longitudeWest, b.latitudeNorth, b.longitudeEast),
        cameraPosition.zoom,
        LatLon(c.latitude, c.longitude),
    )
}

private fun Bay.toNode() = MapNode(id, lat, lon, listOf(id), this)

/** The box grown by [fraction] of its size on every side. */
private fun BBox.expanded(fraction: Double): BBox {
    val dLat = (north - south) * fraction
    val dLon = (east - west) * fraction
    return BBox(south - dLat, west - dLon, north + dLat, east + dLon)
}

private fun MapNode.toFeature(images: PinImages, iconKey: String = images.key(this)): Feature =
    Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
        addStringProperty("id", id)
        addStringProperty("icon", iconKey)
        val b = bay
        if (b == null) {
            addNumberProperty("point_count", count)
        } else {
            // The selected-pin ripple takes the pin's colour from here.
            addStringProperty("fill", b.fee.fillHex(images))
        }
    }

private fun Fee.fillHex(images: PinImages): String = images.fillColor(this).toHex()

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

/** Where the map opens before the rider has moved it anywhere: central London. */
val DEFAULT_CAMERA = CameraSnapshot(lat = 51.5074, lon = -0.1278, zoom = 12.0)

/** Height of the Map/List toggle plus its padding, so the map attribution sits above it. */
private const val ATTRIBUTION_BOTTOM_DP = 76
