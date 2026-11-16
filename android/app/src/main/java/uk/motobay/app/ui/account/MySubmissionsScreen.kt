package uk.motobay.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontWeight
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraPillTabs
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.FrostDivider
import uk.motobay.app.ui.components.FrostPageHeader
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.components.frostOverline
import uk.motobay.app.ui.theme.AuroraPalette
import uk.motobay.app.ui.theme.auroraPalette
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
    val palette = auroraPalette()
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

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FrostPageHeader("My submissions", palette, onBack)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                placeholder = { Text("Search bay, text or reason", style = auroraStyle(14.sp, FontWeight.SemiBold, palette.muted)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = palette.muted) },
                singleLine = true,
                shape = CircleShape,
                textStyle = auroraStyle(14.sp, FontWeight.SemiBold, palette.cardTitleText),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.cardSurface, unfocusedContainerColor = palette.cardSurface,
                    focusedBorderColor = palette.selectedBorder, unfocusedBorderColor = palette.cardBorder,
                    cursorColor = palette.accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            val statuses = ModerationStatus.entries
            AuroraPillTabs(
                labels = statuses.map { it.tabLabel },
                selected = statuses.indexOf(tab),
                palette = palette,
                onSelect = { tab = statuses[it] },
                badge = { i -> mine.count { it.status == statuses[i] } },
                segmentModifier = { i -> Modifier.semantics { contentDescription = "${statuses[i].tabLabel}, ${mine.count { it.status == statuses[i] }}" } },
            )
        }
        if (shown.isEmpty()) {
            Text(
                when {
                    uid == null -> "Sign in to see what you've submitted."
                    query.isNotBlank() -> "Nothing ${tab.tabLabel.lowercase()} matches \"${query.trim()}\"."
                    tab == ModerationStatus.Pending -> "Nothing waiting for review."
                    else -> "Nothing ${tab.tabLabel.lowercase()} yet."
                },
                style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext),
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 24.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                item {
                    AuroraCard(palette, contentPadding = 4) {
                        shown.forEachIndexed { i, s ->
                            if (i > 0) FrostDivider(palette)
                            SubmissionRow(s, dateFormat.format(Date(s.createdAt)), palette) { open = s }
                        }
                    }
                }
            }
        }
    }

    open?.let { s ->
        ModalBottomSheet(
            onDismissRequest = { open = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.searchSurface,
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${s.kind} \u00B7 ${dateFormat.format(Date(s.createdAt))}".uppercase(), style = frostOverline(palette.cardSubtext))
                Text(s.title, style = auroraStyle(22.sp, FontWeight.ExtraBold, palette.cardTitleText).copy(lineHeight = 26.sp))
                s.imageUrl?.let {
                    AsyncImage(
                        model = it, contentDescription = "Your photo", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(18.dp)),
                    )
                }
                Text(s.body, style = auroraStyle(14.sp, FontWeight.Medium, palette.text2))
                StatusLine(s.status, s.reason, palette)
                s.bayId?.let { id ->
                    AuroraPrimaryButton(
                        if (s.status == ModerationStatus.Rejected) "Open bay and try again" else "View bay", palette,
                        onClick = { open = null; onOpenBay(id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}

/** One submission in the list: thumbnail, kind and date, the bay, and the text or rejection reason. */
@Composable
private fun SubmissionRow(s: Submission, date: String, palette: AuroraPalette, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Thumb(s, palette)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${s.kind} \u00B7 $date".uppercase(), style = frostOverline(palette.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.title, style = auroraStyle(15.sp, FontWeight.Bold, palette.cardTitleText).copy(lineHeight = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (s.status == ModerationStatus.Rejected && !s.reason.isNullOrBlank()) "Reason: ${s.reason}" else s.body,
                style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A small photo, or an icon for the kind of submission. */
@Composable
private fun Thumb(s: Submission, palette: AuroraPalette) {
    if (s.imageUrl != null) {
        AsyncImage(
            model = s.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)),
        )
    } else {
        val (fill, ink) = palette.toneBlue
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(fill), contentAlignment = Alignment.Center) {
            Icon(s.icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StatusLine(status: ModerationStatus, reason: String?, palette: AuroraPalette) {
    val (icon, text, colour) = when (status) {
        ModerationStatus.Pending -> Triple(Icons.Outlined.Schedule, "Waiting for review", palette.cardSubtext)
        ModerationStatus.Approved -> Triple(Icons.Filled.CheckCircle, "Approved and public", palette.free)
        ModerationStatus.Rejected -> Triple(Icons.Outlined.Cancel, if (reason.isNullOrBlank()) "Not approved, no reason given" else "Not approved: $reason", palette.unknown)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = colour)
        Text(text, style = auroraStyle(13.sp, FontWeight.Bold, colour))
    }
}
