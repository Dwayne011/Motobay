package uk.motobay.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import uk.motobay.app.data.Legal

/** Google Play's UGC rule: riders can flag someone else's photo or comment for the admin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(what: String, onDismiss: () -> Unit, onSubmit: (reason: String, details: String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf(Legal.REPORT_REASONS.first().first) }
    var details by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Report this $what", style = MaterialTheme.typography.headlineSmall)
            Text("The admin will look at it. The person who posted it isn't told who reported it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Legal.REPORT_REASONS.forEach { (key, label) ->
                Row(
                    Modifier.fillMaxWidth().selectable(selected = reason == key, onClick = { reason = key }, role = Role.RadioButton),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = reason == key, onClick = null)
                    Text(label, Modifier.padding(start = 8.dp))
                }
            }
            OutlinedTextField(
                value = details,
                onValueChange = { details = it.take(300) },
                label = { Text("Anything else? (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = { onSubmit(reason, details.trim()) }, modifier = Modifier.testTag("report-send")) { Text("Send report") }
            }
        }
    }
}
