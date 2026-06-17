package com.drpsphca.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.Html
import androidx.core.app.NotificationCompat
import com.drpsphca.app.MainActivity
import com.drpsphca.app.R
import com.drpsphca.app.data.WordPressClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val prefs = getSharedPreferences("wp_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", false)

        if (!notificationsEnabled) return

        val postIdStr = remoteMessage.data["post_id"]
        
        if (postIdStr != null) {
            val postId = postIdStr.toIntOrNull()
            if (postId != null) {
                fetchAndShowNotification(postId)
            }
        } else {
            // Fallback for generic notifications
            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
            showNotification(title, body, null, null)
        }
    }

    private fun fetchAndShowNotification(postId: Int) {
        runBlocking {
            try {
                val post = WordPressClient.api.getPost(postId)
                
                // Check if it belongs to Blog or Newsletter categories
                val categories = post.embedded?.terms?.flatten()?.filter { it.taxonomy == "category" }
                val isTargetCategory = categories?.any { 
                    it.name.equals("Blog", ignoreCase = true) || it.name.equals("Newsletter", ignoreCase = true) 
                } ?: false

                if (isTargetCategory) {
                    val title = Html.fromHtml(post.title.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
                    val excerpt = Html.fromHtml(post.excerpt.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
                    val imageUrl = post.embedded.featuredMedia?.firstOrNull()?.sourceUrl
                    
                    showNotification(title, excerpt, postId.toString(), imageUrl)
                }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    private fun showNotification(title: String?, body: String?, postId: String?, imageUrl: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (postId != null) {
                putExtra("post_id", postId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, postId?.toIntOrNull() ?: 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap = if (!imageUrl.isNullOrEmpty()) {
            getBitmapFromUrl(imageUrl)
        } else null

        val channelId = "wp_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.phcaapp_notificationsicon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            // Show at the right if not expanded (LargeIcon)
            notificationBuilder.setLargeIcon(bitmap)
            // Show in full if expanded (BigPictureStyle)
            notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?) // Hide large icon when expanded to show full big picture
            )
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WordPress Posts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(postId?.toIntOrNull() ?: 0, notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        // Handle token if needed
    }
}
