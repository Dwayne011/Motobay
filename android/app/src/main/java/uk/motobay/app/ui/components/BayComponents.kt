package uk.motobay.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    Fee.Paid -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    Fee.Unknown -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
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
        leadingContent = { Icon(Icons.Filled.TwoWheeler, contentDescription = null) },
        headlineContent = { Text(bay.name) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FeeLabel(bay.fee)
                bay.capacity?.let { Text("$it spaces") }
            }
        },
        trailingContent = distance?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
    )
}
