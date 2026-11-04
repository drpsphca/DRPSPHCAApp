package com.drpsphca.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun platform() = "Android"

actual fun createPlatformHttpClientEngine(): HttpClientEngine = OkHttp.create()

@Composable
actual fun PlatformArticleImage(
    article: Article,
    modifier: Modifier,
    contentDescription: String,
) {
    SubcomposeAsyncImage(
        model = articleImageRequest(article),
        contentDescription = contentDescription,
        imageLoader = LocalPhcaImageLoader.current,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
        error = {
            Text(
                "Image error: ${it.result.throwable::class.simpleName}: " +
                    it.result.throwable.message.orEmpty(),
            )
        },
    )
}