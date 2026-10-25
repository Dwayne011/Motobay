import Foundation

/// The bay data file built weekly from OpenStreetMap by
/// .github/workflows/update-bay-data.yml (data/uk-bays.json in the repo).
struct BayDataset: Codable {
    var generated: String?
    var count: Int?
    var bays: [Bay]
}

/// Downloads the bay data file, keeps a copy on the device, and only
/// downloads again when the file has changed (ETag).
struct BayDataService {
    var url = URL(string: "https://raw.githubusercontent.com/Dwayne011/Motobay/main/data/uk-bays.json")!

    private var folder: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    }
    private var cacheURL: URL { folder.appendingPathComponent("uk-bays.json") }
    private var etagURL: URL { folder.appendingPathComponent("uk-bays.etag") }

    /// The copy saved by the last successful download, if any.
    func cached() -> [Bay]? {
        guard let data = try? Data(contentsOf: cacheURL) else { return nil }
        return try? JSONDecoder().decode(BayDataset.self, from: data).bays
    }

    /// Downloads the latest file. Returns nil when the saved copy is already current.
    func download() async throws -> [Bay]? {
        var request = URLRequest(url: url)
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
        request.setValue("MotoBays/\(version) (iOS)", forHTTPHeaderField: "User-Agent")
        if FileManager.default.fileExists(atPath: cacheURL.path), let etag = try? String(contentsOf: etagURL, encoding: .utf8) {
            request.setValue(etag, forHTTPHeaderField: "If-None-Match")
        }
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        if http.statusCode == 304 { return nil }
        guard (200..<300).contains(http.statusCode) else { throw BayDataError.http(http.statusCode) }
        let bays = try JSONDecoder().decode(BayDataset.self, from: data).bays
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        try? data.write(to: cacheURL, options: .atomic)
        if let etag = http.value(forHTTPHeaderField: "ETag") { try? etag.write(to: etagURL, atomically: true, encoding: .utf8) }
        return bays
    }
}

enum BayDataError: LocalizedError {
    case http(Int)

    var errorDescription: String? {
        switch self {
        case .http(404): "The data isn't available yet"
        case .http(let code): "Server error (\(code))"
        }
    }
}

/// A short reason a rider can act on, for network errors.
func friendlyMessage(_ error: Error) -> String {
    if let urlError = error as? URLError {
        switch urlError.code {
        case .notConnectedToInternet, .networkConnectionLost: return "No internet connection"
        case .timedOut: return "The server took too long to respond"
        default: break
        }
    }
    return error.localizedDescription
}

/// Made-up bays for offline development (set the MOTOBAY_SAMPLE_DATA
/// environment variable in the Xcode scheme). NOT real locations.
enum SampleBays {
    static let all: [Bay] = [
        sample(1, 51.5136, -0.1365, .free, capacity: 12, covered: false),
        sample(2, 51.5101, -0.1342, .paid, capacity: 8, maxStay: "4 hours"),
        sample(3, 51.5155, -0.1418, .unknown, covered: true),
        sample(4, 51.5079, -0.1281, .free, capacity: 20, openingHours: "24/7"),
        sample(5, 51.5122, -0.1236, .paid, capacity: 6),
        sample(6, 53.4794, -2.2453, .free, capacity: 10),
        sample(7, 55.9533, -3.1883, .paid, capacity: 15),
    ]

    private static func sample(
        _ n: Int, _ lat: Double, _ lon: Double, _ fee: Fee,
        capacity: Int? = nil, covered: Bool? = nil, maxStay: String? = nil, openingHours: String? = nil
    ) -> Bay {
        Bay(id: "sample/\(n)", lat: lat, lon: lon, name: "Sample bay \(n)", fee: fee, covered: covered,
            capacity: capacity, openingHours: openingHours, maxStay: maxStay, operator: nil, street: nil)
    }
}
