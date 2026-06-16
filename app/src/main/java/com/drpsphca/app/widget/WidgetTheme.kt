package com.drpsphca.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    val background = ColorProvider(
        day = Color(0xFFF1F1F1),
        night = Color(0xFF060606)
    )
    val text = ColorProvider(
        day = Color(0xFF121212),
        night = Color(0xFFFFFFFF)
    )
    val white = ColorProvider(day = Color.White, night = Color.White)
    val whiteDisabled = ColorProvider(day = Color.White.copy(alpha = 0.3f), night = Color.White.copy(alpha = 0.3f))
    val featuredPlaceholder = ColorProvider(day = Color(0xFF262626), night = Color(0xFF262626))
}
