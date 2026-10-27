package uk.motobay.app.ui.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private enum class Kind(val label: String, val noun: String) { Photos("Photos", "Photo"), Comments("Comments", "Comment"), Bays("Bays", "Bay") }

private val ModerationStatus.filterLabel: String
    get() = when (this) { ModerationStatus.Pending -> "Waiting"; ModerationStatus.Approved -> "Approved"; ModerationStatus.Rejected -> "Rejected" }

/**
 * Moderation queue: nothing riders submit is public until it's approved here.
 * Approved and rejected items can be revisited, re-decided or deleted for good.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val data by app.community.data.collectAsStateWithLifecycle()
    val allBays by app.allBays.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var kind by rememberSaveable { mutableStateOf(Kind.Photos) }
    var showing by rememberSaveable { mutableStateOf(ModerationStatus.Pending) }
    LaunchedEffect(kind, showing) { app.crashLog.noteScreen("ReviewDest/${kind.label}/${showing.filterLabel}") }
    var viewing by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<Pair<Kind, String>?>(null) }
    var rejecting by remember { mutableStateOf<Pair<Kind, String>?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    val photos = data.photos.filter { it.status == showing }
    val comments = data.comments.filter { it.status == showing }
    val baySubmissions = data.bays.filter { it.status == showing }
    fun waiting(k: Kind) = when (k) {
        Kind.Photos -> data.photos.count { it.status == ModerationStatus.Pending }
        Kind.Comments -> data.comments.count { it.status == ModerationStatus.Pending }
        Kind.Bays -> data.bays.count { it.status == ModerationStatus.Pending }
    }

    fun decide(kind: Kind, id: String, status: ModerationStatus, reason: String? = null) {
        suspend fun apply(s: ModerationStatus, why: String? = null) {
            when (kind) {
                Kind.Photos -> app.community.moderatePhoto(id, s, why)
                Kind.Comments -> app.community.moderateComment(id, s, why)
                Kind.Bays -> app.community.moderateBay(id, s, why)
            }
        }
        val before = showing
        scope.launch {
            if (runCatching { apply(status, reason) }.isFailure) {
                snackbar.showSnackbar("Couldn't update that ${kind.noun.lowercase()}. Check you're online.")
                return@launch
            }
            val verb = when (status) { ModerationStatus.Approved -> "approved"; ModerationStatus.Rejected -> "rejected"; ModerationStatus.Pending -> "back in the queue" }
            if (snackbar.showSnackbar("${kind.noun} $verb", actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
                runCatching { apply(before) }
            }
        }
    }

    fun delete(kind: Kind, id: String) {
        scope.launch {
            val ok = runCatching {
                when (kind) {
                    Kind.Photos -> app.community.deletePhoto(id)
                    Kind.Comments -> app.community.deleteComment(id)
                    Kind.Bays -> app.community.deleteBay(id)
                }
            }.isSuccess
            snackbar.showSnackbar(if (ok) "${kind.noun} deleted" else "Couldn't delete that ${kind.noun.lowercase()}. Check you're online.")
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
                        val count = waiting(k)
                        SegmentedButton(
                            selected = kind == k,
                            onClick = { kind = k },
                            shape = SegmentedButtonDefaults.itemShape(i, Kind.entries.size),
                            icon = {},
                            label = {
                                // Badge beside the word, and the word shrinks rather than clips at large font sizes.
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        k.label, maxLines = 1, softWrap = false,
                                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (count > 0) Badge(Modifier.padding(start = 4.dp)) { Text("$count") }
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = "${k.label}, $count waiting" },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModerationStatus.entries.forEach { s ->
                        FilterChip(selected = showing == s, onClick = { showing = s }, label = { Text(s.filterLabel) })
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
                        Text(if (showing == ModerationStatus.Pending) "All caught up" else "Nothing here", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "No ${showing.filterLabel.lowercase()} ${kind.label.lowercase()}.",
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
                        status = p.status,
                        reason = p.reason,
                        onDecide = { if (it == ModerationStatus.Rejected) rejecting = Kind.Photos to p.id else decide(Kind.Photos, p.id, it) },
                        onDelete = { confirmDelete = Kind.Photos to p.id },
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
                        status = c.status,
                        reason = c.reason,
                        onDecide = { if (it == ModerationStatus.Rejected) rejecting = Kind.Comments to c.id else decide(Kind.Comments, c.id, it) },
                        onDelete = { confirmDelete = Kind.Comments to c.id },
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
                        // An approved new bay is on the map under its own id.
                        onOpenBay = when {
                            b.editOf != null -> ({ onOpenBay(b.editOf) })
                            b.status == ModerationStatus.Approved -> ({ onOpenBay("rider/${b.id}") })
                            else -> null
                        },
                        onOpenMap = { openMap(context, b.lat, b.lon, if (b.editOf != null) original?.title ?: "Bay" else "New bay") },
                        body = facts.joinToString("\n"),
                        status = b.status,
                        reason = b.reason,
                        onDecide = { if (it == ModerationStatus.Rejected) rejecting = Kind.Bays to b.id else decide(Kind.Bays, b.id, it) },
                        onDelete = { confirmDelete = Kind.Bays to b.id },
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

    rejecting?.let { (k, id) ->
        RejectSheet(
            noun = k.noun.lowercase(),
            onDismiss = { rejecting = null },
            onReject = { why -> rejecting = null; decide(k, id, ModerationStatus.Rejected, why) },
        )
    }

    confirmDelete?.let { (k, id) ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this ${k.noun.lowercase()}?") },
            text = { Text("It will be removed for everyone. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = null; delete(k, id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Asks why, so the rider can fix the problem and resubmit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectSheet(noun: String, onDismiss: () -> Unit, onReject: (String) -> Unit) {
    var why by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Reject this $noun?", style = MaterialTheme.typography.headlineSmall)
            Text(
                "The rider will see why, so they can fix it and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = why,
                onValueChange = { why = it.take(300) },
                label = { Text("Reason (optional)") },
                supportingText = { Text("For example: number plate visible, not a motorcycle bay · ${why.length}/300") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = { onReject(why.trim()) }, modifier = Modifier.testTag("reject-confirm")) { Text("Reject") }
            }
        }
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
    status: ModerationStatus,
    reason: String? = null,
    onDecide: (ModerationStatus) -> Unit,
    onDelete: () -> Unit,
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
            if (status == ModerationStatus.Rejected) {
                Text(
                    if (reason.isNullOrBlank()) "Rejected, no reason given" else "Rejected: $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onOpenBay?.let { TextButton(onClick = it) { Text("View bay") } }
                onOpenMap?.let { TextButton(onClick = it) { Text("Open in maps") } }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    // Whatever the item's status, offer the other two outcomes.
                    if (status != ModerationStatus.Rejected) OutlinedButton(onClick = { onDecide(ModerationStatus.Rejected) }) { Text("Reject") }
                    if (status != ModerationStatus.Approved) Button(onClick = { onDecide(ModerationStatus.Approved) }) { Text("Approve") }
                    if (status != ModerationStatus.Pending) OutlinedButton(onClick = { onDecide(ModerationStatus.Pending) }) { Text("Re-queue") }
                }
            }
        }
    }
}
