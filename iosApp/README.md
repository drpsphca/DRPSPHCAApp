# iOS KMP starter

The iOS target embeds the `sharedKit` framework built from `:shared` and hosts
the shared Compose UI in a SwiftUI app.

Open `iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an iOS
simulator or device. The Xcode build phase invokes Gradle to build and embed
the framework; use the repository's Gradle wrapper and ensure Java is available.

The shared app provides tabs for Home, Blog, Newsletter, TV Ad Listings,
PBB Updates, and Bite-sized content. Each section displays its corresponding
public website using the platform's native web view. Android-only services
such as push notifications, home-screen widgets, and store updates are not
included in the iOS app.
