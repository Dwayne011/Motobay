package uk.motobay.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Google Sans where the phone has it (Pixel), the system sans elsewhere. */
val FrostFont: FontFamily = runCatching {
    FontFamily(
        Font(DeviceFontFamilyName("google-sans"), FontWeight.Normal),
        Font(DeviceFontFamilyName("google-sans"), FontWeight.Medium),
        Font(DeviceFontFamilyName("google-sans-medium"), FontWeight.SemiBold),
        Font(DeviceFontFamilyName("google-sans-medium"), FontWeight.Bold),
        Font(DeviceFontFamilyName("google-sans-bold"), FontWeight.ExtraBold),
    )
}.getOrDefault(FontFamily.Default)

/** The M3 type scale set in [FrostFont], so dialogs and sheets match the rest of the app. */
fun frostTypography(): Typography {
    val base = Typography()
    fun TextStyle.frost() = copy(fontFamily = FrostFont)
    return Typography(
        displayLarge = base.displayLarge.frost(), displayMedium = base.displayMedium.frost(), displaySmall = base.displaySmall.frost(),
        headlineLarge = base.headlineLarge.frost(), headlineMedium = base.headlineMedium.frost(), headlineSmall = base.headlineSmall.frost(),
        titleLarge = base.titleLarge.frost(), titleMedium = base.titleMedium.frost(), titleSmall = base.titleSmall.frost(),
        bodyLarge = base.bodyLarge.frost(), bodyMedium = base.bodyMedium.frost(), bodySmall = base.bodySmall.frost(),
        labelLarge = base.labelLarge.frost(), labelMedium = base.labelMedium.frost(), labelSmall = base.labelSmall.frost(),
    )
}

/**
 * The backdrop behind the content screens: the base colour with soft pools of
 * periwinkle, teal, amber and violet, which the glass panels pick up.
 */
fun Modifier.frostBackdrop(palette: AuroraPalette): Modifier = drawBehind {
    drawRect(palette.mapBackground)
    palette.pools.forEach { pool ->
        val centre = Offset(size.width * pool.x, size.height * pool.y)
        val radius = size.width * pool.radius
        drawCircle(
            brush = Brush.radialGradient(listOf(pool.color, pool.color.copy(alpha = 0f)), center = centre, radius = radius),
            radius = radius,
            center = centre,
        )
    }
}

/** A panel's 1dp edge: [AuroraPalette.hi] along the top fading into the hairline border (the same colour unless a highlight is set). */
fun frostEdge(palette: AuroraPalette, border: Color = palette.cardBorder): BorderStroke =
    BorderStroke(1.dp, Brush.verticalGradient(0f to palette.hi, 0.35f to border, 1f to border))
