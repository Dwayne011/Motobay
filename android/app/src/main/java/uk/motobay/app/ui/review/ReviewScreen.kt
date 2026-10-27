package uk.motobay.app.ui.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.components.label
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private enum class Kind(val label: String) { Photos("Photos"), Comments("Comments"), Bays("Bays") }

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
    val baySubmissions = data.bays.filter { it.status == ModerationStatus.Pending }
    val allBays by app.allBays.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var kind by rememberSaveable { mutableStateOf(Kind.Photos) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    fun decide(kind: Kind, id: String, status: ModerationStatus) {
        suspend fun apply(s: ModerationStatus) {
            when (kind) {
                Kind.Photos -> app.community.moderatePhoto(id, s)
                Kind.Comments -> app.community.moderateComment(id, s)
                Kind.Bays -> app.community.moderateBay(id, s)
            }
        }
        scope.launch {
            val noun = when (kind) { Kind.Photos -> "Photo"; Kind.Comments -> "Comment"; Kind.Bays -> "Bay" }
            if (runCatching { apply(status) }.isFailure) {
                snackbar.showSnackbar("Couldn't update that ${noun.lowercase()}. Check you're online.")
                return@launch
            }
            val verb = if (status == ModerationStatus.Approved) "approved" else "rejected"
            if (snackbar.showSnackbar("$noun $verb", actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
                runCatching { apply(ModerationStatus.Pending) }
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
                        val count = when (k) { Kind.Photos -> photos.size; Kind.Comments -> comments.size; Kind.Bays -> baySubmissions.size }
                        SegmentedButton(
                            selected = kind == k,
                            onClick = { kind = k },
                            shape = SegmentedButtonDefaults.itemShape(i, Kind.entries.size),
                            icon = {},
                            label = {
                                // A badge beside the word rather than "(n)" in it: it never gets clipped on narrow screens.
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(k.label, maxLines = 1, softWrap = false)
                                    if (count > 0) Badge(Modifier.padding(start = 4.dp)) { Text("$count") }
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = "${k.label}, $count waiting" },
                        )
                    }
                }
            }
            val empty = when (kind) { Kind.Photos -> photos.isEmpty(); Kind.Comments -> comments.isEmpty(); Kind.Bays -> baySubmissions.isEmpty() }
            if (empty) {
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
            if (kind == Kind.Photos) {
                items(photos, key = { it.id }) { p ->
                    val bay = allBays[p.bayId]
                    ReviewCard(
                        author = p.author, date = dateFormat.format(Date(p.createdAt)),
                        bayName = bay?.title ?: p.bayName,
                        place = bay?.let { placeLine(it.locality, it.lat, it.lon) },
                        onOpenBay = { onOpenBay(p.bayId) },
                        onOpenMap = bay?.let { { openMap(context, it.lat, it.lon, it.title) } },
                        body = p.caption.ifEmpty { "No caption" },
                        imageUrl = p.url,
                        onViewImage = { viewing = p.url },
                        onApprove = { decide(Kind.Photos, p.id, ModerationStatus.Approved) },
                        onReject = { decide(Kind.Photos, p.id, ModerationStatus.Rejected) },
                    )
                }
            } else if (kind == Kind.Comments) {
                items(comments, key = { it.id }) { c ->
                    val bay = allBays[c.bayId]
                    ReviewCard(
                        author = c.author, date = dateFormat.format(Date(c.createdAt)),
                        bayName = bay?.title ?: c.bayName,
                        place = bay?.let { placeLine(it.locality, it.lat, it.lon) },
                        onOpenBay = { onOpenBay(c.bayId) },
                        onOpenMap = bay?.let { { openMap(context, it.lat, it.lon, it.title) } },
                        body = c.text,
                        onApprove = { decide(Kind.Comments, c.id, ModerationStatus.Approved) },
                        onReject = { decide(Kind.Comments, c.id, ModerationStatus.Rejected) },
                    )
                }
            } else {
                items(baySubmissions, key = { it.id }) { b ->
                    val original = b.editOf?.let { allBays[it] }
                    val facts = listOfNotNull(
                        b.name.takeIf { it.isNotBlank() }?.let { "Name: $it" },
                        "Price: ${b.fee.label}",
                        b.capacity?.let { "Spaces: $it" },
                        b.covered?.let { "Covered: ${if (it) "yes" else "no"}" },
                        b.notes.takeIf { it.isNotBlank() }?.let { "Notes: $it" },
                    )
                    ReviewCard(
                        author = b.author, date = dateFormat.format(Date(b.createdAt)),
                        bayName = if (b.editOf != null) "Edit: ${original?.title ?: "bay"}" else "New bay",
                        place = placeLine(original?.locality, b.lat, b.lon),
                        onOpenBay = if (b.editOf != null) ({ onOpenBay(b.editOf) }) else null,
                        onOpenMap = { openMap(context, b.lat, b.lon, if (b.editOf != null) original?.title ?: "Bay" else "New bay") },
                        body = facts.joinToString("\n"),
                        onApprove = { decide(Kind.Bays, b.id, ModerationStatus.Approved) },
                        onReject = { decide(Kind.Bays, b.id, ModerationStatus.Rejected) },
                    )
                }
            }
        }
    }

    viewing?.let { url ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            text = { AsyncImage(model = url, contentDescription = "Submitted photo", modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Close") } },
        )
    }
}

/** "Spitalfields, London E1 · 51.5190, -0.0742" */
private fun placeLine(locality: String?, lat: Double, lon: Double): String =
    listOfNotNull(locality, "%.4f, %.4f".format(Locale.UK, lat, lon)).joinToString(" · ")

private fun openMap(context: Context, lat: Double, lon: Double, label: String) {
    val q = Uri.encode(label)
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon($q)")))
}

@Composable
private fun ReviewCard(
    author: String,
    date: String,
    bayName: String,
    place: String?,
    onOpenBay: (() -> Unit)?,
    onOpenMap: (() -> Unit)?,
    body: String,
    imageUrl: String? = null,
    onViewImage: (() -> Unit)? = null,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "Submitted photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp)
                    .then(if (onViewImage != null) Modifier.clickable(onClick = onViewImage) else Modifier),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$author · $date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(bayName, style = MaterialTheme.typography.titleMedium)
            place?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Place, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onOpenBay?.let { TextButton(onClick = it) { Text("View bay") } }
                onOpenMap?.let { TextButton(onClick = it) { Text("Open in maps") } }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onReject) { Text("Reject") }
                Button(onClick = onApprove) { Text("Approve") }
            }
        }
    }
}
