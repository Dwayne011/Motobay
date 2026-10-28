package uk.motobay.app.ui.explore

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalContext
import uk.motobay.app.util.openDirections
import uk.motobay.app.data.Fee
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedVisibility
import uk.motobay.app.ui.theme.auroraPalette
import uk.motobay.app.ui.theme.AuroraPalette
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
    val context = LocalContext.current
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
    val palette = auroraPalette()
    val mapColors = PinColors(
        free = AuroraPalette.StatusFree,
        paid = AuroraPalette.StatusPaid,
        unknown = AuroraPalette.StatusUnknown,
        pillFill = palette.chipUnselectedBg,
        pillBorder = palette.chipUnselectedBorder,
        pillText = palette.chipUnselectedText,
        selectedFill = palette.searchSurface,
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
                        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!expanded && selectedId != null) return@Column
                        // Locate mini-FAB: 44dp, 14dp corners, 1dp border, 20dp icon.
                        Surface(
                            onClick = onLocate,
                            shape = RoundedCornerShape(14.dp),
                            color = palette.locateFabBg,
                            contentColor = palette.locateFabIcon,
                            border = BorderStroke(1.dp, palette.locateFabBorder),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = palette.locateFabIcon)
                                else Icon(Icons.Filled.MyLocation, contentDescription = "Show my location", Modifier.size(20.dp))
                            }
                        }
                        // Add bay primary FAB: 52dp, 16dp corners, 1dp border, 22dp icon.
                        Surface(
                            onClick = { requireSignIn { vm.startAddingBay() } },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.addFabBg,
                            contentColor = palette.addFabIcon,
                            border = BorderStroke(1.dp, palette.addFabBorder),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = "Add a bay", Modifier.size(22.dp))
                            }
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
                                    inputField = { SearchField(search, vm, mapMoving, palette) },
                                    expanded = search.expanded,
                                    onExpandedChange = vm::onSearchExpandedChange,
                                    colors = searchBarColors(search.expanded),
                                    modifier = Modifier.fillMaxWidth(),
                                    content = suggestions,
                                )
                                if (!search.expanded) {
                                    Filters(filters, counts, mapMoving, palette, vm::setPrice, vm::toggleCovered)
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
            // The preview card sits above the bottom controls, so fly the picked bay above it.
            val cardClearancePx = with(density) { PREVIEW_CLEARANCE.roundToPx() }
            BackHandler(enabled = selectedBay != null) { vm.clearSelection() }

            val pick: (Bay) -> Unit = { bay -> vm.select(bay, bottomPaddingPx = cardClearancePx) }

            run {
                Box(Modifier.fillMaxSize().background(palette.mapBackground)) {
                    // The map stays alive under the list so switching back keeps its state.
                    mapWith(pick, Modifier.fillMaxSize())
                    // Top protection vignette, so the status bar and search bar read over busy tiles.
                    Box(
                        Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(palette.topVignetteStart, Color.Transparent))),
                    )
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
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SearchBar(
                                inputField = { SearchField(search, vm, mapMoving, palette) },
                                expanded = search.expanded,
                                onExpandedChange = vm::onSearchExpandedChange,
                                colors = searchBarColors(search.expanded),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = side)
                                    .padding(top = if (search.expanded) 0.dp else 8.dp),
                                content = suggestions,
                            )
                            if (!search.expanded) {
                                Box(Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp)) {
                                    Filters(filters, counts, mapMoving, palette, vm::setPrice, vm::toggleCovered)
                                }
                            }
                        }
                        if (!search.expanded) {
                            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                // The picked bay's preview, fading in above the controls while the map is still.
                                AnimatedVisibility(
                                    visible = selectedBay != null && compactView == CompactView.Map && !mapMoving,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    val bay = selectedBay ?: return@AnimatedVisibility
                                    Column {
                                        SelectedBayPreviewCard(
                                            bay = bay,
                                            distanceMetres = userPosition?.let { distanceMetres(it, bay) },
                                            isSaved = bay.id in favourites,
                                            palette = palette,
                                            onToggleSave = { app.user.toggleFavourite(bay) },
                                            onGo = { openDirections(context, bay) },
                                            onOpen = { onOpenBay(bay.id) },
                                            onClose = vm::clearSelection,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                                ViewToggle(
                                    view = compactView,
                                    countLabel = countLabel,
                                    palette = palette,
                                    onSelect = { vm.compactView.value = it },
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
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

/**
 * The input slot of the search bar. Collapsed, it is the spec's 54dp aurora
 * capsule (a tap expands the search); expanded, it is the real text field so
 * typing, the keyboard and suggestions work as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(search: SearchUiState, vm: ExploreViewModel, mapMoving: Boolean, palette: AuroraPalette) {
    if (!search.expanded) {
        AuroraSearchCapsule(
            query = search.query,
            mapMoving = mapMoving,
            palette = palette,
            onClick = { vm.onSearchExpandedChange(true) },
            onClear = { vm.onQueryChange("") },
        )
        return
    }
    SearchBarDefaults.InputField(
        query = search.query,
        onQueryChange = vm::onQueryChange,
        onSearch = { vm.onSearchSubmit() },
        expanded = true,
        onExpandedChange = vm::onSearchExpandedChange,
        placeholder = { Text("Search a town, street or postcode", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            IconButton(onClick = { vm.onSearchExpandedChange(false) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
            }
        },
        trailingIcon = {
            if (search.query.isNotEmpty()) {
                IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
            }
        },
    )
}

/** The collapsed search bar: 54dp capsule, 2dp aurora rim, 16dp inner padding, 20dp icon, 15sp text. */
@Composable
private fun AuroraSearchCapsule(
    query: String,
    mapMoving: Boolean,
    palette: AuroraPalette,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .auroraGlow(palette, boosted = mapMoving)
            .padding(2.dp)
            .clip(CircleShape)
            .background(palette.searchSurface)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = if (query.isEmpty()) "Search a town, street or postcode" else "Search: $query" }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = if (mapMoving) palette.searchIconActive else palette.searchIconIdle,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (query.isEmpty()) "Search a town, street or postcode" else query,
                color = palette.searchPlaceholder,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Clear search",
                    tint = palette.searchIconIdle,
                    modifier = Modifier.padding(start = 8.dp).size(20.dp).clip(CircleShape).clickable(onClick = onClear),
                )
            } else {
                // Telemetry status: a "Scanning" pill crossfades in while the map camera moves.
                AnimatedContent(targetState = mapMoving, label = "ScanningStatusBadge") { scanning ->
                    if (scanning) {
                        Text(
                            "Scanning",
                            color = palette.searchIconActive,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(palette.searchIconActive.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}

/** Search bar container: see-through behind the collapsed capsule, the theme surface once it fills the screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun searchBarColors(expanded: Boolean) =
    if (expanded) SearchBarDefaults.colors() else SearchBarDefaults.colors(containerColor = Color.Transparent)

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

/** The spec's status dot for each price pill; null for All. */
private val PriceFilter.dot: Color?
    get() = when (this) {
        PriceFilter.All -> null
        PriceFilter.Free -> AuroraPalette.StatusFree
        PriceFilter.Paid -> AuroraPalette.StatusPaid
        PriceFilter.Unknown -> AuroraPalette.StatusUnknown
    }

/**
 * One scrollable row of 36dp chips over the map: price is a single choice
 * (All, Free, Paid, Unknown), Covered is a toggle. Each price chip carries a
 * live count of bays in view as an odometer badge; while the map is moving the
 * row collapses into a single telemetry pill until the camera settles.
 */
@Composable
private fun Filters(
    filters: ExploreFilters,
    counts: Map<PriceFilter, Int>,
    mapMoving: Boolean,
    palette: AuroraPalette,
    onPrice: (PriceFilter) -> Unit,
    onCovered: () -> Unit,
) {
    AnimatedContent(
        targetState = mapMoving,
        transitionSpec = { (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically()) },
        label = "MapPanFilterCollapse",
    ) { moving ->
        if (moving) {
            val label = filters.price.label
            val count = counts[filters.price] ?: 0
            Surface(
                shape = CircleShape,
                color = palette.searchSurface,
                border = BorderStroke(1.dp, palette.searchIconActive.copy(alpha = 0.45f)),
                modifier = Modifier.heightIn(min = 36.dp).semantics { contentDescription = "Map moving, $label $count" },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(palette.searchIconActive)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Telemetry Active \u2022 ${label.uppercase()} ($count ${if (count == 1) "bay" else "bays"})",
                        color = palette.chipUnselectedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
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
                        count = counts[p] ?: 0,
                        palette = palette,
                        leading = {
                            val dot = p.dot
                            if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                        },
                    )
                }
                Pill(
                    selected = filters.coveredOnly,
                    onClick = onCovered,
                    label = "Covered",
                    count = null,
                    palette = palette,
                    leading = {
                        Icon(Icons.Outlined.Roofing, contentDescription = null, tint = palette.chipUnselectedText, modifier = Modifier.size(15.dp))
                    },
                )
            }
        }
    }
}

/** A small count badge whose digits roll up or down like an odometer when the number changes. */
@Composable
private fun Odometer(count: Int, selected: Boolean, palette: AuroraPalette) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            } else {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            }
        },
        label = "OdometerCount",
    ) { n ->
        Text(
            n.toString(),
            color = if (selected) palette.chipBadgeSelectedText else palette.chipBadgeUnselectedText,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) palette.chipBadgeSelectedBg else palette.chipBadgeUnselectedBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** A 36dp filter chip: a 15dp check when selected, otherwise [leading], then the 13sp label and optional count. */
@Composable
private fun Pill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    count: Int?,
    palette: AuroraPalette,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) palette.chipSelectedBg else palette.chipUnselectedBg,
        border = BorderStroke(1.dp, if (selected) palette.chipSelectedBorder else palette.chipUnselectedBorder),
        // 36dp by the spec; taller only when a large font scale needs it.
        modifier = Modifier.heightIn(min = 36.dp).semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = palette.chipSelectedText, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            } else {
                leading()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = if (selected) palette.chipSelectedText else palette.chipUnselectedText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
            if (count != null) {
                Spacer(Modifier.width(6.dp))
                Odometer(count, selected, palette)
            }
        }
    }
}

/** An 8dp dot that breathes while telemetry is live (still in previews and screenshot tests). */
@Composable
private fun PulsingDot(color: Color) {
    val alpha = if (LocalInspectionMode.current) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "TelemetryDot")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
            label = "TelemetryDotAlpha",
        )
        a
    }
    Box(Modifier.size(8.dp).graphicsLayer { this.alpha = alpha }.clip(CircleShape).background(color))
}

/** How much map the preview card and bottom controls cover, so a picked bay is flown above them. */
private val PREVIEW_CLEARANCE = 200.dp

/**
 * The spec's compact preview of the picked bay: status, spaces and distance on
 * top, the name and street below, a star to save it and a Go button for
 * directions. Tapping the card opens the full bay page.
 */
@Composable
private fun SelectedBayPreviewCard(
    bay: Bay,
    distanceMetres: Double?,
    isSaved: Boolean,
    palette: AuroraPalette,
    onToggleSave: () -> Unit,
    onGo: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val dot = when (bay.fee) {
        Fee.Free -> AuroraPalette.StatusFree
        Fee.Paid -> AuroraPalette.StatusPaid
        Fee.Unknown -> AuroraPalette.StatusUnknown
    }
    val typeLabel = when (bay.fee) {
        Fee.Free -> "Free"
        Fee.Paid -> "Paid"
        Fee.Unknown -> "Unknown rules"
    }
    val meta = buildString {
        bay.capacity?.let { append(" \u2022 $it ${if (it == 1) "space" else "spaces"}") }
        distanceMetres?.let { append(" \u2022 ${formatDistance(it)}") }
    }
    val subtitle = listOfNotNull(bay.street, bay.area ?: bay.city).joinToString(", ")
        .ifEmpty { if (bay.covered == true) "Covered bay" else "Motorcycle bay" }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("bay-preview"),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(typeLabel, color = palette.cardTitleText, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(meta, color = palette.cardSubtext, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(2.dp))
                Text(bay.name, color = palette.cardTitleText, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = palette.cardSubtext, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = onToggleSave,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSaved) AuroraPalette.StatusPaid.copy(alpha = 0.2f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isSaved) AuroraPalette.StatusPaid.copy(alpha = 0.5f) else palette.cardBorder),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isSaved) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isSaved) "Remove from saved" else "Save bay",
                            tint = if (isSaved) AuroraPalette.StatusPaid else palette.cardSubtext,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Surface(onClick = onGo, shape = RoundedCornerShape(12.dp), color = palette.addFabBg, modifier = Modifier.height(36.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Go", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.cardSubtext, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
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
