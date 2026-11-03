package com.drpsphca.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun platform(): String

@Composable
expect fun PlatformWebContent(url: String, modifier: Modifier)