import SwiftUI
import FirebaseCore

@main
struct DRPSPHCAiOSApp: App {
    @UIApplicationDelegateAdaptor(PBBNotificationDelegate.self) private var notificationDelegate

    init() {
        guard Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil else {
            NSLog("Firebase is not configured: GoogleService-Info.plist is missing from the iOS app target.")
            return
        }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}
