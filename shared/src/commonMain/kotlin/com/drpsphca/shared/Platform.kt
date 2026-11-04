package com.drpsphca.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.client.engine.HttpClientEngine

expect fun platform(): String

expect fun createPlatformHttpClientEngine(): HttpClientEngine

@Composable
expect fun PlatformArticleImage(
    article: Article,
    modifier: Modifier,
    contentDescription: String,
)