package uk.motobay.app.ui.explore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.core.content.ContextCompat
import org.maplibre.android.maps.Style
import uk.motobay.app.R
import uk.motobay.app.data.Fee
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Material "place" teardrop, in a 24-unit box: head centre (12, 9), tip (12, 22). */
private const val PIN_PATH = "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"
private const val PIN_UNITS = 24f
private const val PIN_TIP_Y = 22f
private const val PIN_HEAD_Y = 9f

/** Pin height in dp (box height; the shape itself spans 20/24 of it). */
const val PIN_DP = 44f
const val SELECTED_PIN_DP = 62f
/** Outline width in dp around a pin; the tip sits this far above the bitmap's bottom edge. */
const val PIN_STROKE_DP = 1.5f
const val SELECTED_PIN_STROKE_DP = 3f
private const val GLYPH_FRACTION = 0.5f
private const val CLUSTER_STROKE_DP = 2f
private const val CLUSTER_TEXT_SP = 13f

/** Cluster disc radius in dp for a given member count. */
fun clusterRadiusDp(count: Int): Float = when {
    count >= 1000 -> 32f
    count >= 100 -> 26f
    count >= 10 -> 20f
    else -> 16f
}

fun abbreviateCount(n: Int): String = if (n >= 1_000) "${n / 1_000}k" else n.toString()

/**
 * A rendered marker: the bitmap MapLibre draws, and where its anchor point
 * sits inside it so the Compose overlay can draw it at exactly the same spot.
 */
class MarkerImage(val bitmap: Bitmap, val anchorX: Float, val anchorY: Float) {
    val image: ImageBitmap by lazy { bitmap.asImageBitmap() }
}

/**
 * Pre-rendered, anti-aliased marker bitmaps in the current theme colours.
 * MapLibre's tintable (SDF) icons need a real distance field to look sharp,
 * so instead each colour state gets its own crisp bitmap: a teardrop with
 * outline and motorcycle baked in, or a numbered cluster disc. Bitmaps are
 * rendered at the screen's density and tagged with it, so MapLibre sizes
 * them in dp and the overlay can blit them 1:1.
 */
class PinImages(private val context: Context, private val colors: PinColors) {
    private val density = context.resources.displayMetrics.density
    private val cache = HashMap<String, MarkerImage>()
    private var installedIn: Style? = null
    private val installed = HashSet<String>()

    fun pinKey(fee: Fee, selected: Boolean = false) = "pin/$fee" + if (selected) "/selected" else ""
    fun clusterKey(count: Int) = "cluster/${abbreviateCount(count)}/${clusterRadiusDp(count).toInt()}"
    fun key(node: MapNode): String = node.bay?.let { pinKey(it.fee) } ?: clusterKey(node.count)

    fun fillColor(fee: Fee) = when (fee) {
        Fee.Free -> colors.free
        Fee.Paid -> colors.paid
        Fee.Unknown -> colors.unknown
    }

    operator fun get(key: String): MarkerImage = cache.getOrPut(key) { render(key) }

    /** Makes sure every key is registered in [style]; cheap when already there. */
    fun install(style: Style, keys: Iterable<String>) {
        if (installedIn !== style) {
            installedIn = style
            installed.clear()
        }
        for (k in keys) if (installed.add(k)) style.addImage(k, get(k).bitmap)
    }

    private fun render(key: String): MarkerImage {
        val parts = key.split("/")
        return if (parts[0] == "pin") {
            val fee = Fee.valueOf(parts[1])
            val selected = parts.size > 2
            val (fill, ink) = when (fee) {
                Fee.Free -> colors.free to colors.onFree
                Fee.Paid -> colors.paid to colors.onPaid
                Fee.Unknown -> colors.unknown to colors.onUnknown
            }
            if (selected) {
                pin(SELECTED_PIN_DP, fill.toArgb(), ink.toArgb(), colors.selectedRing.toArgb(), SELECTED_PIN_STROKE_DP)
            } else {
                pin(PIN_DP, fill.toArgb(), ink.toArgb(), colors.stroke.toArgb(), PIN_STROKE_DP)
            }
        } else {
            cluster(parts[1], parts[2].toFloat())
        }
    }

    private fun bitmap(w: Int, h: Int) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        density = context.resources.displayMetrics.densityDpi
    }

    private fun pin(heightDp: Float, fill: Int, ink: Int, outline: Int, strokeDp: Float): MarkerImage {
        val k = heightDp * density / PIN_UNITS // px per path unit
        val stroke = strokeDp * density
        val w = ceil(PIN_UNITS * k).toInt()
        // Crop to the shape (tip at 22/24, top at 2/24) plus the outline.
        val h = ceil((PIN_TIP_Y - 2f) * k + 2 * stroke).toInt()
        val bmp = bitmap(w, h)
        val canvas = Canvas(bmp)
        canvas.translate(0f, stroke - 2f * k)
        val path: Path = PathParser().parsePathString(PIN_PATH).toPath().asAndroidPath()
        canvas.scale(k, k)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Outline first, drawn centred on the edge at twice the width, then the fill covers the inner half.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2 * stroke / k
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = outline
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawPath(path, paint)
        // Motorcycle in the head.
        canvas.scale(1 / k, 1 / k)
        val glyph = ContextCompat.getDrawable(context, R.drawable.ic_motorcycle)!!.mutate()
        glyph.setTint(ink)
        val g = (heightDp * GLYPH_FRACTION * density).roundToInt()
        val cx = (PIN_UNITS / 2 * k).roundToInt()
        val cy = (PIN_HEAD_Y * k).roundToInt()
        glyph.setBounds(cx - g / 2, cy - g / 2, cx + g / 2, cy + g / 2)
        glyph.draw(canvas)
        // Anchor: the tip, which sits `stroke` above the bottom edge.
        return MarkerImage(bmp, w / 2f, h - stroke)
    }

    private fun cluster(label: String, radiusDp: Float): MarkerImage {
        val r = radiusDp * density
        val stroke = CLUSTER_STROKE_DP * density
        val side = ceil(2 * (r + stroke)).toInt()
        val bmp = bitmap(side, side)
        val canvas = Canvas(bmp)
        val c = side / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = colors.stroke.toArgb()
        canvas.drawCircle(c, c, r + stroke, paint)
        paint.color = colors.cluster.toArgb()
        canvas.drawCircle(c, c, r, paint)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.onCluster.toArgb()
            typeface = Typeface.DEFAULT_BOLD
            textSize = CLUSTER_TEXT_SP * density
            textAlign = Paint.Align.CENTER
        }
        val baseline = c - (text.descent() + text.ascent()) / 2
        canvas.drawText(label, c, baseline, text)
        return MarkerImage(bmp, c, c)
    }
}
