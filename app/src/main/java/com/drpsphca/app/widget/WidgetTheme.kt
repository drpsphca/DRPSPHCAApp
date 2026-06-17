package com.drpsphca.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    val background = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF121212)
    )
    val titleText = ColorProvider(
        day = Color(0xFF025CA1),
        night = Color(0xFFFFFFFF)
    )
    val excerptText = ColorProvider(
        day = Color(0xFF121212),
        night = Color(0xFFFFFFFF)
    )
    val white = ColorProvider(day = Color.White, night = Color.White)
    val whiteDisabled = ColorProvider(day = Color.White.copy(alpha = 0.3f), night = Color.White.copy(alpha = 0.3f))
    val featuredPlaceholder = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF121212))
}
