package uk.motobay.app.ui.bay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BaySubmission
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.ModerationStatus

/**
 * Bottom sheet for adding a bay at [at], or suggesting corrections to [editing].
 * The author, id and timestamps are filled in by the repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayFormSheet(
    at: LatLon,
    editing: Bay?,
    onDismiss: () -> Unit,
    onSubmit: (BaySubmission) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(editing?.name?.takeUnless { it == "Motorcycle parking" }.orEmpty()) }
    var fee by rememberSaveable { mutableStateOf(editing?.fee ?: Fee.Unknown) }
    var spaces by rememberSaveable { mutableStateOf(editing?.capacity?.toString().orEmpty()) }
    var covered by rememberSaveable { mutableStateOf(editing?.covered) }
    var notes by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (editing == null) "Add a bay" else "Suggest an edit", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (editing == null) "Tell other riders about this bay. It's checked before it appears on the map."
                else "Know something we don't? Corrections are checked before they're published.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("Name or street (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Price", style = MaterialTheme.typography.titleSmall)
            Choice(
                options = listOf(Fee.Free to "Free", Fee.Paid to "Paid", Fee.Unknown to "Not sure"),
                selected = fee,
                onSelect = { fee = it },
            )

            Text("Covered", style = MaterialTheme.typography.titleSmall)
            Choice(
                options = listOf(true to "Yes", false to "No", null to "Not sure"),
                selected = covered,
                onSelect = { covered = it },
            )

            OutlinedTextField(
                value = spaces,
                onValueChange = { v -> spaces = v.filter(Char::isDigit).take(3) },
                label = { Text("Number of spaces (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(300) },
                label = { Text("Anything else? (optional)") },
                supportingText = { Text("For example: paid 8am–6:30pm, pay by app, often full") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    onSubmit(
                        BaySubmission(
                            id = "", editOf = editing?.id,
                            lat = at.lat, lon = at.lon,
                            name = name.trim(), fee = fee,
                            capacity = spaces.toIntOrNull()?.takeIf { it > 0 },
                            covered = covered, notes = notes.trim(),
                            author = "", createdAt = 0, status = ModerationStatus.Pending,
                        ),
                    )
                }) { Text(if (editing == null) "Add bay" else "Send") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(label) },
            )
        }
    }
}
