import SwiftUI

@main
struct MotoBayApp: App {
    @State private var bays = BayStore(
        service: BayDataService(),
        useSampleData: ProcessInfo.processInfo.environment["MOTOBAY_SAMPLE_DATA"] == "1"
    )
    @State private var user = UserStore()
    @State private var community = CommunityStore()
    @State private var location = LocationProvider()
    @State private var placeSearch = PlaceSearch()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(bays)
                .environment(user)
                .environment(community)
                .environment(location)
                .environment(placeSearch)
                .task { await bays.load() }
        }
    }
}

/// Tab bar navigation, the iOS convention. On iPad (iOS 18+) the tab bar can
/// also be shown as a sidebar.
struct RootView: View {
    @Environment(UserStore.self) private var user
    @Environment(CommunityStore.self) private var community

    var body: some View {
        TabView {
            ExploreView()
                .tabItem { Label("Explore", systemImage: "map") }
            SavedView()
                .tabItem { Label("Saved", systemImage: "star") }
            if user.settings.moderator {
                ReviewView()
                    .tabItem { Label("Review", systemImage: "checkmark.bubble") }
                    .badge(community.pendingCount)
            }
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}
