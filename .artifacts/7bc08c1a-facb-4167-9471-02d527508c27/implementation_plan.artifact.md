# Implementation Plan - App Fixes for Links and Transitions

This plan addresses two issues:
1. External links in `PostDetailScreen` (WebView) not opening in external browsers.
2. Unusual page exit transitions on Samsung devices.

## Proposed Changes

### 1. Fix External Links in WebView
The current implementation uses `WebView` with `setSupportMultipleWindows(true)` but does not implement `onCreateWindow`. This causes links with `target="_blank"` to fail. Additionally, we will ensure all external links are properly intercepted and sent to the system browser.

#### [MODIFY] [PostDetailScreen.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/screens/PostDetailScreen.kt)
- Implement `onCreateWindow` in `WebChromeClient` to handle new window requests by opening them in an external browser.
- Simplify `shouldOverrideUrlLoading` to reliably catch and redirect external URLs.

### 2. Fix Unusual Transitions on Samsung Devices
The `NavHost` transitions currently use `scaleIn(initialScale = 1f)` and `scaleOut(targetScale = 1f)`. These redundant scale operations can cause rendering issues and jittery animations on some devices, particularly Samsung. We will simplify the transitions to use only `fadeIn` and `fadeOut`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/MainActivity.kt)
- Remove `scaleIn` and `scaleOut` from the `NavHost` transitions.
- Ensure smooth cross-fade animations for all screen transitions.

## Verification Plan

### Automated Tests
- N/A (UI and WebView behavior are best verified manually)

### Manual Verification
- **Link Fix:**
    - Open a post in the app.
    - Click on an external link (e.g., a link to another website).
    - Verify that it opens in the default web browser (Chrome, Samsung Internet, etc.).
- **Transition Fix:**
    - Navigate between screens (Home -> Post Detail -> Back).
    - Verify that the transition is a smooth fade without any "shrinking" or "unusual" card-like animations, especially on Samsung devices.
