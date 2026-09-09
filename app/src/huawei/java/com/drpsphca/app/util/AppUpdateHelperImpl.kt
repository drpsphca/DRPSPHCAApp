package com.drpsphca.app.util

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import com.huawei.hms.jos.JosApps
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack
import com.huawei.updatesdk.service.otaupdate.UpdateKey
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo
import android.util.Log
import androidx.core.content.IntentCompat

class AppUpdateHelperImpl(private val activity: Activity) : AppUpdateHelper {
    
    override fun checkForUpdates(activity: Activity) {
        JosApps.getJosAppsClient(activity).init(null)
        val client = JosApps.getAppUpdateClient(activity)
        client.checkAppUpdate(activity, object : CheckUpdateCallBack {
            override fun onUpdateInfo(intent: Intent?) {
                if (intent != null) {
                    val status = intent.getIntExtra(UpdateKey.STATUS, -99)
                    if (status == 7) { // 7 corresponds to UpdateStatus.HAS_UPGRADE_INFO
                        val info = IntentCompat.getSerializableExtra(intent, UpdateKey.INFO, ApkUpgradeInfo::class.java)
                        if (info != null) {
                            client.showUpdateDialog(activity, info, true)
                        }
                    }
                }
            }

            override fun onUpdateStoreError(p0: Int) {
                Log.e("HuaweiUpdate", "Update store error: $p0")
            }

            override fun onMarketStoreError(p0: Int) {
                Log.e("HuaweiUpdate", "Market store error: $p0")
            }

            override fun onMarketInstallInfo(intent: Intent?) {
                Log.d("HuaweiUpdate", "Market install info: $intent")
            }
        })
    }

    @Composable
    override fun UpdateDialog(isDarkMode: Boolean) {
        // Huawei handles its own dialog via showUpdateDialog
    }
}
