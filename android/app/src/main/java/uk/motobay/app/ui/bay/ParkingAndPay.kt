package uk.motobay.app.ui.bay

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uk.motobay.app.alerts.ChargeAlerts
import uk.motobay.app.data.Bay
import uk.motobay.app.data.Parked
import uk.motobay.app.data.ParkingPlan
import uk.motobay.app.data.Payment
import uk.motobay.app.data.parkedHere
import uk.motobay.app.data.plan
import uk.motobay.app.data.summary
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraOutlinedButton
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.AuroraTextAction
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.theme.AuroraPalette

/**
 * "I parked here" on a bay page. Once parked, a card says whether it's charged and,
 * for a bay that starts charging later, that a reminder will come 15 minutes before.
 * Free for everyone. Where the rider parked stays on the phone.
 */
@Composable
fun ParkHereSection(bay: Bay, palette: AuroraPalette) {
    val app = LocalApp.current
    val parked by app.user.parked.collectAsStateWithLifecycle()
    if (parked?.bayId == bay.id) {
        ParkedCard(parked!!, palette, onOpen = null)
    } else {
        val park = rememberParkAction()
        AuroraOutlinedButton(
            if (parked == null) "I parked here" else "I parked here instead",
            palette, onClick = { park(bay) },
            modifier = Modifier.fillMaxWidth().testTag("park-here"),
        )
    }
}

/** Parks at a bay: saves it, sets the alarm, and asks for notifications if a reminder is due. */
@Composable
fun rememberParkAction(): (Bay) -> Unit {
    val app = LocalApp.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) scope.launch { snackbar.showSnackbar("Notifications are off, so we can't remind you before charging starts.") }
    }
    return { bay ->
        val parked = bay.parkedHere(app.now())
        app.user.setParked(parked)
        val plan = ChargeAlerts.schedule(context, parked)
        if (plan is ParkingPlan.FreeUntil && !ChargeAlerts.canNotify(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else scope.launch { snackbar.showSnackbar("Notifications are off for MotoBays, so we can't remind you.") }
        }
    }
}

/** Where the rider parked and what to watch for. [onOpen] shows the bay (from the Saved tab). */
@Composable
fun ParkedCard(parked: Parked, palette: AuroraPalette, onOpen: (() -> Unit)?) {
    val app = LocalApp.current
    val context = LocalContext.current
    val now = app.now()
    val plan = parked.plan(now)
    AuroraCard(palette, radius = 18, contentPadding = 14, modifier = Modifier.testTag("parked-card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.LocalParking, contentDescription = null, tint = palette.accent)
            Text(
                if (onOpen == null) "You parked here" else "You parked at ${parked.title}",
                style = auroraStyle(15.sp, FontWeight.ExtraBold, palette.cardTitleText), modifier = Modifier.weight(1f),
            )
        }
        VSpace(4)
        Text(plan.summary(now), style = auroraStyle(13.sp, FontWeight.SemiBold, palette.text2))
        if (plan is ParkingPlan.FreeUntil && !ChargeAlerts.canNotify(context)) {
            VSpace(4)
            Text("Notifications are off for MotoBays, so the reminder won't show.", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext))
        }
        VSpace(6)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            onOpen?.let { AuroraTextAction("Open bay", palette.searchIconActive, onClick = it) }
            AuroraTextAction("I've left", palette.searchIconActive, onClick = {
                app.user.setParked(null)
                ChargeAlerts.schedule(context, null)
            }, modifier = Modifier.testTag("left-bay"))
        }
    }
}

/**
 * "Pay with RingGo" and the location code from the council's data. Copies the code and
 * opens the provider's app (or its website). Councils warn of fake QR codes on signs,
 * so this only ever opens the provider's own app or site.
 */
@Composable
fun PayButton(payment: Payment, palette: AuroraPalette) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    AuroraPrimaryButton(
        "Pay with ${payment.provider} · location ${payment.locationCode}", palette,
        icon = Icons.Filled.Payments,
        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("pay-button"),
        onClick = {
            copyText(context, "Location code", payment.locationCode)
            scope.launch { snackbar.showSnackbar("Location ${payment.locationCode} copied. Paste it into ${payment.provider}.") }
            openProvider(context, payment.provider)
        },
    )
}

private fun copyText(context: Context, label: String, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Provider to (Android package, website). Only RingGo so far: every council in the data uses it. */
private val PROVIDERS = mapOf(
    "RingGo" to ("co.uk.ringgo.android" to "https://myringgo.co.uk/"),
)

private fun openProvider(context: Context, provider: String) {
    val (pkg, web) = PROVIDERS[provider] ?: return
    val launch = context.packageManager.getLaunchIntentForPackage(pkg)
    runCatching { context.startActivity(launch ?: Intent(Intent.ACTION_VIEW, Uri.parse(web))) }
}
