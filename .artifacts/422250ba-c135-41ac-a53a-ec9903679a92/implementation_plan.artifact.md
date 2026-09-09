# Implementation Plan - App Update Checks

This plan outlines the implementation of app update checks for both Google Play and Huawei AppGallery releases, ensuring users are notified when a new version of the app is available.

## Proposed Changes

### 1. Dependencies and Configuration
Add the necessary SDKs for Google Play In-App Updates and Huawei AppGallery Kit.

#### [MODIFY] [libs.versions.toml](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/gradle/libs.versions.toml)
- Add `google-app-update` and `google-app-update-ktx` (version 2.1.0).
- Add `huawei-appgallery-kit` (version 6.5.0.300).

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/build.gradle.kts)
- Add `googlePlayImplementation` for Google Play update libraries.
- Add `huaweiImplementation` for Huawei AppGallery library.

---

### 2. App Update Abstraction
Create a flavor-agnostic interface to handle update checks.

#### [NEW] [AppUpdateHelper.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/util/AppUpdateHelper.kt)
- Define `AppUpdateHelper` interface with `checkForUpdates(activity: Activity)` and a `@Composable UpdateDialog()`.

---

### 3. Flavor-Specific Implementations
Implement the update logic for each distribution platform.

#### [NEW] [AppUpdateHelperImpl.kt (Google Play)](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/googlePlay/java/com/drpsphca/app/util/AppUpdateHelperImpl.kt)
- Use `AppUpdateManager` to check for updates.
- If an update is available, show a custom Compose `AlertDialog` with "Update Now" (links to Play Store) and "Not Now" (dismisses).

#### [NEW] [AppUpdateHelperImpl.kt (Huawei)](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/huawei/java/com/drpsphca/app/util/AppUpdateHelperImpl.kt)
- Initialize Huawei Game Service using `JosApps.getJosAppsClient(activity).init()`.
- Use `AppUpdateClient` to check for updates and call `showUpdateDialog` with `isMustBtn=true` to trigger the system update dialog.

---

### 4. Integration
Wire the update check into the main entry point of the application.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/MainActivity.kt)
- Instantiate `AppUpdateHelperImpl` in `onCreate`.
- Call `checkForUpdates` during initialization.
- Include `appUpdateHelper.UpdateDialog()` within the Compose `setContent` block.

## Verification Plan

### Manual Verification
1. **Google Play Flavor:**
    - Build the `googlePlay` variant.
    - Simulate an update availability (e.g., by lowering the version code in the build script vs a published version).
    - Verify the "App Update Available" popup appears with the correct text.
    - Tap "Update Now" and verify it opens the Play Store.
    - Tap "Not Now" and verify it dismisses.
2. **Huawei Flavor:**
    - Build the `huawei` variant.
    - Verify that `checkForUpdates` calls the Huawei HMS APIs correctly.
    - Verify the Huawei system update dialog appears when an update is detected.
