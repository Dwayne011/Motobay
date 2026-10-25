package uk.motobay.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.motobay.app.MotoBayApplication
import uk.motobay.app.data.Bay
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.Fee
import uk.motobay.app.util.distanceMetres

enum class PriceFilter { All, Free, Paid }
enum class CompactView { Map, List }

data class BayWithDistance(val bay: Bay, val distanceMetres: Double?)

data class ExploreFilters(
    val price: PriceFilter = PriceFilter.All,
    val coveredOnly: Boolean = false,
)

/** Bays only load at this zoom or closer, to keep Overpass queries small. */
const val MIN_BAY_ZOOM = 13.0

class ExploreViewModel(private val app: MotoBayApplication) : ViewModel() {
    val filters = MutableStateFlow(ExploreFilters())
    val compactView = MutableStateFlow(CompactView.Map)
    val viewport = MutableStateFlow<MapViewport?>(null)
    val command = MutableStateFlow<MapCommand?>(null)
    val loading = MutableStateFlow(false)
    val searching = MutableStateFlow(false)
    val locating = MutableStateFlow(false)

    /** Bay shown in the side pane on expanded layouts. */
    val selectedId = MutableStateFlow<String?>(null)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val userPosition = app.location.position

    /** Bays inside the map view, filtered, nearest first. */
    val visible: StateFlow<List<BayWithDistance>> =
        combine(app.bays.bays, viewport, filters, userPosition) { all, vp, f, pos ->
            if (vp == null) return@combine emptyList()
            val origin = pos ?: vp.center
            all.values.asSequence()
                .filter { it in vp.box }
                .filter { f.price == PriceFilter.All || (f.price == PriceFilter.Free && it.fee == Fee.Free) || (f.price == PriceFilter.Paid && it.fee == Fee.Paid) }
                .filter { !f.coveredOnly || it.covered == true }
                .map { it to distanceMetres(origin, it) }
                .sortedBy { it.second }
                .map { (bay, d) -> BayWithDistance(bay, if (pos != null) d else null) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allBays = app.bays.bays

    fun onViewportChange(vp: MapViewport) {
        viewport.value = vp
        app.user.updateSettings { it.copy(lastMap = CameraSnapshot(vp.center.lat, vp.center.lon, vp.zoom)) }
        if (vp.zoom < MIN_BAY_ZOOM) return
        viewModelScope.launch {
            loading.value = true
            runCatching { app.bays.ensureLoaded(vp.box) }
                .onFailure { _messages.send("Couldn't load bays. Check your connection and try again.") }
            loading.value = false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searching.value = true
            runCatching { app.osm.searchPlace(query.trim()) }
                .onSuccess { place ->
                    if (place == null) _messages.send("No UK place found for \"$query\"")
                    else command.value = MapCommand.Fit(place.bbox)
                }
                .onFailure { _messages.send("Search is unavailable right now") }
            searching.value = false
        }
    }

    /** Called after location permission is granted. */
    fun locate() {
        viewModelScope.launch {
            locating.value = true
            val pos = app.location.locate()
            locating.value = false
            if (pos == null) _messages.send("Couldn't find your location. Is location turned on?")
            else command.value = MapCommand.Fly(pos.lat, pos.lon, zoom = 16.0)
        }
    }

    fun select(bay: Bay) {
        selectedId.value = bay.id
        command.value = MapCommand.Fly(bay.lat, bay.lon)
    }

    fun setPrice(p: PriceFilter) = filters.update { it.copy(price = p) }
    fun toggleCovered() = filters.update { it.copy(coveredOnly = !it.coveredOnly) }
}
