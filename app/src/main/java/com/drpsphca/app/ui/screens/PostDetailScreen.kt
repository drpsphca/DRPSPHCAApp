package com.drpsphca.app.ui.screens

import android.view.View
import android.webkit.WebChromeClient
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
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body {
                    font-family: sans-serif;
                    color: ${if (isDarkTheme) "#FFFFFF" else "#000000"};
                    background-color: ${if (isDarkTheme) "#121212" else "#FFFFFF"};
                    line-height: 1.6;
                    padding: 16px;
                    margin: 0;
                    word-wrap: break-word;
                }
                h1, h2, h3, h4, h5, h6 {
                    font-weight: bold;
                }
                img, iframe, video, embed, object {
                    max-width: 100% !important;
                    height: auto !important;
                    display: block;
                    margin: 8px auto;
                }
                /* Ensure iframes maintain an aspect ratio if they don't have one */
                .video-container {
                    position: relative;
                    padding-bottom: 56.25%; /* 16:9 */
                    height: 0;
                    overflow: hidden;
                }
                .video-container iframe {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
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
            // Enable hardware acceleration
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                
                // Content scaling settings
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            
            // Allow full screen and better video support
            webChromeClient = WebChromeClient()
            
            // Use a valid base URL (like your website) so YouTube can verify the origin
            loadDataWithBaseURL("https://drpsphca.com", htmlContent, "text/html", "utf-8", null)
        }
    }, modifier = Modifier.fillMaxSize())
}
