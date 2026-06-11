package com.drpsphca.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import com.drpsphca.app.R

val Gilroy = FontFamily(
    Font(R.font.phca_gilroy_regular, FontWeight.Normal),
    Font(R.font.phca_gilroy_medium, FontWeight.Medium),
    Font(R.font.phca_gilroy_bold, FontWeight.Bold)
)

val NotoSerif = FontFamily(
    Font(R.font.google_noto_serif, FontWeight.Normal),
    Font(R.font.google_noto_serif_medium, FontWeight.Medium),
    Font(R.font.google_noto_serif_bold, FontWeight.Bold),
    Font(R.font.google_noto_serif_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.google_noto_serif_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.google_noto_serif_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Gilroy,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
