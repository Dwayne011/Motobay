package uk.motobay.app.ui.bay

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraOutlinedButton
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.AuroraTag
import uk.motobay.app.ui.components.AuroraTextAction
import uk.motobay.app.ui.components.Coral
import uk.motobay.app.ui.components.HSpace
import uk.motobay.app.ui.components.Slate950
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.components.label
import uk.motobay.app.ui.theme.AuroraPalette
import uk.motobay.app.ui.theme.auroraPalette
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import uk.motobay.app.data.Bay
import uk.motobay.app.data.DisplayNames
import uk.motobay.app.data.BayComment
import uk.motobay.app.data.BayPhoto
import uk.motobay.app.data.BayLoadState
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.components.ReportSheet
import uk.motobay.app.data.ReportTarget
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalRequireSignIn
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.util.formatDistance
import uk.motobay.app.util.openDirections
import uk.motobay.app.util.openInMapsApp
import uk.motobay.app.util.shareBay
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Full-screen bay detail (compact and medium windows). */
@Composable
fun BayDetailRoute(id: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val palette = auroraPalette()
    val allBays by app.allBays.collectAsStateWithLifecycle()
    val loadState by app.bays.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { if (allBays[id] == null) app.bays.load() }

    Surface(color = palette.mapBackground, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            val bay = allBays[id]
            when {
                bay != null -> BayDetailContent(bay, onDeleted = onBack, onBack = onBack)
                loadState == BayLoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = palette.searchIconActive)
                else -> Column(Modifier.padding(16.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.cardTitleText) }
                    Text(
                        "This bay couldn't be found. It may have been removed from the map.",
                        style = auroraStyle(12.sp, color = palette.cardSubtext),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Everything about one bay: facts, actions, community photos and comments.
 * Moderators can also delete rider content from here, in case something was
 * approved by mistake; [onDeleted] runs after the bay itself is deleted.
 */
@Composable
fun BayDetailContent(bay: Bay, onDeleted: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    val app = LocalApp.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val palette = auroraPalette()
    val account by app.auth.user.collectAsStateWithLifecycle()
    val moderator = account?.isModerator == true
    val community by app.community.data.collectAsStateWithLifecycle()
    // Approved corrections riders made to this bay, newest first.
    val corrections = community.bays.filter { it.editOf == bay.id && it.status == ModerationStatus.Approved }
    var confirm by remember { mutableStateOf<ModeratorDelete?>(null) }
    val favourites by app.user.favourites.collectAsStateWithLifecycle()
    val position by app.location.position.collectAsStateWithLifecycle()
    val saved = bay.id in favourites
    var editing by rememberSaveable { mutableStateOf(false) }
    var reportingBay by remember { mutableStateOf(false) }
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val requireSignIn = LocalRequireSignIn.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header: back arrow, 20sp title, 12sp place and distance.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.cardTitleText) }
            }
            Column(Modifier.weight(1f)) {
                Text(bay.title, style = auroraStyle(20.sp, FontWeight.Bold, palette.cardTitleText))
                VSpace(2)
                val subtitle = listOfNotNull(bay.locality, bay.operator, position?.let { "${formatDistance(distanceMetres(it, bay))} away" })
                    .joinToString(" \u2022 ").ifEmpty { "Motorcycle parking" }
                Text(subtitle, style = auroraStyle(12.sp, color = palette.cardSubtext))
            }
        }

        // Price tag and the other facts as capsule tags.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AuroraTag(if (bay.fee == Fee.Unknown) "Price unknown" else bay.fee.label, palette)
            bay.capacity?.let { AuroraTag("$it spaces", palette) }
            bay.covered?.let { AuroraTag(if (it) "Covered" else "Not covered", palette) }
            bay.maxStay?.let { AuroraTag("Max stay $it", palette) }
        }
        bay.openingHours?.let { Text(it, style = auroraStyle(12.sp, color = palette.cardSubtext)) }

        val unknowns = listOfNotNull(
            "free or paid".takeIf { bay.fee == Fee.Unknown },
            "covered".takeIf { bay.covered == null },
        )
        if (unknowns.isNotEmpty()) {
            // Crowdsourcing banner.
            AuroraCard(palette, radius = 16, color = palette.searchSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Do you know if this bay is ${unknowns.joinToString(", or whether it's ")}?",
                        style = auroraStyle(12.sp, color = palette.cardTitleText),
                        modifier = Modifier.weight(1f),
                    )
                    HSpace(12)
                    AuroraTextAction("Tell us", palette.searchIconActive, onClick = { requireSignIn { editing = true } })
                }
            }
        }

        // Action toolbar: Directions, Save, Share, Report; all 40dp with 12dp corners.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var menu by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f)) {
                AuroraPrimaryButton(
                    "Directions", palette, onClick = { openDirections(context, bay) },
                    icon = Icons.Filled.Navigation, modifier = Modifier.fillMaxWidth().height(40.dp),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Open in another maps app") },
                        leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        onClick = { menu = false; openInMapsApp(context, bay) },
                    )
                }
            }
            Surface(
                onClick = {
                    app.user.toggleFavourite(bay)
                    scope.launch { snackbar.showSnackbar(if (saved) "Removed from saved" else "Saved") }
                },
                shape = RoundedCornerShape(12.dp),
                color = if (saved) Color(0x33F59E0B) else Color.Transparent,
                border = BorderStroke(1.dp, if (saved) Color(0x80F59E0B) else palette.cardBorder),
                modifier = Modifier.height(40.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (saved) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null,
                        tint = if (saved) AuroraPalette.StatusPaid else palette.cardSubtext, modifier = Modifier.size(16.dp),
                    )
                    Text(if (saved) "Saved" else "Save", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardTitleText), maxLines = 1, softWrap = false)
                }
            }
            ToolbarIcon(Icons.Filled.Share, "Share", palette) { shareBay(context, bay) }
            ToolbarIcon(Icons.Outlined.Flag, "Report this bay", palette) { requireSignIn { reportingBay = true } }
            ToolbarIcon(Icons.Filled.Map, "Open in another maps app", palette) { menu = true }
        }

        PhotosSection(bay, moderator, palette)
        CommentsSection(bay, moderator, palette)

        HorizontalDivider(color = palette.cardBorder)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Data", style = auroraStyle(11.sp, FontWeight.Bold, palette.cardTitleText))
            Text(
                when {
                    bay.isSample -> "This is a development sample, not a real bay."
                    bay.isRiderAdded -> "Added by ${bay.addedBy ?: "a rider"}."
                    else -> "Bay data \u00a9 OpenStreetMap contributors, under the Open Database Licence."
                },
                style = auroraStyle(10.sp, color = palette.cardSubtext),
            )
            Surface(
                onClick = { requireSignIn { editing = true } },
                shape = RoundedCornerShape(18.dp),
                color = palette.searchSurface,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier.fillMaxWidth().height(36.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.cardSubtext, modifier = Modifier.size(14.dp))
                    Text("Suggest an edit", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardTitleText))
                }
            }
            if (moderator && bay.isRiderAdded) {
                AuroraOutlinedButton("Delete this bay", palette, color = Coral, onClick = { confirm = ModeratorDelete.Bay }, modifier = Modifier.fillMaxWidth())
            }
            if (moderator && corrections.isNotEmpty()) {
                AuroraOutlinedButton(
                    if (corrections.size == 1) "Remove the rider correction" else "Remove ${corrections.size} rider corrections",
                    palette, color = Coral, onClick = { confirm = ModeratorDelete.Corrections }, modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!bay.isSample && !bay.isRiderAdded) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                        val (type, num) = bay.id.split("/")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/edit?$type=$num")))
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = palette.searchIconActive, modifier = Modifier.size(14.dp))
                    Text("Edit on OpenStreetMap", style = auroraStyle(11.sp, FontWeight.Medium, palette.searchIconActive))
                }
            }
        }
    }

    if (reportingBay) {
        ReportSheet(
            what = "bay",
            onDismiss = { reportingBay = false },
            onSubmit = { reason, details ->
                reportingBay = false
                scope.launch {
                    val ok = runCatching { app.community.report(ReportTarget.Bay, bay.id, bay.addedBy ?: "", reason, details) }.isSuccess
                    snackbar.showSnackbar(if (ok) "Thanks, the bay has been reported." else "Couldn't send that report. Check you're online.")
                }
            },
        )
    }

    confirm?.let { what ->
        val (title, text) = when (what) {
            ModeratorDelete.Bay -> "Delete this bay?" to "It will disappear from the map for everyone. This can't be undone."
            ModeratorDelete.Corrections -> "Remove rider corrections?" to "The bay goes back to its OpenStreetMap details. This can't be undone."
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        val ok = runCatching {
                            when (what) {
                                ModeratorDelete.Bay -> app.community.deleteBay(bay.id.removePrefix("rider/"))
                                ModeratorDelete.Corrections -> corrections.forEach { app.community.deleteBay(it.id) }
                            }
                        }.isSuccess
                        if (ok && what == ModeratorDelete.Bay) onDeleted?.invoke()
                        snackbar.showSnackbar(
                            when {
                                !ok -> "Couldn't delete that. Check you're online."
                                what == ModeratorDelete.Bay -> "Bay deleted"
                                else -> "Corrections removed"
                            },
                        )
                    }
                }) { Text(if (what == ModeratorDelete.Bay) "Delete" else "Remove") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }

    if (editing) {
        BayFormSheet(
            at = LatLon(bay.lat, bay.lon),
            editing = bay,
            onDismiss = { editing = false },
            onSubmit = { submission ->
                editing = false
                scope.launch {
                    val ok = runCatching {
                        app.community.submitBay(submission.copy(author = settings.displayName.ifBlank { DisplayNames.ANONYMOUS }))
                    }.isSuccess
                    snackbar.showSnackbar(if (ok) "Thanks! Your correction will be checked before it's published." else "Couldn't send that. Check you're signed in and online.")
                }
            },
        )
    }
}

/** 40dp bordered icon button for the action toolbar. */
@Composable
private fun ToolbarIcon(icon: ImageVector, description: String, palette: AuroraPalette, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, tint = palette.cardSubtext, modifier = Modifier.size(16.dp)) }
    }
}

private enum class ModeratorDelete { Bay, Corrections }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotosSection(bay: Bay, moderator: Boolean, palette: AuroraPalette) {
    val app = LocalApp.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val community by app.community.data.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val blocked = settings.blocked.map { it.uid }.toSet()
    val approved = community.photos.filter { it.bayId == bay.id && it.status == ModerationStatus.Approved && it.authorUid !in blocked }
    val pending = community.photos.count { it.bayId == bay.id && it.status == ModerationStatus.Pending }
    val account by app.auth.user.collectAsStateWithLifecycle()
    var reporting by remember { mutableStateOf<BayPhoto?>(null) }

    val requireSignIn = LocalRequireSignIn.current
    var chooser by remember { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var viewing by remember { mutableStateOf<BayPhoto?>(null) }
    var deleting by remember { mutableStateOf<BayPhoto?>(null) }

    // Android photo picker: no storage permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) draft = uri }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) draft = cameraUri }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Photos", style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText), modifier = Modifier.weight(1f))
            Surface(
                onClick = { requireSignIn { chooser = true } },
                shape = CircleShape,
                color = palette.searchSurface,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier.height(32.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = palette.cardSubtext, modifier = Modifier.size(14.dp))
                    Text("Add photo", style = auroraStyle(11.sp, FontWeight.Medium, palette.cardTitleText), maxLines = 1, softWrap = false)
                }
            }
        }
        if (approved.isEmpty()) {
            Text("No photos yet. Help other riders by adding one.", style = auroraStyle(12.sp, color = palette.cardSubtext))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(approved, key = { it.id }) { p ->
                    AsyncImage(
                        model = p.url,
                        contentDescription = p.caption.ifEmpty { "Photo of ${bay.name} by ${p.author}" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).clickable { viewing = p },
                    )
                }
            }
        }
        if (pending > 0) {
            Text(
                if (pending == 1) "1 photo is waiting for review." else "$pending photos are waiting for review.",
                style = auroraStyle(10.sp, color = palette.cardSubtext),
            )
        }
    }

    if (chooser) {
        ModalBottomSheet(onDismissRequest = { chooser = false }, sheetState = rememberModalBottomSheetState()) {
            ListItem(
                headlineContent = { Text("Take a photo") },
                leadingContent = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                modifier = Modifier.clickable {
                    chooser = false
                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    cameraUri = uri
                    camera.launch(uri)
                },
            )
            ListItem(
                headlineContent = { Text("Choose from gallery") },
                leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                modifier = Modifier.clickable {
                    chooser = false
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            Box(Modifier.padding(bottom = 24.dp))
        }
    }

    draft?.let { uri ->
        var caption by rememberSaveable { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!busy) draft = null },
            title = { Text("Add a photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(MaterialTheme.shapes.medium),
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(140) },
                        label = { Text("Caption (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Photos are checked before they're published. Please avoid faces and number plates.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        val ok = runCatching {
                            app.community.submitPhoto(bay, uri, caption.trim(), settings.displayName.ifBlank { DisplayNames.ANONYMOUS })
                        }.isSuccess
                        busy = false
                        draft = null
                        snackbar.showSnackbar(if (ok) "Thanks! Your photo will appear once it's been reviewed." else "That photo couldn't be saved")
                    }
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { draft = null }) { Text("Cancel") } },
        )
    }

    viewing?.let { photo ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(model = photo.url, contentDescription = null, modifier = Modifier.fillMaxWidth())
                    Text("${photo.author} · ${photo.caption.ifEmpty { "No caption" }}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Close") } },
            dismissButton = {
                Row {
                    if (moderator) {
                        TextButton(onClick = { viewing = null; deleting = photo }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    } else if (photo.authorUid.isNotEmpty() && photo.authorUid != account?.uid) {
                        TextButton(onClick = { viewing = null; requireSignIn { reporting = photo } }) { Text("Report") }
                        TextButton(onClick = {
                            viewing = null
                            app.user.block(photo.authorUid, photo.author)
                            scope.launch { snackbar.showSnackbar("${photo.author} blocked. Unblock in Settings.") }
                        }) { Text("Block") }
                    }
                }
            },
        )
    }

    reporting?.let { photo ->
        ReportSheet(
            what = "photo",
            onDismiss = { reporting = null },
            onSubmit = { reason, details ->
                reporting = null
                scope.launch {
                    val ok = runCatching { app.community.report(ReportTarget.Photo, photo.id, photo.authorUid, reason, details) }.isSuccess
                    snackbar.showSnackbar(if (ok) "Thanks, the photo has been reported." else "Couldn't send that report. Check you're online.")
                }
            },
        )
    }

    deleting?.let { photo ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this photo?") },
            text = { Text("It will be removed for everyone. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        val ok = runCatching { app.community.deletePhoto(photo.id) }.isSuccess
                        snackbar.showSnackbar(if (ok) "Photo deleted" else "Couldn't delete that photo. Check you're online.")
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CommentsSection(bay: Bay, moderator: Boolean, palette: AuroraPalette) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val requireSignIn = LocalRequireSignIn.current
    val community by app.community.data.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val blocked = settings.blocked.map { it.uid }.toSet()
    val approved = community.comments.filter { it.bayId == bay.id && it.status == ModerationStatus.Approved && it.authorUid !in blocked }
    val pending = community.comments.count { it.bayId == bay.id && it.status == ModerationStatus.Pending }
    val account by app.auth.user.collectAsStateWithLifecycle()
    var reporting by remember { mutableStateOf<BayComment?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<BayComment?>(null) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Comments", style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText))
        if (approved.isEmpty()) {
            Text("No comments yet. Is it easy to find? Is it usually full?", style = auroraStyle(12.sp, color = palette.cardSubtext))
        }
        approved.forEach { c ->
            AuroraCard(palette, radius = 12, contentPadding = 12) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(palette.chipSelectedBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(c.author.take(1).uppercase(), style = auroraStyle(13.sp, FontWeight.Bold, Color.White))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${c.author} \u2022 ${dateFormat.format(Date(c.createdAt))}", style = auroraStyle(11.sp, color = palette.cardSubtext))
                        Text(c.text, style = auroraStyle(12.sp, color = palette.cardTitleText))
                    }
                    when {
                        moderator -> IconButton(onClick = { deleting = c }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete comment", tint = Coral, modifier = Modifier.size(18.dp))
                        }
                        c.authorUid.isNotEmpty() && c.authorUid != account?.uid -> {
                            var menu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${c.author}'s comment", tint = palette.cardSubtext, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Report") },
                                        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                        onClick = { menu = false; requireSignIn { reporting = c } },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Block ${c.author}") },
                                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                        onClick = {
                                            menu = false
                                            app.user.block(c.authorUid, c.author)
                                            scope.launch { snackbar.showSnackbar("${c.author} blocked. Unblock in Settings.") }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (pending > 0) {
            Text(
                if (pending == 1) "1 comment is waiting for review." else "$pending comments are waiting for review.",
                style = auroraStyle(10.sp, color = palette.cardSubtext),
            )
        }
        // Comment input box: 12dp corners, 1dp border, transparent field inside.
        Surface(shape = RoundedCornerShape(12.dp), color = Color.Transparent, border = BorderStroke(1.dp, palette.cardBorder)) {
            Column(Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(500) },
                    placeholder = { Text("Add a comment", style = auroraStyle(12.sp, color = palette.cardSubtext)) },
                    textStyle = auroraStyle(12.sp, color = palette.cardTitleText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = palette.cardTitleText, unfocusedTextColor = palette.cardTitleText,
                        cursorColor = palette.searchIconActive,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                )
                VSpace(8)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Comments are checked before they're published \u2022 ${text.length}/500",
                        style = auroraStyle(10.sp, color = palette.cardSubtext),
                        modifier = Modifier.weight(1f),
                    )
                    HSpace(8)
                    val canPost = text.isNotBlank()
                    Surface(
                        onClick = {
                            requireSignIn {
                                val body = text.trim()
                                scope.launch {
                                    val ok = runCatching {
                                        app.community.submitComment(bay, body, settings.displayName.ifBlank { DisplayNames.ANONYMOUS })
                                    }.isSuccess
                                    if (ok) text = ""
                                    snackbar.showSnackbar(if (ok) "Thanks! Your comment will appear once it's been reviewed." else "Couldn't post that. Check you're signed in and online.")
                                }
                            }
                        },
                        enabled = canPost,
                        shape = CircleShape,
                        color = if (canPost) palette.searchIconActive else palette.searchSurface,
                        border = if (canPost) null else BorderStroke(1.dp, palette.cardBorder),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text("Post", style = auroraStyle(11.sp, FontWeight.SemiBold, if (canPost) Slate950 else palette.cardSubtext))
                        }
                    }
                }
            }
        }
    }

    reporting?.let { comment ->
        ReportSheet(
            what = "comment",
            onDismiss = { reporting = null },
            onSubmit = { reason, details ->
                reporting = null
                scope.launch {
                    val ok = runCatching { app.community.report(ReportTarget.Comment, comment.id, comment.authorUid, reason, details) }.isSuccess
                    snackbar.showSnackbar(if (ok) "Thanks, the comment has been reported." else "Couldn't send that report. Check you're online.")
                }
            },
        )
    }

    deleting?.let { comment ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this comment?") },
            text = { Text("\"${comment.text.take(80)}\" will be removed for everyone. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        val ok = runCatching { app.community.deleteComment(comment.id) }.isSuccess
                        snackbar.showSnackbar(if (ok) "Comment deleted" else "Couldn't delete that comment. Check you're online.")
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
