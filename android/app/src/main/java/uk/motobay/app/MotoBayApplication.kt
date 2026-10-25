package uk.motobay.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import uk.motobay.app.data.BayRepository
import uk.motobay.app.data.CommunityRepository
import uk.motobay.app.data.PlaceSearch
import uk.motobay.app.data.UserRepository
import uk.motobay.app.location.LocationProvider

/** Holds the app's single instances (manual dependency injection). */
class MotoBayApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob())

    lateinit var bays: BayRepository
        private set
    lateinit var community: CommunityRepository
        private set
    lateinit var user: UserRepository
        private set
    lateinit var location: LocationProvider
        private set
    /** Replaced with recorded responses in tests. */
    var placeSearch = PlaceSearch()

    override fun onCreate() {
        super.onCreate()
        bays = BayRepository(filesDir, BuildConfig.BAY_DATA_URL, useSampleData = BuildConfig.SAMPLE_DATA)
        community = CommunityRepository(this, appScope)
        user = UserRepository(filesDir, appScope)
        location = LocationProvider(this)
    }
}
