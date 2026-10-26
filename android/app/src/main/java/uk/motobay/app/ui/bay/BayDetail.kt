package uk.motobay.app.ui.bay

import android.content.Intent
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayLoadState
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalRequireSignIn
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.components.BayFacts
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.util.formatDistance
import uk.motobay.app.util.openDirections
import uk.motobay.app.util.openInMapsApp
import uk.motobay.app.util.shareBay
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Full-screen bay detail (compact and medium windows). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayDetailRoute(id: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val allBays by app.allBays.collectAsStateWithLifecycle()
    val loadState by app.bays.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { if (allBays[id] == null) app.bays.load() }
    // M3 small top app bar: changes to surfaceContainer when content scrolls under it.
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Bay details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val bay = allBays[id]
            when {
                bay != null -> BayDetailContent(bay)
                loadState == BayLoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Text(
                    "This bay couldn't be found. It may have been removed from the map.",
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Everything about one bay: facts, actions, community photos and comments. */
@Composable
fun BayDetailContent(bay: Bay) {
    val app = LocalApp.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val favourites by app.user.favourites.collectAsStateWithLifecycle()
    val position by app.location.position.collectAsStateWithLifecycle()
    val saved = bay.id in favourites
    var editing by rememberSaveable { mutableStateOf(false) }
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val requireSignIn = LocalRequireSignIn.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column {
            Text(bay.title, style = MaterialTheme.typography.headlineSmall)
            val subtitle = listOfNotNull(bay.locality, bay.operator, position?.let { "${formatDistance(distanceMetres(it, bay))} away" })
                .joinToString(" · ").ifEmpty { "Motorcycle parking" }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        BayFacts(bay)

        if (bay.fee == Fee.Unknown) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Do you know if this bay is free or paid?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { requireSignIn { editing = true } }) { Text("Tell us") }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var menu by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { openDirections(context, bay) }) {
                    Icon(Icons.Filled.Directions, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Text("Directions", Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Open in another maps app") },
                        leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        onClick = { menu = false; openInMapsApp(context, bay) },
                    )
                }
            }
            FilledTonalButton(onClick = {
                app.user.toggleFavourite(bay)
                scope.launch { snackbar.showSnackbar(if (saved) "Removed from saved" else "Saved") }
            }) {
                Icon(if (saved) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Text(if (saved) "Saved" else "Save", Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
            IconButton(onClick = { shareBay(context, bay) }) { Icon(Icons.Filled.Share, contentDescription = "Share") }
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.Map, contentDescription = "Open in another maps app") }
        }

        HorizontalDivider()
        PhotosSection(bay)
        HorizontalDivider()
        CommentsSection(bay)
        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Data", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when {
                    bay.isSample -> "This is a development sample, not a real bay."
                    bay.isRiderAdded -> "Added by ${bay.addedBy ?: "a rider"}."
                    else -> "Bay data © OpenStreetMap contributors, under the Open Database Licence."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { requireSignIn { editing = true } }) {
                Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Text("Suggest an edit", Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
            if (!bay.isSample && !bay.isRiderAdded) {
                TextButton(onClick = {
                    val (type, num) = bay.id.split("/")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/edit?$type=$num")))
                }) {
                    Icon(Icons.Filled.EditLocationAlt, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Text("Edit on OpenStreetMap", Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
            }
        }
    }

    if (editing) {
        BayFormSheet(
            at = LatLon(bay.lat, bay.lon),
            editing = bay,
            onDismiss = { editing = false },
            onSubmit = { submission ->
                editing = false
                val moderator = app.auth.user.value?.isModerator == true
                scope.launch {
                    val ok = runCatching {
                        app.community.submitBay(
                            submission.copy(author = settings.displayName.ifBlank { "Anonymous rider" }),
                            autoApprove = moderator,
                        )
                    }.isSuccess
                    snackbar.showSnackbar(
                        when {
                            !ok -> "Couldn't send that. Check you're signed in and online."
                            moderator -> "Bay updated"
                            else -> "Thanks! Your correction will be checked before it's published."
                        },
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotosSection(bay: Bay) {
    val app = LocalApp.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val community by app.community.data.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val approved = community.photos.filter { it.bayId == bay.id && it.status == ModerationStatus.Approved }
    val pending = community.photos.count { it.bayId == bay.id && it.status == ModerationStatus.Pending }

    val requireSignIn = LocalRequireSignIn.current
    var chooser by remember { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }

    // Android photo picker: no storage permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) draft = uri }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) draft = cameraUri }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Photos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { requireSignIn { chooser = true } }) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Text("Add photo", Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
        if (approved.isEmpty()) {
            Text("No photos yet. Help other riders by adding one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(approved, key = { it.id }) { p ->
                    AsyncImage(
                        model = p.url,
                        contentDescription = p.caption.ifEmpty { "Photo of ${bay.name} by ${p.author}" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.small).clickable { viewing = p.url },
                    )
                }
            }
        }
        if (pending > 0) {
            Text(
                if (pending == 1) "1 photo is waiting for review." else "$pending photos are waiting for review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            app.community.submitPhoto(bay, uri, caption.trim(), settings.displayName.ifBlank { "Anonymous rider" })
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

    viewing?.let { url ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            text = { AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun CommentsSection(bay: Bay) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val requireSignIn = LocalRequireSignIn.current
    val community by app.community.data.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val approved = community.comments.filter { it.bayId == bay.id && it.status == ModerationStatus.Approved }
    val pending = community.comments.count { it.bayId == bay.id && it.status == ModerationStatus.Pending }
    var text by rememberSaveable { mutableStateOf("") }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Comments", style = MaterialTheme.typography.titleMedium)
        if (approved.isEmpty()) {
            Text("No comments yet. Is it easy to find? Is it usually full?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        approved.forEach { c ->
            ListItem(
                headlineContent = { Text(c.text) },
                overlineContent = { Text("${c.author} · ${dateFormat.format(Date(c.createdAt))}") },
                leadingContent = {
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(c.author.take(1).uppercase(), color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                },
            )
        }
        if (pending > 0) {
            Text(
                if (pending == 1) "1 comment is waiting for review." else "$pending comments are waiting for review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(500) },
            label = { Text("Add a comment") },
            supportingText = { Text("Comments are checked before they're published · ${text.length}/500") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(enabled = text.isNotBlank(), onClick = {
                requireSignIn {
                    val body = text.trim()
                    scope.launch {
                        val ok = runCatching {
                            app.community.submitComment(bay, body, settings.displayName.ifBlank { "Anonymous rider" })
                        }.isSuccess
                        if (ok) text = ""
                        snackbar.showSnackbar(if (ok) "Thanks! Your comment will appear once it's been reviewed." else "Couldn't post that. Check you're signed in and online.")
                    }
                }
            }) { Text("Post") }
        }
    }
}
