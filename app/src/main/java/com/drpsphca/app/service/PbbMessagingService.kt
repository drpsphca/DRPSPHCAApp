package com.drpsphca.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drpsphca.app.MainActivity
import com.drpsphca.app.R
import com.drpsphca.app.ui.screens.PostDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class PbbMessagingService : FirebaseMessagingService() {

    companion object {
        const val PREFS_NAME = "fcm_notifications_prefs"
        const val KEY_SEEN_URLS = "fcm_seen_urls"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        val title = remoteMessage.notification?.title ?: data["title"]
        var body = remoteMessage.notification?.body ?: data["body"]

        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            return
        }

        // Clean HTML tags from excerpt/body
        body = cleanHtmlText(body)
        if (body.isBlank()) {
            return
        }

        val url = data["url"]
        if (!url.isNullOrEmpty()) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val seenUrls = prefs.getStringSet(KEY_SEEN_URLS, emptySet()) ?: emptySet()
            if (seenUrls.contains(url)) {
                // Do not send the same post URL twice
                return
            }
            val updatedUrls = seenUrls.toMutableSet().apply { add(url) }
            prefs.edit().putStringSet(KEY_SEEN_URLS, updatedUrls).apply()
        }

        val channelKey = data["channel"]?.lowercase(Locale.ROOT)
        val typeKey = data["type"]?.lowercase(Locale.ROOT)
        val imageUrl = data["image_url"] ?: data["featured_image"]
        val seasonId = data["season"]

        val channelId = when (channelKey) {
            "blog" -> "phca-blog"
            "newsletter" -> "phca-newsletter"
            "tval" -> "phca-tval"
            "app" -> "phca-app"
            else -> "pbb_updates"
        }

        showNotification(
            channelId = channelId,
            title = cleanHtmlText(title),
            body = body,
            type = typeKey ?: "post",
            channel = channelKey,
            seasonId = seasonId,
            url = url,
            imageUrl = imageUrl
        )
    }

    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        type: String,
        channel: String?,
        seasonId: String?,
        url: String?,
        imageUrl: String?
    ) {
        val targetActivity = when (channel) {
            "blog", "newsletter", "tval", "app" -> PostDetailActivity::class.java
            else -> MainActivity::class.java
        }

        val intent = Intent(this, targetActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!url.isNullOrEmpty()) {
                putExtra("post_url", url)
            }
            if (!channel.isNullOrEmpty()) {
                putExtra("fcm_channel", channel)
            }
            if (!seasonId.isNullOrEmpty()) {
                putExtra("fcm_season", seasonId)
            }
            if (targetActivity == MainActivity::class.java) {
                putExtra("fcm_type", type)
            }
        }

        val notificationId = (url?.hashCode() ?: (System.currentTimeMillis() % 100000).toInt())
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val validImageUrl = if (isValidImageFormat(imageUrl)) imageUrl else null
        val bitmap = if (!validImageUrl.isNullOrEmpty()) {
            getBitmapFromUrl(validImageUrl)
        } else null

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.phcaapp_notificationsicon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            notificationBuilder.setLargeIcon(bitmap)
            notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when (channelId) {
                "phca-blog" -> "DRPS PHCA Blog"
                "phca-newsletter" -> "DRPS PHCA Newsletter"
                "phca-tval" -> "DRPS PHCA TVAL"
                "phca-app" -> "DRPS PHCA App-Exclusive"
                else -> "PBB Updates"
            }
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun isValidImageFormat(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val lower = url.lowercase(Locale.ROOT)
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doInput = true
            connection.connect()
            BitmapFactory.decodeStream(connection.inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanHtmlText(html: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html).toString().trim()
            }
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "").trim()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        // Handle token update if needed
    }
}
