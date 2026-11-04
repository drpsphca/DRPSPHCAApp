# iOS KMP starter

The iOS target embeds the `sharedKit` framework built from `:shared` and hosts
the shared Compose UI in a SwiftUI app.

Open `iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an iOS
simulator or device. The Xcode build phase invokes Gradle to build and embed
the framework; use the repository's Gradle wrapper and ensure Java is available.

The shared Compose app provides native screens for Home, Blog, Newsletter,
TV Ad Listings, and PBB Updates, plus a Bite-sized filter within Blog.
Authenticated WordPress REST APIs supply posts and featured-image metadata;
the PBB JSON API supplies PBB data. Articles are rendered with native Compose
text and controls, not a web view. The configured WordPress API keys are sent
as Bearer tokens, matching the Android client. Because they are compiled into
the shared framework, they can be extracted from a distributed app; restrict
their permissions and prefer a server-side proxy for production. Android-only
services such as push notifications, home-screen widgets, and store updates
are not included in the
iOS app. The Android font files, logos, navigation drawables, launcher
artwork, tag background, and app strings are available from shared Compose
resources. Android-only resource declarations (such as backup rules, widget
configuration, and network security configuration) are not copied because
iOS uses separate platform equivalents.
