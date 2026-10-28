package uk.motobay.app.ui.explore

import androidx.compose.ui.graphics.Color

/**
 * Design tokens from the "M3 Dynamic Aurora & Telemetry" spec. These are fixed
 * colours, on purpose: the search bar and filter chips sit over the map as dark
 * glass whatever the app theme, like the spec's mock.
 */
object AuroraTokens {
    /** Search bar and telemetry pill surface: #1D2228 at 95%. */
    val SearchSurface = Color(0xF21D2228)
    val SearchText = Color(0xFFE2E8F0)
    val SearchPlaceholder = Color(0xFFCBD5E1)
    val Sky = Color(0xFF38BDF8)
    val Iris = Color(0xFFC084FC)

    val ChipSelectedFill = Color(0xFF4C617B)
    val ChipSelectedBorder = Color(0xFF93B4DB)
    val ChipUnselectedFill = Color(0xE614171C)
    val ChipUnselectedBorder = Color(0xFF475569)
    val ChipLabel = Color.White

    val BadgeSelectedText = Color(0xFFE0F2FE)
    val BadgeUnselectedText = Color(0xFF94A3B8)
    val BadgeSelectedFill = Color(0x40000000)
    val BadgeUnselectedFill = Color(0xFF1E293B)

    val TelemetryText = Color(0xFFE2E8F0)
    val TelemetryBorder = Color(0x7338BDF8)

    val DotFree = Color(0xFF22C55E)
    val DotPaid = Color(0xFFF59E0B)
    val DotUnknown = Color(0xFFEF4444)
}
