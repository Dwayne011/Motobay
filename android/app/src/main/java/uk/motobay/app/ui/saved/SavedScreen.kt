package uk.motobay.app.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.motobay.app.data.Bay
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraHeader
import uk.motobay.app.ui.components.AuroraTag
import uk.motobay.app.ui.components.HSpace
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.components.label
import uk.motobay.app.ui.theme.AuroraPalette
import uk.motobay.app.ui.theme.auroraPalette
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.util.formatDistance

/** Bookmarked bays, nearest first, as the spec's star cards. */
@Composable
fun SavedScreen(onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val palette = auroraPalette()
    val favourites by app.user.favourites.collectAsStateWithLifecycle()
    val position by app.location.position.collectAsStateWithLifecycle()
    val items = favourites.values
        .map { bay -> bay to position?.let { distanceMetres(it, bay) } }
        .sortedWith(compareBy({ it.second ?: 0.0 }, { it.first.title }))

    LazyColumn(
        Modifier.fillMaxSize().background(palette.mapBackground).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AuroraHeader("Saved", palette) }
        if (items.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxSize().padding(vertical = 64.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.StarBorder, contentDescription = null, Modifier.size(48.dp), tint = palette.cardSubtext)
                    Text("No saved bays yet", style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText))
                    Text(
                        "Save the bays you use most so you can get directions in one tap.",
                        style = auroraStyle(12.sp, color = palette.cardSubtext),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(items, key = { it.first.id }) { (bay, d) ->
                SavedBayCard(bay, d?.let(::formatDistance), palette) { onOpenBay(bay.id) }
            }
        }
    }
}

@Composable
private fun SavedBayCard(bay: Bay, distance: String?, palette: AuroraPalette, onClick: () -> Unit) {
    AuroraCard(palette, elevation = 4, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            // 36dp amber star tile.
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33F59E0B)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = AuroraPalette.StatusPaid, modifier = Modifier.size(18.dp))
            }
            HSpace(12)
            Column(Modifier.weight(1f)) {
                Text(bay.title, style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val where = bay.locality ?: bay.street
                if (!where.isNullOrEmpty()) {
                    VSpace(2)
                    Text(where, style = auroraStyle(12.sp, color = palette.cardSubtext), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AuroraTag(bay.fee.label, palette)
                    bay.capacity?.let { AuroraTag("$it spaces", palette) }
                    if (bay.covered == true) AuroraTag("Covered", palette)
                }
            }
            if (distance != null) {
                HSpace(10)
                Text(distance, style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardTitleText), maxLines = 1, softWrap = false)
            }
        }
    }
}
