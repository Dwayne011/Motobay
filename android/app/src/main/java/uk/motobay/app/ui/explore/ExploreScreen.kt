package uk.motobay.app.ui.explore

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.components.BayListItem
import uk.motobay.app.ui.theme.MotoBayColors
import uk.motobay.app.ui.theme.isAppInDarkTheme
import uk.motobay.app.util.formatDistance

/**
 * Map and list of bays. Layout follows M3 window size classes:
 * - Compact and medium (under 840dp): full-bleed map with floating search
 *   and filters; a Map/List segmented button switches views; opening a bay
 *   navigates to its own screen.
 * - Expanded (840dp and up): list-detail canonical layout, with the list or
 *   bay detail in a side pane next to the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val vm: ExploreViewModel = viewModel { ExploreViewModel(app) }
    val snackbar = LocalSnackbar.current

    val visible by vm.visible.collectAsStateWithLifecycle()
    val viewport by vm.viewport.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    val command by vm.command.collectAsStateWithLifecycle()
    val compactView by vm.compactView.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val userPosition by vm.userPosition.collectAsStateWithLifecycle()
    val allBays by vm.allBays.collectAsStateWithLifecycle()
    val settings by app.user.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.locate()
    }
    val onLocate = {
        if (app.location.hasPermission()) vm.locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val zoomedOut = (viewport?.zoom ?: 0.0) < MIN_BAY_ZOOM
    val scheme = MaterialTheme.colorScheme
    val ext = MotoBayColors.extended
    val pinColors = PinColors(
        free = ext.success, onFree = ext.onSuccess,
        paid = scheme.secondary, onPaid = scheme.onSecondary,
        unknown = scheme.outline, onUnknown = scheme.surface,
        selected = scheme.primary, onSelected = scheme.onPrimary,
        stroke = scheme.surface, user = scheme.primary,
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp
        val mapBays = visible.map { it.bay }.let { list ->
            val sel = selectedId?.let { allBays[it] }
            if (expanded && sel != null && list.none { it.id == sel.id }) list + sel else list
        }

        val map: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                BayMap(
                    bays = mapBays,
                    selectedId = if (expanded) selectedId else null,
                    userPosition = userPosition,
                    command = command,
                    colors = pinColors,
                    dark = isAppInDarkTheme(settings.themeMode),
                    initialCamera = settings.lastMap,
                    onBayClick = { id ->
                        if (expanded) allBays[id]?.let(vm::select) else onOpenBay(id)
                    },
                    onViewportChange = vm::onViewportChange,
                    onCommandHandled = { vm.command.value = null },
                    modifier = Modifier.fillMaxSize(),
                )
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                FloatingActionButton(
                    onClick = onLocate,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    if (locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.MyLocation, contentDescription = "Show my location")
                }
            }
        }

        val list: @Composable (Modifier) -> Unit = { modifier ->
            when {
                zoomedOut -> Hint("Zoom in or search for a place to see motorcycle bays.", modifier)
                visible.isEmpty() && !loading -> Hint("No bays match here. Try another area or clear the filters.", modifier)
                else -> LazyColumn(modifier) {
                    items(visible, key = { it.bay.id }) { item ->
                        BayListItem(
                            bay = item.bay,
                            distance = item.distanceMetres?.let(::formatDistance),
                            selected = expanded && item.bay.id == selectedId,
                            onClick = { if (expanded) vm.select(item.bay) else onOpenBay(item.bay.id) },
                        )
                    }
                }
            }
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
                                PlaceSearch(searching, vm::search)
                                Filters(filters, vm::setPrice, vm::toggleCovered)
                                Text(
                                    if (zoomedOut) "Nearby bays" else "${visible.size} ${if (visible.size == 1) "bay" else "bays"} in view",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                            list(Modifier.fillMaxSize())
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
                Column(
                    Modifier.statusBarsPadding().padding(16.dp).widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlaceSearch(searching, vm::search)
                    Filters(filters, vm::setPrice, vm::toggleCovered)
                }
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 3.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                ) {
                    SingleChoiceSegmentedButtonRow {
                        val count = if (zoomedOut) "" else " (${visible.size})"
                        listOf(CompactView.Map to "Map", CompactView.List to "List$count").forEachIndexed { i, (view, label) ->
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
        }
    }
}

/** M3 search bar, used here for entering a place (no suggestions view). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSearch(searching: Boolean, onSearch: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = onSearch,
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Search a UK town or postcode") },
                leadingIcon = {
                    if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        windowInsets = WindowInsets(0),
        modifier = Modifier.fillMaxWidth(),
    ) {}
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
