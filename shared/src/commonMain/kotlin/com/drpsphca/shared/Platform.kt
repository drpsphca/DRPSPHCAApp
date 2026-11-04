package com.drpsphca.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.client.engine.HttpClientEngine

expect fun platform(): String

expect fun isTabletDevice(): Boolean

expect fun shareArticle(title: String, url: String)

expect fun createPlatformHttpClientEngine(): HttpClientEngine

@Composable
expect fun loadPersistedPbbSeason(): String?

expect fun persistPbbSeason(seasonId: String)

expect fun consumePendingNotificationRoute(): String?

@Composable
expect fun PlatformArticleImage(
    article: Article,
    modifier: Modifier,
    contentDescription: String,
    onClick: (() -> Unit)?,
)