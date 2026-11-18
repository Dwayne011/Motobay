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
import uk.motobay.app.ui.bay.ParkedCard
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraHeader
import uk.motobay.app.ui.components.AuroraTag
import uk.motobay.app.ui.components.HSpace
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.data.Fee
import uk.motobay.app.ui.components.AuroraSectionHeader
import uk.motobay.app.ui.components.frostOverline
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import uk.motobay.app.ui.components.feeText
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
    val parked by app.user.parked.collectAsStateWithLifecycle()
    val items = favourites.values
        .map { bay -> bay to position?.let { distanceMetres(it, bay) } }
        .sortedWith(compareBy({ it.second ?: 0.0 }, { it.first.title }))

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (items.isNotEmpty()) Text("YOUR BAYS \u00B7 ${items.size}", style = frostOverline(palette.cardSubtext))
                AuroraHeader("Saved", palette)
            }
        }
        // Where the rider parked, with the charge reminder, above their saved bays.
        parked?.let { p -> item(key = "parked") { ParkedCard(p, palette, onOpen = { onOpenBay(p.bayId) }) } }
        if (items.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxSize().padding(vertical = 64.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.StarBorder, contentDescription = null, Modifier.size(48.dp), tint = palette.cardSubtext)
                    Text("No saved bays yet", style = auroraStyle(15.sp, FontWeight.Bold, palette.cardTitleText))
                    Text(
                        "Save the bays you use most so you can get directions in one tap.",
                        style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // One glass panel per price group, nearest first within each.
            listOf(Fee.Free to "Free", Fee.Paid to "Paid", Fee.Unknown to "Price unknown").forEach { (fee, label) ->
                val group = items.filter { it.first.fee == fee }
                if (group.isEmpty()) return@forEach
                item(key = "header-$fee") {
                    AuroraSectionHeader(label, palette, Modifier.padding(start = 10.dp, top = 4.dp))
                }
                item(key = "group-$fee") {
                    AuroraCard(palette, contentPadding = 4) {
                        group.forEachIndexed { i, (bay, d) ->
                            if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(palette.divider))
                            SavedBayRow(bay, d?.let(::formatDistance), palette) { onOpenBay(bay.id) }
                        }
                    }
                }
            }
        }
    }
}

/** A saved bay as a Frost row: a tinted star tile, the name, a summary line and the distance. */
@Composable
private fun SavedBayRow(bay: Bay, distance: String?, palette: AuroraPalette, onClick: () -> Unit) {
    val (fill, ink) = when (bay.fee) {
        Fee.Free -> palette.toneFree
        Fee.Paid -> palette.tonePaid
        Fee.Unknown -> palette.tile to palette.text2
    }
    val summary = listOfNotNull(
        bay.feeText,
        bay.capacity?.let { "$it ${if (it == 1) "space" else "spaces"}" },
        if (bay.covered == true) "Covered" else null,
        bay.locality ?: bay.street,
    ).joinToString(" \u00B7 ")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        }
        HSpace(12)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(bay.title, style = auroraStyle(15.sp, FontWeight.Bold, palette.cardTitleText).copy(lineHeight = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(summary, style = auroraStyle(11.sp, FontWeight.SemiBold, palette.cardSubtext).copy(lineHeight = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (distance != null) {
            HSpace(10)
            Text(distance, style = auroraStyle(13.sp, FontWeight.Bold, palette.text2), maxLines = 1, softWrap = false)
        }
    }
}
