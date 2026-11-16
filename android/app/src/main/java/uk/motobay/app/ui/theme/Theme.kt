package uk.motobay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.harmonize
import uk.motobay.app.data.Settings
import uk.motobay.app.data.ThemeMode

/**
 * M3 "custom colour" roles the standard ColorScheme doesn't have: green for
 * free bays and amber for paid ones. Both are harmonised towards the primary
 * colour, as M3 recommends, so they sit well with any source colour.
 */
@Immutable
data class ExtendedColors(
    val success: Color = Color.Unspecified,
    val onSuccess: Color = Color.Unspecified,
    val successContainer: Color = Color.Unspecified,
    val onSuccessContainer: Color = Color.Unspecified,
    val warning: Color = Color.Unspecified,
    val onWarning: Color = Color.Unspecified,
    val warningContainer: Color = Color.Unspecified,
    val onWarningContainer: Color = Color.Unspecified,
    /** Map pin fills. Deliberately saturated, the same in light and dark, so bays stand out from place icons. */
    val pinFree: Color = Color.Unspecified,
    val onPinFree: Color = Color.Unspecified,
    val pinPaid: Color = Color.Unspecified,
    val onPinPaid: Color = Color.Unspecified,
    val pinUnknown: Color = Color.Unspecified,
    val onPinUnknown: Color = Color.Unspecified,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

private val SuccessSeed = Color(0xFF2E7D32)
private val WarningSeed = Color(0xFFF9A825)
private val PinRaspberry = Color(0xFFD81B60)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/** The Frost accent, the seed for the Material colour scheme. */
private val FrostSeed = Color(0xFF3B5B92)

/**
 * Material 3 theme.
 *
 * Colour: the app's own screens use the fixed Frost palette (see AuroraPalette).
 * Material parts such as dialogs, sheets and snackbars use a "tonal spot" scheme
 * generated from the Frost accent, so they match. Dark mode uses the inverted
 * tones of the same palettes.
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
    // The Frost look has fixed colours, so the Material scheme (dialogs, sheets, snackbars)
    // comes from the Frost accent rather than a chosen colour or the wallpaper.
    val scheme: ColorScheme = remember(dark) {
        dynamicColorScheme(seedColor = FrostSeed, isDark = dark, style = PaletteStyle.TonalSpot)
    }

    val extended = remember(scheme.primary, dark) {
        fun custom(seed: Color) = dynamicColorScheme(
            seedColor = seed.harmonize(scheme.primary),
            isDark = dark,
            style = PaletteStyle.TonalSpot,
        )
        val success = custom(SuccessSeed)
        val warning = custom(WarningSeed)
        // Light-scheme primaries are the saturated mid tones (tone 40); the amber pin uses
        // the light tone (80) with dark ink, since dark amber reads as brown.
        fun strong(seed: Color, lightTone: Boolean) = dynamicColorScheme(
            seedColor = seed.harmonize(scheme.primary), isDark = lightTone, style = PaletteStyle.TonalSpot,
        ).primary
        ExtendedColors(
            success.primary, success.onPrimary, success.primaryContainer, success.onPrimaryContainer,
            warning.primary, warning.onPrimary, warning.primaryContainer, warning.onPrimaryContainer,
            pinFree = strong(SuccessSeed, lightTone = false), onPinFree = Color.White,
            pinPaid = strong(WarningSeed, lightTone = true), onPinPaid = Color(0xFF3A2A00),
            // Not derived from the brand on purpose: the map provider colours transit and
            // EV-charger icons blue, so a blue pin blends in whatever the brand colour is.
            pinUnknown = PinRaspberry, onPinUnknown = Color.White,
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = scheme, typography = frostTypography(), content = content)
    }
}

object MotoBayColors {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
