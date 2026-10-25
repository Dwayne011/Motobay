package uk.motobay.app.ui.explore

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.motobay.app.data.BayLoadState
import uk.motobay.app.data.PlaceSuggestion
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.components.BayListItem
import uk.motobay.app.ui.theme.MotoBayColors
import uk.motobay.app.ui.theme.isAppInDarkTheme
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
    val settings by app.user.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = search.expanded) { vm.onSearchExpandedChange(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.locate()
    }
    val onLocate = {
        if (app.location.hasPermission()) vm.locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val scheme = MaterialTheme.colorScheme
    val ext = MotoBayColors.extended
    val pinColors = PinColors(
        free = ext.success, onFree = ext.onSuccess,
        paid = scheme.secondary, onPaid = scheme.onSecondary,
        unknown = scheme.outline, onUnknown = scheme.surface,
        selected = scheme.primary, onSelected = scheme.onPrimary,
        cluster = scheme.primaryContainer, onCluster = scheme.onPrimaryContainer,
        stroke = scheme.surface, user = scheme.primary,
    )
    val countLabel = if (visible.size >= 200) "200+" else "${visible.size}"

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp

        val map: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                BayMap(
                    bays = mapBays,
                    selected = if (expanded) selectedId?.let { allBays[it] } else null,
                    userPosition = userPosition,
                    command = command,
                    colors = pinColors,
                    dark = isAppInDarkTheme(settings.themeMode),
                    initialCamera = settings.lastMap,
                    onBayClick = { id -> if (expanded) allBays[id]?.let(vm::select) else onOpenBay(id) },
                    onViewportChange = vm::onViewportChange,
                    onCommandHandled = { vm.command.value = null },
                    modifier = Modifier.fillMaxSize(),
                )
                if (loadState == BayLoadState.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
                (loadState as? BayLoadState.Failed)?.let { failed ->
                    LoadErrorBanner(
                        failed.message,
                        onRetry = vm::retryLoad,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 88.dp, bottom = 88.dp),
                    )
                }
                FloatingActionButton(onClick = onLocate, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    if (locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.MyLocation, contentDescription = "Show my location")
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
            SuggestionsContent(search, onLocate = { onLocate() }, onPick = vm::onSuggestionClick)
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
            Box(Modifier.fillMaxSize()) {
                // The map stays alive under the list so switching back keeps its state.
                map(Modifier.fillMaxSize())
                if (compactView == CompactView.List) {
                    Surface(Modifier.fillMaxSize()) {
                        list(Modifier.fillMaxSize().statusBarsPadding().padding(top = 136.dp, bottom = 72.dp))
                    }
                }
                if (!search.expanded) {
                    Box(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 72.dp).widthIn(max = 560.dp)) {
                        Filters(filters, vm::setPrice, vm::toggleCovered)
                    }
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 3.dp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow {
                            listOf(CompactView.Map to "Map", CompactView.List to "List ($countLabel)").forEachIndexed { i, (view, label) ->
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
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }
                // M3 search bar: tapping it expands to a full-screen view of suggestions.
                val sidePadding by animateDpAsState(if (search.expanded) 0.dp else 16.dp, label = "searchPadding")
                SearchBar(
                    inputField = { SearchField(search, vm) },
                    expanded = search.expanded,
                    onExpandedChange = vm::onSearchExpandedChange,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .padding(horizontal = sidePadding).padding(top = if (search.expanded) 0.dp else 8.dp),
                    content = suggestions,
                )
            }
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
        placeholder = { Text("Search a town, street or postcode") },
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

/** Suggestions shown while the search bar is expanded; narrows as the rider types. */
@Composable
private fun SuggestionsContent(search: SearchUiState, onLocate: () -> Unit, onPick: (PlaceSuggestion) -> Unit) {
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)
    if (search.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    LazyColumn {
        if (search.query.trim().length < 2) {
            item {
                ListItem(
                    headlineContent = { Text("Your location") },
                    leadingContent = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                    colors = transparent,
                    modifier = Modifier.clickable(onClick = onLocate),
                )
            }
            item {
                Text(
                    "Start typing a town, street or postcode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@LazyColumn
        }
        search.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
        if (!search.loading && search.error == null && search.suggestions.isEmpty()) {
            item {
                Text(
                    "No UK places match \"${search.query.trim()}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(search.suggestions, key = { "${it.title}|${it.subtitle}|${it.point}" }) { s ->
            ListItem(
                headlineContent = { Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = s.subtitle.takeIf { it.isNotEmpty() }?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                leadingContent = { Icon(s.kind.icon, contentDescription = null) },
                colors = transparent,
                modifier = Modifier.clickable { onPick(s) },
            )
        }
    }
}

private val PlaceSuggestion.Kind.icon: ImageVector
    get() = when (this) {
        PlaceSuggestion.Kind.Town -> Icons.Filled.LocationCity
        PlaceSuggestion.Kind.Street -> Icons.Filled.EditRoad
        PlaceSuggestion.Kind.Postcode -> Icons.Filled.MarkunreadMailbox
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

/** Price as a segmented button (quick single choice), covered as a filter chip. */
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
                        label = { Text(p.name) },
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
