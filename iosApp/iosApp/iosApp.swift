import SwiftUI

@main
struct DRPSPHCAiOSApp: App {
    @UIApplicationDelegateAdaptor(PBBNotificationDelegate.self) private var notificationDelegate

    init() {
        PBBFirebaseBridge.configureIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}
