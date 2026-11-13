package uk.motobay.app.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.components.label
import java.text.DateFormat
import java.util.Date

/** One row per thing the signed-in rider has submitted. */
private data class Submission(
    val id: String,
    val kind: String,
    val icon: ImageVector,
    val bayId: String?,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val createdAt: Long,
    val status: ModerationStatus,
    val reason: String?,
) {
    fun matches(query: String) = query.isBlank() || listOf(kind, title, body, reason.orEmpty()).any { it.contains(query, ignoreCase = true) }
}

private val ModerationStatus.tabLabel: String
    get() = when (this) { ModerationStatus.Pending -> "Waiting"; ModerationStatus.Approved -> "Approved"; ModerationStatus.Rejected -> "Rejected" }

/**
 * Everything the rider has sent in, split by review status and searchable.
 * Rows are compact; tapping one opens the full details, including why
 * something was turned down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubmissionsScreen(onBack: () -> Unit, onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val account by app.auth.user.collectAsStateWithLifecycle()
    val data by app.community.data.collectAsStateWithLifecycle()
    val allBays by app.allBays.collectAsStateWithLifecycle()
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val uid = account?.uid
    var tab by rememberSaveable { mutableStateOf(ModerationStatus.Pending) }
    var query by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf<Submission?>(null) }

    val mine = if (uid == null) emptyList() else buildList {
        data.photos.filter { it.authorUid == uid }.forEach { p ->
            add(Submission("photo/${p.id}", "Photo", Icons.Outlined.PhotoCamera, p.bayId, allBays[p.bayId]?.title ?: p.bayName, p.caption.ifEmpty { "No caption" }, p.url, p.createdAt, p.status, p.reason))
        }
        data.comments.filter { it.authorUid == uid }.forEach { c ->
            add(Submission("comment/${c.id}", "Comment", Icons.Outlined.ChatBubbleOutline, c.bayId, allBays[c.bayId]?.title ?: c.bayName, c.text, null, c.createdAt, c.status, c.reason))
        }
        data.bays.filter { it.authorUid == uid }.forEach { b ->
            val original = b.editOf?.let { allBays[it] }
            val facts = listOfNotNull(
                b.name.takeIf { it.isNotBlank() }?.let { "Name: $it" },
                "Price: ${b.fee.label}",
                b.capacity?.let { "Spaces: $it" },
                b.covered?.let { "Covered: ${if (it) "yes" else "no"}" },
                b.notes.takeIf { it.isNotBlank() }?.let { "Notes: $it" },
            )
            val openId = b.editOf ?: "rider/${b.id}".takeIf { b.status == ModerationStatus.Approved }
            val isEdit = b.editOf != null
            add(Submission("bay/${b.id}", if (isEdit) "Correction" else "New bay", if (isEdit) Icons.Outlined.EditLocationAlt else Icons.Outlined.AddLocationAlt, openId, original?.title ?: b.name.ifBlank { "New bay" }, facts.joinToString(" · "), null, b.createdAt, b.status, b.reason))
        }
    }.sortedByDescending { it.createdAt }
    val shown = mine.filter { it.status == tab && it.matches(query.trim()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My submissions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                placeholder = { Text("Search bay, text or reason") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                ModerationStatus.entries.forEachIndexed { i, s ->
                    val count = mine.count { it.status == s }
                    SegmentedButton(
                        selected = tab == s,
                        onClick = { tab = s },
                        shape = SegmentedButtonDefaults.itemShape(i, ModerationStatus.entries.size),
                        icon = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    s.tabLabel, maxLines = 1, softWrap = false,
                                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (count > 0) Badge(Modifier.padding(start = 4.dp)) { Text("$count") }
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "${s.tabLabel}, $count" },
                    )
                }
            }
            if (shown.isEmpty()) {
                Text(
                    when {
                        uid == null -> "Sign in to see what you've submitted."
                        query.isNotBlank() -> "Nothing ${tab.tabLabel.lowercase()} matches \"${query.trim()}\"."
                        tab == ModerationStatus.Pending -> "Nothing waiting for review."
                        else -> "Nothing ${tab.tabLabel.lowercase()} yet."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown, key = { it.id }) { s ->
                    ListItem(
                        headlineContent = { Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        overlineContent = { Text("${s.kind} · ${dateFormat.format(Date(s.createdAt))}") },
                        supportingContent = {
                            Text(
                                if (s.status == ModerationStatus.Rejected && !s.reason.isNullOrBlank()) "Reason: ${s.reason}" else s.body,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = { Thumb(s) },
                        modifier = Modifier.clickable { open = s },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    open?.let { s ->
        ModalBottomSheet(onDismissRequest = { open = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${s.kind} · ${dateFormat.format(Date(s.createdAt))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.title, style = MaterialTheme.typography.headlineSmall)
                s.imageUrl?.let {
                    AsyncImage(
                        model = it, contentDescription = "Your photo", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(MaterialTheme.shapes.medium),
                    )
                }
                Text(s.body, style = MaterialTheme.typography.bodyLarge)
                StatusLine(s.status, s.reason)
                s.bayId?.let { id ->
                    Button(onClick = { open = null; onOpenBay(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (s.status == ModerationStatus.Rejected) "Open bay and try again" else "View bay")
                    }
                }
            }
        }
    }
}

/** A small photo, or an icon for the kind of submission. */
@Composable
private fun Thumb(s: Submission) {
    if (s.imageUrl != null) {
        AsyncImage(
            model = s.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
        )
    } else {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(s.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun StatusLine(status: ModerationStatus, reason: String?) {
    val (icon, text, colour) = when (status) {
        ModerationStatus.Pending -> Triple(Icons.Outlined.Schedule, "Waiting for review", MaterialTheme.colorScheme.onSurfaceVariant)
        ModerationStatus.Approved -> Triple(Icons.Filled.CheckCircle, "Approved and public", MaterialTheme.colorScheme.primary)
        ModerationStatus.Rejected -> Triple(Icons.Outlined.Cancel, if (reason.isNullOrBlank()) "Not approved, no reason given" else "Not approved: $reason", MaterialTheme.colorScheme.error)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = colour)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colour)
    }
}
