package uk.motobay.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.components.label
import java.text.DateFormat
import java.util.Date

/** One row per thing the signed-in rider has submitted, newest first. */
private data class Submission(
    val id: String,
    val kind: String,
    val bayId: String?,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val createdAt: Long,
    val status: ModerationStatus,
    val reason: String?,
)

/** Everything the rider has sent in, with where it is in review and why if it was turned down. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubmissionsScreen(onBack: () -> Unit, onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val account by app.auth.user.collectAsStateWithLifecycle()
    val data by app.community.data.collectAsStateWithLifecycle()
    val allBays by app.allBays.collectAsStateWithLifecycle()
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val uid = account?.uid

    val mine = if (uid == null) emptyList() else buildList {
        data.photos.filter { it.authorUid == uid }.forEach { p ->
            add(Submission("photo/${p.id}", "Photo", p.bayId, allBays[p.bayId]?.title ?: p.bayName, p.caption.ifEmpty { "No caption" }, p.url, p.createdAt, p.status, p.reason))
        }
        data.comments.filter { it.authorUid == uid }.forEach { c ->
            add(Submission("comment/${c.id}", "Comment", c.bayId, allBays[c.bayId]?.title ?: c.bayName, c.text, null, c.createdAt, c.status, c.reason))
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
            add(Submission("bay/${b.id}", if (b.editOf != null) "Correction" else "New bay", openId, original?.title ?: "New bay", facts.joinToString("\n"), null, b.createdAt, b.status, b.reason))
        }
    }.sortedByDescending { it.createdAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My submissions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = padding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (mine.isEmpty()) {
                item {
                    Text(
                        if (uid == null) "Sign in to see what you've submitted." else "Nothing yet. Photos, comments and bay changes you send in will show here with their review status.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            }
            items(mine, key = { it.id }) { s ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    s.imageUrl?.let {
                        AsyncImage(model = it, contentDescription = "Your photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().size(160.dp))
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${s.kind} · ${dateFormat.format(Date(s.createdAt))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s.title, style = MaterialTheme.typography.titleMedium)
                        Text(s.body, style = MaterialTheme.typography.bodyMedium)
                        StatusLine(s.status, s.reason)
                        s.bayId?.let { id ->
                            TextButton(onClick = { onOpenBay(id) }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (s.status == ModerationStatus.Rejected) "Open bay and try again" else "View bay")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(status: ModerationStatus, reason: String?) {
    val (icon, text, colour) = when (status) {
        ModerationStatus.Pending -> Triple(Icons.Outlined.Schedule, "Waiting for review", MaterialTheme.colorScheme.onSurfaceVariant)
        ModerationStatus.Approved -> Triple(Icons.Filled.CheckCircle, "Approved and public", MaterialTheme.colorScheme.primary)
        ModerationStatus.Rejected -> Triple(Icons.Outlined.Cancel, if (reason.isNullOrBlank()) "Not approved" else "Not approved: $reason", MaterialTheme.colorScheme.error)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = colour)
        Text(text, style = MaterialTheme.typography.bodySmall, color = colour)
    }
}
