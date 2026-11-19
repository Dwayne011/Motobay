package uk.motobay.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.OFFLINE_TILE_LIMIT
import uk.motobay.app.data.tileCount
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.AuroraSectionHeader
import uk.motobay.app.ui.components.AuroraTextAction
import uk.motobay.app.ui.components.FrostDivider
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.explore.styleUrl
import uk.motobay.app.ui.theme.AuroraPalette
import uk.motobay.app.ui.theme.isAppInDarkTheme
import uk.motobay.app.util.distanceMetres
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline maps: save the map of the area last shown on the Explore map, so it still draws
 * with no signal. Bays, crime and garage data are always kept on the phone already.
 */
@Composable
fun OfflineMapsPage(palette: AuroraPalette) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val areas by app.offlineAreas.areas.collectAsStateWithLifecycle()
    val progress by app.offlineAreas.progress.collectAsStateWithLifecycle()
    val dark = isAppInDarkTheme(settings.themeMode)
    LaunchedEffect(Unit) { app.offlineAreas.refresh() }

    val box = settings.lastMap?.box
    val tiles = box?.let { tileCount(it) }
    val tooBig = tiles != null && tiles > OFFLINE_TILE_LIMIT

    AuroraCard(palette, radius = 18, contentPadding = 14) {
        Text("Save the area on the map", style = auroraStyle(15.sp, FontWeight.ExtraBold, palette.cardTitleText))
        VSpace(4)
        Text(
            when {
                box == null -> "Open the Explore map first, then come back here to save the area it shows."
                tooBig -> "The area on the map is too big to save. Zoom in on Explore to about a city's size, then try again."
                else -> "Saves the map of the area last shown on Explore, so it works with no signal. Bays are always saved on your phone."
            },
            style = auroraStyle(13.sp, FontWeight.SemiBold, palette.text2),
        )
        VSpace(10)
        progress?.let {
            LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().testTag("offline-progress"))
            VSpace(4)
            Text("Downloading… ${(it * 100).toInt()}%", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext))
        } ?: AuroraPrimaryButton(
            "Download this area", palette, enabled = box != null && !tooBig,
            modifier = Modifier.fillMaxWidth().testTag("offline-download"),
            onClick = {
                val b = box ?: return@AuroraPrimaryButton
                // Name it after the area most bays around the middle are in, e.g. "Soho".
                val centre = LatLon((b.south + b.north) / 2, (b.west + b.east) / 2)
                val name = app.allBays.value.values.filter { it.area != null && it in b }
                    .minByOrNull { distanceMetres(centre, it) }?.area ?: "Map area"
                scope.launch {
                    val ok = runCatching { app.offlineAreas.download(name, b, styleUrl(dark)) }.isSuccess
                    snackbar.showSnackbar(if (ok) "$name saved for offline use." else "Couldn't save the map. Check your connection and try again.")
                }
            },
        )
    }

    if (areas.isNotEmpty()) {
        AuroraSectionHeader("Saved areas", palette, Modifier.padding(start = 10.dp, top = 4.dp))
        AuroraCard(palette, contentPadding = 4) {
            areas.forEachIndexed { i, area ->
                if (i > 0) FrostDivider(palette)
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(area.name, style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText))
                        val date = if (area.createdAt > 0) DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.UK).format(Date(area.createdAt)) else null
                        val size = "%.1f MB".format(Locale.UK, area.bytes / 1_048_576.0)
                        Text(
                            listOfNotNull(date, size, "not finished".takeIf { !area.complete }).joinToString(" · "),
                            style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext),
                        )
                    }
                    AuroraTextAction("Delete", palette.searchIconActive, onClick = {
                        scope.launch { runCatching { app.offlineAreas.delete(area.id) } }
                    })
                }
            }
        }
    }
}
