package uk.motobay.app.ui.components

import androidx.compose.foundation.BorderStroke
import java.util.Locale
import uk.motobay.app.data.RatingSummary
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.motobay.app.ui.theme.AuroraPalette

/** Tailwind's slate-950, the ink the spec puts on sky-blue surfaces. */
val Slate950 = Color(0xFF020617)
val Coral = Color(0xFFEF4444)

/** A text style of exactly [size] with a comfortable line height, as the spec sets every label by size and weight. */
fun auroraStyle(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified, mono: Boolean = false): TextStyle =
    TextStyle(fontSize = size, lineHeight = size * 1.4f, fontWeight = weight, color = color, fontFamily = if (mono) FontFamily.Monospace else null)

/** The 20sp bold page title the three tabs start with. */
@Composable
fun AuroraHeader(text: String, palette: AuroraPalette, modifier: Modifier = Modifier) {
    Text(text, style = auroraStyle(20.sp, FontWeight.Bold, palette.cardTitleText), modifier = modifier)
}

/** 11sp bold uppercase section label in the accent colour. */
@Composable
fun AuroraSectionHeader(text: String, palette: AuroraPalette, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = auroraStyle(11.sp, FontWeight.Bold, palette.searchIconActive), modifier = modifier)
}

/** The spec's card: rounded, glass surface, 1dp border, optional shadow. */
@Composable
fun AuroraCard(
    palette: AuroraPalette,
    modifier: Modifier = Modifier,
    radius: Int = 18,
    elevation: Int = 0,
    color: Color = palette.cardSurface,
    border: Color = palette.cardBorder,
    onClick: (() -> Unit)? = null,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius.dp)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, border = BorderStroke(1.dp, border), shadowElevation = elevation.dp, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(contentPadding.dp), content = content)
        }
    } else {
        Surface(shape = shape, color = color, border = BorderStroke(1.dp, border), shadowElevation = elevation.dp, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(contentPadding.dp), content = content)
        }
    }
}

/**
 * A 36dp pill of equal segments: the active one is sky blue with slate ink,
 * the rest show muted text. [badge] draws a 16dp red count on a segment.
 */
@Composable
fun AuroraPillTabs(
    labels: List<String>,
    selected: Int,
    palette: AuroraPalette,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badge: (Int) -> Int = { 0 },
    segmentModifier: (Int) -> Modifier = { Modifier },
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.searchSurface,
        border = BorderStroke(1.dp, palette.segmentedContainerBorder),
        modifier = modifier.fillMaxWidth().heightIn(min = 36.dp),
    ) {
        Row(Modifier.padding(2.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { i, label ->
                val active = i == selected
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) palette.searchIconActive else Color.Transparent)
                        .clickable(onClick = { onSelect(i) }, role = Role.Tab)
                        .semantics { this.selected = active }
                        .then(segmentModifier(i))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = auroraStyle(13.sp, if (active) FontWeight.Bold else FontWeight.Medium, if (active) Slate950 else palette.cardSubtext),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val n = badge(i)
                    if (n > 0) {
                        Box(Modifier.size(16.dp).background(Coral, CircleShape), contentAlignment = Alignment.Center) {
                            Text("$n", style = auroraStyle(9.sp, FontWeight.Bold, Color.White), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

/** A 32dp status chip: filled slate when active, outlined otherwise. */
@Composable
fun AuroraChip(label: String, active: Boolean, palette: AuroraPalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (active) palette.segmentedActiveBg else Color.Transparent,
        border = BorderStroke(1.dp, if (active) palette.segmentedActiveBg else palette.cardBorder),
        modifier = modifier.heightIn(min = 32.dp).semantics { selected = active },
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(label, style = auroraStyle(12.sp, FontWeight.Medium, if (active) Color.White else palette.cardSubtext), maxLines = 1, softWrap = false)
        }
    }
}

/** 36dp filled action button: accent background, slate ink, 12sp bold. */
@Composable
fun AuroraPrimaryButton(text: String, palette: AuroraPalette, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val bg = if (palette.isDark) Color(0xFF93B4DB) else palette.searchIconActive
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) bg else bg.copy(alpha = 0.4f),
        modifier = modifier.heightIn(min = 36.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = Slate950, modifier = Modifier.size(14.dp))
            Text(text, style = auroraStyle(12.sp, FontWeight.Bold, Slate950), maxLines = 1, softWrap = false)
        }
    }
}

/** 36dp outlined action button: 1dp border, 12sp semibold. */
@Composable
fun AuroraOutlinedButton(text: String, palette: AuroraPalette, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = palette.cardTitleText) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = modifier.heightIn(min = 36.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(text, style = auroraStyle(12.sp, FontWeight.SemiBold, if (enabled) color else color.copy(alpha = 0.4f)), maxLines = 1, softWrap = false)
        }
    }
}

/** A plain text action, e.g. the red "Sign out". */
@Composable
fun AuroraTextAction(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(
        text,
        style = auroraStyle(12.sp, FontWeight.Bold, if (enabled) color else color.copy(alpha = 0.4f)),
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick, role = Role.Button).padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Small capsule tag, e.g. the price on a saved bay. */
@Composable
fun AuroraTag(text: String, palette: AuroraPalette, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(palette.chipUnselectedBg).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, style = auroraStyle(11.sp, FontWeight.Medium, palette.chipUnselectedText), maxLines = 1, softWrap = false)
    }
}

/** Title and subtext with a 16dp chevron, used for the Settings rows. */
@Composable
fun RowScope.AuroraRowText(title: String, subtitle: String?, palette: AuroraPalette, titleColor: Color = palette.cardTitleText) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = auroraStyle(13.sp, FontWeight.SemiBold, titleColor))
        if (subtitle != null) Text(subtitle, style = auroraStyle(10.sp, FontWeight.Normal, palette.cardSubtext))
    }
}

@Composable
fun VSpace(dp: Int) = Spacer(Modifier.height(dp.dp))

@Composable
fun HSpace(dp: Int) = Spacer(Modifier.width(dp.dp))

/**
 * A bay's average rating: a gold star, "4.5" and "(12)", or "No ratings" when
 * nobody has rated it yet. [compact] is the smaller size for the map's preview card.
 */
@Composable
fun RatingBadge(summary: RatingSummary?, palette: AuroraPalette, modifier: Modifier = Modifier, compact: Boolean = false) {
    val description = summary?.let { "Rated %.1f out of 5 by %d %s".format(Locale.UK, it.average, it.count, if (it.count == 1) "rider" else "riders") }
        ?: "No ratings yet"
    val icon = if (compact) 12.dp else 14.dp
    Surface(
        shape = CircleShape,
        color = palette.chipUnselectedBg,
        border = BorderStroke(1.dp, palette.chipUnselectedBorder),
        modifier = modifier.clearAndSetSemantics { contentDescription = description }.testTag("rating-badge"),
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
        ) {
            if (summary == null) {
                Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = palette.cardSubtext, modifier = Modifier.size(icon))
                Text("No ratings", style = auroraStyle(if (compact) 10.sp else 11.sp, FontWeight.Medium, palette.cardSubtext), maxLines = 1, softWrap = false)
            } else {
                Icon(Icons.Filled.Star, contentDescription = null, tint = AuroraPalette.StatusPaid, modifier = Modifier.size(icon))
                Text("%.1f".format(Locale.UK, summary.average), style = auroraStyle(if (compact) 12.sp else 13.sp, FontWeight.Bold, palette.cardTitleText), maxLines = 1, softWrap = false)
                Text("(${summary.count})", style = auroraStyle(if (compact) 10.sp else 11.sp, color = palette.cardSubtext), maxLines = 1, softWrap = false)
            }
        }
    }
}
