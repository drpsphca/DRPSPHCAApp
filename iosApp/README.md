# iOS KMP starter

The iOS target embeds the `sharedKit` framework built from `:shared` and hosts
the shared Compose UI in a SwiftUI app.

Open `iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an iOS
simulator or device. The Xcode build phase invokes Gradle to build and embed
the framework; use the repository's Gradle wrapper and ensure Java is available.

The shared screen is an integration starter only. Existing Android-only
features, services, and platform-specific dependencies have not been ported.
