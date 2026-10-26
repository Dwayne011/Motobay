import Foundation
import CoreLocation

enum Fee: String, Codable, CaseIterable {
    case free, paid, unknown

    var label: String {
        switch self {
        case .free: "Free"
        case .paid: "Paid"
        case .unknown: "Price unknown"
        }
    }
}

/// A motorcycle parking location, normalised from OpenStreetMap tags.
struct Bay: Codable, Identifiable, Hashable {
    /// OSM element id, for example "node/123456".
    let id: String
    let lat: Double
    let lon: Double
    let name: String
    let fee: Fee
    let covered: Bool?
    let capacity: Int?
    let openingHours: String?
    let maxStay: String?
    let `operator`: String?
    let street: String?
    /// Neighbourhood, town and outward postcode from reverse geocoding (optional in the file).
    var area: String?
    var city: String?
    var postcode: String?

    var coordinate: CLLocationCoordinate2D { .init(latitude: lat, longitude: lon) }
    var location: CLLocation { .init(latitude: lat, longitude: lon) }
    var isSample: Bool { id.hasPrefix("sample/") }
}

struct BBox: Equatable {
    var south: Double, west: Double, north: Double, east: Double

    func contains(_ bay: Bay) -> Bool {
        (south...north).contains(bay.lat) && (west...east).contains(bay.lon)
    }
}

enum ModerationStatus: String, Codable {
    case pending, approved, rejected
}

struct BayPhoto: Codable, Identifiable, Hashable {
    let id: UUID
    let bayId: String
    let bayName: String
    /// File name in the app's photo folder (local mock). A real backend would return a URL.
    let fileName: String
    let caption: String
    let author: String
    let createdAt: Date
    var status: ModerationStatus
}

struct BayComment: Codable, Identifiable, Hashable {
    let id: UUID
    let bayId: String
    let bayName: String
    let text: String
    let author: String
    let createdAt: Date
    var status: ModerationStatus
}

enum Distance {
    /// UK-style distance: yards under a quarter mile, then miles.
    static func format(metres: Double) -> String {
        let miles = metres / 1609.344
        if miles < 0.25 { return "\(Int((metres * 1.0936 / 10).rounded()) * 10) yd" }
        if miles < 10 { return String(format: "%.1f mi", miles) }
        return "\(Int(miles.rounded())) mi"
    }
}
