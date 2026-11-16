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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
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
import uk.motobay.app.ui.theme.FrostFont
import uk.motobay.app.ui.theme.frostEdge
import androidx.compose.ui.unit.em

/** Tailwind's slate-950, the ink the spec puts on sky-blue surfaces. */
val Slate950 = Color(0xFF020617)
val Coral = Color(0xFFEF4444)

/** A text style of exactly [size] with a comfortable line height, as the spec sets every label by size and weight. */
fun auroraStyle(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified, mono: Boolean = false): TextStyle =
    TextStyle(fontSize = size, lineHeight = size * 1.4f, fontWeight = weight, color = color, fontFamily = if (mono) FontFamily.Monospace else FrostFont)

/** The 22sp extra-bold page title the tabs start with. */
@Composable
fun AuroraHeader(text: String, palette: AuroraPalette, modifier: Modifier = Modifier) {
    Text(text, style = auroraStyle(22.sp, FontWeight.ExtraBold, palette.cardTitleText).copy(lineHeight = 26.sp), modifier = modifier)
}

/** Frost's overline: 11sp extra-bold capitals, slightly tracked, in the accent colour. */
@Composable
fun AuroraSectionHeader(text: String, palette: AuroraPalette, modifier: Modifier = Modifier, color: Color = palette.accent) {
    Text(text.uppercase(), style = frostOverline(color), modifier = modifier)
}

fun frostOverline(color: Color): TextStyle =
    TextStyle(fontFamily = FrostFont, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em, color = color)

/**
 * A Frost panel: a mostly solid fill with a 1dp hairline edge,
 * rounded 28dp by default. [elevation] adds a soft shadow and is for opaque
 * fills only, since a shadow shows through glass.
 */
@Composable
fun AuroraCard(
    palette: AuroraPalette,
    modifier: Modifier = Modifier,
    radius: Int = 28,
    elevation: Int = 0,
    color: Color = palette.cardSurface,
    border: Color = palette.cardBorder,
    onClick: (() -> Unit)? = null,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius.dp)
    val edge = frostEdge(palette, border)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, border = edge, shadowElevation = elevation.dp, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(contentPadding.dp), content = content)
        }
    } else {
        Surface(shape = shape, color = color, border = edge, shadowElevation = elevation.dp, modifier = modifier.fillMaxWidth()) {
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
        shape = CircleShape,
        color = palette.cardSurface,
        border = frostEdge(palette, palette.segmentedContainerBorder),
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
    ) {
        // Intrinsic height: the segments fill the row's height, not whatever the parent offers.
        Row(Modifier.height(IntrinsicSize.Min).padding(3.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { i, label ->
                val active = i == selected
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 34.dp)
                        .clip(CircleShape)
                        .background(if (active) palette.accent else Color.Transparent)
                        .clickable(onClick = { onSelect(i) }, role = Role.Tab)
                        .semantics { this.selected = active }
                        .then(segmentModifier(i))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = auroraStyle(13.sp, FontWeight.Bold, if (active) palette.onAccent else palette.cardSubtext),
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

/** A 36dp Frost chip: the accent when active, a soft tile otherwise. */
@Composable
fun AuroraChip(label: String, active: Boolean, palette: AuroraPalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) palette.accent else palette.tile,
        modifier = modifier.heightIn(min = 36.dp).semantics { selected = active },
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, style = auroraStyle(13.sp, FontWeight.Bold, if (active) palette.onAccent else palette.text2), maxLines = 1, softWrap = false)
        }
    }
}

/** 40dp Frost pill button: the accent with its own ink, 13sp extra bold. */
@Composable
fun AuroraPrimaryButton(text: String, palette: AuroraPalette, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val bg = palette.accent
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) bg else bg.copy(alpha = 0.4f),
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(16.dp))
            Text(text, style = auroraStyle(13.sp, FontWeight.ExtraBold, palette.onAccent), maxLines = 1, softWrap = false)
        }
    }
}

/** 40dp secondary pill button: a soft tile, 13sp bold. */
@Composable
fun AuroraOutlinedButton(text: String, palette: AuroraPalette, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = palette.cardTitleText) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = palette.tile,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text, style = auroraStyle(13.sp, FontWeight.Bold, if (enabled) color else color.copy(alpha = 0.4f)), maxLines = 1, softWrap = false)
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
    Box(modifier.clip(CircleShape).background(palette.tile).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(text, style = auroraStyle(11.sp, FontWeight.Bold, palette.text2), maxLines = 1, softWrap = false)
    }
}

/** Title and subtext with a 16dp chevron, used for the Settings rows. */
@Composable
fun RowScope.AuroraRowText(title: String, subtitle: String?, palette: AuroraPalette, titleColor: Color = palette.cardTitleText) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = auroraStyle(15.sp, FontWeight.Bold, titleColor).copy(lineHeight = 18.sp))
        if (subtitle != null) Text(subtitle, style = auroraStyle(11.sp, FontWeight.SemiBold, palette.cardSubtext).copy(lineHeight = 14.sp))
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
        color = palette.tile,
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
                Icon(Icons.Filled.Star, contentDescription = null, tint = palette.paid, modifier = Modifier.size(icon))
                Text("%.1f".format(Locale.UK, summary.average), style = auroraStyle(if (compact) 12.sp else 13.sp, FontWeight.ExtraBold, palette.cardTitleText), maxLines = 1, softWrap = false)
                Text("(${summary.count})", style = auroraStyle(if (compact) 10.sp else 11.sp, color = palette.cardSubtext), maxLines = 1, softWrap = false)
            }
        }
    }
}

/** A Frost page header: a round back button, an optional overline over the 22sp title, and optional actions on the right. */
@Composable
fun FrostPageHeader(
    title: String,
    palette: AuroraPalette,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    overline: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) FrostRoundIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", palette, onBack)
        Column(Modifier.weight(1f).padding(start = if (onBack == null) 6.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (overline != null) Text(overline.uppercase(), style = frostOverline(palette.cardSubtext), maxLines = 1)
            AuroraHeader(title, palette)
        }
        actions()
    }
}

/**
 * A Frost menu row, as on the first Settings page: a 38dp tinted icon tile, the title, its
 * current value underneath, and a chevron (or [trailing] instead).
 */
@Composable
fun FrostMenuRow(
    icon: ImageVector,
    tone: Pair<Color, Color>,
    title: String,
    caption: String?,
    palette: AuroraPalette,
    onClick: (() -> Unit)?,
    titleColor: Color = palette.cardTitleText,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tone.first), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tone.second, modifier = Modifier.size(19.dp))
        }
        AuroraRowText(title, caption, palette, titleColor)
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(20.dp))
        }
    }
}

/** A 44dp round tile button with a 20dp icon. */
@Composable
fun FrostRoundIcon(icon: ImageVector, description: String, palette: AuroraPalette, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = palette.tile, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, tint = palette.text2, modifier = Modifier.size(20.dp)) }
    }
}

/** A row inside a Frost panel: title, optional subtitle, optional trailing icon; tappable when [onClick] is set. */
@Composable
fun FrostListRow(
    title: String,
    subtitle: String?,
    palette: AuroraPalette,
    trailing: ImageVector? = null,
    trailingDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AuroraRowText(title, subtitle, palette)
        if (trailing != null) {
            HSpace(10)
            Icon(trailing, contentDescription = trailingDescription, tint = palette.muted, modifier = Modifier.size(20.dp))
        }
    }
}

/** A hairline between rows inside a Frost panel. */
@Composable
fun FrostDivider(palette: AuroraPalette) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(palette.divider))
}
