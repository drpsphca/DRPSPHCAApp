# Implementation Plan - Complete Fullscreen Viewing Experience

This plan enhances the fullscreen video experience for embedded content by hiding all system bars (notification bar, navigation bar, and system buttons) when entering fullscreen mode and restoring them upon exit.

## Proposed Changes

### 1. Fullscreen System Bar Management

#### [MODIFY] [PostDetailScreen.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/screens/PostDetailScreen.kt)
- Import `android.app.Activity`, `androidx.core.view.WindowInsetsCompat`, and `androidx.core.view.WindowInsetsControllerCompat`.
- Update the `WebChromeClient` implementation:
    - In `onShowCustomView`:
        - Retrieve the current `Activity`'s window.
        - Use `WindowInsetsControllerCompat` to hide `systemBars()` (status bar and navigation bar).
        - Set `systemBarsBehavior` to `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` to ensure the user can still access bars if needed by swiping.
    - In `onHideCustomView`:
        - Retrieve the current `Activity`'s window.
        - Use `WindowInsetsControllerCompat` to show `systemBars()`, restoring the original UI state.

## Verification Plan

### Manual Verification
1. **Enter Fullscreen:** Open a post with an embedded video (e.g., YouTube), play it, and tap the "Fullscreen" icon.
    - Verify that the status bar, navigation bar, and system buttons (Back, Home, Recents) are completely hidden.
    - Verify the video fills the entire screen.
2. **Exit Fullscreen:** Tap the exit fullscreen icon or use the system back gesture/button.
    - Verify that the status bar, navigation bar, and system buttons are immediately restored.
    - Verify the UI returns to the standard `PostDetailScreen` layout.
