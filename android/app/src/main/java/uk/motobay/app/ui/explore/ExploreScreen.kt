package uk.motobay.app.ui.explore

import uk.motobay.app.data.VehicleCrimeData
import androidx.compose.material.icons.outlined.LocalPolice
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalContext
import uk.motobay.app.util.openDirections
import uk.motobay.app.data.Fee
import uk.motobay.app.ui.components.RatingBadge
import uk.motobay.app.ui.components.CouncilCheckedLabel
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.components.frostOverline
import uk.motobay.app.ui.theme.FrostFont
import uk.motobay.app.ui.theme.frostEdge
import uk.motobay.app.ui.theme.frostBackdrop
import uk.motobay.app.ui.LocalNavClearance
import uk.motobay.app.data.chargeStatus
import uk.motobay.app.util.shareBay
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Share
import uk.motobay.app.ui.components.feeText
import uk.motobay.app.data.ratingSummary
import uk.motobay.app.data.RatingSummary
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
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
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.AuroraTag
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.AuroraOutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import uk.motobay.app.ui.components.AuroraChip
import uk.motobay.app.data.formatPence
import uk.motobay.app.data.FuelStation
import uk.motobay.app.data.FuelNearby
import uk.motobay.app.data.FuelGrade
import androidx.compose.material.icons.outlined.LocalGasStation
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
fun ExploreScreen(onOpenBay: (String) -> Unit, onOpenSupporter: () -> Unit = {}) {
    val app = LocalApp.current
    val context = LocalContext.current
    val vm: ExploreViewModel = viewModel { ExploreViewModel(app) }
    val snackbar = LocalSnackbar.current

    val visible by vm.visible.collectAsStateWithLifecycle()
    val mapBays by vm.mapBays.collectAsStateWithLifecycle()
    val mapMoving by vm.mapMoving.collectAsStateWithLifecycle()
    val supporter by app.supporter.state.collectAsStateWithLifecycle()
    // "Free now" is a Supporter extra: without it the chip explains what it is instead of filtering.
    val onFreeNow = { if (supporter.isSupporter) vm.toggleFreeNow() else onOpenSupporter() }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showLayers by rememberSaveable { mutableStateOf(false) }
    val community by app.community.data.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val loadState by vm.loadState.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    val command by vm.command.collectAsStateWithLifecycle()
    val compactView by vm.compactView.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val fuelOn by vm.fuelLayer.collectAsStateWithLifecycle()
    val fuelGrade by vm.fuelGrade.collectAsStateWithLifecycle()
    val cheapestFuel by vm.cheapestFuel.collectAsStateWithLifecycle()
    val fuelData by app.fuel.data.collectAsStateWithLifecycle()
    val nearDestination by vm.nearDestination.collectAsStateWithLifecycle()
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
    // At launch, go to the rider: at once if location is allowed, or after asking on the very first launch.
    // (Previews and screenshot tests have no location or permission dialog.)
    val disclaimerAccepted = app.user.settings.collectAsStateWithLifecycle().value.disclaimerAccepted
    val inspection = LocalInspectionMode.current
    LaunchedEffect(disclaimerAccepted) {
        if (!inspection && vm.centreOnLaunch()) {
            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val onLocate = {
        if (app.location.hasPermission()) vm.locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val scheme = MaterialTheme.colorScheme
    val palette = auroraPalette()
    val crimeOn by vm.crimeLayer.collectAsStateWithLifecycle()
    val crimeData by app.vehicleCrime.data.collectAsStateWithLifecycle()
    // Room for the floating navigation capsule, which sits over the map.
    val navClearance = LocalNavClearance.current
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
                    crimePoints = if (crimeOn) crimeData?.points else null,
                    fuelPrices = remember(fuelOn, fuelData, fuelGrade) {
                        if (!fuelOn) null else fuelData?.stations?.mapNotNull { s -> s.price(fuelGrade)?.let { s to it } }
                    },
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
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 88.dp, bottom = 88.dp + navClearance),
                        )
                    }
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp + navClearance),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!expanded && selectedId != null) return@Column
                        // Map layers (fuel prices, vehicle crime): the same 44dp round glass button as Locate.
                        Surface(
                            onClick = { showLayers = true },
                            shape = CircleShape,
                            color = if (fuelOn || crimeOn) palette.selected else palette.locateFabBg,
                            contentColor = palette.locateFabIcon,
                            border = frostEdge(palette, if (fuelOn || crimeOn) palette.selectedBorder else palette.locateFabBorder),
                            modifier = Modifier.size(44.dp).testTag("layers-button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Layers, contentDescription = "Map layers", Modifier.size(20.dp))
                            }
                        }
                        // Locate: a 44dp round glass button with a 20dp icon.
                        Surface(
                            onClick = onLocate,
                            shape = CircleShape,
                            color = palette.locateFabBg,
                            contentColor = palette.locateFabIcon,
                            border = frostEdge(palette, palette.locateFabBorder),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = palette.locateFabIcon)
                                else Icon(Icons.Filled.MyLocation, contentDescription = "Show my location", Modifier.size(20.dp))
                            }
                        }
                        // Add a bay: a 52dp round accent button with a 22dp icon.
                        Surface(
                            onClick = { requireSignIn { vm.startAddingBay() } },
                            shape = CircleShape,
                            color = palette.addFabBg,
                            contentColor = palette.addFabIcon,
                            shadowElevation = 6.dp,
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
                                    Filters(filters, counts, mapMoving, palette, supporter.isSupporter, vm::setPrice, onFreeNow, onOpenFilters = { showFilters = true })
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
                        Box(Modifier.fillMaxSize().frostBackdrop(palette)) {
                            listWith(pick, Modifier.fillMaxSize().padding(top = headerDp + 8.dp, bottom = 88.dp + navClearance))
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
                                    Filters(filters, counts, mapMoving, palette, supporter.isSupporter, vm::setPrice, onFreeNow, onOpenFilters = { showFilters = true })
                                }
                                if (crimeOn && !mapMoving) CrimeLegend(crimeData, palette, Modifier.padding(horizontal = 16.dp))
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
                                            rating = community.ratingSummary(bay.id),
                                            // Supporter extra: whether it's charged right now.
                                            chargeStatus = if (supporter.isSupporter) bay.chargeStatus(app.now()) else null,
                                            isSaved = bay.id in favourites,
                                            palette = palette,
                                            onToggleSave = { app.user.toggleFavourite(bay) },
                                            onShare = { shareBay(context, bay) },
                                            onGo = { openDirections(context, bay) },
                                            onOpen = { onOpenBay(bay.id) },
                                            onClose = vm::clearSelection,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                                // "Park near my destination": after searching a street, postcode or place.
                                AnimatedVisibility(
                                    visible = selectedBay == null && destination != null && compactView == CompactView.Map && !mapMoving,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    val d = destination ?: return@AnimatedVisibility
                                    Column {
                                        DestinationCard(d, nearDestination, palette, onPick = { vm.select(it) }, onClose = vm::clearDestination)
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                                // The cheapest fuel near the rider, while the fuel layer is on.
                                AnimatedVisibility(
                                    visible = fuelOn && selectedBay == null && destination == null && compactView == CompactView.Map && !mapMoving,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Column {
                                        FuelCard(
                                            fuelGrade, cheapestFuel, loaded = fuelData != null, palette,
                                            onGrade = { vm.fuelGrade.value = it },
                                            onGo = { s -> openDirections(context, s.lat, s.lon, s.title) },
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                                ViewToggle(
                                    view = compactView,
                                    countLabel = countLabel,
                                    palette = palette,
                                    onSelect = { vm.compactView.value = it },
                                    modifier = Modifier.padding(bottom = 12.dp + navClearance),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            filters, counts, supporter.isSupporter, palette,
            onPrice = vm::setPrice, onCovered = vm::toggleCovered,
            onFreeNow = { if (supporter.isSupporter) vm.toggleFreeNow() else { showFilters = false; onOpenSupporter() } },
            onClear = vm::clearFilters,
            onDismiss = { showFilters = false },
        )
    }
    if (showLayers) {
        LayersSheet(
            fuelOn, fuelGrade, crimeOn, palette,
            onFuel = vm::toggleFuelLayer, onGrade = { vm.fuelGrade.value = it }, onCrime = vm::toggleCrimeLayer,
            onDismiss = { showLayers = false },
        )
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
            tint = auroraPalette().accent,
            modifier = Modifier.align(Alignment.Center).size(48.dp).offset(y = (-24).dp),
        )
        Box(Modifier.align(Alignment.Center).size(6.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
        val palette = auroraPalette()
        Surface(
            color = palette.searchSurface,
            shape = RoundedCornerShape(28.dp),
            border = frostEdge(palette),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp).widthIn(max = 560.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Add a bay", style = auroraStyle(17.sp, FontWeight.ExtraBold, palette.cardTitleText).copy(lineHeight = 22.sp))
                Text("Move the map until the pin sits on the bay, then tap Next.", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext))
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalNavClearance.current),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(onClick = onCancel, shape = CircleShape, color = palette.searchSurface, border = frostEdge(palette), modifier = Modifier.heightIn(min = 48.dp)) {
                Box(Modifier.padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                    Text("Cancel", style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText))
                }
            }
            Surface(onClick = onNext, shape = CircleShape, color = palette.accent, modifier = Modifier.heightIn(min = 48.dp)) {
                Box(Modifier.padding(horizontal = 26.dp), contentAlignment = Alignment.Center) {
                    Text("Next", style = auroraStyle(14.sp, FontWeight.ExtraBold, palette.onAccent))
                }
            }
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

/** The collapsed search bar: a 52dp Frost glass pill, 18dp muted icon, 14sp semibold text. */
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
            .height(52.dp)
            .clip(CircleShape)
            .background(palette.searchSurface)
            .border(frostEdge(palette), CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = if (query.isEmpty()) "Search a town, street or postcode" else "Search: $query" }
            .padding(start = 18.dp, end = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = if (mapMoving) palette.accent else palette.searchIconIdle,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (query.isEmpty()) "Search a town, street or postcode" else query,
                color = if (query.isEmpty()) palette.searchPlaceholder else palette.cardTitleText,
                fontFamily = FrostFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
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
                            color = palette.accent,
                            fontFamily = FrostFont,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(palette.selected)
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
                    listOfNotNull(bay.locality, bay.feeText).joinToString(" · "),
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

/** The status dot for each price pill; null for All. */
private fun PriceFilter.dot(palette: AuroraPalette): Color? = when (this) {
    PriceFilter.All -> null
    PriceFilter.Free -> palette.free
    PriceFilter.Paid -> palette.paid
    PriceFilter.Unknown -> palette.unknown
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
    supporter: Boolean,
    onPrice: (PriceFilter) -> Unit,
    onFreeNow: () -> Unit,
    onOpenFilters: () -> Unit,
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
                border = BorderStroke(1.dp, palette.selectedBorder),
                modifier = Modifier.heightIn(min = 36.dp).semantics { contentDescription = "Map moving, $label $count" },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(palette.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Telemetry Active \u2022 ${label.uppercase()} ($count ${if (count == 1) "bay" else "bays"})",
                        color = palette.chipUnselectedText,
                        fontFamily = FrostFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        } else {
            // Filters (with a count of the ones set inside it), then the quick chips. Everything else,
            // and the map layers, live in the Filters and Layers sheets, so the row never scrolls.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val hidden = filters.hiddenCount
                Surface(
                    onClick = onOpenFilters,
                    shape = CircleShape,
                    color = if (hidden > 0) palette.chipSelectedBg else palette.chipUnselectedBg,
                    border = if (hidden > 0) null else frostEdge(palette, palette.chipUnselectedBorder),
                    modifier = Modifier.heightIn(min = 36.dp).testTag("filters-button")
                        .semantics { contentDescription = if (hidden > 0) "Filters, $hidden on" else "Filters" },
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = if (hidden > 0) palette.chipSelectedText else palette.chipUnselectedText, modifier = Modifier.size(18.dp))
                        if (hidden > 0) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(if (hidden > 0) palette.chipSelectedText else palette.accent), contentAlignment = Alignment.Center) {
                                Text("$hidden", style = auroraStyle(11.sp, FontWeight.ExtraBold, palette.chipSelectedBg))
                            }
                        }
                    }
                }
                // Free and Paid: tap to show only those, tap again for all bays.
                listOf(PriceFilter.Free, PriceFilter.Paid).forEach { p ->
                    Pill(
                        selected = filters.price == p,
                        onClick = { onPrice(if (filters.price == p) PriceFilter.All else p) },
                        label = p.label,
                        count = null,
                        palette = palette,
                        leading = { p.dot(palette)?.let { Box(Modifier.size(8.dp).clip(CircleShape).background(it)) } },
                    )
                }
                // Supporter extra: free to park in right now, counting paid bays outside their charged hours.
                Pill(
                    selected = supporter && filters.freeNow,
                    onClick = onFreeNow,
                    label = "Free now",
                    count = null,
                    palette = palette,
                    leading = {
                        Icon(
                            if (supporter) Icons.Outlined.Schedule else Icons.Outlined.Lock,
                            contentDescription = if (supporter) null else "Supporter extra",
                            tint = palette.chipUnselectedText,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                )
            }
        }
    }
}

/** How many filters are set inside the Filters sheet but not visible as a chip: Unknown price and Covered only. */
private val ExploreFilters.hiddenCount: Int
    get() = (if (price == PriceFilter.Unknown) 1 else 0) + (if (coveredOnly) 1 else 0)

/** Every bay filter: price, covered and Free now, with how many bays match. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    filters: ExploreFilters,
    counts: Map<PriceFilter, Int>,
    supporter: Boolean,
    palette: AuroraPalette,
    onPrice: (PriceFilter) -> Unit,
    onCovered: () -> Unit,
    onFreeNow: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.searchSurface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).testTag("filters-sheet")) {
            Text("Filters", style = auroraStyle(20.sp, FontWeight.ExtraBold, palette.cardTitleText))
            VSpace(14)
            Text("PRICE", style = frostOverline(palette.accent))
            VSpace(8)
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PriceFilter.entries.forEach { p ->
                    Pill(
                        selected = filters.price == p,
                        onClick = { onPrice(p) },
                        label = p.label,
                        count = null,
                        palette = palette,
                        leading = { p.dot(palette)?.let { Box(Modifier.size(8.dp).clip(CircleShape).background(it)) } },
                    )
                }
            }
            VSpace(16)
            Text("OPTIONS", style = frostOverline(palette.accent))
            SheetSwitchRow("Covered only", "Bays with a roof or shelter", filters.coveredOnly, palette, onCovered)
            HorizontalDivider(color = palette.divider)
            SheetSwitchRow(
                "Free now", "Includes paid bays outside their charging hours", supporter && filters.freeNow, palette, onFreeNow,
                badge = if (supporter) null else "SUPPORTER",
            )
            VSpace(18)
            val count = counts[filters.price] ?: 0
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AuroraOutlinedButton("Clear all", palette, onClick = onClear, modifier = Modifier.weight(1f).height(46.dp))
                AuroraPrimaryButton(
                    if (count == 1) "Show 1 bay" else "Show $count bays", palette, onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp).testTag("filters-show"),
                )
            }
        }
    }
}

/** Fuel prices and vehicle crime, laid over the map; motorcycle garages are always shown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayersSheet(
    fuelOn: Boolean,
    fuelGrade: FuelGrade,
    crimeOn: Boolean,
    palette: AuroraPalette,
    onFuel: () -> Unit,
    onGrade: (FuelGrade) -> Unit,
    onCrime: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.searchSurface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).testTag("layers-sheet")) {
            Text("Map layers", style = auroraStyle(20.sp, FontWeight.ExtraBold, palette.cardTitleText))
            VSpace(6)
            SheetSwitchRow("Fuel prices", "The cheapest forecourts near you", fuelOn, palette, onFuel, icon = Icons.Outlined.LocalGasStation)
            if (fuelOn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    FuelGrade.entries.forEach { g -> AuroraChip(g.label, g == fuelGrade, palette, onClick = { onGrade(g) }) }
                }
            }
            HorizontalDivider(color = palette.divider)
            SheetSwitchRow("Vehicle crime", "Police-recorded, last 3 months", crimeOn, palette, onCrime, icon = Icons.Outlined.LocalPolice)
            VSpace(14)
            Text("ALWAYS ON", style = frostOverline(palette.accent))
            VSpace(8)
            Text("Motorcycle garages", style = auroraStyle(15.sp, FontWeight.Bold, palette.cardTitleText))
            Text("Shown like other places on the map", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext))
            VSpace(18)
            AuroraPrimaryButton("Done", palette, onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp))
        }
    }
}

/** A label, a line of explanation and a switch; the whole row is the touch target. */
@Composable
private fun SheetSwitchRow(
    title: String,
    caption: String,
    checked: Boolean,
    palette: AuroraPalette,
    onToggle: () -> Unit,
    badge: String? = null,
    icon: ImageVector? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch, onClick = onToggle).padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = palette.text2, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = auroraStyle(15.sp, FontWeight.Bold, palette.cardTitleText))
                badge?.let { AuroraTag(it, palette) }
            }
            Text(caption, style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext))
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * What the crime layer shows, under the filters while it's on: the months and the source,
 * and that it covers England, Wales and Northern Ireland.
 */
@Composable
private fun CrimeLegend(data: VehicleCrimeData?, palette: AuroraPalette, modifier: Modifier = Modifier) {
    val text = if (data == null) "Loading vehicle crime\u2026"
    else "Vehicle crime reported to police, ${data.period()} \u00B7 England, Wales and NI"
    Row(
        modifier
            .clip(CircleShape)
            .background(palette.searchSurface)
            .border(frostEdge(palette), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("crime-legend"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(width = 28.dp, height = 8.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color(0x66FFC460), Color(0xFFFF8040), Color(0xFFE53946)))),
        )
        Text(text, style = auroraStyle(11.sp, FontWeight.Bold, palette.text2), maxLines = 2)
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
            fontFamily = FrostFont,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
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
        border = if (selected) null else frostEdge(palette, palette.chipUnselectedBorder),
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
                fontFamily = FrostFont,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
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
 * The Frost preview of the picked bay: a glass panel with status, spaces and
 * distance as an overline, the name large below, the rating chip top right,
 * the Supporter "free right now" line, then save, share and directions.
 * Tapping the panel opens the full bay page.
 */
@Composable
private fun SelectedBayPreviewCard(
    bay: Bay,
    distanceMetres: Double?,
    rating: RatingSummary?,
    chargeStatus: String?,
    isSaved: Boolean,
    palette: AuroraPalette,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onGo: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val dot = when (bay.fee) {
        Fee.Free -> palette.free
        Fee.Paid -> palette.paid
        Fee.Unknown -> palette.unknown
    }
    val typeLabel = when (bay.fee) {
        Fee.Free -> if (bay.feeUnconfirmed) "Free (unconfirmed)" else "Free"
        Fee.Paid -> "Paid"
        Fee.Unknown -> "Unknown rules"
    }
    val overline = buildString {
        append(typeLabel)
        bay.capacity?.let { append(" \u00B7 $it ${if (it == 1) "space" else "spaces"}") }
        distanceMetres?.let { append(" \u00B7 ${formatDistance(it)}") }
    }.uppercase()
    val subtitle = bay.locality
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = palette.searchSurface,
        border = frostEdge(palette),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("bay-preview"),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                        Spacer(Modifier.width(6.dp))
                        Text(overline, style = frostOverline(palette.cardSubtext), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(bay.title, style = auroraStyle(22.sp, FontWeight.ExtraBold, palette.cardTitleText).copy(lineHeight = 26.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    subtitle?.let { Text(it, style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext).copy(lineHeight = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    if (bay.isCouncilChecked) CouncilCheckedLabel(palette)
                }
                Spacer(Modifier.width(12.dp))
                RatingBadge(rating, palette, compact = true)
            }
            chargeStatus?.let { status ->
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(palette.selected)
                        .border(1.dp, palette.selectedBorder, CircleShape)
                        .heightIn(min = 32.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                    Text(status, style = auroraStyle(12.sp, FontWeight.Bold, palette.accent).copy(lineHeight = 16.sp), modifier = Modifier.testTag("preview-charge-status"))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PreviewRoundButton(
                    icon = if (isSaved) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    description = if (isSaved) "Remove from saved" else "Save bay",
                    tint = if (isSaved) palette.paid else palette.text2,
                    palette = palette,
                    onClick = onToggleSave,
                )
                PreviewRoundButton(Icons.Filled.Share, "Share bay", palette.text2, palette, onShare)
                Surface(onClick = onGo, shape = CircleShape, color = palette.accent, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(18.dp))
                        Text("Directions", style = auroraStyle(14.sp, FontWeight.ExtraBold, palette.onAccent).copy(lineHeight = 18.sp), maxLines = 1)
                    }
                }
                PreviewRoundButton(Icons.Filled.Close, "Close", palette.text2, palette, onClose)
            }
        }
    }
}

/** A 48dp round tile button on the preview card. */
@Composable
private fun PreviewRoundButton(icon: ImageVector, description: String, tint: Color, palette: AuroraPalette, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = palette.tile, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
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


/** The bays nearest a searched place, with walking time; tap one to see it. */
@Composable
private fun DestinationCard(
    destination: Destination,
    bays: List<BayWithDistance>,
    palette: AuroraPalette,
    onPick: (Bay) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = palette.searchSurface,
        border = frostEdge(palette),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("destination-card"),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PARKING NEAR", style = frostOverline(palette.cardSubtext))
                    Text(destination.title, style = auroraStyle(17.sp, FontWeight.ExtraBold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.text2) }
            }
            if (bays.isEmpty()) {
                Text(
                    "No bays within 1 km that match your filters.",
                    style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext),
                )
            }
            bays.forEach { (bay, metres) ->
                val dot = when (bay.fee) {
                    Fee.Free -> palette.free
                    Fee.Paid -> palette.paid
                    Fee.Unknown -> palette.unknown
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPick(bay) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Column(Modifier.weight(1f)) {
                        Text(bay.title, style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(bay.feeText, style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext), maxLines = 1)
                    }
                    Text(
                        "${walkingMinutes(metres ?: 0.0)} min walk",
                        style = auroraStyle(13.sp, FontWeight.ExtraBold, palette.accent),
                    )
                }
            }
        }
    }
}


/**
 * The cheapest forecourts for the chosen grade near the rider: price and distance only,
 * every brand treated alike, as the Fuel Finder scheme asks.
 */
@Composable
private fun FuelCard(
    grade: FuelGrade,
    nearby: List<FuelNearby>,
    loaded: Boolean,
    palette: AuroraPalette,
    onGrade: (FuelGrade) -> Unit,
    onGo: (FuelStation) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = palette.searchSurface,
        border = frostEdge(palette),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("fuel-card"),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CHEAPEST NEARBY", style = frostOverline(palette.cardSubtext), modifier = Modifier.weight(1f))
                FuelGrade.entries.forEach { g ->
                    AuroraChip(g.short, g == grade, palette, onClick = { onGrade(g) })
                }
            }
            when {
                !loaded -> Text("Loading fuel prices\u2026", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext))
                nearby.isEmpty() -> Text("No ${grade.short} prices within 5 miles.", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext))
            }
            nearby.forEach { n ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onGo(n.station) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(formatPence(n.price), style = auroraStyle(16.sp, FontWeight.ExtraBold, palette.accent))
                    Column(Modifier.weight(1f)) {
                        Text(n.station.title, style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatDistance(n.metres) + (n.station.postcode?.let { " \u00b7 $it" } ?: ""), style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardSubtext), maxLines = 1)
                    }
                    Icon(Icons.Filled.Navigation, contentDescription = "Directions to ${n.station.title}", tint = palette.text2, modifier = Modifier.size(18.dp))
                }
            }
            Text("Prices from the government's Fuel Finder scheme.", style = auroraStyle(10.sp, color = palette.cardSubtext))
        }
    }
}
