package uk.motobay.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.motobay.app.MotoBayApplication
import uk.motobay.app.data.Bay
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.Fee
import uk.motobay.app.data.PlaceSuggestion
import uk.motobay.app.data.friendlyMessage
import uk.motobay.app.util.distanceMetres

enum class PriceFilter { All, Free, Paid }
enum class CompactView { Map, List }

data class BayWithDistance(val bay: Bay, val distanceMetres: Double?)

data class ExploreFilters(
    val price: PriceFilter = PriceFilter.All,
    val coveredOnly: Boolean = false,
)

data class SearchUiState(
    val query: String = "",
    val expanded: Boolean = false,
    val loading: Boolean = false,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val error: String? = null,
)

/** The list shows at most this many of the nearest bays in view. */
private const val LIST_LIMIT = 200

@OptIn(FlowPreview::class)
class ExploreViewModel(private val app: MotoBayApplication) : ViewModel() {
    val filters = MutableStateFlow(ExploreFilters())
    val compactView = MutableStateFlow(CompactView.Map)
    val viewport = MutableStateFlow<MapViewport?>(null)
    val command = MutableStateFlow<MapCommand?>(null)
    val locating = MutableStateFlow(false)
    val search = MutableStateFlow(SearchUiState())

    /** Bay shown in the side pane on expanded layouts. */
    val selectedId = MutableStateFlow<String?>(null)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val userPosition = app.location.position
    val loadState = app.bays.state
    val allBays = app.bays.bays

    /** Every bay matching the filters. The map clusters them, so it shows the whole UK. */
    val mapBays: StateFlow<List<Bay>> = combine(allBays, filters) { all, f ->
        all.values.filter { bay ->
            (f.price == PriceFilter.All || (f.price == PriceFilter.Free && bay.fee == Fee.Free) || (f.price == PriceFilter.Paid && bay.fee == Fee.Paid)) &&
                (!f.coveredOnly || bay.covered == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Bays inside the map view, nearest first, for the list. */
    val visible: StateFlow<List<BayWithDistance>> =
        combine(mapBays, viewport, userPosition) { bays, vp, pos ->
            if (vp == null) return@combine emptyList()
            val origin = pos ?: vp.center
            bays.asSequence()
                .filter { it in vp.box }
                .map { it to distanceMetres(origin, it) }
                .sortedBy { it.second }
                .take(LIST_LIMIT)
                .map { (bay, d) -> BayWithDistance(bay, if (pos != null) d else null) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        retryLoad()
        // Fetch suggestions 250ms after the rider stops typing; a new keystroke cancels the old request.
        viewModelScope.launch {
            search.map { it.query to it.expanded }.distinctUntilChanged().debounce(250).collectLatest { (query, expanded) ->
                if (!expanded || query.trim().length < 2) {
                    search.update { it.copy(suggestions = emptyList(), loading = false, error = null) }
                    return@collectLatest
                }
                search.update { it.copy(loading = true, error = null) }
                runCatching { app.placeSearch.suggest(query, userPosition.value ?: viewport.value?.center) }
                    .onSuccess { list -> search.update { if (it.query == query) it.copy(suggestions = list, loading = false) else it } }
                    .onFailure { e -> search.update { it.copy(loading = false, error = "Search unavailable: ${e.friendlyMessage()}") } }
            }
        }
    }

    fun retryLoad() {
        viewModelScope.launch { app.bays.load() }
    }

    fun onViewportChange(vp: MapViewport) {
        viewport.value = vp
        app.user.updateSettings { it.copy(lastMap = CameraSnapshot(vp.center.lat, vp.center.lon, vp.zoom)) }
    }

    fun onQueryChange(q: String) = search.update { it.copy(query = q) }

    fun onSearchExpandedChange(expanded: Boolean) = search.update { it.copy(expanded = expanded) }

    fun onSuggestionClick(s: PlaceSuggestion) {
        search.update { it.copy(query = s.title, expanded = false, suggestions = emptyList()) }
        viewModelScope.launch {
            val point = s.point ?: runCatching { app.placeSearch.resolvePostcode(s.title) }.getOrNull()
            when {
                s.bbox != null -> command.value = MapCommand.Fit(s.bbox)
                point != null -> command.value = MapCommand.Fly(point.lat, point.lon, zoom = if (s.kind == PlaceSuggestion.Kind.Town) 13.0 else 16.0)
                else -> _messages.send("Couldn't find that place")
            }
        }
    }

    /** Searches with the keyboard's search key: goes to the top suggestion. */
    fun onSearchSubmit() {
        search.value.suggestions.firstOrNull()?.let(::onSuggestionClick)
    }

    /** Called after location permission is granted. */
    fun locate() {
        search.update { it.copy(expanded = false) }
        viewModelScope.launch {
            locating.value = true
            val pos = app.location.locate()
            locating.value = false
            if (pos == null) _messages.send("Couldn't find your location. Is location turned on?")
            else command.value = MapCommand.Fly(pos.lat, pos.lon, zoom = 15.0)
        }
    }

    fun select(bay: Bay) {
        selectedId.value = bay.id
        command.value = MapCommand.Fly(bay.lat, bay.lon)
    }

    fun setPrice(p: PriceFilter) = filters.update { it.copy(price = p) }
    fun toggleCovered() = filters.update { it.copy(coveredOnly = !it.coveredOnly) }
}
