# iOS KMP starter

The iOS target embeds the `sharedKit` framework built from `:shared` and hosts
the shared Compose UI in a SwiftUI app.

Run `pod install` from `iosApp/` after cloning or changing `Podfile`, then open
`iosApp.xcworkspace` in Xcode or select the shared `iosApp` run configuration
in Android Studio. CocoaPods provides FirebaseCore and FirebaseMessaging to
both IDEs without relying on Xcode-only Swift Package resolution. The Xcode
build phase invokes Gradle to build and embed the shared framework; Java is
required.

The shared Compose app provides native screens for Home, Blog, Newsletter,
TV Ad Listings, and PBB Updates, plus a Bite-sized filter within Blog.
Authenticated WordPress REST APIs supply posts and featured-image metadata;
the PBB JSON API supplies PBB data. Articles are rendered with native Compose
text and controls, not a web view. The configured WordPress API keys are sent
as Bearer tokens, matching the Android client. Because they are compiled into
the shared framework, they can be extracted from a distributed app; restrict
their permissions and prefer a server-side proxy for production. The iOS
target includes Firebase Messaging integration for PBB announcements,
nominations, and evictions, plus optional PHCA feed topics. To activate
Firebase, add the real `GoogleService-Info.plist` for Firebase project
`device-streaming-bafa1723` and bundle ID `com.drpsphca.app.ios` to the app
target at `iosApp/iosApp/GoogleService-Info.plist`; the Xcode project bundles
it as an app resource, and `PBBFirebaseBridge` initializes Firebase before
`PBBNotificationDelegate` subscribes to topics. Push delivery also requires a
valid APNs key and matching signing/provisioning configuration. Android font
files, logos, navigation drawables, launcher artwork, tag background, and app
strings are available from shared Compose resources. Android-only resource
declarations (such as backup rules, widget configuration, and network security
configuration) are not copied because iOS uses separate platform equivalents.
