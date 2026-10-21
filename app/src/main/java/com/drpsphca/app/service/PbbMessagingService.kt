package com.drpsphca.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drpsphca.app.MainActivity
import com.drpsphca.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PbbMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "pbb_updates"
        const val CHANNEL_NAME = "PBB Updates"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"]

        val title = remoteMessage.notification?.title ?: data["title"]
        val body = remoteMessage.notification?.body ?: data["body"]

        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            return
        }

        val seasonId = data["season"]
        val url = data["url"]

        showPbbNotification(
            title = title,
            body = body,
            type = type,
            seasonId = seasonId,
            url = url
        )
    }

    private fun showPbbNotification(
        title: String,
        body: String,
        type: String?,
        seasonId: String?,
        url: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fcm_type", type ?: "announcement")
            if (!seasonId.isNullOrEmpty()) {
                putExtra("fcm_season", seasonId)
            }
            if (!url.isNullOrEmpty()) {
                putExtra("fcm_url", url)
            }
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.phcaapp_notificationsicon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        // Handle token update if needed
    }
}
