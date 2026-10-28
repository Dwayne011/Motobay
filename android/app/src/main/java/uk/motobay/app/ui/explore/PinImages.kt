package uk.motobay.app.ui.explore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import org.maplibre.android.maps.Style
import uk.motobay.app.data.Fee
import kotlin.math.ceil
import kotlin.math.max

/** Marker pill height in dp; the selected pill is this scaled 1.12x by the map. */
const val MARKER_DP = 32f
private const val MARKER_RADIUS_DP = 16f
private const val MARKER_MIN_WIDTH_DP = 44f
private const val MARKER_PAD_DP = 10f
private const val MARKER_BORDER_DP = 1.5f
private const val DOT_DP = 8f
private const val DOT_GAP_DP = 6f
private const val COUNT_SP = 12f

fun abbreviateCount(n: Int): String = if (n >= 1_000) "${n / 1_000}k" else n.toString()

/** The short tag a bay's pill shows: its price or "?" when the rules are unknown. */
fun Fee.tag(): String = when (this) {
    Fee.Free -> "Free"
    Fee.Paid -> "Paid"
    Fee.Unknown -> "?"
}

/**
 * A rendered marker: the bitmap MapLibre draws, and where its anchor point
 * sits inside it so the Compose overlay can draw it at exactly the same spot.
 */
class MarkerImage(val bitmap: Bitmap, val anchorX: Float, val anchorY: Float) {
    val image: ImageBitmap by lazy { bitmap.asImageBitmap() }
}

/**
 * Pre-rendered, anti-aliased marker bitmaps: the spec's 32dp capsule pills.
 * A bay's pill shows its status dot and price tag; a cluster's pill shows how
 * many bays it holds. The selected bay's pill takes its status colour as the
 * border. Bitmaps are rendered at the screen's density and tagged with it, so
 * MapLibre sizes them in dp and the overlay can blit them 1:1.
 */
class PinImages(private val context: Context, private val colors: PinColors) {
    private val density = context.resources.displayMetrics.density
    private val cache = HashMap<String, MarkerImage>()
    private var installedIn: Style? = null
    private val installed = HashSet<String>()

    fun pinKey(fee: Fee, selected: Boolean = false) = "pin/$fee" + if (selected) "/selected" else ""
    fun clusterKey(count: Int) = "cluster/${abbreviateCount(count)}"
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
            pill(
                label = fee.tag(),
                dot = fillColor(fee).toArgb(),
                fill = (if (selected) colors.selectedFill else colors.pillFill).toArgb(),
                border = (if (selected) fillColor(fee) else colors.pillBorder).toArgb(),
                ink = colors.pillText.toArgb(),
            )
        } else {
            pill(label = parts[1], dot = null, fill = colors.pillFill.toArgb(), border = colors.pillBorder.toArgb(), ink = colors.pillText.toArgb())
        }
    }

    private fun bitmap(w: Int, h: Int) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        density = context.resources.displayMetrics.densityDpi
    }

    private fun pill(label: String, dot: Int?, fill: Int, border: Int, ink: Int): MarkerImage {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            typeface = Typeface.DEFAULT_BOLD
            textSize = COUNT_SP * density
        }
        val stroke = MARKER_BORDER_DP * density
        val dotW = if (dot != null) (DOT_DP + DOT_GAP_DP) * density else 0f
        val content = dotW + text.measureText(label)
        val w = ceil(max(MARKER_MIN_WIDTH_DP * density, content + 2 * MARKER_PAD_DP * density) + 2 * stroke).toInt()
        val h = ceil(MARKER_DP * density + 2 * stroke).toInt()
        val bmp = bitmap(w, h)
        val canvas = Canvas(bmp)
        val r = MARKER_RADIUS_DP * density + stroke
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Border first, then the fill inset by the border width.
        paint.color = border
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, paint)
        paint.color = fill
        canvas.drawRoundRect(RectF(stroke, stroke, w - stroke, h - stroke), r - stroke, r - stroke, paint)
        val cy = h / 2f
        var x = (w - content) / 2f
        if (dot != null) {
            paint.color = dot
            canvas.drawCircle(x + DOT_DP * density / 2, cy, DOT_DP * density / 2, paint)
            x += dotW
        }
        val baseline = cy - (text.descent() + text.ascent()) / 2
        canvas.drawText(label, x, baseline, text)
        return MarkerImage(bmp, w / 2f, h / 2f)
    }
}
