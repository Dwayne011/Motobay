package uk.motobay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Fixed colours for the app's chrome, in the Frost design language: frosted glass
 * panels with a light top edge, a periwinkle accent, and soft colour pools behind
 * the content screens. A dark and a light palette, picked by the app theme. They
 * do not follow the seed or wallpaper colour.
 *
 * The older field names are kept so every screen picks the new look up at once:
 * [searchIconActive] is the accent, [cardSubtext] the secondary text, and the
 * search, chip, button and nav fills are the denser glass that sits over the map.
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
    // Frost additions.
    /** The accent: selected chips, primary buttons, links and section labels. */
    val accent: Color,
    /** Ink on the accent. */
    val onAccent: Color,
    /** Body text one step softer than the titles. */
    val text2: Color,
    /** Hints, counts and inactive icons. */
    val muted: Color,
    /** Small filled tiles inside a panel (icon wells, round buttons, the rating chip). */
    val tile: Color,
    /** Hairlines between rows inside a panel. */
    val divider: Color,
    /** The light top edge of a glass panel. */
    val hi: Color,
    /** Selected state: a wash of the accent, with [selectedBorder]. */
    val selected: Color,
    val selectedBorder: Color,
    /** Status colours for dots, tuned for the glass. */
    val free: Color,
    val paid: Color,
    val unknown: Color,
    /** Tinted icon wells: (fill, ink) for free, paid and other. */
    val toneFree: Pair<Color, Color>,
    val tonePaid: Pair<Color, Color>,
    val toneBlue: Pair<Color, Color>,
    val toneViolet: Pair<Color, Color>,
    /** The colour pools behind the content screens: (colour, x, y, radius as a fraction of the width). */
    val pools: List<Pool>,
) {
    @Immutable
    data class Pool(val color: Color, val x: Float, val y: Float, val radius: Float)

    companion object {
        /** Saturated status colours for the map pins, shared by both modes. */
        val StatusFree = Color(0xFF22C55E)
        val StatusPaid = Color(0xFFF59E0B)
        val StatusUnknown = Color(0xFFEF4444)

        private val DarkGlass = Color(0xB8181D2E) // over the map: 72%
        private val DarkPanel = Color(0x94181D2E) // over the colour pools: 58%
        private val DarkBorder = Color(0x1AFFFFFF)
        private val DarkAccent = Color(0xFFC9D8FF)
        private val DarkOnAccent = Color(0xFF0E1A3A)

        val Dark = AuroraPalette(
            isDark = true,
            mapBackground = Color(0xFF0A0E18),
            topVignetteStart = Color(0x990A0E18),
            searchSurface = DarkGlass,
            searchPlaceholder = Color(0xFF8E97AB),
            searchIconIdle = Color(0xFF8E97AB),
            searchIconActive = DarkAccent,
            chipSelectedBg = DarkAccent,
            chipSelectedBorder = DarkAccent,
            chipSelectedText = DarkOnAccent,
            chipUnselectedBg = DarkGlass,
            chipUnselectedBorder = DarkBorder,
            chipUnselectedText = Color(0xFFF2F4F8),
            chipBadgeSelectedBg = Color(0x1F0E1A3A),
            chipBadgeSelectedText = DarkOnAccent,
            chipBadgeUnselectedBg = Color(0x1AFFFFFF),
            chipBadgeUnselectedText = Color(0xFF8E97AB),
            cardSurface = DarkPanel,
            cardBorder = DarkBorder,
            cardTitleText = Color(0xFFF2F4F8),
            cardSubtext = Color(0xFFB4BCCC),
            segmentedContainerBg = DarkGlass,
            segmentedContainerBorder = DarkBorder,
            segmentedActiveBg = DarkAccent,
            segmentedActiveText = DarkOnAccent,
            segmentedInactiveText = Color(0xFFB4BCCC),
            locateFabBg = DarkGlass,
            locateFabIcon = Color(0xFFDDE4F5),
            locateFabBorder = DarkBorder,
            addFabBg = DarkAccent,
            addFabIcon = DarkOnAccent,
            addFabBorder = Color.Transparent,
            navBarBg = DarkGlass,
            navBarBorder = DarkBorder,
            navPillActiveBg = Color(0x2EC9D8FF),
            navIconActive = DarkAccent,
            navTextActive = DarkAccent,
            navTextInactive = Color(0xFF8E97AB),
            auroraSweepColors = listOf(Color.Transparent, Color.Transparent),
            outerAuraIdleAlpha = 0f,
            outerAuraBoostAlpha = 0f,
            accent = DarkAccent,
            onAccent = DarkOnAccent,
            text2 = Color(0xFFDDE4F5),
            muted = Color(0xFF8E97AB),
            tile = Color(0x1AFFFFFF),
            divider = Color(0x0FFFFFFF),
            hi = Color(0x1FFFFFFF),
            selected = Color(0x2EC9D8FF),
            selectedBorder = Color(0x66C9D8FF),
            free = Color(0xFF8FE3B5),
            paid = Color(0xFFFFD08A),
            unknown = Color(0xFFFFA3AE),
            toneFree = Color(0x29C8F0D8) to Color(0xFFC8F0D8),
            tonePaid = Color(0x29FFE2C2) to Color(0xFFFFE2C2),
            toneBlue = Color(0x29C9D8FF) to Color(0xFFC9D8FF),
            toneViolet = Color(0x29E2D4FF) to Color(0xFFE2D4FF),
            pools = listOf(
                Pool(Color(0x616E82FF), 0.85f, 0.04f, 0.78f), // periwinkle, top right
                Pool(Color(0x383CBEAA), 0.04f, 0.40f, 0.52f), // teal, left
                Pool(Color(0x2EFFBE78), 0.96f, 0.70f, 0.54f), // amber, right
                Pool(Color(0x47A06EFF), 0.20f, 0.98f, 0.72f), // violet, bottom left
            ),
        )

        private val LightGlass = Color(0xCCFBFCFE) // over the map: 80%
        private val LightPanel = Color(0x9EFBFCFE) // over the colour pools: 62%
        private val LightBorder = Color(0x140B1A3A)
        private val LightAccent = Color(0xFF3B5B92)

        val Light = AuroraPalette(
            isDark = false,
            mapBackground = Color(0xFFE8ECF5),
            topVignetteStart = Color(0x99E8ECF5),
            searchSurface = LightGlass,
            searchPlaceholder = Color(0xFF6B7385),
            searchIconIdle = Color(0xFF6B7385),
            searchIconActive = LightAccent,
            chipSelectedBg = LightAccent,
            chipSelectedBorder = LightAccent,
            chipSelectedText = Color.White,
            chipUnselectedBg = LightGlass,
            chipUnselectedBorder = LightBorder,
            chipUnselectedText = Color(0xFF141925),
            chipBadgeSelectedBg = Color(0x33FFFFFF),
            chipBadgeSelectedText = Color.White,
            chipBadgeUnselectedBg = Color(0x121B2A4A),
            chipBadgeUnselectedText = Color(0xFF6B7385),
            cardSurface = LightPanel,
            cardBorder = LightBorder,
            cardTitleText = Color(0xFF141925),
            cardSubtext = Color(0xFF545C6C),
            segmentedContainerBg = LightGlass,
            segmentedContainerBorder = LightBorder,
            segmentedActiveBg = LightAccent,
            segmentedActiveText = Color.White,
            segmentedInactiveText = Color(0xFF545C6C),
            locateFabBg = LightGlass,
            locateFabIcon = Color(0xFF2D3546),
            locateFabBorder = LightBorder,
            addFabBg = LightAccent,
            addFabIcon = Color.White,
            addFabBorder = Color.Transparent,
            navBarBg = LightGlass,
            navBarBorder = LightBorder,
            navPillActiveBg = Color(0x1F3B5B92),
            navIconActive = LightAccent,
            navTextActive = LightAccent,
            navTextInactive = Color(0xFF6B7385),
            auroraSweepColors = listOf(Color.Transparent, Color.Transparent),
            outerAuraIdleAlpha = 0f,
            outerAuraBoostAlpha = 0f,
            accent = LightAccent,
            onAccent = Color.White,
            text2 = Color(0xFF2D3546),
            muted = Color(0xFF6B7385),
            tile = Color(0x121B2A4A),
            divider = Color(0x0F0B1A3A),
            hi = Color(0xD9FFFFFF),
            selected = Color(0x1F3B5B92),
            selectedBorder = Color(0x593B5B92),
            free = Color(0xFF1F7A47),
            paid = Color(0xFF9A5300),
            unknown = Color(0xFFB3261E),
            toneFree = Color(0x241F7A47) to Color(0xFF1F7A47),
            tonePaid = Color(0x249A5300) to Color(0xFF9A5300),
            toneBlue = Color(0x1F3B5B92) to Color(0xFF3B5B92),
            toneViolet = Color(0x1F6A4FB0) to Color(0xFF6A4FB0),
            pools = listOf(
                Pool(Color(0x476E82FF), 0.85f, 0.04f, 0.78f),
                Pool(Color(0x2E3CBEAA), 0.04f, 0.40f, 0.52f),
                Pool(Color(0x2EFFB46E), 0.96f, 0.70f, 0.54f),
                Pool(Color(0x38A06EFF), 0.20f, 0.98f, 0.72f),
            ),
        )
    }
}

/** The palette for the current theme: dark when the Material surfaces are dark. */
@Composable
fun auroraPalette(): AuroraPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) AuroraPalette.Dark else AuroraPalette.Light
