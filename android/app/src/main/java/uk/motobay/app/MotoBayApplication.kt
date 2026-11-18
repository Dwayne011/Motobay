package uk.motobay.app

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import uk.motobay.app.data.AuthRepository
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayRepository
import uk.motobay.app.data.BayStatusRepository
import uk.motobay.app.data.FirestoreBayStatusRepository
import uk.motobay.app.data.VehicleCrimeRepository
import uk.motobay.app.data.combinedBays
import uk.motobay.app.data.CommunityRepository
import uk.motobay.app.data.DisplayNames
import uk.motobay.app.data.FirebaseAuthRepository
import uk.motobay.app.data.FirestoreDisplayNames
import uk.motobay.app.data.FirestoreCommunityRepository
import uk.motobay.app.data.PlaceSearch
import uk.motobay.app.data.PlayBillingSupporter
import uk.motobay.app.data.Supporter
import uk.motobay.app.data.UserRepository
import uk.motobay.app.location.LocationProvider
import uk.motobay.app.util.CrashLog
import java.time.LocalDateTime
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uk.motobay.app.data.LastFix
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.widget.NearestFreeWidget
import java.time.ZoneId

/** Holds the app's single instances (manual dependency injection). */
open class MotoBayApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob())

    lateinit var bays: BayRepository

    /** Police-recorded vehicle crime near the bays, for the map layer and bay pages. */
    lateinit var vehicleCrime: VehicleCrimeRepository
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var community: CommunityRepository
        private set
    lateinit var displayNames: DisplayNames
        private set
    /** "Space now / full" and safety details from riders. */
    lateinit var bayStatus: BayStatusRepository
        private set
    lateinit var user: UserRepository
        private set
    lateinit var location: LocationProvider
        private set
    lateinit var crashLog: CrashLog
        private set
    /** The one-off Supporter purchase and what it unlocks. */
    lateinit var supporter: Supporter
        private set
    /** OpenStreetMap bays plus approved rider additions and corrections. */
    lateinit var allBays: StateFlow<Map<String, Bay>>
        private set
    /** The map has already gone to the rider (or asked to) since the app was started; it happens once per launch. */
    var centredOnLaunch = false

    /** A bay to open, from a notification or the widget; the app shows it and clears this. */
    val openBayRequest = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** Replaced with recorded responses in tests. */
    var placeSearch = PlaceSearch()

    override fun onCreate() {
        super.onCreate()
        crashLog = CrashLog(this, BuildConfig.VERSION_NAME).also { it.install(); it.collectSystemExitReason(this) }
        installAppCheck()
        bays = BayRepository(filesDir, BuildConfig.BAY_DATA_URL, useSampleData = useSampleData())
        vehicleCrime = VehicleCrimeRepository(filesDir, BuildConfig.VEHICLE_CRIME_URL)
        auth = createAuth()
        community = createCommunity(auth)
        displayNames = createDisplayNames(auth)
        bayStatus = createBayStatus(auth)
        user = UserRepository(filesDir, appScope)
        location = LocationProvider(this)
        allBays = combinedBays(bays.bays, community.data, appScope)
        supporter = createSupporter()
        keepWidgetCurrent()
    }

    /**
     * Saves where the rider is (only when they've moved 100 m or more, and only on the
     * phone) and redraws the home-screen widget when that, the bays or Supporter change.
     */
    @OptIn(FlowPreview::class)
    private fun keepWidgetCurrent() {
        appScope.launch {
            location.position.filterNotNull().collect { p ->
                val last = user.settings.value.lastFix
                if (last == null || distanceMetres(p, last.lat, last.lon) >= 100) {
                    user.updateSettings { it.copy(lastFix = LastFix(p.lat, p.lon)) }
                }
            }
        }
        appScope.launch {
            combine(user.settings.map { it.lastFix }.distinctUntilChanged(), allBays, supporter.state.map { it.isSupporter }) { _, _, _ -> }
                .debounce(1_000)
                .collect { runCatching { NearestFreeWidget.refresh(this@MotoBayApplication) } }
        }
    }

    /** The time on the street: every bay is in the UK, wherever the phone's clock is set. Fixed in tests. */
    open fun now(): LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

    protected open fun createSupporter(): Supporter = PlayBillingSupporter(this)

    /**
     * App Check: Firestore, Storage and Auth only answer requests that carry a token
     * proving they came from this app (Play Integrity in release, a registered debug
     * token in debug). Enforcement is switched on per service in the Firebase console.
     */
    protected open fun installAppCheck() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
    }

    // Overridden in tests so they don't need Firebase or the network.
    protected open fun useSampleData(): Boolean = BuildConfig.SAMPLE_DATA

    protected open fun createAuth(): AuthRepository = FirebaseAuthRepository(this, appScope)

    protected open fun createCommunity(auth: AuthRepository): CommunityRepository =
        FirestoreCommunityRepository(this, auth, appScope)

    protected open fun createDisplayNames(auth: AuthRepository): DisplayNames = FirestoreDisplayNames(auth)

    protected open fun createBayStatus(auth: AuthRepository): BayStatusRepository = FirestoreBayStatusRepository(auth)
}
