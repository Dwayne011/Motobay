package uk.motobay.app.ui.bay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayFeature
import uk.motobay.app.data.SPACE_REPORT_FRESH_MS
import uk.motobay.app.data.SUSPENDED_REPORT_FRESH_MS
import uk.motobay.app.data.epochMillis
import uk.motobay.app.data.text
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalRequireSignIn
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraChip
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.theme.AuroraPalette

/**
 * Live reports from riders: is there space right now, and is the bay secure (ground
 * anchor, CCTV, lighting). Signed-in riders only; reports show at once as totals, and
 * nobody can see who sent them.
 */
@Composable
fun BayStatusSection(bay: Bay, palette: AuroraPalette) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val requireSignIn = LocalRequireSignIn.current
    val account by app.auth.user.collectAsStateWithLifecycle()
    val status by remember(bay.id) { app.bayStatus.status(bay.id) }.collectAsStateWithLifecycle(null)
    val mine by remember(bay.id, account?.uid) { app.bayStatus.mine(bay.id) }.collectAsStateWithLifecycle(null)
    val nowMillis = app.now().epochMillis()

    fun send(what: String, action: suspend () -> Unit) = requireSignIn {
        scope.launch {
            val ok = runCatching { action() }.isSuccess
            snackbar.showSnackbar(if (ok) what else "Couldn't send that. Check you're online, or wait a minute and try again.")
        }
    }

    AuroraCard(palette, modifier = Modifier.testTag("bay-status")) {
        Text("Is there space?", style = auroraStyle(15.sp, FontWeight.ExtraBold, palette.cardTitleText))
        VSpace(2)
        Text(
            status?.space?.text(nowMillis) ?: "No recent reports.",
            style = auroraStyle(13.sp, FontWeight.SemiBold, palette.text2),
            modifier = Modifier.testTag("space-status"),
        )
        VSpace(8)
        // The rider's own report stays lit while it's fresh.
        val mySpaceAt = mine?.spaceAt ?: 0
        val mySuspended = mine?.suspended == true && mySpaceAt > nowMillis - SUSPENDED_REPORT_FRESH_MS
        val myFull = mine?.full?.takeIf { !mySuspended && mySpaceAt > nowMillis - SPACE_REPORT_FRESH_MS }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuroraChip("Space now", myFull == false, palette, onClick = {
                send("Thanks! Riders can see there's space.") { app.bayStatus.reportSpace(bay, full = false) }
            })
            AuroraChip("Full", myFull == true, palette, onClick = {
                send("Thanks! Riders can see it's full.") { app.bayStatus.reportSpace(bay, full = true) }
            })
            AuroraChip("Suspended", mySuspended, palette, onClick = {
                send("Thanks! Riders can see it's suspended.") { app.bayStatus.reportSuspended(bay) }
            })
        }

        HorizontalDivider(color = palette.divider, modifier = Modifier.padding(vertical = 14.dp))
        Text("Security", style = auroraStyle(15.sp, FontWeight.ExtraBold, palette.cardTitleText))
        VSpace(2)
        Text("Tell other riders what's at this bay.", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext))
        BayFeature.entries.forEach { feature ->
            val votes = status?.features?.get(feature)
            val my = mine?.features?.get(feature)
            VSpace(10)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(feature.label, style = auroraStyle(13.sp, FontWeight.Bold, palette.cardTitleText))
                    Text(
                        votes?.text() ?: "Not known yet",
                        style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext),
                        modifier = Modifier.testTag("feature-${feature.key}"),
                    )
                }
                // Tapping your own answer again takes it back.
                AuroraChip("Yes", my == true, palette, onClick = {
                    send("Thanks!") { app.bayStatus.setFeature(bay, feature, if (my == true) null else true) }
                })
                AuroraChip("No", my == false, palette, onClick = {
                    send("Thanks!") { app.bayStatus.setFeature(bay, feature, if (my == false) null else false) }
                })
            }
        }
    }
}
