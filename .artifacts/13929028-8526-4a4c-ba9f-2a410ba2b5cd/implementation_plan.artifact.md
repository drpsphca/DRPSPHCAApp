# Implementation Plan - App Update: Contact Us and Rate This App

This plan addresses the Google Play News and Magazines Policy violation by adding a "Contact Us" section and introduces a "Rate This App" feature for both Google Play and Huawei AppGallery releases.

## User Review Required

> [!IMPORTANT]
> The implementation assumes that `R.drawable.logo` is the correct "DRPSPHCA.com Logo" as identified in the project resources.
> The "Rate This App" feature uses build flavors to determine whether to use the Google Play In-App Review API or redirect to the Huawei AppGallery.

## Proposed Changes

### Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/gradle/libs.versions.toml)
- Add versions and library definitions for `com.google.android.play:review`.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/build.gradle.kts)
- Add `googlePlayImplementation` for the review SDK.

---

### UI Components & Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/MainActivity.kt)
- **`MorePopupMenu`**: Add "Contact Us" and "Rate This App" menu items with their respective icons (`phcaapp_contact` and `phcaapp_star`).
- **`TopBarWithActions`**: Update to pass down the new click listeners to `MorePopupMenu`.
- **`WordPressApp`**:
    - Add a new route `"contactus"` to the `NavHost`.
    - Implement `requestReview` logic based on the app's distribution flavor.
- **`ContactUsScreen` [NEW Composable]**: Implement the screen with centered content, Gilroy Bold font, and clickable links for email, phone, and website.

---

### Logic & Helpers

#### [MODIFY] [MainActivity.kt](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/java/com/drpsphca/app/MainActivity.kt)
- Add a helper function to trigger the Google Play In-App Review or open the Huawei AppGallery rating page.

## Verification Plan

### Automated Tests
- Build the app for both `googlePlay` and `huawei` flavors to ensure no compilation errors.

### Manual Verification
- Deploy to a device/emulator.
- Open the top navigation bar menu.
- Verify "Contact Us" and "Rate This App" items are visible.
- Tap "Contact Us":
    - Verify screen content matches specifications (Logo, Header, Clickable links).
    - Verify links (`mailto:`, `tel:`, web) open correctly.
- Tap "Rate This App":
    - In `googlePlay` flavor, verify the In-App Review popup (if conditions met) or log the flow.
    - In `huawei` flavor, verify it attempts to open the AppGallery.
