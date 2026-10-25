package uk.motobay.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import java.io.File
import java.text.DateFormat
import java.util.Date

private enum class Kind(val label: String) { Photos("Photos"), Comments("Comments") }

/** Moderation queue: nothing riders submit is public until it's approved here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val data by app.community.data.collectAsStateWithLifecycle()
    val photos = data.photos.filter { it.status == ModerationStatus.Pending }
    val comments = data.comments.filter { it.status == ModerationStatus.Pending }
    var kind by rememberSaveable { mutableStateOf(Kind.Photos) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    fun decide(isPhoto: Boolean, id: String, status: ModerationStatus) {
        val apply = { s: ModerationStatus ->
            if (isPhoto) app.community.moderatePhoto(id, s) else app.community.moderateComment(id, s)
        }
        apply(status)
        scope.launch {
            val noun = if (isPhoto) "Photo" else "Comment"
            val verb = if (status == ModerationStatus.Approved) "approved" else "rejected"
            if (snackbar.showSnackbar("$noun $verb", actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
                apply(ModerationStatus.Pending)
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Review") }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 16.dp,
                top = padding.calculateTopPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Kind.entries.forEachIndexed { i, k ->
                        val count = if (k == Kind.Photos) photos.size else comments.size
                        SegmentedButton(
                            selected = kind == k,
                            onClick = { kind = k },
                            shape = SegmentedButtonDefaults.itemShape(i, Kind.entries.size),
                            label = { Text("${k.label} ($count)") },
                        )
                    }
                }
            }
            val isPhoto = kind == Kind.Photos
            if ((isPhoto && photos.isEmpty()) || (!isPhoto && comments.isEmpty())) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.TaskAlt, contentDescription = null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("All caught up", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "No ${kind.label.lowercase()} waiting for review.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (isPhoto) {
                items(photos, key = { it.id }) { p ->
                    ReviewCard(
                        author = p.author, date = dateFormat.format(Date(p.createdAt)), bayName = p.bayName,
                        onOpenBay = { onOpenBay(p.bayId) },
                        body = p.caption.ifEmpty { "No caption" },
                        imagePath = p.path,
                        onApprove = { decide(true, p.id, ModerationStatus.Approved) },
                        onReject = { decide(true, p.id, ModerationStatus.Rejected) },
                    )
                }
            } else {
                items(comments, key = { it.id }) { c ->
                    ReviewCard(
                        author = c.author, date = dateFormat.format(Date(c.createdAt)), bayName = c.bayName,
                        onOpenBay = { onOpenBay(c.bayId) },
                        body = c.text,
                        imagePath = null,
                        onApprove = { decide(false, c.id, ModerationStatus.Approved) },
                        onReject = { decide(false, c.id, ModerationStatus.Rejected) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    author: String,
    date: String,
    bayName: String,
    onOpenBay: () -> Unit,
    body: String,
    imagePath: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        imagePath?.let {
            AsyncImage(
                model = File(it),
                contentDescription = "Submitted photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$author · $date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onOpenBay, contentPadding = PaddingValues(0.dp)) {
                Text(bayName, style = MaterialTheme.typography.titleMedium)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onReject) { Text("Reject") }
                Button(onClick = onApprove) { Text("Approve") }
            }
        }
    }
}
