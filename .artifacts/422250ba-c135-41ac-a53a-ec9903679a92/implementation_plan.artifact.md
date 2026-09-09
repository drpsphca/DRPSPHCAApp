# Implementation Plan - App Updates and Fixes

This plan outlines the changes required to address four specific issues related to TV Ad Listings dates, WebView rotation, fullscreen video experience, and media playback control.

## Proposed Changes

### 1. TV Ad Listings Date Fix
The publishing date for TV Ad Listings (TVAL) will now be based on the `modified` date from the WordPress API instead of the `date` field. This affects the display date and the date filtering.

#### [MODIFY] [Post.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/data/Post.kt)
- Add `modified: String` field to the `Post` data class.

#### [MODIFY] [WordPressApi.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/data/WordPressApi.kt)
- Update `getPosts` to include optional `modified_after` and `modified_before` query parameters.

#### [MODIFY] [WordPressViewModel.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/viewmodel/WordPressViewModel.kt)
- Update `toUiModel()` and `toDetailUiModel()` extension functions to accept an `isTval` Boolean parameter.
- Use `modified` date instead of `date` if `isTval` is true.
- Update `doFetchTVALPosts()` to use `modified_after` and `modified_before` query parameters when a date filter is applied.
- Ensure all calls to `toUiModel()` and `toDetailUiModel()` for TVAL posts pass `isTval = true`.

---

### 2. WebView Rotation Fix
Prevent the WebView from reloading its content when the device rotates.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/AndroidManifest.xml)
- Add `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|density"` to the `MainActivity` declaration to handle configuration changes manually.

#### [MODIFY] [PostDetailScreen.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/screens/PostDetailScreen.kt)
- Optimize the `AndroidView`'s `update` block to avoid re-loading the same HTML content if only the layout bounds changed due to rotation.

---

### 3. Fullscreen Viewing Fix
Ensure a seamless fullscreen viewing experience for embedded videos (YouTube, Vimeo, etc.).

#### [MODIFY] [PostDetailScreen.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/screens/PostDetailScreen.kt)
- Enhance `WebChromeClient` implementation to properly handle `onShowCustomView` and `onHideCustomView`.
- Update the injected JavaScript `wrapIframes` to ensure all video embeds are detected and configured for potential fullscreen interaction.
- Ensure `settings.javaScriptCanOpenWindowsAutomatically = true` and `settings.setSupportMultipleWindows(true)` if needed for some embeds.

---

### 4. Stop Media on Back Button
Stop any playing media (video or audio) when the user navigates back or the post detail screen is dismissed.

#### [MODIFY] [PostDetailScreen.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/ui/screens/PostDetailScreen.kt)
- In the `BackHandler`, call `webView.onPause()` before handling the custom view or navigating back.
- Use `DisposableEffect` to call `webView.onPause()` and `webView.destroy()` when the Composable leaves the composition to ensure all background media/processes are stopped.

## Verification Plan

### Automated Tests
- Run existing unit tests for `WordPressViewModel` to ensure no regressions in post fetching.
- (Optional) Add a unit test to verify that TVAL posts use the `modified` date.

### Manual Verification
1. **TVAL Date:** Open TV Ad Listings and verify that the date shown matches the "Modified Date" from the WordPress dashboard. Test the date selector to ensure it filters correctly.
2. **Rotation:** Open any post, play a video, and rotate the device. The video should continue playing without the WebView reloading or the video resetting.
3. **Fullscreen:** Tap the fullscreen icon on an embedded YouTube video. It should expand to fill the entire screen.
4. **Back Button:** Play a video or Spotify embed, then tap the back button. The audio should stop immediately.
