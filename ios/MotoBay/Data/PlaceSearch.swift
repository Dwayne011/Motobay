import MapKit
import Observation

/// Search-as-you-type for UK places, using Apple's own search completer (the
/// same engine as Apple Maps' suggestions). No API key needed.
@MainActor
@Observable
final class PlaceSearch: NSObject, MKLocalSearchCompleterDelegate {
    private(set) var suggestions: [MKLocalSearchCompletion] = []
    private(set) var failed = false

    private let completer = MKLocalSearchCompleter()

    /// Great Britain and Northern Ireland.
    static let ukRegion = MKCoordinateRegion(center: .init(latitude: 54.5, longitude: -3.5),
                                             span: .init(latitudeDelta: 11.5, longitudeDelta: 11))

    override init() {
        super.init()
        completer.delegate = self
        completer.resultTypes = [.address, .pointOfInterest]
        completer.region = Self.ukRegion
    }

    /// Updates suggestions for what the rider has typed so far.
    func update(query: String) {
        let q = query.trimmingCharacters(in: .whitespaces)
        if q.count < 2 {
            suggestions = []
            completer.cancel()
        } else {
            completer.queryFragment = q
        }
    }

    /// Finds the map region for a chosen suggestion.
    func region(for completion: MKLocalSearchCompletion) async -> MKCoordinateRegion? {
        let request = MKLocalSearch.Request(completion: completion)
        request.region = Self.ukRegion
        guard let response = try? await MKLocalSearch(request: request).start() else { return nil }
        if let item = response.mapItems.first, response.mapItems.count == 1 {
            return MKCoordinateRegion(center: item.placemark.coordinate, latitudinalMeters: 1500, longitudinalMeters: 1500)
        }
        return response.boundingRegion
    }

    nonisolated func completerDidUpdateResults(_ completer: MKLocalSearchCompleter) {
        let results = completer.results
        Task { @MainActor in
            self.failed = false
            self.suggestions = results
        }
    }

    nonisolated func completer(_ completer: MKLocalSearchCompleter, didFailWithError error: Error) {
        Task { @MainActor in self.failed = true }
    }
}
