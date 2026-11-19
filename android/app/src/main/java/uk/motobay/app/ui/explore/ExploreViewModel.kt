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
import uk.motobay.app.data.DisplayNames
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.BaySubmission
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.FuelGrade
import uk.motobay.app.data.FuelNearby
import uk.motobay.app.data.cheapestNear
import uk.motobay.app.data.RecentPlace
import uk.motobay.app.data.PlaceSuggestion
import uk.motobay.app.data.friendlyMessage
import uk.motobay.app.data.isFreeAt
import uk.motobay.app.util.distanceMetres

enum class PriceFilter(val label: String) { All("All"), Free("Free"), Paid("Paid"), Unknown("Unknown") }
enum class CompactView { Map, List }

data class BayWithDistance(val bay: Bay, val distanceMetres: Double?)

data class ExploreFilters(
    val price: PriceFilter = PriceFilter.All,
    val coveredOnly: Boolean = false,
    /** Supporter extra: only bays that are free to park in at this moment. */
    val freeNow: Boolean = false,
)

data class SearchUiState(
    val query: String = "",
    val expanded: Boolean = false,
    val loading: Boolean = false,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val error: String? = null,
)

/** The pin colour (fee) a filter segment shows; null for All. */
val PriceFilter.fee: Fee?
    get() = when (this) {
        PriceFilter.All -> null
        PriceFilter.Free -> Fee.Free
        PriceFilter.Paid -> Fee.Paid
        PriceFilter.Unknown -> Fee.Unknown
    }

/** Where the rider is going, from a search. */
data class Destination(val title: String, val lat: Double, val lon: Double)

/** "Park near my destination" lists up to this many bays within this distance. */
const val DESTINATION_LIMIT = 3
const val DESTINATION_RADIUS_METRES = 1_000.0

/**
 * Walking minutes for a straight-line distance: streets add about a third, at 4.8 km/h
 * (80 m a minute). At least a minute.
 */
fun walkingMinutes(metres: Double): Int = kotlin.math.max(1, kotlin.math.ceil(metres * 1.3 / 80.0).toInt())

/** The [DESTINATION_LIMIT] bays nearest [at] within [DESTINATION_RADIUS_METRES], nearest first. */
fun nearestTo(bays: List<Bay>, at: LatLon): List<BayWithDistance> =
    bays.asSequence()
        .map { it to distanceMetres(at, it) }
        .filter { it.second <= DESTINATION_RADIUS_METRES }
        .sortedBy { it.second }
        .take(DESTINATION_LIMIT)
        .map { (bay, m) -> BayWithDistance(bay, m) }
        .toList()

/** The list shows at most this many of the nearest bays in view. */
private const val LIST_LIMIT = 200

@OptIn(FlowPreview::class)
class ExploreViewModel(private val app: MotoBayApplication) : ViewModel() {
    val filters = MutableStateFlow(ExploreFilters())
    val compactView = MutableStateFlow(CompactView.Map)
    /** Whether the vehicle crime heat layer is showing. */
    val crimeLayer = MutableStateFlow(false)
    val viewport = MutableStateFlow<MapViewport?>(null)
    /** True while the map camera is in motion (a pan, zoom or fly); the UI calms down until it settles. */
    val mapMoving = MutableStateFlow(false)
    val command = MutableStateFlow<MapCommand?>(null)
    val locating = MutableStateFlow(false)
    val search = MutableStateFlow(SearchUiState())

    /** A searched place the rider is heading to; its nearest bays are listed with walking times. */
    val destination = MutableStateFlow<Destination?>(null)

    fun clearDestination() { destination.value = null }

    /** Bay shown in the side pane on expanded layouts. */
    val selectedId = MutableStateFlow<String?>(null)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val userPosition = app.location.position
    val loadState = app.bays.state
    /** OpenStreetMap bays plus approved rider additions and corrections. */
    val allBays = app.allBays
    val recentPlaces = app.user.settings.map { it.recentPlaces }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favourites = app.user.favourites

    /** True while the rider is placing a new bay; the map centre marks the spot. */
    val addingBay = MutableStateFlow(false)

    /** Where the new bay will go, once the rider confirms the spot. */
    val newBayAt = MutableStateFlow<LatLon?>(null)

    /** Every bay matching the filters. The map clusters them, so it shows the whole UK. */
    val mapBays: StateFlow<List<Bay>> = combine(allBays, filters) { all, f ->
        val now = app.now()
        all.values.filter { bay ->
            (f.price == PriceFilter.All || bay.fee == f.price.fee) &&
                (!f.coveredOnly || bay.covered == true) &&
                (!f.freeNow || bay.isFreeAt(now))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The bays nearest [destination] that match the filters, with walking time. */
    val nearDestination: StateFlow<List<BayWithDistance>> = combine(mapBays, destination) { bays, d ->
        if (d == null) emptyList() else nearestTo(bays, LatLon(d.lat, d.lon))
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

    /**
     * How many bays each price pill would show in the current view, so the pills
     * can carry live counts. Ignores the price choice (that is what the pill
     * picks) but respects "covered".
     */
    val counts: StateFlow<Map<PriceFilter, Int>> =
        combine(allBays, viewport, filters) { all, vp, f ->
            if (vp == null) return@combine emptyMap()
            val now = app.now()
            val inView = all.values.filter { it in vp.box && (!f.coveredOnly || it.covered == true) && (!f.freeNow || it.isFreeAt(now)) }
            PriceFilter.entries.associateWith { p -> inView.count { p == PriceFilter.All || it.fee == p.fee } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun onMapMovingChange(moving: Boolean) {
        mapMoving.value = moving
    }

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

    /** Each time the screen is shown: check for newer bay data and refresh the position if permission is already granted. */
    fun refresh() {
        retryLoad()
        if (app.location.hasPermission()) viewModelScope.launch { app.location.locate() }
    }

    fun onViewportChange(vp: MapViewport) {
        viewport.value = vp
        app.user.updateSettings {
            it.copy(lastMap = CameraSnapshot(vp.center.lat, vp.center.lon, vp.zoom, vp.box.south, vp.box.west, vp.box.north, vp.box.east))
        }
    }

    fun onQueryChange(q: String) = search.update { it.copy(query = q) }

    fun onSearchExpandedChange(expanded: Boolean) = search.update { it.copy(expanded = expanded) }

    fun onSuggestionClick(s: PlaceSuggestion) {
        search.update { it.copy(query = s.title, expanded = false, suggestions = emptyList()) }
        viewModelScope.launch {
            val point = s.point ?: runCatching { app.placeSearch.resolvePostcode(s.title) }.getOrNull()
            point?.let { app.user.addRecentPlace(RecentPlace(s.title, s.subtitle, it.lat, it.lon)) }
            // A street, postcode or place (not a whole town): list the bays nearest to it.
            destination.value = point?.takeIf { s.kind != PlaceSuggestion.Kind.Town && s.bbox == null }?.let { Destination(s.title, it.lat, it.lon) }
            when {
                s.bbox != null -> command.value = MapCommand.Fit(s.bbox)
                point != null -> command.value = MapCommand.Fly(point.lat, point.lon, zoom = if (s.kind == PlaceSuggestion.Kind.Town) 13.0 else 16.0)
                else -> _messages.send("Couldn't find that place")
            }
        }
    }

    fun onRecentClick(place: RecentPlace) {
        search.update { it.copy(query = place.title, expanded = false) }
        app.user.addRecentPlace(place)
        destination.value = Destination(place.title, place.lat, place.lon)
        command.value = MapCommand.Fly(place.lat, place.lon, zoom = 15.0)
    }

    fun onSavedBayClick(bay: Bay) {
        search.update { it.copy(expanded = false) }
        select(bay)
    }

    fun clearRecents() = app.user.clearRecentPlaces()

    /** Starts placing a new bay, optionally at a long-pressed point. */
    fun startAddingBay(at: LatLon? = null) {
        search.update { it.copy(expanded = false) }
        compactView.value = CompactView.Map
        if (at != null) command.value = MapCommand.Fly(at.lat, at.lon, zoom = maxOf(viewport.value?.zoom ?: 0.0, 17.0))
        addingBay.value = true
    }

    fun cancelAddingBay() {
        addingBay.value = false
        newBayAt.value = null
    }

    /** The rider has lined the pin up with the bay; show the details form. */
    fun confirmBayLocation() {
        newBayAt.value = viewport.value?.center
            ?: app.user.settings.value.lastMap?.let { LatLon(it.lat, it.lon) }
    }

    fun submitNewBay(submission: BaySubmission) {
        cancelAddingBay()
        viewModelScope.launch {
            val ok = runCatching {
                app.community.submitBay(submission.copy(author = app.user.settings.value.displayName.ifBlank { DisplayNames.ANONYMOUS }))
            }.isSuccess
            _messages.send(if (ok) "Thanks! Your bay will appear once it's been reviewed." else "Couldn't send that bay. Check you're signed in and online.")
        }
    }

    /** Searches with the keyboard's search key: goes to the top suggestion. */
    fun onSearchSubmit() {
        search.value.suggestions.firstOrNull()?.let(::onSuggestionClick)
    }

    /** The rider said no to location: say so once, searching still works. */
    fun locationDenied() {
        viewModelScope.launch { _messages.send("Location is off. Search for a town, street or postcode instead.") }
    }

    /**
     * Once per launch, take the map to the rider. With location already allowed
     * that happens straight away. Otherwise, on the first launch only (and once
     * the opening notice has been read), returns true so the screen can ask for
     * permission; a rider who says no isn't asked again and starts where they left off.
     */
    fun centreOnLaunch(): Boolean {
        if (app.centredOnLaunch) return false
        if (app.location.hasPermission()) {
            app.centredOnLaunch = true
            viewModelScope.launch {
                locating.value = true
                val pos = app.location.locate()
                locating.value = false
                // No complaint if there's no fix: the rider didn't ask, and the map is still where they left it.
                if (pos != null && selectedId.value == null && !addingBay.value) command.value = MapCommand.Fly(pos.lat, pos.lon, zoom = 15.0)
            }
            return false
        }
        val settings = app.user.settings.value
        if (!settings.disclaimerAccepted || settings.locationAsked) return false
        app.centredOnLaunch = true
        app.user.updateSettings { it.copy(locationAsked = true) }
        return true
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

    /** Shows a bay. [bottomPaddingPx] is the height of the sheet that will cover the map. */
    fun select(bay: Bay, bottomPaddingPx: Int = 0) {
        selectedId.value = bay.id
        command.value = MapCommand.Fly(bay.lat, bay.lon, bottomPaddingPx = bottomPaddingPx)
    }

    fun clearSelection() {
        selectedId.value = null
    }

    fun setPrice(p: PriceFilter) = filters.update { it.copy(price = p) }
    fun toggleCovered() = filters.update { it.copy(coveredOnly = !it.coveredOnly) }

    /** Shows or hides the vehicle crime layer, fetching the data the first time it's needed. */
    fun toggleCrimeLayer() {
        crimeLayer.update { !it }
        if (crimeLayer.value) viewModelScope.launch { app.vehicleCrime.load() }
    }
    fun toggleFreeNow() = filters.update { it.copy(freeNow = !it.freeNow) }

    /** Whether fuel prices are showing, and for which grade. */
    val fuelLayer = MutableStateFlow(false)
    val fuelGrade = MutableStateFlow(FuelGrade.E10)

    fun toggleFuelLayer() {
        fuelLayer.update { !it }
        if (fuelLayer.value) viewModelScope.launch { app.fuel.load() }
    }

    /** The cheapest forecourts for the chosen grade near the rider, or near the map centre. */
    val cheapestFuel: StateFlow<List<FuelNearby>> =
        combine(app.fuel.data, fuelLayer, fuelGrade, viewport, userPosition) { data, on, grade, vp, pos ->
            // Near the rider, else the map centre, else where the map was last left.
            val at = pos ?: vp?.center ?: app.user.settings.value.lastMap?.let { LatLon(it.lat, it.lon) }
            if (!on || data == null || at == null) emptyList() else cheapestNear(data.stations, at, grade)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
