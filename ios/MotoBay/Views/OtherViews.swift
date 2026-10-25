import SwiftUI

struct SavedView: View {
    @Environment(UserStore.self) private var user
    @Environment(LocationProvider.self) private var location

    private var bays: [Bay] {
        let all = Array(user.favourites.values)
        guard let here = location.location else { return all.sorted { $0.name < $1.name } }
        return all.sorted { $0.location.distance(from: here) < $1.location.distance(from: here) }
    }

    var body: some View {
        NavigationStack {
            List(bays) { bay in
                NavigationLink(value: bay) {
                    BayRow(bay: bay, distance: location.location.map { bay.location.distance(from: $0) })
                }
                .swipeActions {
                    Button("Remove", systemImage: "star.slash", role: .destructive) { user.toggleSaved(bay) }
                }
            }
            .overlay {
                if bays.isEmpty {
                    ContentUnavailableView("No Saved Bays", systemImage: "star",
                                           description: Text("Save the bays you use most so you can get directions in one tap."))
                }
            }
            .navigationTitle("Saved")
            .navigationDestination(for: Bay.self) { BayDetailView(bay: $0) }
        }
    }
}

/// Moderation queue: nothing riders submit is public until it's approved here.
struct ReviewView: View {
    enum Kind: String, CaseIterable, Identifiable {
        case photos = "Photos", comments = "Comments"
        var id: Self { self }
    }

    @Environment(CommunityStore.self) private var community
    @State private var kind: Kind = .photos

    private var pendingPhotos: [BayPhoto] { community.contents.photos.filter { $0.status == .pending } }
    private var pendingComments: [BayComment] { community.contents.comments.filter { $0.status == .pending } }

    var body: some View {
        NavigationStack {
            List {
                Picker("Type", selection: $kind) {
                    Text("Photos (\(pendingPhotos.count))").tag(Kind.photos)
                    Text("Comments (\(pendingComments.count))").tag(Kind.comments)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())

                switch kind {
                case .photos:
                    ForEach(pendingPhotos) { photo in
                        ReviewRow(author: photo.author, date: photo.createdAt, bayName: photo.bayName,
                                  text: photo.caption.isEmpty ? "No caption" : photo.caption,
                                  imageURL: community.url(for: photo),
                                  approve: { community.setStatus(photo: photo.id, .approved) },
                                  reject: { community.setStatus(photo: photo.id, .rejected) })
                    }
                case .comments:
                    ForEach(pendingComments) { comment in
                        ReviewRow(author: comment.author, date: comment.createdAt, bayName: comment.bayName,
                                  text: comment.text, imageURL: nil,
                                  approve: { community.setStatus(comment: comment.id, .approved) },
                                  reject: { community.setStatus(comment: comment.id, .rejected) })
                    }
                }
            }
            .overlay {
                if (kind == .photos && pendingPhotos.isEmpty) || (kind == .comments && pendingComments.isEmpty) {
                    ContentUnavailableView("All Caught Up", systemImage: "checkmark.circle",
                                           description: Text("Nothing waiting for review."))
                }
            }
            .navigationTitle("Review")
        }
    }
}

private struct ReviewRow: View {
    let author: String
    let date: Date
    let bayName: String
    let text: String
    let imageURL: URL?
    let approve: () -> Void
    let reject: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let imageURL {
                AsyncImage(url: imageURL) { $0.resizable().scaledToFit() } placeholder: { ProgressView() }
                    .frame(maxHeight: 280)
                    .clipShape(.rect(cornerRadius: 8))
            }
            Text("\(author) · \(date.formatted(date: .abbreviated, time: .shortened))")
                .font(.caption).foregroundStyle(.secondary)
            Text(bayName).font(.headline)
            Text(text)
            HStack {
                Button("Reject", role: .destructive, action: reject).buttonStyle(.bordered)
                Spacer()
                Button("Approve", action: approve).buttonStyle(.borderedProminent)
            }
            .padding(.top, 4)
        }
        .padding(.vertical, 4)
        .swipeActions(edge: .leading) {
            Button("Approve", systemImage: "checkmark", action: approve).tint(.green)
        }
        .swipeActions(edge: .trailing) {
            Button("Reject", systemImage: "xmark", role: .destructive, action: reject)
        }
    }
}

struct SettingsView: View {
    @Environment(UserStore.self) private var user

    var body: some View {
        @Bindable var user = user
        NavigationStack {
            Form {
                Section {
                    TextField("Display name", text: $user.settings.displayName)
                        .textContentType(.nickname)
                } header: {
                    Text("Profile")
                } footer: {
                    Text("Shown next to your photos and comments.")
                }

                Section {
                    Toggle("Moderator Mode", isOn: $user.settings.moderator)
                } header: {
                    Text("Moderation")
                } footer: {
                    Text("Demo only. Shows the Review tab. Real moderator access will come from your account.")
                }

                Section("About") {
                    Text("Bay locations © OpenStreetMap contributors, under the Open Database Licence. Place search by Nominatim. Maps by Apple.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
        }
    }
}
