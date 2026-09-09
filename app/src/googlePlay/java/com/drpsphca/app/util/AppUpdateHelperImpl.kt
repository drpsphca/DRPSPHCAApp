package com.drpsphca.app.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Intent
import android.net.Uri

class AppUpdateHelperImpl(private val activity: Activity) : AppUpdateHelper {
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private val _showUpdateDialog = MutableStateFlow(false)

    override fun checkForUpdates(activity: Activity) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                _showUpdateDialog.value = true
            }
        }
    }

    @Composable
    override fun UpdateDialog(isDarkMode: Boolean) {
        val show by _showUpdateDialog.collectAsState()
        if (show) {
            AlertDialog(
                onDismissRequest = { _showUpdateDialog.value = false },
                title = { Text("App Update Available") },
                text = { Text("A new app update is available on Google Play. Tap \"Update Now\" to install the latest app update.") },
                confirmButton = {
                    TextButton(onClick = {
                        _showUpdateDialog.value = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}"))
                            activity.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}"))
                            activity.startActivity(intent)
                        }
                    }) {
                        Text("Update Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { _showUpdateDialog.value = false }) {
                        Text("Not Now")
                    }
                }
            )
        }
    }
}
