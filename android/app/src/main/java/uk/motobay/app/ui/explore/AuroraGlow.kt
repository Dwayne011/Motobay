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
import uk.motobay.app.ui.theme.AuroraPalette

/** Where each of the palette's seven sweep colours sits round the capsule. */
private val AuroraPositions = floatArrayOf(0.00f, 0.15f, 0.36f, 0.58f, 0.80f, 0.95f, 1.00f)

/** A sweep gradient turned by [angle] degrees about the capsule's centre, so only the beam moves, not the capsule. */
private class RotatedSweep(private val colors: List<Color>, private val angle: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width / 2f
        val cy = size.height / 2f
        return android.graphics.SweepGradient(
            cx, cy,
            IntArray(colors.size) { colors[it].toArgb() },
            AuroraPositions,
        ).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setRotate(angle, cx, cy) })
        }
    }
}

private const val IDLE_ORBIT_MS = 3200
private const val BOOSTED_ORBIT_MS = 1350

/**
 * A beam of colour travelling round the edge of a capsule, as a soft bloom plus
 * a crisp trace, in the [palette]'s colours. [boosted] (map moving, search
 * focused) spins it 2.4x faster and brightens the bloom. [visible] false draws nothing, e.g. while the search bar
 * is expanded to full screen and no longer a capsule. Previews and screenshot
 * tests get a still frame.
 */
fun Modifier.auroraGlow(
    palette: AuroraPalette,
    boosted: Boolean,
    visible: Boolean = true,
    strokeWidth: Dp = 2.dp,
): Modifier = composed {
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
    val bloomAlpha = if (boosted) palette.outerAuraBoostAlpha else palette.outerAuraIdleAlpha
    drawWithCache {
        val stroke = strokeWidth.toPx()
        val bloom = stroke * 3f
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        val sweep = RotatedSweep(palette.auroraSweepColors, angle)
        onDrawWithContent {
            drawRoundRect(brush = sweep, cornerRadius = radius, style = Stroke(width = bloom), alpha = bloomAlpha)
            drawRoundRect(brush = sweep, cornerRadius = radius, style = Stroke(width = stroke))
            drawContent()
        }
    }
}
