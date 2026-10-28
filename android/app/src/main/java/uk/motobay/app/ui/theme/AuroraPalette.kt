package uk.motobay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Fixed colours for the Explore screen chrome, from the "M3 Dynamic Aurora &
 * Telemetry" spec: a dark and a light palette, picked by the app theme. These
 * sit over the map as glass, so they do not follow the seed or wallpaper colour.
 */
@Immutable
data class AuroraPalette(
    val isDark: Boolean,
    val mapBackground: Color,
    val topVignetteStart: Color,
    val searchSurface: Color,
    val searchPlaceholder: Color,
    val searchIconIdle: Color,
    val searchIconActive: Color,
    val chipSelectedBg: Color,
    val chipSelectedBorder: Color,
    val chipSelectedText: Color,
    val chipUnselectedBg: Color,
    val chipUnselectedBorder: Color,
    val chipUnselectedText: Color,
    val chipBadgeSelectedBg: Color,
    val chipBadgeSelectedText: Color,
    val chipBadgeUnselectedBg: Color,
    val chipBadgeUnselectedText: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val cardTitleText: Color,
    val cardSubtext: Color,
    val segmentedContainerBg: Color,
    val segmentedContainerBorder: Color,
    val segmentedActiveBg: Color,
    val segmentedActiveText: Color,
    val segmentedInactiveText: Color,
    val locateFabBg: Color,
    val locateFabIcon: Color,
    val locateFabBorder: Color,
    val addFabBg: Color,
    val addFabIcon: Color,
    val addFabBorder: Color,
    val navBarBg: Color,
    val navBarBorder: Color,
    val navPillActiveBg: Color,
    val navIconActive: Color,
    val navTextActive: Color,
    val navTextInactive: Color,
    val auroraSweepColors: List<Color>,
    val outerAuraIdleAlpha: Float,
    val outerAuraBoostAlpha: Float,
) {
    companion object {
        /** Semantic status dots, shared by both modes. */
        val StatusFree = Color(0xFF22C55E)
        val StatusPaid = Color(0xFFF59E0B)
        val StatusUnknown = Color(0xFFEF4444)

        val Dark = AuroraPalette(
            isDark = true,
            mapBackground = Color(0xFF111315),
            topVignetteStart = Color(0xE60C0E12),
            searchSurface = Color(0xF21D2228),
            searchPlaceholder = Color(0xFFCBD5E1),
            searchIconIdle = Color(0xFFCBD5E1),
            searchIconActive = Color(0xFF38BDF8),
            chipSelectedBg = Color(0xFF4C617B),
            chipSelectedBorder = Color(0xFF93B4DB),
            chipSelectedText = Color(0xFFFFFFFF),
            chipUnselectedBg = Color(0xE614171C),
            chipUnselectedBorder = Color(0xFF475569),
            chipUnselectedText = Color(0xFFE2E8F0),
            chipBadgeSelectedBg = Color(0x40000000),
            chipBadgeSelectedText = Color(0xFFE0F2FE),
            chipBadgeUnselectedBg = Color(0xFF1E293B),
            chipBadgeUnselectedText = Color(0xFF94A3B8),
            cardSurface = Color(0xF2191E26),
            cardBorder = Color(0x1AFFFFFF),
            cardTitleText = Color(0xFFFFFFFF),
            cardSubtext = Color(0xFF94A3B8),
            segmentedContainerBg = Color(0xF21B1F26),
            segmentedContainerBorder = Color(0xFF475569),
            segmentedActiveBg = Color(0xFF3A495C),
            segmentedActiveText = Color(0xFFFFFFFF),
            segmentedInactiveText = Color(0xFFCBD5E1),
            locateFabBg = Color(0xF21D222A),
            locateFabIcon = Color(0xFFE2E8F0),
            locateFabBorder = Color(0x1AFFFFFF),
            addFabBg = Color(0xFF4C617B),
            addFabIcon = Color(0xFFFFFFFF),
            addFabBorder = Color(0x6693B4DB),
            navBarBg = Color(0xFF181B20),
            navBarBorder = Color(0x1AFFFFFF),
            navPillActiveBg = Color(0xFF3F4E62),
            navIconActive = Color(0xFFFFFFFF),
            navTextActive = Color(0xFFFFFFFF),
            navTextInactive = Color(0xFF94A3B8),
            auroraSweepColors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color(0xFF38BDF8), // M3 Expressive Primary Sky
                Color(0xFFC084FC), // M3 Tertiary Iris Violet
                Color(0xFF2DD4BF), // M3 Secondary Teal
                Color(0xFF818CF8), // M3 Expressive Indigo
                Color.Transparent,
            ),
            outerAuraIdleAlpha = 0.28f,
            outerAuraBoostAlpha = 0.55f,
        )

        val Light = AuroraPalette(
            isDark = false,
            mapBackground = Color(0xFFEBF0F5),
            topVignetteStart = Color(0xD9F8FAFC),
            searchSurface = Color(0xF2FFFFFF),
            searchPlaceholder = Color(0xFF334155),
            searchIconIdle = Color(0xFF475569),
            searchIconActive = Color(0xFF0284C7),
            chipSelectedBg = Color(0xFF3B526E),
            chipSelectedBorder = Color(0xFF1E293B),
            chipSelectedText = Color(0xFFFFFFFF),
            chipUnselectedBg = Color(0xEBFFFFFF),
            chipUnselectedBorder = Color(0xFFCBD5E1),
            chipUnselectedText = Color(0xFF1E293B),
            chipBadgeSelectedBg = Color(0x33FFFFFF),
            chipBadgeSelectedText = Color(0xFFFFFFFF),
            chipBadgeUnselectedBg = Color(0xFFF1F5F9),
            chipBadgeUnselectedText = Color(0xFF475569),
            cardSurface = Color(0xF2FFFFFF),
            cardBorder = Color(0xFFCBD5E1),
            cardTitleText = Color(0xFF0F172A),
            cardSubtext = Color(0xFF475569),
            segmentedContainerBg = Color(0xF2FFFFFF),
            segmentedContainerBorder = Color(0xFFCBD5E1),
            segmentedActiveBg = Color(0xFF3B526E),
            segmentedActiveText = Color(0xFFFFFFFF),
            segmentedInactiveText = Color(0xFF475569),
            locateFabBg = Color(0xF2FFFFFF),
            locateFabIcon = Color(0xFF1E293B),
            locateFabBorder = Color(0xFFCBD5E1),
            addFabBg = Color(0xFF3B526E),
            addFabIcon = Color(0xFFFFFFFF),
            addFabBorder = Color(0x8093B4DB),
            navBarBg = Color(0xFFFFFFFF),
            navBarBorder = Color(0xFFE2E8F0),
            navPillActiveBg = Color(0xFFDCE7F5),
            navIconActive = Color(0xFF1E3A5F),
            navTextActive = Color(0xFF0F172A),
            navTextInactive = Color(0xFF64748B),
            auroraSweepColors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color(0xFF0284C7), // Vivid Daylight Sky
                Color(0xFF9333EA), // Vivid Daylight Iris Purple
                Color(0xFF0D9488), // Deep Daylight Teal
                Color(0xFF4F46E5), // Royal Indigo
                Color.Transparent,
            ),
            outerAuraIdleAlpha = 0.35f,
            outerAuraBoostAlpha = 0.65f,
        )
    }
}

/** The palette for the current theme: dark when the Material surfaces are dark. */
@Composable
fun auroraPalette(): AuroraPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) AuroraPalette.Dark else AuroraPalette.Light
