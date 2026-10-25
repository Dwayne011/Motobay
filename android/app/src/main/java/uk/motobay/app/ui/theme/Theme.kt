package uk.motobay.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.harmonize
import uk.motobay.app.data.Settings
import uk.motobay.app.data.ThemeMode

/**
 * M3 "custom colour" roles the standard ColorScheme doesn't have. Used for
 * free bays. The green is harmonised towards the primary colour, as M3
 * recommends, so it sits well with any source colour.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

private val SuccessSeed = Color(0xFF2E7D32)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/**
 * Material 3 theme.
 *
 * Colour: every role (primary, secondary, tertiary, surfaces) is generated
 * from one source colour with the M3 "tonal spot" scheme. Dark mode uses the
 * inverted tones of the same palettes. On Android 12+ riders can opt into
 * wallpaper-based dynamic colour instead.
 *
 * Elevation: Compose M3 components use tonal elevation (surface blended with
 * the primary tint via `tonalElevation` and the surfaceContainer roles), so no
 * shadows are added for standard depth.
 *
 * Typography: the default M3 type scale (Display, Headline, Title, Body,
 * Label in Large/Medium/Small).
 */
@Composable
fun MotoBayTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(settings.themeMode)
    val context = LocalContext.current
    val useWallpaper = settings.wallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val scheme: ColorScheme = when {
        useWallpaper && dark -> dynamicDarkColorScheme(context)
        useWallpaper -> dynamicLightColorScheme(context)
        else -> remember(settings.sourceColor, dark) {
            dynamicColorScheme(seedColor = Color(settings.sourceColor), isDark = dark, style = PaletteStyle.TonalSpot)
        }
    }

    val extended = remember(scheme.primary, dark) {
        val success = dynamicColorScheme(
            seedColor = SuccessSeed.harmonize(scheme.primary),
            isDark = dark,
            style = PaletteStyle.TonalSpot,
        )
        ExtendedColors(success.primary, success.onPrimary, success.primaryContainer, success.onPrimaryContainer)
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}

object MotoBayColors {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
