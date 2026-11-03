package com.drpsphca.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import kotlinx.cinterop.ExperimentalForeignApi

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformWebContent(url: String, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        factory = {
            WKWebView().apply {
                NSURL.URLWithString(url)?.let { address ->
                    loadRequest(NSURLRequest.requestWithURL(address))
                }
            }
        },
        update = { webView ->
            if (webView.URL?.absoluteString != url) {
                NSURL.URLWithString(url)?.let { address ->
                    webView.loadRequest(NSURLRequest.requestWithURL(address))
                }
            }
        },
    )
}
