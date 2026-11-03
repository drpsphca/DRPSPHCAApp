package com.drpsphca.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channels = listOf(
                NotificationChannel("phca-blog", "DRPS PHCA Blog", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("phca-newsletter", "DRPS PHCA Newsletter", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("phca-tval", "DRPS PHCA TVAL", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("phca-app", "DRPS PHCA App-Exclusive", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("pbb_updates", "PBB Updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }
}
