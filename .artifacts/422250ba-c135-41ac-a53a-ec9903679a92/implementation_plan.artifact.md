# Implementation Plan - Immersive Fullscreen and Refined Transitions

This plan addresses the requirements for a complete immersive fullscreen experience for video embeds and simplifies navigation exit transitions to use a uniform fade-out effect.

## Proposed Changes

### 1. Immersive Fullscreen for Video Embeds
Hide all app-level navigation components (Top App Bar and Bottom Navigation Bar) when a video is in fullscreen mode.

#### [MODIFY] [PostDetailScreen.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/screens/PostDetailScreen.kt)
- Add `onFullscreenToggle: (Boolean) -> Unit = {}` parameter to the `PostDetailScreen` composable.
- Invoke `onFullscreenToggle(true)` in `onShowCustomView`.
- Invoke `onFullscreenToggle(false)` in `onHideCustomView`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/MainActivity.kt)
- In `PostDetailRoute`:
    - Introduce a state `var isFullscreen by remember { mutableStateOf(false) }`.
    - Wrap the `TopAppBar` in a `Box` or use conditional logic to hide it when `isFullscreen` is true.
    - Pass the `onFullscreenToggle` callback to `PostDetailScreen` to update the `isFullscreen` state.
    - Ensure `innerPadding` is handled correctly when the `TopAppBar` is hidden (e.g., set `windowInsets` to 0 or remove padding).

---

### 2. Fade-Only Exit Transitions
Simplify navigation exit animations to use a uniform fade-out effect for all back navigation sources.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/MainActivity.kt)
- Remove `isSystemBackTriggered` state and associated `BackHandler` and `LaunchedEffect` logic in `WordPressApp`.
- Update `NavHost`'s `popExitTransition` to always return `fadeOut()` for all routes.

## Verification Plan

### Manual Verification
1. **Immersive Fullscreen:**
    - Open a post with a video and enter fullscreen.
    - Verify that the app's `TopAppBar` (with the "Post" title and back button) is completely hidden along with system bars.
    - Exit fullscreen and verify the `TopAppBar` and system bars are restored.
2. **Fade Exit Transitions:**
    - Navigate to any screen and press the system back button. Verify the screen fades out without zooming.
    - Navigate to any screen and press the app's back arrow icon. Verify the screen fades out exactly like the system back button.
