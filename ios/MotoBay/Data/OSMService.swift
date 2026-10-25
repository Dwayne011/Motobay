import Foundation

/// OpenStreetMap services: Overpass for bay data and Nominatim for place
/// search. The public servers are for light use only; point these URLs at a
/// self-hosted or paid instance before launch.
struct OSMService {
    var overpassURL = URL(string: "https://overpass-api.de/api/interpreter")!
    var nominatimURL = URL(string: "https://nominatim.openstreetmap.org/search")!

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        // Nominatim and Overpass ask apps to identify themselves.
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
        config.httpAdditionalHeaders = ["User-Agent": "MotoBays/\(version) (iOS)"]
        return URLSession(configuration: config)
    }()

    struct Place {
        let label: String
        let bbox: BBox
    }

    /// Motorcycle parking inside a box: dedicated `amenity=motorcycle_parking`
    /// plus car parks tagged `motorcycle=designated`.
    func bays(in b: BBox) async throws -> [Bay] {
        let box = "\(b.south),\(b.west),\(b.north),\(b.east)"
        return try await overpass("""
        [out:json][timeout:25];
        (
          nwr["amenity"="motorcycle_parking"](\(box));
          nwr["amenity"="parking"]["motorcycle"="designated"](\(box));
        );
        out center tags;
        """)
    }

    /// One bay by OSM id ("node/123"), for saved bays and shared links.
    func bay(id: String) async throws -> Bay? {
        let parts = id.split(separator: "/")
        guard parts.count == 2, ["node", "way", "relation"].contains(String(parts[0])), Int64(parts[1]) != nil else { return nil }
        return try await overpass("[out:json][timeout:25];\(parts[0])(\(parts[1]));out center tags;").first
    }

    /// Looks up a UK town, street or postcode.
    func searchPlace(_ query: String) async throws -> Place? {
        var components = URLComponents(url: nominatimURL, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            .init(name: "q", value: query),
            .init(name: "format", value: "jsonv2"),
            .init(name: "countrycodes", value: "gb"),
            .init(name: "limit", value: "1"),
        ]
        var request = URLRequest(url: components.url!)
        request.setValue("en-GB", forHTTPHeaderField: "Accept-Language")
        let data = try await fetch(request)

        struct Result: Decodable {
            let display_name: String
            let boundingbox: [String]
        }
        guard let r = try JSONDecoder().decode([Result].self, from: data).first else { return nil }
        let bb = r.boundingbox.compactMap(Double.init)
        guard bb.count == 4 else { return nil }
        return Place(label: r.display_name, bbox: BBox(south: bb[0], west: bb[2], north: bb[1], east: bb[3]))
    }

    private func overpass(_ query: String) async throws -> [Bay] {
        var request = URLRequest(url: overpassURL)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let encoded = query.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? ""
        request.httpBody = Data("data=\(encoded)".utf8)
        let data = try await fetch(request)
        return try Self.parse(data)
    }

    private func fetch(_ request: URLRequest) async throws -> Data {
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return data
    }

    // MARK: Parsing

    private struct Response: Decodable {
        let elements: [Element]
    }

    private struct Element: Decodable {
        struct Center: Decodable { let lat: Double; let lon: Double }
        let type: String
        let id: Int64
        let lat: Double?
        let lon: Double?
        let center: Center?
        let tags: [String: String]?
    }

    static func parse(_ data: Data) throws -> [Bay] {
        try JSONDecoder().decode(Response.self, from: data).elements.compactMap { el in
            guard let lat = el.lat ?? el.center?.lat, let lon = el.lon ?? el.center?.lon else { return nil }
            let tags = el.tags ?? [:]
            let street = tags["addr:street"]
            return Bay(
                id: "\(el.type)/\(el.id)",
                lat: lat,
                lon: lon,
                name: tags["name"] ?? street.map { "\($0) motorcycle bay" } ?? "Motorcycle parking",
                fee: parseFee(tags),
                covered: tags["covered"].map { ["yes", "roof", "partial"].contains($0) },
                capacity: (tags["capacity:motorcycle"] ?? tags["capacity"]).flatMap { Int($0) },
                openingHours: tags["opening_hours"],
                maxStay: tags["maxstay"],
                operator: tags["operator"],
                street: street
            )
        }
    }

    private static func parseFee(_ tags: [String: String]) -> Fee {
        let fee = (tags["fee"] ?? tags["fee:motorcycle"] ?? "").lowercased()
        if fee == "no" { return .free }
        if !fee.isEmpty || tags["charge"] != nil { return .paid }
        return .unknown
    }
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
