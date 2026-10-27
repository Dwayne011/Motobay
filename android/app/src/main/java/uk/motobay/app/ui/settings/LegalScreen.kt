package uk.motobay.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import uk.motobay.app.BuildConfig
import uk.motobay.app.data.Legal

/** Disclaimer, data licences and the privacy policy, reachable from Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legal & about") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Guidance only", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(Legal.DISCLAIMER, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 8.dp))
                    Text(Legal.UGC_TERMS, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 8.dp))
                }
            }

            ListItem(
                headlineContent = { Text("Privacy policy") },
                supportingContent = { Text("How MotoBays handles your account, photos and location (UK GDPR)") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { open(Legal.PRIVACY_POLICY_URL) },
            )
            ListItem(
                headlineContent = { Text("Delete your account") },
                supportingContent = { Text("You can also delete it from Settings > Account. This page explains what is removed.") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { open(Legal.DELETE_ACCOUNT_URL) },
            )
            HorizontalDivider()

            Text(
                "Data sources and licences",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            )
            Legal.ATTRIBUTIONS.forEach { a ->
                ListItem(
                    headlineContent = { Text(a.title) },
                    supportingContent = { Text(a.text) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open ${a.title} licence") },
                    modifier = Modifier.clickable { open(a.url) },
                )
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("MotoBays ${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("Build ${BuildConfig.VERSION_CODE} · ${BuildConfig.APPLICATION_ID}") },
            )
        }
    }
}

/** First-launch notice. Can only be dismissed with the button, and is then remembered. */
@Composable
fun DisclaimerDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Before you ride") },
        text = { Text("${Legal.DISCLAIMER}\n\n${Legal.UGC_TERMS}") },
        confirmButton = { Button(onClick = onAccept) { Text("I understand") } },
    )
}
