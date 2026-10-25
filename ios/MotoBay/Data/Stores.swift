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

/// Every UK motorcycle bay, from one data file that's downloaded once and
/// kept on the device. Later launches show the saved copy straight away and
/// only download again when the file has changed.
@MainActor
@Observable
final class BayStore {
    private(set) var bays: [String: Bay] = [:]
    private(set) var isLoading = false
    /// Set when there's nothing to show because the download failed.
    private(set) var errorMessage: String?

    private let service: BayDataService
    private let useSampleData: Bool

    init(service: BayDataService, useSampleData: Bool) {
        self.service = service
        self.useSampleData = useSampleData
    }

    /// Shows the saved copy, then checks for a newer file. Safe to call again to retry.
    func load() async {
        if useSampleData {
            bays = Dictionary(uniqueKeysWithValues: SampleBays.all.map { ($0.id, $0) })
            return
        }
        guard !isLoading else { return }
        if bays.isEmpty, let cached = service.cached() { publish(cached) }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            if let fresh = try await service.download() { publish(fresh) }
        } catch {
            // A saved copy is still usable, so only report failure when there's nothing to show.
            if bays.isEmpty { errorMessage = "Couldn't load bays: \(friendlyMessage(error))" }
        }
    }

    private func publish(_ list: [Bay]) {
        bays = Dictionary(list.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
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
