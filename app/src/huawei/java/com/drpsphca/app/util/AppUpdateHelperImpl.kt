package com.drpsphca.app.util

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import com.huawei.hms.jos.JosApps
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack
import com.huawei.updatesdk.service.otaupdate.UpdateKey
import com.huawei.updatesdk.service.otaupdate.UpdateStatus
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo
import android.util.Log

class AppUpdateHelperImpl(private val activity: Activity) : AppUpdateHelper {
    
    override fun checkForUpdates(activity: Activity) {
        JosApps.getJosAppsClient(activity).init()
        val client = JosApps.getAppUpdateClient(activity)
        client.checkAppUpdate(activity, object : CheckUpdateCallBack {
            override fun onUpdateInfo(intent: Intent?) {
                if (intent != null) {
                    val status = intent.getIntExtra(UpdateKey.STATUS, -99)
                    if (status == UpdateStatus.HAS_UPGRADE_INFO) {
                        val info = intent.getSerializableExtra(UpdateKey.INFO) as? ApkUpgradeInfo
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

            override fun onMarketBusy() {
                Log.e("HuaweiUpdate", "Market busy")
            }
        })
    }

    @Composable
    override fun UpdateDialog(isDarkMode: Boolean) {
        // Huawei handles its own dialog via showUpdateDialog
    }
}
