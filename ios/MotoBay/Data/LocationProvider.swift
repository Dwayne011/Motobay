import CoreLocation
import Observation

/// The rider's position. Only requested when the rider taps the location
/// button, and never stored or sent anywhere.
@MainActor
@Observable
final class LocationProvider: NSObject, CLLocationManagerDelegate {
    private(set) var location: CLLocation?
    private(set) var authorization: CLAuthorizationStatus = .notDetermined

    private let manager = CLLocationManager()
    private var waiting: [CheckedContinuation<CLLocation?, Never>] = []

    override init() {
        super.init()
        authorization = manager.authorizationStatus
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
    }

    var isDenied: Bool { authorization == .denied || authorization == .restricted }

    /// Asks for permission if needed, then returns one location fix.
    func locate() async -> CLLocation? {
        if isDenied { return nil }
        return await withCheckedContinuation { continuation in
            waiting.append(continuation)
            if authorization == .notDetermined {
                // The fix is requested once the rider answers the permission prompt.
                manager.requestWhenInUseAuthorization()
            } else {
                manager.requestLocation()
            }
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            self.authorization = status
            switch status {
            case .denied, .restricted: self.finish(nil)
            case .authorizedWhenInUse, .authorizedAlways:
                if !self.waiting.isEmpty { self.manager.requestLocation() }
            default: break
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let latest = locations.last
        Task { @MainActor in
            self.location = latest
            self.finish(latest)
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in self.finish(nil) }
    }

    private func finish(_ location: CLLocation?) {
        let pending = waiting
        waiting.removeAll()
        pending.forEach { $0.resume(returning: location) }
    }
}
