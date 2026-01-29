
package com.drpsphca.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowSizeClass(): WindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 600 -> WindowSize.COMPACT
        screenWidth < 840 -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}
