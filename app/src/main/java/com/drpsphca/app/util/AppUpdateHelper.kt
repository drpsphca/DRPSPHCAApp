package com.drpsphca.app.util

import android.app.Activity
import androidx.compose.runtime.Composable

interface AppUpdateHelper {
    fun checkForUpdates(activity: Activity)

    @Composable
    fun UpdateDialog(isDarkMode: Boolean)
}
