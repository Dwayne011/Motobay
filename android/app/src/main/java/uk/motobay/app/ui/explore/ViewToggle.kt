package uk.motobay.app.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Roboto's capital letters are this fraction of the font size tall. */
private const val CAP_HEIGHT = 0.711f

/**
 * The floating Map / List switch on phones. Each segment's icon is lined up
 * with the optical centre of its label (the middle of the capital letters),
 * not the middle of the text box, which sits lower because of descenders.
 */
@Composable
fun ViewToggle(view: CompactView, countLabel: String, onSelect: (CompactView) -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 3.dp, modifier = modifier.testTag("view-toggle")) {
        SingleChoiceSegmentedButtonRow {
            // Non-breaking space: a label's minimum width is its longest word, so "List 200+"
            // would otherwise be measured as wide as "200+" and clipped.
            listOf(CompactView.Map to "Map", CompactView.List to "List\u00A0$countLabel").forEachIndexed { i, (v, label) ->
                SegmentedButton(
                    selected = view == v,
                    onClick = { onSelect(v) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    icon = {},
                    label = {
                        IconLabel(
                            icon = when {
                                view == v -> Icons.Filled.Check
                                v == CompactView.Map -> Icons.Filled.Map
                                else -> Icons.AutoMirrored.Filled.List
                            },
                            label = label,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun IconLabel(icon: ImageVector, label: String) {
    val style = MaterialTheme.typography.labelLarge
    val capHalf = with(LocalDensity.current) { (style.fontSize.toPx() * CAP_HEIGHT / 2).roundToInt() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        // Give the icon a "baseline" capHalf below its centre, so aligning baselines
        // puts the icon's centre on the capital-letter centre of the text.
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(SegmentedButtonDefaults.IconSize).alignBy { it.measuredHeight / 2 + capHalf },
        )
        Text(label, style = style, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
    }
}
