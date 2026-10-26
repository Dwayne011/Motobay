package uk.motobay.app.ui.explore

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayLoadState
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.PlaceSuggestion
import uk.motobay.app.data.RecentPlace
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.bay.BayFormSheet
import uk.motobay.app.ui.components.BayListItem
import uk.motobay.app.ui.components.BayPin
import uk.motobay.app.ui.components.label
import uk.motobay.app.ui.components.pinColors
import uk.motobay.app.ui.theme.MotoBayColors
import uk.motobay.app.ui.theme.isAppInDarkTheme
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.util.formatDistance

/**
 * Map and list of bays. Layout follows M3 window size classes:
 * - Compact and medium (under 840dp): full-bleed map with a floating search
 *   bar that expands to a full-screen suggestions view; a Map/List segmented
 *   button switches views; opening a bay navigates to its own screen.
 * - Expanded (840dp and up): list-detail canonical layout, with a docked
 *   search bar and the list or bay detail in a side pane next to the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val vm: ExploreViewModel = viewModel { ExploreViewModel(app) }
    val snackbar = LocalSnackbar.current

    val visible by vm.visible.collectAsStateWithLifecycle()
    val mapBays by vm.mapBays.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val loadState by vm.loadState.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    val command by vm.command.collectAsStateWithLifecycle()
    val compactView by vm.compactView.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val userPosition by vm.userPosition.collectAsStateWithLifecycle()
    val allBays by vm.allBays.collectAsStateWithLifecycle()
    val recents by vm.recentPlaces.collectAsStateWithLifecycle()
    val favourites by vm.favourites.collectAsStateWithLifecycle()
    val addingBay by vm.addingBay.collectAsStateWithLifecycle()
    val newBayAt by vm.newBayAt.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = search.expanded) { vm.onSearchExpandedChange(false) }
    BackHandler(enabled = addingBay) { vm.cancelAddingBay() }
    // Distances in the suggestions need a recent position; refresh it quietly if allowed.
    LaunchedEffect(search.expanded) {
        if (search.expanded && app.location.hasPermission()) app.location.locate()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.locate()
    }
    val onLocate = {
        if (app.location.hasPermission()) vm.locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val scheme = MaterialTheme.colorScheme
    val ext = MotoBayColors.extended
    val mapColors = PinColors(
        free = ext.success, onFree = ext.onSuccess,
        paid = ext.warning, onPaid = ext.onWarning,
        unknown = scheme.primary, onUnknown = scheme.onPrimary,
        selectedRing = scheme.onSurface,
        cluster = scheme.primaryContainer, onCluster = scheme.onPrimaryContainer,
        stroke = scheme.surface, user = scheme.tertiary,
    )
    val countLabel = if (visible.size >= 200) "200+" else "${visible.size}"
    val savedBays = remember(favourites, allBays) { favourites.keys.mapNotNull { allBays[it] ?: favourites[it] } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp

        val map: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                BayMap(
                    bays = mapBays,
                    selected = if (expanded) selectedId?.let { allBays[it] } else null,
                    userPosition = userPosition,
                    command = command,
                    colors = mapColors,
                    dark = isAppInDarkTheme(settings.themeMode),
                    initialCamera = settings.lastMap,
                    onBayClick = { id -> if (expanded) allBays[id]?.let(vm::select) else onOpenBay(id) },
                    onViewportChange = vm::onViewportChange,
                    onCommandHandled = { vm.command.value = null },
                    onLongPress = { vm.startAddingBay(it) },
                    modifier = Modifier.fillMaxSize(),
                )
                if (loadState == BayLoadState.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
                if (addingBay) {
                    AddBayOverlay(onCancel = vm::cancelAddingBay, onNext = vm::confirmBayLocation)
                } else {
                    (loadState as? BayLoadState.Failed)?.let { failed ->
                        LoadErrorBanner(
                            failed.message,
                            onRetry = vm::retryLoad,
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 88.dp, bottom = 88.dp),
                        )
                    }
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SmallFloatingActionButton(
                            onClick = onLocate,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            if (locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.MyLocation, contentDescription = "Show my location")
                        }
                        FloatingActionButton(onClick = { vm.startAddingBay() }) {
                            Icon(Icons.Filled.AddLocationAlt, contentDescription = "Add a bay")
                        }
                    }
                }
            }
        }

        val list: @Composable (Modifier) -> Unit = { modifier ->
            when {
                visible.isNotEmpty() -> LazyColumn(modifier) {
                    items(visible, key = { it.bay.id }) { item ->
                        BayListItem(
                            bay = item.bay,
                            distance = item.distanceMetres?.let(::formatDistance),
                            selected = expanded && item.bay.id == selectedId,
                            onClick = { if (expanded) vm.select(item.bay) else onOpenBay(item.bay.id) },
                        )
                    }
                }
                loadState == BayLoadState.Loading -> Hint("Loading bays…", modifier)
                loadState is BayLoadState.Failed -> Hint((loadState as BayLoadState.Failed).message, modifier)
                else -> Hint("No bays here. Zoom out, move the map or change the filters.", modifier)
            }
        }

        val suggestions: @Composable ColumnScope.() -> Unit = {
            SuggestionsContent(
                search = search,
                position = userPosition,
                recents = recents,
                savedBays = savedBays,
                onLocate = { vm.onSearchExpandedChange(false); onLocate() },
                onPick = vm::onSuggestionClick,
                onFill = vm::onQueryChange,
                onRecent = vm::onRecentClick,
                onClearRecents = vm::clearRecents,
                onSavedBay = { bay ->
                    if (expanded) vm.onSavedBayClick(bay) else { vm.onSearchExpandedChange(false); onOpenBay(bay.id) }
                },
            )
        }

        if (expanded) {
            Row(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Side pane: tonal elevation level 1 (surfaceContainerLow).
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.widthIn(max = 400.dp).fillMaxHeight().weight(0.4f, fill = false),
                ) {
                    val selectedBay = selectedId?.let { allBays[it] }
                    if (selectedBay != null) {
                        Column {
                            TopAppBar(
                                title = { Text("Bay details") },
                                navigationIcon = {
                                    IconButton(onClick = { vm.selectedId.value = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                windowInsets = WindowInsets(0),
                            )
                            BayDetailContent(selectedBay)
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DockedSearchBar(
                                    inputField = { SearchField(search, vm) },
                                    expanded = search.expanded,
                                    onExpandedChange = vm::onSearchExpandedChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    content = suggestions,
                                )
                                if (!search.expanded) {
                                    Filters(filters, vm::setPrice, vm::toggleCovered)
                                    Text(
                                        "$countLabel ${if (visible.size == 1) "bay" else "bays"} in view",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                            }
                            if (!search.expanded) list(Modifier.fillMaxSize())
                        }
                    }
                }
                Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    map(Modifier.fillMaxSize())
                }
            }
        } else {
            var headerHeight by remember { mutableIntStateOf(0) }
            val headerDp = with(LocalDensity.current) { headerHeight.toDp() }
            Box(Modifier.fillMaxSize()) {
                // The map stays alive under the list so switching back keeps its state.
                map(Modifier.fillMaxSize())
                if (compactView == CompactView.List && !addingBay) {
                    Surface(Modifier.fillMaxSize()) {
                        list(Modifier.fillMaxSize().padding(top = headerDp + 8.dp, bottom = 88.dp))
                    }
                }
                if (!addingBay) {
                    // Search bar and filters stack in one column, so they never overlap
                    // whatever the phone's text size. The bar expands to full screen.
                    val side by animateDpAsState(if (search.expanded) 0.dp else 16.dp, label = "searchSidePadding")
                    Column(
                        Modifier.onSizeChanged { if (!search.expanded) headerHeight = it.height },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SearchBar(
                            inputField = { SearchField(search, vm) },
                            expanded = search.expanded,
                            onExpandedChange = vm::onSearchExpandedChange,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = side)
                                .padding(top = if (search.expanded) 0.dp else 8.dp),
                            content = suggestions,
                        )
                        if (!search.expanded) {
                            Box(Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp)) {
                                Filters(filters, vm::setPrice, vm::toggleCovered)
                            }
                        }
                    }
                    if (!search.expanded) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 3.dp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        ) {
                            SingleChoiceSegmentedButtonRow {
                                listOf(CompactView.Map to "Map", CompactView.List to "List $countLabel").forEachIndexed { i, (view, label) ->
                                    SegmentedButton(
                                        selected = compactView == view,
                                        onClick = { vm.compactView.value = view },
                                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                                        icon = {
                                            SegmentedButtonDefaults.Icon(active = compactView == view) {
                                                Icon(
                                                    if (view == CompactView.Map) Icons.Filled.Map else Icons.AutoMirrored.Filled.List,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                                )
                                            }
                                        },
                                        label = { Text(label, maxLines = 1, softWrap = false) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    newBayAt?.let { at ->
        BayFormSheet(
            at = at,
            editing = null,
            onDismiss = { vm.newBayAt.value = null },
            onSubmit = vm::submitNewBay,
        )
    }
}

/** Crosshair pin fixed at the map centre while the rider positions a new bay. */
@Composable
private fun AddBayOverlay(onCancel: () -> Unit, onNext: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // The pin's tip sits on the map centre.
        Icon(
            Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center).size(48.dp).offset(y = (-24).dp),
        )
        Box(Modifier.align(Alignment.Center).size(6.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp).widthIn(max = 560.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Add a bay", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Move the map until the pin sits on the bay, then tap Next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            ) { Text("Cancel") }
            Button(onClick = onNext) { Text("Next") }
        }
    }
}

/** The text field inside the search bar, shared by the full-screen and docked versions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(search: SearchUiState, vm: ExploreViewModel) {
    SearchBarDefaults.InputField(
        query = search.query,
        onQueryChange = vm::onQueryChange,
        onSearch = { vm.onSearchSubmit() },
        expanded = search.expanded,
        onExpandedChange = vm::onSearchExpandedChange,
        placeholder = { Text("Search a town, street or postcode", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            if (search.expanded) {
                IconButton(onClick = { vm.onSearchExpandedChange(false) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                }
            } else {
                Icon(Icons.Filled.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (search.query.isNotEmpty()) {
                IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
            }
        },
    )
}

private enum class EmptyTab { Recent, Saved }

/**
 * What the expanded search shows. Before typing: shortcuts and Recent / Saved
 * lists. While typing: suggestions that narrow with each letter, laid out like
 * Google Maps (icon with distance, the typed part dimmed, a fill-in arrow).
 */
@Composable
private fun SuggestionsContent(
    search: SearchUiState,
    position: LatLon?,
    recents: List<RecentPlace>,
    savedBays: List<Bay>,
    onLocate: () -> Unit,
    onPick: (PlaceSuggestion) -> Unit,
    onFill: (String) -> Unit,
    onRecent: (RecentPlace) -> Unit,
    onClearRecents: () -> Unit,
    onSavedBay: (Bay) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(EmptyTab.Recent) }
    if (search.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    LazyColumn {
        if (search.query.trim().length < 2) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = onLocate,
                        label = { Text("Your location") },
                        leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null, Modifier.size(18.dp)) },
                    )
                    FilterChip(
                        selected = tab == EmptyTab.Recent,
                        onClick = { tab = EmptyTab.Recent },
                        label = { Text("Recent") },
                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, Modifier.size(18.dp)) },
                    )
                    FilterChip(
                        selected = tab == EmptyTab.Saved,
                        onClick = { tab = EmptyTab.Saved },
                        label = { Text("Saved") },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, Modifier.size(18.dp)) },
                    )
                }
            }
            when (tab) {
                EmptyTab.Recent -> recentItems(recents, position, onRecent, onFill, onClearRecents)
                EmptyTab.Saved -> savedItems(savedBays, position, onSavedBay)
            }
            return@LazyColumn
        }
        search.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
        if (!search.loading && search.error == null && search.suggestions.isEmpty()) {
            item { EmptyText("No UK places match \"${search.query.trim()}\"") }
        }
        items(search.suggestions, key = { "${it.title}|${it.subtitle}|${it.point}" }) { s ->
            SuggestionRow(
                icon = s.kind.icon,
                title = highlightTyped(s.title, search.query),
                subtitle = s.subtitle,
                distance = position?.let { p -> s.point?.let { formatDistance(distanceMetres(p, it.lat, it.lon)) } },
                onClick = { onPick(s) },
                onFill = { onFill(s.title) },
            )
        }
    }
}

private fun LazyListScope.recentItems(
    recents: List<RecentPlace>,
    position: LatLon?,
    onRecent: (RecentPlace) -> Unit,
    onFill: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recents.isEmpty()) {
        item { EmptyText("Places you search for will show here.") }
        return
    }
    items(recents, key = { "recent|${it.title}|${it.subtitle}" }) { r ->
        SuggestionRow(
            icon = Icons.Filled.History,
            title = AnnotatedString(r.title),
            subtitle = r.subtitle,
            distance = position?.let { formatDistance(distanceMetres(it, r.lat, r.lon)) },
            onClick = { onRecent(r) },
            onFill = { onFill(r.title) },
        )
    }
    item {
        TextButton(onClick = onClear, modifier = Modifier.padding(start = 8.dp)) { Text("Clear recent searches") }
    }
}

private fun LazyListScope.savedItems(saved: List<Bay>, position: LatLon?, onSaved: (Bay) -> Unit) {
    if (saved.isEmpty()) {
        item { EmptyText("Bays you save will show here.") }
        return
    }
    items(saved, key = { "saved|${it.id}" }) { bay ->
        Row(
            Modifier.fillMaxWidth().clickable { onSaved(bay) }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BayPin(bay.fee, size = 36.dp)
                position?.let { DistanceText(formatDistance(distanceMetres(it, bay))) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(bay.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(bay.fee.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(Modifier.padding(start = 84.dp))
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: AnnotatedString,
    subtitle: String,
    distance: String?,
    onClick: () -> Unit,
    onFill: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            distance?.let { DistanceText(it) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onFill) {
            Icon(Icons.Filled.NorthWest, contentDescription = "Use as search text", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(start = 84.dp))
}

@Composable
private fun DistanceText(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

/** Dims the part the rider has typed, so the rest of the name stands out (as Google Maps does). */
@Composable
private fun highlightTyped(title: String, query: String): AnnotatedString {
    val typed = query.trim()
    val start = if (typed.isEmpty()) -1 else title.indexOf(typed, ignoreCase = true)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        if (start < 0) {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(title) }
            return@buildAnnotatedString
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(title.substring(0, start)) }
        withStyle(SpanStyle(color = dim)) { append(title.substring(start, start + typed.length)) }
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(title.substring(start + typed.length)) }
    }
}

private val PlaceSuggestion.Kind.icon: ImageVector
    get() = when (this) {
        PlaceSuggestion.Kind.Town -> Icons.Filled.LocationCity
        PlaceSuggestion.Kind.Street -> Icons.Filled.EditRoad
        PlaceSuggestion.Kind.Postcode -> Icons.Filled.MarkunreadMailbox
        PlaceSuggestion.Kind.Station -> Icons.Filled.Train
        PlaceSuggestion.Kind.Place -> Icons.Filled.Place
    }

@Composable
private fun LoadErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

/**
 * Price as a segmented button (quick single choice), covered as a filter chip.
 * The coloured dots match the pin colours, so they double as the map legend.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Filters(filters: ExploreFilters, onPrice: (PriceFilter) -> Unit, onCovered: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
            SingleChoiceSegmentedButtonRow {
                PriceFilter.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = filters.price == p,
                        onClick = { onPrice(p) },
                        shape = SegmentedButtonDefaults.itemShape(i, PriceFilter.entries.size),
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dot = when (p) {
                                    PriceFilter.Free -> pinColors(Fee.Free).first
                                    PriceFilter.Paid -> pinColors(Fee.Paid).first
                                    PriceFilter.All -> null
                                }
                                if (dot != null && filters.price != p) {
                                    Box(Modifier.size(10.dp).background(dot, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(p.name, maxLines = 1)
                            }
                        },
                    )
                }
            }
        }
        FilterChip(
            selected = filters.coveredOnly,
            onClick = onCovered,
            label = { Text("Covered") },
            leadingIcon = if (filters.coveredOnly) {
                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
            } else {
                null
            },
            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(24.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
