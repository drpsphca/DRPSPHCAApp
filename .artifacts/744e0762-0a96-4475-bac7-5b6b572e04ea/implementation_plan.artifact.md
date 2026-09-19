# Implementation Plan - Match System Back to App Back Animation

This plan removes the unusual system back animation (Predictive Back peek/scale) to ensure that the system back gesture/button uses the exact same smooth fade-out animation as the app's internal back button.

## Proposed Changes

### 1. Disable System Predictive Back Interception
We will disable the system's "Predictive Back" feature for this app. This prevents Android from applying its own scaling animation during back gestures, allowing the `NavHost`'s custom fade transitions to handle the visual transition exclusively.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/fongl/StudioProjects/DRPSPHCAApp/app/src/main/AndroidManifest.xml)
- Change `android:enableOnBackInvokedCallback` to `"false"`.

### 2. Maintain Unified Fade Transitions
We will keep the current `NavHost` configuration in `MainActivity.kt` which provides the 300ms fade-in/fade-out animations.

## Verification Plan

### Manual Verification
1. **System Back Gesture:**
    - Perform a back gesture from a detail screen.
    - Verify the screen fades out smoothly without any scaling or "peek" effect.
2. **System Back Button:**
    - Tap the system back button.
    - Verify the animation is identical to the app's back arrow button.
3. **Consistency:**
    - Compare the app's back arrow vs system back. They should now be visually indistinguishable.
