package uk.motobay.app.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.motobay.app.ui.theme.AuroraPalette

/**
 * The floating Map / List switch on phones, to the spec: a 220 x 40dp capsule
 * with a 1dp border and 2dp inset, whose active segment is a 36dp pill.
 */
@Composable
fun ViewToggle(
    view: CompactView,
    countLabel: String,
    palette: AuroraPalette,
    onSelect: (CompactView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = palette.segmentedContainerBg,
        border = BorderStroke(1.dp, palette.segmentedContainerBorder),
        // 220dp wide, growing only if a large-text label needs more room.
        modifier = modifier.widthIn(min = 220.dp).width(IntrinsicSize.Max).height(40.dp).testTag("view-toggle"),
    ) {
        Row(Modifier.padding(2.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            // Non-breaking space keeps "List 200+" on one line.
            Segment(view == CompactView.Map, Icons.Filled.Map, "Map", palette) { onSelect(CompactView.Map) }
            Segment(view == CompactView.List, Icons.AutoMirrored.Filled.List, "List\u00A0$countLabel", palette) { onSelect(CompactView.List) }
        }
    }
}

@Composable
private fun RowScope.Segment(active: Boolean, icon: ImageVector, label: String, palette: AuroraPalette, onClick: () -> Unit) {
    val tint = if (active) palette.segmentedActiveText else palette.segmentedInactiveText
    Row(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) palette.segmentedActiveBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick, role = Role.RadioButton)
            .semantics { selected = active },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (active) Icons.Filled.Check else icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}
