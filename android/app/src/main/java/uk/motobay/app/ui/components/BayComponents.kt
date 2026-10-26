package uk.motobay.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.motobay.app.data.Bay
import uk.motobay.app.data.Fee
import uk.motobay.app.ui.theme.MotoBayColors

val Fee.label: String
    get() = when (this) {
        Fee.Free -> "Free"
        Fee.Paid -> "Paid"
        Fee.Unknown -> "Price unknown"
    }

/** Container / content colours for a price, used by chips and map pins. */
@Composable
fun feeColors(fee: Fee): Pair<Color, Color> = when (fee) {
    Fee.Free -> MotoBayColors.extended.successContainer to MotoBayColors.extended.onSuccessContainer
    Fee.Paid -> MotoBayColors.extended.warningContainer to MotoBayColors.extended.onWarningContainer
    Fee.Unknown -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
}

/** Strong pin colours, matching the map: brand colour when the price isn't known. */
@Composable
fun pinColors(fee: Fee): Pair<Color, Color> = with(MotoBayColors.extended) {
    when (fee) {
        Fee.Free -> pinFree to onPinFree
        Fee.Paid -> pinPaid to onPinPaid
        Fee.Unknown -> pinUnknown to onPinUnknown
    }
}

/** The map pin, for lists and legends. */
@Composable
fun BayPin(fee: Fee, size: Dp = 32.dp) {
    val (container, content) = pinColors(fee)
    Surface(color = container, contentColor = content, shape = CircleShape, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.TwoWheeler, contentDescription = null, Modifier.size(size * 0.6f))
        }
    }
}

/** Small tonal label showing the price. */
@Composable
fun FeeLabel(fee: Fee, modifier: Modifier = Modifier) {
    val (container, content) = feeColors(fee)
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            fee.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Facts about a bay, as M3 assist chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BayFacts(bay: Bay) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val (container, content) = feeColors(bay.fee)
        AssistChip(
            onClick = {},
            label = { Text(bay.fee.label) },
            colors = AssistChipDefaults.assistChipColors(containerColor = container, labelColor = content),
            border = null,
        )
        bay.capacity?.let { Fact(Icons.Filled.TwoWheeler, "$it spaces") }
        bay.covered?.let { Fact(Icons.Filled.Roofing, if (it) "Covered" else "Not covered") }
        bay.maxStay?.let { Fact(Icons.Filled.HourglassBottom, "Max stay $it") }
        bay.openingHours?.let { Fact(Icons.Filled.AccessTime, it) }
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
    )
}

/** M3 two-line list item for a bay. */
@Composable
fun BayListItem(bay: Bay, distance: String?, selected: Boolean = false, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = if (selected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors(containerColor = Color.Transparent)
        },
        leadingContent = { BayPin(bay.fee) },
        headlineContent = { Text(bay.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bay.locality?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FeeLabel(bay.fee)
                    val facts = listOfNotNull(
                        bay.capacity?.let { "$it spaces" },
                        bay.covered?.takeIf { it }?.let { "Covered" },
                        bay.maxStay?.let { "Max $it" },
                    )
                    if (facts.isNotEmpty()) Text(facts.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        trailingContent = distance?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
    )
}
