package com.drpsphca.app.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.drpsphca.app.ui.viewmodel.PostDetailUiModel

@Composable
fun PostDetailScreen(post: PostDetailUiModel) {
    val isDarkTheme = isSystemInDarkTheme()

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body {
                    font-family: sans-serif;
                    color: ${if (isDarkTheme) "#FFFFFF" else "#000000"};
                    background-color: ${if (isDarkTheme) "#121212" else "#FFFFFF"};
                    line-height: 1.6;
                    padding: 16px;
                }
                h1, h2, h3, h4, h5, h6 {
                    font-weight: bold;
                }
                img, iframe {
                    max-width: 100%;
                    height: auto;
                }
            </style>
        </head>
        <body>
            <h1>${post.plainTitle}</h1>
            ${post.content}
        </body>
        </html>
    """

    AndroidView(factory = {
        WebView(it).apply {
            loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }, modifier = Modifier.fillMaxSize())
}
