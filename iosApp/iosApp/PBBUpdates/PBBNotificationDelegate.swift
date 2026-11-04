import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications

final class PBBNotificationDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private let routeDefaultsKey = "pbb_updates.pending_notification_route"
    private let topics = [
        "announcements",
        "nominations",
        "evictions",
        "phca-blog",
        "phca-newsletter",
        "phca-tval",
        "phca-app",
    ]

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        guard configureFirebase() else { return true }

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error {
                NSLog("Notification authorization failed: %@", error.localizedDescription)
            }
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        guard FirebaseApp.app() != nil else { return }
        Messaging.messaging().apnsToken = deviceToken
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        var route: [String: String] = [:]
        for key in ["channel", "type", "topic", "season", "tab", "id"] {
            if let value = userInfo[key] as? String {
                route[key] = value
            }
        }
        route["url"] = (userInfo["url"] as? String) ?? (userInfo["post_url"] as? String)
        route["season"] = route["season"] ?? (userInfo["fcm_season"] as? String)
        route["type"] = route["type"] ?? (userInfo["fcm_type"] as? String)

        do {
            let data = try JSONSerialization.data(withJSONObject: route, options: [.sortedKeys])
            UserDefaults.standard.set(String(decoding: data, as: UTF8.self), forKey: routeDefaultsKey)
        } catch {
            NSLog("Could not save notification route: %@", error.localizedDescription)
        }
        completionHandler()
    }

    private func configureFirebase() -> Bool {
        guard Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil else {
            NSLog("Firebase Messaging is not configured: GoogleService-Info.plist is missing from the iOS app target.")
            return false
        }

        FirebaseApp.configure()
        for topic in topics {
            Messaging.messaging().subscribe(toTopic: topic) { error in
                if let error {
                    NSLog("Firebase topic subscription failed for %@: %@", topic, error.localizedDescription)
                }
            }
            return true
        }
    }
}
