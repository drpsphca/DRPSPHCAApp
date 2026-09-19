package com.drpsphca.app.util

import android.app.Activity
import androidx.compose.runtime.Composable

class AppUpdateHelperImpl(activity: Activity) : AppUpdateHelper {
    override fun checkForUpdates(activity: Activity) {
        // No-op
    }

    @Composable
    override fun UpdateDialog(isDarkMode: Boolean) {
        // No-op
    }
}
