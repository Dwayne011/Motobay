package uk.motobay.app.ui.explore

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.Roofing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.BottomSheetScaffold
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayLoadState
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.PlaceSuggestion
import uk.motobay.app.data.RecentPlace
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.LocalRequireSignIn
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
 *   button switches views; tapping a bay opens a bottom sheet over the map
 *   that swipes up to full height.
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
    val mapMoving by vm.mapMoving.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
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
    val requireSignIn = LocalRequireSignIn.current
    val newBayAt by vm.newBayAt.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    // Coming (back) to the screen: pick up new bay data and, if allowed, a fresh position for distances.
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = search.expanded) { vm.onSearchExpandedChange(false) }
    BackHandler(enabled = addingBay) { vm.cancelAddingBay() }
    // Distances in the suggestions need a recent position; refresh it quietly if allowed.
    LaunchedEffect(search.expanded) {
        if (search.expanded && app.location.hasPermission()) app.location.locate()
    }

    // "Don't allow" just leaves the map where it is; approximate-only still works (coarse fixes are fine for finding bays).
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.locate() else vm.locationDenied()
    }
    val onLocate = {
        if (app.location.hasPermission()) vm.locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val scheme = MaterialTheme.colorScheme
    val ext = MotoBayColors.extended
    val mapColors = PinColors(
        free = ext.pinFree, onFree = ext.onPinFree,
        paid = ext.pinPaid, onPaid = ext.onPinPaid,
        unknown = ext.pinUnknown, onUnknown = ext.onPinUnknown,
        selectedRing = scheme.onSurface,
        cluster = scheme.primaryContainer, onCluster = scheme.onPrimaryContainer,
        stroke = scheme.surface, user = scheme.tertiary,
    )
    val countLabel = if (visible.size >= 200) "200+" else "${visible.size}"
    val savedBays = remember(favourites, allBays) { favourites.keys.mapNotNull { allBays[it] ?: favourites[it] } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp

        val mapWith: @Composable ((Bay) -> Unit, Modifier) -> Unit = { onPick, modifier ->
            Box(modifier) {
                BayMap(
                    bays = mapBays,
                    selected = selectedId?.let { allBays[it] ?: favourites[it] },
                    userPosition = userPosition,
                    command = command,
                    colors = mapColors,
                    dark = isAppInDarkTheme(settings.themeMode),
                    initialCamera = settings.lastMap,
                    onBayClick = { id -> allBays[id]?.let(onPick) },
                    onViewportChange = vm::onViewportChange,
                    onMovingChange = vm::onMapMovingChange,
                    onCommandHandled = { vm.command.value = null },
                    onLongPress = { requireSignIn { vm.startAddingBay(it) } },
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
                        if (!expanded && selectedId != null) return@Column
                        SmallFloatingActionButton(
                            onClick = onLocate,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            if (locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.MyLocation, contentDescription = "Show my location")
                        }
                        FloatingActionButton(onClick = { requireSignIn { vm.startAddingBay() } }) {
                            Icon(Icons.Filled.AddLocationAlt, contentDescription = "Add a bay")
                        }
                    }
                }
            }
        }

        val listWith: @Composable ((Bay) -> Unit, Modifier) -> Unit = { onPick, modifier ->
            when {
                visible.isNotEmpty() -> LazyColumn(modifier) {
                    if (userPosition == null) item(key = "location-nudge") { LocationNudge(onLocate) }
                    items(visible, key = { it.bay.id }) { item ->
                        BayListItem(
                            bay = item.bay,
                            distance = item.distanceMetres?.let(::formatDistance),
                            selected = item.bay.id == selectedId,
                            onClick = { onPick(item.bay) },
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
                onSavedBay = { bay -> vm.onSearchExpandedChange(false); vm.select(bay, bottomPaddingPx = if (expanded) 0 else 300) },
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
                    val selectedBay = selectedId?.let { allBays[it] ?: favourites[it] }
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
                                    inputField = { SearchField(search, vm, mapMoving) },
                                    expanded = search.expanded,
                                    onExpandedChange = vm::onSearchExpandedChange,
                                    modifier = Modifier.fillMaxWidth().padding(AURORA_INSET),
                                    content = suggestions,
                                )
                                if (!search.expanded) {
                                    Filters(filters, counts, mapMoving, vm::setPrice, vm::toggleCovered)
                                    Text(
                                        "$countLabel ${if (visible.size == 1) "bay" else "bays"} in view",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                            }
                            if (!search.expanded) listWith(vm::select, Modifier.fillMaxSize())
                        }
                    }
                }
                Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    mapWith(vm::select, Modifier.fillMaxSize())
                }
            }
        } else {
            var headerHeight by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val headerDp = with(density) { headerHeight.toDp() }
            val selectedBay = selectedId?.let { allBays[it] ?: favourites[it] }
            val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
            val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
            val scope = rememberCoroutineScope()
            val peekDp = 300.dp
            val peekPx = with(density) { peekDp.roundToPx() }

            // Open the sheet part-way when a bay is picked; clear the pick when it's swiped away.
            LaunchedEffect(selectedBay?.id) {
                if (selectedBay != null) sheetState.partialExpand() else sheetState.hide()
            }
            LaunchedEffect(sheetState.currentValue) {
                if (sheetState.currentValue == SheetValue.Hidden && selectedId != null) vm.clearSelection()
            }
            BackHandler(enabled = selectedBay != null) { vm.clearSelection() }

            val pick: (Bay) -> Unit = { bay -> vm.select(bay, bottomPaddingPx = peekPx) }

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = if (selectedBay != null) peekDp else 0.dp,
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                sheetContent = {
                    selectedBay?.let { bay ->
                        Box(Modifier.fillMaxSize()) {
                            BayDetailContent(bay)
                            IconButton(
                                onClick = vm::clearSelection,
                                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 8.dp),
                            ) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                        }
                    }
                },
            ) {
                Box(Modifier.fillMaxSize()) {
                    // The map stays alive under the list so switching back keeps its state.
                    mapWith(pick, Modifier.fillMaxSize())
                    if (compactView == CompactView.List && !addingBay) {
                        Surface(Modifier.fillMaxSize()) {
                            listWith(pick, Modifier.fillMaxSize().padding(top = headerDp + 8.dp, bottom = 88.dp))
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
                                inputField = { SearchField(search, vm, mapMoving) },
                                expanded = search.expanded,
                                onExpandedChange = vm::onSearchExpandedChange,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = side)
                                    .padding(top = if (search.expanded) 0.dp else 8.dp)
                                    .padding(if (search.expanded) 0.dp else AURORA_INSET),
                                content = suggestions,
                            )
                            if (!search.expanded) {
                                Box(Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp)) {
                                    Filters(filters, counts, mapMoving, vm::setPrice, vm::toggleCovered)
                                }
                            }
                        }
                        if (!search.expanded && selectedBay == null) {
                            ViewToggle(
                                view = compactView,
                                countLabel = countLabel,
                                onSelect = { vm.compactView.value = it },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            )
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
private fun SearchField(search: SearchUiState, vm: ExploreViewModel, mapMoving: Boolean) {
    SearchBarDefaults.InputField(
        // The aurora glow hugs the field itself; it goes out while the bar fills the screen.
        modifier = Modifier.auroraGlow(boosted = mapMoving || search.expanded, visible = !search.expanded),
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
                // One row that scrolls sideways, so labels never wrap at large text sizes.
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = false,
                        onClick = onLocate,
                        label = { Text("Your location", maxLines = 1, softWrap = false) },
                        leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null, Modifier.size(18.dp)) },
                    )
                    FilterChip(
                        selected = tab == EmptyTab.Recent,
                        onClick = { tab = EmptyTab.Recent },
                        label = { Text("Recent", maxLines = 1, softWrap = false) },
                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, Modifier.size(18.dp)) },
                    )
                    FilterChip(
                        selected = tab == EmptyTab.Saved,
                        onClick = { tab = EmptyTab.Saved },
                        label = { Text("Saved", maxLines = 1, softWrap = false) },
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
                Text(bay.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(bay.locality, bay.fee.label).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 84.dp))
    }
}

/** Shown at the top of the list until we have a position to measure distances from. */
@Composable
private fun LocationNudge(onLocate: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Turn on location to see how far away each bay is",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onLocate) { Text("Allow") }
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

/** Gap between the aurora glow and the search bar, so the bloom has room to breathe. */
private val AURORA_INSET = 4.dp

/**
 * One scrollable row of pills, like the chips over a Google Maps map: price is a
 * single choice (All, Free, Paid, Unknown), Covered is a toggle. The coloured
 * dots match the pin colours, so the row doubles as the map legend. Each price
 * pill carries a live count of bays in view; while the map is moving the row
 * collapses into a single telemetry pill until the camera settles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filters(
    filters: ExploreFilters,
    counts: Map<PriceFilter, Int>,
    mapMoving: Boolean,
    onPrice: (PriceFilter) -> Unit,
    onCovered: () -> Unit,
) {
    AnimatedContent(
        targetState = mapMoving,
        transitionSpec = {
            (fadeIn(tween(220)) + expandVertically(tween(220))) togetherWith (fadeOut(tween(160)) + shrinkVertically(tween(160)))
        },
        label = "filters",
    ) { moving ->
        if (moving) {
            val label = filters.price.label
            val count = counts[filters.price] ?: 0
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.height(44.dp).semantics { contentDescription = "Map moving, $label $count" },
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text(
                        "Telemetry active \u00b7 $label",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Odometer(count, selected = true)
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PriceFilter.entries.forEach { p ->
                    val selected = filters.price == p
                    Pill(
                        selected = selected,
                        onClick = { onPrice(p) },
                        label = p.label,
                        count = counts[p],
                        leading = {
                            val dot = p.fee?.let { pinColors(it).first }
                            when {
                                selected -> Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize))
                                dot != null -> Box(Modifier.size(10.dp).background(dot, CircleShape))
                            }
                        },
                    )
                }
                Pill(
                    selected = filters.coveredOnly,
                    onClick = onCovered,
                    label = "Covered",
                    count = null,
                    leading = {
                        Icon(
                            if (filters.coveredOnly) Icons.Filled.Check else Icons.Outlined.Roofing,
                            contentDescription = null,
                            Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

/** A small count badge whose digits roll up or down like an odometer when the number changes. */
@Composable
private fun Odometer(count: Int, selected: Boolean) {
    val bg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        Modifier.background(bg, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(260)) { if (up) it else -it } + fadeIn(tween(260))) togetherWith
                    (slideOutVertically(tween(260)) { if (up) -it else it } + fadeOut(tween(260)))
            },
            label = "odometer",
        ) { n ->
            Text(n.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pill(selected: Boolean, onClick: () -> Unit, label: String, count: Int?, leading: @Composable () -> Unit) {
    // Taller than the M3 default and with a visible outline, so they read over the map like Google Maps' chips.
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                if (count != null) Odometer(count, selected)
            }
        },
        leadingIcon = leading,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = FilterChipDefaults.filterChipElevation(elevation = 3.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp, selectedBorderWidth = 1.5.dp,
        ),
        modifier = Modifier.height(44.dp),
    )
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
