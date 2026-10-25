import MapKit
import PhotosUI
import SwiftUI

/// Everything about one bay: facts, actions, community photos and comments.
struct BayDetailView: View {
    let bay: Bay

    @Environment(UserStore.self) private var user
    @Environment(CommunityStore.self) private var community
    @Environment(LocationProvider.self) private var location
    @Environment(\.openURL) private var openURL

    @State private var showDirections = false
    @State private var commentText = ""
    @State private var confirmation: String?

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text(bay.name).font(.title2.bold())
                    Text(subtitle).font(.subheadline).foregroundStyle(.secondary)
                }
                .listRowSeparator(.hidden)
                facts
                actions
            }

            PhotosSection(bay: bay, onSubmitted: { confirmation = "Thanks! Your photo will appear once it's been reviewed." })

            Section {
                let approved = community.comments(for: bay, status: .approved)
                if approved.isEmpty {
                    Text("No comments yet. Is it easy to find? Is it usually full?").foregroundStyle(.secondary)
                }
                ForEach(approved) { c in
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(c.author) · \(c.createdAt.formatted(date: .abbreviated, time: .omitted))")
                            .font(.caption).foregroundStyle(.secondary)
                        Text(c.text)
                    }
                }
                TextField("Add a comment", text: $commentText, axis: .vertical)
                    .lineLimit(2...6)
                    .onChange(of: commentText) { _, new in if new.count > 500 { commentText = String(new.prefix(500)) } }
                Button("Post Comment") {
                    community.submitComment(commentText.trimmingCharacters(in: .whitespacesAndNewlines), bay: bay, author: user.authorName)
                    commentText = ""
                    confirmation = "Thanks! Your comment will appear once it's been reviewed."
                }
                .disabled(commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            } header: {
                Text("Comments")
            } footer: {
                let pending = community.comments(for: bay, status: .pending).count
                Text(pending > 0
                     ? "\(pending) \(pending == 1 ? "comment is" : "comments are") waiting for review. Comments are checked before they're published."
                     : "Comments are checked before they're published.")
            }

            Section("Data") {
                Text(bay.isSample
                     ? "This is a development sample, not a real bay."
                     : "Bay data © OpenStreetMap contributors, under the Open Database Licence. Something wrong? You can fix it on OpenStreetMap.")
                    .font(.footnote).foregroundStyle(.secondary)
                if !bay.isSample, let url = URL(string: "https://www.openstreetmap.org/edit?\(bay.id.replacingOccurrences(of: "/", with: "="))") {
                    Link("Edit on OpenStreetMap", destination: url)
                }
            }
        }
        .navigationTitle("Bay Details")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog("Get directions with", isPresented: $showDirections, titleVisibility: .visible) {
            Button("Apple Maps") { openInAppleMaps() }
            Button("Google Maps") { open("https://www.google.com/maps/dir/?api=1&destination=\(bay.lat),\(bay.lon)&travelmode=driving") }
            Button("Waze") { open("https://waze.com/ul?ll=\(bay.lat),\(bay.lon)&navigate=yes") }
        }
        .alert(confirmation ?? "", isPresented: Binding(get: { confirmation != nil }, set: { if !$0 { confirmation = nil } })) {
            Button("OK", role: .cancel) {}
        }
    }

    private var subtitle: String {
        let parts = [bay.operator, location.location.map { "\(Distance.format(metres: bay.location.distance(from: $0))) away" }].compactMap { $0 }
        return parts.isEmpty ? "Motorcycle parking" : parts.joined(separator: " · ")
    }

    private var facts: some View {
        ViewThatFits(in: .horizontal) {
            HStack { factItems }
            VStack(alignment: .leading, spacing: 8) { factItems }
        }
        .font(.subheadline)
    }

    @ViewBuilder private var factItems: some View {
        Label(bay.fee.label, systemImage: bay.fee == .free ? "sterlingsign.circle" : "sterlingsign.circle.fill")
            .foregroundStyle(bay.fee.tint)
        if let capacity = bay.capacity { Label("\(capacity) spaces", systemImage: "motorcycle") }
        if let covered = bay.covered { Label(covered ? "Covered" : "Not covered", systemImage: covered ? "umbrella.fill" : "umbrella") }
        if let maxStay = bay.maxStay { Label("Max stay \(maxStay)", systemImage: "hourglass") }
        if let hours = bay.openingHours { Label(hours, systemImage: "clock") }
    }

    private var actions: some View {
        HStack(spacing: 12) {
            Button {
                showDirections = true
            } label: {
                Label("Directions", systemImage: "arrow.triangle.turn.up.right.diamond.fill").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)

            Button {
                user.toggleSaved(bay)
            } label: {
                Label(user.isSaved(bay) ? "Saved" : "Save", systemImage: user.isSaved(bay) ? "star.fill" : "star")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            ShareLink(item: URL(string: "https://www.openstreetmap.org/\(bay.id)")!, subject: Text(bay.name)) {
                Image(systemName: "square.and.arrow.up")
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Share")
        }
        .controlSize(.large)
        .labelStyle(.titleAndIcon)
    }

    private func openInAppleMaps() {
        let item = MKMapItem(placemark: MKPlacemark(coordinate: bay.coordinate))
        item.name = bay.name
        item.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving])
    }

    private func open(_ string: String) {
        if let url = URL(string: string) { openURL(url) }
    }
}

/// Approved photos plus the "Add Photo" flow (camera or photo library).
private struct PhotosSection: View {
    let bay: Bay
    let onSubmitted: () -> Void

    @Environment(UserStore.self) private var user
    @Environment(CommunityStore.self) private var community

    @State private var showSource = false
    @State private var showCamera = false
    @State private var showLibrary = false
    @State private var libraryItem: PhotosPickerItem?
    @State private var draft: UIImage?
    @State private var viewing: BayPhoto?

    var body: some View {
        Section {
            let approved = community.photos(for: bay, status: .approved)
            if approved.isEmpty {
                Text("No photos yet. Help other riders by adding one.").foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(approved) { photo in
                            Button { viewing = photo } label: {
                                AsyncImage(url: community.url(for: photo)) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Color.secondary.opacity(0.2)
                                }
                                .frame(width: 110, height: 110)
                                .clipShape(.rect(cornerRadius: 8))
                            }
                            .accessibilityLabel(photo.caption.isEmpty ? "Photo by \(photo.author)" : photo.caption)
                        }
                    }
                }
            }
            Button("Add Photo", systemImage: "camera") { showSource = true }
            .confirmationDialog("Add a photo", isPresented: $showSource) {
                if UIImagePickerController.isSourceTypeAvailable(.camera) {
                    Button("Take Photo") { showCamera = true }
                }
                Button("Choose from Library") { showLibrary = true }
            }
            .photosPicker(isPresented: $showLibrary, selection: $libraryItem, matching: .images)
            .onChange(of: libraryItem) { _, item in
                guard let item else { return }
                Task {
                    if let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                        draft = image
                    }
                    libraryItem = nil
                }
            }
            .fullScreenCover(isPresented: $showCamera) {
                CameraPicker { image in draft = image }.ignoresSafeArea()
            }
            .sheet(isPresented: Binding(get: { draft != nil }, set: { if !$0 { draft = nil } })) {
                if let draft {
                    SubmitPhotoSheet(image: draft) { caption in
                        try community.submitPhoto(draft, bay: bay, caption: caption, author: user.authorName)
                        self.draft = nil
                        onSubmitted()
                    }
                }
            }
            .sheet(item: $viewing) { photo in
                NavigationStack {
                    AsyncImage(url: community.url(for: photo)) { $0.resizable().scaledToFit() } placeholder: { ProgressView() }
                        .navigationTitle(photo.caption.isEmpty ? "Photo" : photo.caption)
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar { Button("Done") { viewing = nil } }
                }
            }
        } header: {
            Text("Photos")
        } footer: {
            let pending = community.photos(for: bay, status: .pending).count
            if pending > 0 { Text("\(pending) \(pending == 1 ? "photo is" : "photos are") waiting for review.") }
        }
    }
}

private struct SubmitPhotoSheet: View {
    let image: UIImage
    let submit: (String) throws -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var caption = ""
    @State private var failed = false

    var body: some View {
        NavigationStack {
            Form {
                Image(uiImage: image).resizable().scaledToFit().frame(maxHeight: 280).frame(maxWidth: .infinity)
                TextField("Caption (optional)", text: $caption)
                    .onChange(of: caption) { _, new in if new.count > 140 { caption = String(new.prefix(140)) } }
                Section {
                    Text("Photos are checked before they're published. Please avoid faces and number plates.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Add Photo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Submit") {
                        do { try submit(caption.trimmingCharacters(in: .whitespaces)) } catch { failed = true }
                    }
                }
            }
            .alert("That photo couldn't be saved", isPresented: $failed) { Button("OK", role: .cancel) {} }
        }
    }
}

/// The system camera, wrapped for SwiftUI.
struct CameraPicker: UIViewControllerRepresentable {
    let onImage: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ controller: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPicker
        init(_ parent: CameraPicker) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage { parent.onImage(image) }
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}
