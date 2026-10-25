import MapKit
import SwiftUI

enum PriceFilter: String, CaseIterable, Identifiable {
    case all = "All", free = "Free", paid = "Paid"
    var id: Self { self }
}

/// Map and list of bays.
/// - iPhone: map with a search field and filter bar; a toolbar button switches to a list.
/// - iPad: split view with the list in the sidebar and the map beside it.
/// Bay details open in an inspector: a sheet on iPhone, a side panel on iPad.
/// Typing in the search field shows place suggestions that narrow as you type.
struct ExploreView: View {
    @Environment(BayStore.self) private var store
    @Environment(LocationProvider.self) private var location
    @Environment(PlaceSearch.self) private var placeSearch
    @Environment(\.horizontalSizeClass) private var sizeClass

    @State private var position: MapCameraPosition = .region(LastRegion.load())
    @State private var region: MKCoordinateRegion?
    @State private var selectedId: String?
    @State private var price: PriceFilter = .all
    @State private var coveredOnly = false
    @State private var showList = false
    @State private var query = ""
    @State private var searchActive = false
    @State private var banner: String?

    /// SwiftUI's Map has no clustering, so at most this many markers are drawn (nearest first).
    private let maxMarkers = 300

    /// Bays in view that match the filters, nearest first.
    private var visible: [Bay] {
        guard let region else { return [] }
        let box = region.bbox
        let origin = location.location ?? CLLocation(latitude: region.center.latitude, longitude: region.center.longitude)
        return store.bays.values
            .filter { box.contains($0) }
            .filter { price == .all || (price == .free && $0.fee == .free) || (price == .paid && $0.fee == .paid) }
            .filter { !coveredOnly || $0.covered == true }
            .sorted { $0.location.distance(from: origin) < $1.location.distance(from: origin) }
    }

    private var selectedBay: Bay? { selectedId.flatMap { store.bays[$0] } }

    var body: some View {
        let bays = visible
        Group {
            if sizeClass == .regular {
                NavigationSplitView {
                    bayList(bays)
                        .navigationTitle("Explore")
                        .safeAreaInset(edge: .top) { filterBar }
                } detail: {
                    map(bays)
                }
                .searchable(text: $query, isPresented: $searchActive, placement: .sidebar, prompt: "Town, street or postcode")
                .searchSuggestions { suggestions }
            } else {
                NavigationStack {
                    ZStack {
                        map(bays)
                        if showList {
                            bayList(bays).background(Color(.systemBackground))
                        }
                    }
                    .safeAreaInset(edge: .top) { filterBar }
                    .navigationTitle("Explore")
                    .navigationBarTitleDisplayMode(.inline)
                    .searchable(text: $query, isPresented: $searchActive,
                                placement: .navigationBarDrawer(displayMode: .always), prompt: "Town, street or postcode")
                    .searchSuggestions { suggestions }
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(showList ? "Map" : "List", systemImage: showList ? "map" : "list.bullet") {
                                withAnimation { showList.toggle() }
                            }
                        }
                    }
                }
            }
        }
        .onChange(of: query) { _, new in placeSearch.update(query: new) }
        .onSubmit(of: .search) {
            if let first = placeSearch.suggestions.first { Task { await go(to: first) } }
        }
        .inspector(isPresented: Binding(get: { selectedBay != nil }, set: { if !$0 { selectedId = nil } })) {
            if let bay = selectedBay {
                NavigationStack {
                    BayDetailView(bay: bay)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Close", systemImage: "xmark") { selectedId = nil }
                            }
                        }
                }
                .presentationDetents([.medium, .large])
                .inspectorColumnWidth(min: 320, ideal: 380, max: 440)
            }
        }
    }

    // MARK: Search

    @ViewBuilder private var suggestions: some View {
        if placeSearch.failed {
            Text("Search is unavailable right now").foregroundStyle(.secondary)
        }
        ForEach(placeSearch.suggestions, id: \.self) { completion in
            Button {
                Task { await go(to: completion) }
            } label: {
                Label {
                    VStack(alignment: .leading) {
                        Text(completion.title).foregroundStyle(.primary)
                        if !completion.subtitle.isEmpty {
                            Text(completion.subtitle).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                } icon: {
                    Image(systemName: "mappin.circle.fill").foregroundStyle(.red)
                }
            }
        }
    }

    private func go(to completion: MKLocalSearchCompletion) async {
        query = completion.title
        searchActive = false
        guard let target = await placeSearch.region(for: completion) else {
            show("Couldn't find that place")
            return
        }
        withAnimation { position = .region(target) }
    }

    // MARK: Map

    private func map(_ bays: [Bay]) -> some View {
        Map(position: $position, selection: $selectedId) {
            ForEach(bays.prefix(maxMarkers)) { bay in
                Marker(bay.name, systemImage: "motorcycle", coordinate: bay.coordinate)
                    .tint(bay.fee.tint)
                    .tag(bay.id)
            }
            UserAnnotation()
        }
        .mapControls {
            MapCompass()
            MapScaleView()
        }
        .onMapCameraChange(frequency: .onEnd) { context in
            region = context.region
            LastRegion.save(context.region)
        }
        .overlay(alignment: .bottomTrailing) {
            Button {
                Task { await goToMyLocation() }
            } label: {
                Image(systemName: "location.fill")
                    .font(.title3)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .foregroundStyle(.tint)
            .background(.regularMaterial, in: .rect(cornerRadius: 10))
            .padding()
            .accessibilityLabel("Show my location")
        }
        .overlay(alignment: .top) {
            VStack(spacing: 8) {
                if store.isLoading && store.bays.isEmpty {
                    ProgressView("Loading bays…").padding(8).background(.regularMaterial, in: .capsule)
                }
                if let error = store.errorMessage {
                    HStack {
                        Text(error).font(.footnote)
                        Button("Retry") { Task { await store.load() } }.font(.footnote.bold())
                    }
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(.regularMaterial, in: .capsule)
                }
                if bays.count > maxMarkers && !showList {
                    Text("Showing the nearest \(maxMarkers) bays. Zoom in to see more.")
                        .font(.footnote)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(.regularMaterial, in: .capsule)
                }
                if let banner {
                    Text(banner)
                        .font(.footnote)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(.regularMaterial, in: .capsule)
                        .transition(.opacity)
                }
            }
            .padding(.top, 8)
        }
    }

    // MARK: List and filters

    private func bayList(_ bays: [Bay]) -> some View {
        List(selection: sizeClass == .regular ? $selectedId : .constant(nil)) {
            if bays.isEmpty && !store.isLoading {
                ContentUnavailableView("No Bays Here", systemImage: "motorcycle",
                                       description: Text("Zoom out, move the map or change the filters."))
            } else {
                ForEach(bays.prefix(maxMarkers)) { bay in
                    Button { selectedId = bay.id } label: {
                        BayRow(bay: bay, distance: location.location.map { bay.location.distance(from: $0) })
                    }
                    .tag(bay.id)
                    .foregroundStyle(.primary)
                }
            }
        }
        .listStyle(.plain)
    }

    private var filterBar: some View {
        HStack(spacing: 12) {
            Picker("Price", selection: $price) {
                ForEach(PriceFilter.allCases) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)
            Toggle("Covered", systemImage: "umbrella", isOn: $coveredOnly)
                .toggleStyle(.button)
                .labelStyle(.titleOnly)
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(.bar)
    }

    // MARK: Actions

    private func goToMyLocation() async {
        if location.isDenied {
            show("Location is off for MotoBays. You can turn it on in Settings.")
            return
        }
        guard let here = await location.locate() else {
            show("Couldn't find your location")
            return
        }
        withAnimation {
            position = .region(MKCoordinateRegion(center: here.coordinate, latitudinalMeters: 1200, longitudinalMeters: 1200))
        }
    }

    private func show(_ message: String) {
        withAnimation { banner = message }
        Task {
            try? await Task.sleep(for: .seconds(4))
            withAnimation { if banner == message { banner = nil } }
        }
    }
}

struct BayRow: View {
    let bay: Bay
    let distance: CLLocationDistance?

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "motorcycle")
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(bay.fee.tint, in: .circle)
            VStack(alignment: .leading, spacing: 2) {
                Text(bay.name).font(.body)
                HStack(spacing: 6) {
                    Text(bay.fee.label)
                    if let capacity = bay.capacity { Text("· \(capacity) spaces") }
                }
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }
            Spacer()
            if let distance {
                Text(Distance.format(metres: distance)).font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

extension Fee {
    var tint: Color {
        switch self {
        case .free: .green
        case .paid: .orange
        case .unknown: .gray
        }
    }
}

extension MKCoordinateRegion {
    var bbox: BBox {
        BBox(south: center.latitude - span.latitudeDelta / 2, west: center.longitude - span.longitudeDelta / 2,
             north: center.latitude + span.latitudeDelta / 2, east: center.longitude + span.longitudeDelta / 2)
    }
}

/// Remembers the map position, so the app reopens where the rider left it.
enum LastRegion {
    private static let key = "lastRegion"

    static func load() -> MKCoordinateRegion {
        if let v = UserDefaults.standard.array(forKey: key) as? [Double], v.count == 4 {
            return MKCoordinateRegion(center: .init(latitude: v[0], longitude: v[1]),
                                      span: .init(latitudeDelta: v[2], longitudeDelta: v[3]))
        }
        // Great Britain and Northern Ireland.
        return MKCoordinateRegion(center: .init(latitude: 54.5, longitude: -3.0),
                                  span: .init(latitudeDelta: 11, longitudeDelta: 11))
    }

    static func save(_ r: MKCoordinateRegion) {
        UserDefaults.standard.set([r.center.latitude, r.center.longitude, r.span.latitudeDelta, r.span.longitudeDelta], forKey: key)
    }
}
