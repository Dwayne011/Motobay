import Foundation
import Observation
import UIKit

/// Reads and writes a Codable value as JSON in Application Support.
struct JSONFile<T: Codable> {
    let url: URL

    init(_ name: String) {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        url = dir.appendingPathComponent(name)
    }

    func load() -> T? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(T.self, from: data)
    }

    func save(_ value: T) {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(value) else { return }
        try? data.write(to: url, options: .atomic)
    }
}

// MARK: - Bays

/// Loads bays on demand for whatever part of the UK is on screen. Results are
/// cached in 0.05° tiles (roughly 5 km) so panning back doesn't refetch.
@MainActor
@Observable
final class BayStore {
    private(set) var bays: [String: Bay] = [:]
    private(set) var isLoading = false

    private let service: OSMService
    private let useSampleData: Bool
    private var loadedTiles = Set<String>()
    private static let tile = 0.05

    init(service: OSMService, useSampleData: Bool) {
        self.service = service
        self.useSampleData = useSampleData
    }

    /// Loads every bay in the box. Throws if the server can't be reached.
    func ensureLoaded(_ box: BBox) async throws {
        let missing = tiles(for: box).filter { !loadedTiles.contains($0) }
        guard !missing.isEmpty else { return }
        missing.forEach { loadedTiles.insert($0) }
        let area = bbox(of: missing)
        isLoading = true
        defer { isLoading = false }
        do {
            let list = useSampleData ? SampleBays.all.filter(area.contains) : try await service.bays(in: area)
            for bay in list { bays[bay.id] = bay }
        } catch {
            missing.forEach { loadedTiles.remove($0) }
            throw error
        }
    }

    /// One bay from cache or the server.
    func bay(id: String) async throws -> Bay? {
        if let cached = bays[id] { return cached }
        let bay = useSampleData ? SampleBays.all.first { $0.id == id } : try await service.bay(id: id)
        if let bay { bays[bay.id] = bay }
        return bay
    }

    private func tiles(for b: BBox) -> [String] {
        let t = Self.tile
        let ys = Int(floor(b.south / t))...Int(floor(b.north / t))
        let xs = Int(floor(b.west / t))...Int(floor(b.east / t))
        return ys.flatMap { y in xs.map { x in "\(y):\(x)" } }
    }

    private func bbox(of keys: [String]) -> BBox {
        let pairs = keys.map { $0.split(separator: ":").compactMap { Int($0) } }
        let ys = pairs.map { $0[0] }, xs = pairs.map { $0[1] }
        let t = Self.tile
        return BBox(south: Double(ys.min()!) * t, west: Double(xs.min()!) * t,
                    north: Double(ys.max()! + 1) * t, east: Double(xs.max()! + 1) * t)
    }
}

// MARK: - User

struct UserSettings: Codable {
    var displayName = ""
    /// Shows the Review tab. A local demo switch only. In production,
    /// moderator access must come from the backend (a role on your account).
    var moderator = false
}

/// Saved bays and settings, stored on the device.
@MainActor
@Observable
final class UserStore {
    private let settingsFile = JSONFile<UserSettings>("settings.json")
    private let favouritesFile = JSONFile<[String: Bay]>("favourites.json")

    var settings: UserSettings { didSet { settingsFile.save(settings) } }
    /// Snapshots, so the Saved list works offline.
    private(set) var favourites: [String: Bay] { didSet { favouritesFile.save(favourites) } }

    init() {
        settings = settingsFile.load() ?? UserSettings()
        favourites = favouritesFile.load() ?? [:]
    }

    func isSaved(_ bay: Bay) -> Bool { favourites[bay.id] != nil }

    func toggleSaved(_ bay: Bay) {
        if favourites[bay.id] != nil { favourites[bay.id] = nil } else { favourites[bay.id] = bay }
    }

    var authorName: String {
        let name = settings.displayName.trimmingCharacters(in: .whitespaces)
        return name.isEmpty ? "Anonymous rider" : name
    }
}

// MARK: - Community

/// Photos and comments that riders submit. Everything starts as pending and is
/// only shown publicly after a moderator approves it.
///
/// This is a local, on-device mock of the backend so the app can be built and
/// tested now. Replace these methods with API calls once a backend (for
/// example Supabase or Firebase) is chosen; views only use this class.
@MainActor
@Observable
final class CommunityStore {
    struct Contents: Codable {
        var photos: [BayPhoto] = []
        var comments: [BayComment] = []
    }

    private let file = JSONFile<Contents>("community.json")
    private(set) var contents: Contents { didSet { file.save(contents) } }

    let photoFolder: URL = {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("photos", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }()

    init() {
        contents = file.load() ?? Contents()
    }

    var pendingCount: Int {
        contents.photos.filter { $0.status == .pending }.count + contents.comments.filter { $0.status == .pending }.count
    }

    func photos(for bay: Bay, status: ModerationStatus) -> [BayPhoto] {
        contents.photos.filter { $0.bayId == bay.id && $0.status == status }
    }

    func comments(for bay: Bay, status: ModerationStatus) -> [BayComment] {
        contents.comments.filter { $0.bayId == bay.id && $0.status == status }
    }

    func url(for photo: BayPhoto) -> URL { photoFolder.appendingPathComponent(photo.fileName) }

    /// Saves a downscaled JPEG (which also drops location metadata) for review.
    func submitPhoto(_ image: UIImage, bay: Bay, caption: String, author: String) throws {
        let id = UUID()
        let fileName = "\(id.uuidString).jpg"
        guard let data = image.downscaled(maxSide: 1600).jpegData(compressionQuality: 0.82) else {
            throw CocoaError(.fileWriteUnknown)
        }
        try data.write(to: photoFolder.appendingPathComponent(fileName), options: .atomic)
        contents.photos.insert(
            BayPhoto(id: id, bayId: bay.id, bayName: bay.name, fileName: fileName, caption: caption,
                     author: author, createdAt: .now, status: .pending),
            at: 0
        )
    }

    func submitComment(_ text: String, bay: Bay, author: String) {
        contents.comments.insert(
            BayComment(id: UUID(), bayId: bay.id, bayName: bay.name, text: text, author: author,
                       createdAt: .now, status: .pending),
            at: 0
        )
    }

    func setStatus(photo id: UUID, _ status: ModerationStatus) {
        guard let i = contents.photos.firstIndex(where: { $0.id == id }) else { return }
        contents.photos[i].status = status
    }

    func setStatus(comment id: UUID, _ status: ModerationStatus) {
        guard let i = contents.comments.firstIndex(where: { $0.id == id }) else { return }
        contents.comments[i].status = status
    }
}

extension UIImage {
    func downscaled(maxSide: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxSide else { return self }
        let scale = maxSide / longest
        let target = CGSize(width: size.width * scale, height: size.height * scale)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
