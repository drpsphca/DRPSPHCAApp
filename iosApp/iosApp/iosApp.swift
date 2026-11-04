import SwiftUI

@main
struct DRPSPHCAiOSApp: App {
    @UIApplicationDelegateAdaptor(PBBNotificationDelegate.self) private var notificationDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}
