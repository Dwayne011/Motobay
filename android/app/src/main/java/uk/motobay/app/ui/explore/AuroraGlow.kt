package uk.motobay.app.ui.explore

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The "M3 Dynamic Aurora" sweep: sky, iris violet, teal, indigo, with a clear gap so the beam reads as travelling. */
private val AuroraStops = arrayOf(
    0.00f to Color.Transparent,
    0.15f to Color.Transparent,
    0.36f to Color(0xFF38BDF8), // primary sky
    0.58f to Color(0xFFC084FC), // tertiary iris violet
    0.80f to Color(0xFF2DD4BF), // secondary teal
    0.95f to Color(0xFF818CF8), // expressive indigo
    1.00f to Color.Transparent,
)

/** A sweep gradient turned by [angle] degrees about the capsule's centre, so only the beam moves, not the capsule. */
private class RotatedSweep(private val angle: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width / 2f
        val cy = size.height / 2f
        return android.graphics.SweepGradient(
            cx, cy,
            IntArray(AuroraStops.size) { AuroraStops[it].second.toArgb() },
            FloatArray(AuroraStops.size) { AuroraStops[it].first },
        ).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setRotate(angle, cx, cy) })
        }
    }
}

private const val IDLE_ORBIT_MS = 3200
private const val BOOSTED_ORBIT_MS = 1350

/**
 * A beam of colour travelling round the edge of a capsule, as a soft bloom plus
 * a crisp trace. [boosted] (map moving, search focused) spins it 2.4x faster and
 * brightens the bloom. [visible] false draws nothing, e.g. while the search bar
 * is expanded to full screen and no longer a capsule. Previews and screenshot
 * tests get a still frame.
 */
fun Modifier.auroraGlow(boosted: Boolean, visible: Boolean = true, strokeWidth: Dp = 2.dp): Modifier = composed {
    if (!visible) return@composed this
    val still = LocalInspectionMode.current
    val angle: Float = if (still) {
        200f
    } else {
        val transition = rememberInfiniteTransition(label = "AuroraPerimeterGlow")
        val a by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (boosted) BOOSTED_ORBIT_MS else IDLE_ORBIT_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "AuroraSweepAngle",
        )
        a
    }
    val bloomAlpha = if (boosted) 0.48f else 0.25f
    drawWithCache {
        val stroke = strokeWidth.toPx()
        val bloom = stroke * 3.2f
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        val sweep = RotatedSweep(angle)
        onDrawWithContent {
            drawRoundRect(brush = sweep, cornerRadius = radius, style = Stroke(width = bloom), alpha = bloomAlpha)
            drawRoundRect(brush = sweep, cornerRadius = radius, style = Stroke(width = stroke))
            drawContent()
        }
    }
}
