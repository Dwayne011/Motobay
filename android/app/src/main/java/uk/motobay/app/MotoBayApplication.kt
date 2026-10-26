package uk.motobay.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import uk.motobay.app.data.AuthRepository
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayRepository
import uk.motobay.app.data.combinedBays
import uk.motobay.app.data.CommunityRepository
import uk.motobay.app.data.FirebaseAuthRepository
import uk.motobay.app.data.FirestoreCommunityRepository
import uk.motobay.app.data.PlaceSearch
import uk.motobay.app.data.UserRepository
import uk.motobay.app.location.LocationProvider

/** Holds the app's single instances (manual dependency injection). */
open class MotoBayApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob())

    lateinit var bays: BayRepository
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var community: CommunityRepository
        private set
    lateinit var user: UserRepository
        private set
    lateinit var location: LocationProvider
        private set
    /** OpenStreetMap bays plus approved rider additions and corrections. */
    lateinit var allBays: StateFlow<Map<String, Bay>>
        private set
    /** Replaced with recorded responses in tests. */
    var placeSearch = PlaceSearch()

    override fun onCreate() {
        super.onCreate()
        bays = BayRepository(filesDir, BuildConfig.BAY_DATA_URL, useSampleData = BuildConfig.SAMPLE_DATA)
        auth = createAuth()
        community = createCommunity(auth)
        user = UserRepository(filesDir, appScope)
        location = LocationProvider(this)
        allBays = combinedBays(bays.bays, community.data, appScope)
    }

    // Overridden in tests so they don't need Firebase.
    protected open fun createAuth(): AuthRepository = FirebaseAuthRepository(this, appScope)

    protected open fun createCommunity(auth: AuthRepository): CommunityRepository =
        FirestoreCommunityRepository(this, auth, appScope)
}
