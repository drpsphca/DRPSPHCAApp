@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.drpsphca.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.UIKitView
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CancellationException
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIModalPresentationPageSheet
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

actual fun platform() = "iOS"

actual fun isTabletDevice() = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad

actual fun shareArticle(title: String, url: String) {
    val keyWindow = UIApplication.sharedApplication.keyWindow
        ?: error("Unable to share this post because the iOS window is unavailable.")
    val rootViewController = keyWindow.rootViewController
        ?: error("Unable to share this post because the iOS view controller is unavailable.")
    val shareText = if (url.isBlank()) title else "$title\n$url"
    val activityViewController = UIActivityViewController(
        activityItems = listOf(shareText),
        applicationActivities = null,
    )
    if (isTabletDevice()) {
        activityViewController.modalPresentationStyle = UIModalPresentationPageSheet
    }
    rootViewController.presentViewController(activityViewController, animated = true, completion = null)
}

actual fun createPlatformHttpClientEngine(): HttpClientEngine = Darwin.create()

@Composable
actual fun loadPersistedPbbSeason(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("pbb_updates.selected_season_id")

actual fun persistPbbSeason(seasonId: String) {
    NSUserDefaults.standardUserDefaults.setObject(seasonId, forKey = "pbb_updates.selected_season_id")
}

actual fun consumePendingNotificationRoute(): String? {
    val defaults = NSUserDefaults.standardUserDefaults
    val route = defaults.stringForKey("pbb_updates.pending_notification_route")
    defaults.removeObjectForKey("pbb_updates.pending_notification_route")
    return route
}

private val articleImageClient = HttpClient(Darwin.create())

@Composable
actual fun PlatformArticleImage(
    article: Article,
    modifier: Modifier,
    contentDescription: String,
    onClick: (() -> Unit)?,
) {
    var error by remember(article.image) { mutableStateOf<String?>(null) }
    val image by produceState<UIImage?>(null, article.image, article.imageAuthorization) {
        error = null
        val imageUrl = article.image ?: return@produceState
        try {
            val bytes = articleImageClient.get(imageUrl) {
                header(HttpHeaders.UserAgent, "com.drpsphca.app")
                header(HttpHeaders.Accept, "image/avif,image/*;q=0.8")
                val authorization = article.imageAuthorization
                val origin = article.imageAuthorizationOrigin
                if (
                    !authorization.isNullOrBlank() &&
                    !origin.isNullOrBlank() &&
                    (imageUrl == origin || imageUrl.startsWith("$origin/"))
                ) {
                    header(HttpHeaders.Authorization, authorization)
                }
            }.body<ByteArray>()
            require(bytes.isNotEmpty()) { "The server returned an empty image." }
            val data = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            value = UIImage(data = data)
            if (value == null) error = "iOS could not decode this image."
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            error = exception.message ?: "Image download failed."
        }
    }

    Box(
        modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        UIKitView(
            factory = {
                UIImageView().apply {
                    contentMode = UIViewContentModeScaleAspectFill
                    clipsToBounds = true
                    userInteractionEnabled = false
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.image = image },
        )
        when {
            image == null && error == null -> CircularProgressIndicator()
            error != null -> Text("Image unavailable: $error")
        }
    }
}