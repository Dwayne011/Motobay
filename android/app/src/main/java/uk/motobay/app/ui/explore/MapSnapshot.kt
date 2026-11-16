package uk.motobay.app.ui.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import uk.motobay.app.data.LatLon

/**
 * Screenshot tests only. MapLibre needs a real device, so the store screenshots use a
 * picture of the map rendered elsewhere (scripts/store/render_map.mjs) at a known camera,
 * and the app's own pins are drawn over it here exactly where MapLibre would put them.
 */
class MapSnapshot(val image: ImageBitmap, val lat: Double, val lon: Double, val zoom: Double)

val LocalMapSnapshot = staticCompositionLocalOf<MapSnapshot?> { null }

/** The map picture with the bays' pins (and clusters) on top; reports the viewport like the real map. */
@Composable
internal fun SnapshotMap(
    snapshot: MapSnapshot,
    bays: List<Bay>,
    selected: Bay?,
    colors: PinColors,
    onViewportChange: (MapViewport) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val images = remember(context, colors) { PinImages(context, colors) }
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier) {
        val widthDp = maxWidth.value.toDouble()
        val heightDp = maxHeight.value.toDouble()
        // Web Mercator in dp: MapLibre (native and JS) uses 512-unit tiles.
        val world = 512.0 * 2.0.pow(snapshot.zoom)
        fun x(lon: Double) = (lon + 180) / 360 * world
        fun y(lat: Double): Double {
            val s = sin(Math.toRadians(lat))
            return (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * world
        }
        fun lonAt(px: Double) = px / world * 360 - 180
        fun latAt(py: Double) = Math.toDegrees(atan(sinh(PI - 2 * PI * py / world)))
        val cx = x(snapshot.lon)
        val cy = y(snapshot.lat)
        val box = BBox(
            south = latAt(cy + heightDp / 2), west = lonAt(cx - widthDp / 2),
            north = latAt(cy - heightDp / 2), east = lonAt(cx + widthDp / 2),
        )
        LaunchedEffect(box) { onViewportChange(MapViewport(box, snapshot.zoom, LatLon(snapshot.lat, snapshot.lon))) }
        val nodes = remember(bays, box) { clusterBays(bays, floor(snapshot.zoom).toInt().coerceAtMost(CLUSTER_MAX_ZOOM), box) }
        Canvas(Modifier.fillMaxSize()) {
            drawImage(snapshot.image, dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
            fun at(lat: Double, lon: Double) = Offset(
                ((x(lon) - cx + widthDp / 2) * density).toFloat(),
                ((y(lat) - cy + heightDp / 2) * density).toFloat(),
            )
            for (n in nodes) {
                if (selected != null && n.bay?.id == selected.id) continue
                val img = images[images.key(n)]
                val p = at(n.lat, n.lon)
                drawImage(img.image, Offset(p.x - img.anchorX, p.y - img.anchorY))
            }
            selected?.let { bay ->
                val img = images[images.pinKey(bay.fee, selected = true)]
                val p = at(bay.lat, bay.lon)
                drawImage(img.image, Offset(p.x - img.anchorX, p.y - img.anchorY))
            }
        }
    }
}
