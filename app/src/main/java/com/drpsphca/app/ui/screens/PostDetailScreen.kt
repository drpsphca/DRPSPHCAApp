package com.drpsphca.app.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.drpsphca.app.ui.viewmodel.PostDetailUiModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PostDetailScreen(
    post: PostDetailUiModel, 
    isDarkMode: Boolean, 
    isOffline: Boolean = false,
    onTagClick: (String) -> Unit = {}
) {
    val isDarkTheme = isDarkMode
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Handle back button for fullscreen video
    BackHandler(enabled = customView != null) {
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                @font-face {
                    font-family: 'Gilroy';
                    src: url('file:///android_res/font/phca_gilroy_regular.otf');
                    font-weight: normal;
                }
                @font-face {
                    font-family: 'Gilroy';
                    src: url('file:///android_res/font/phca_gilroy_medium.otf');
                    font-weight: 500;
                }
                @font-face {
                    font-family: 'Gilroy';
                    src: url('file:///android_res/font/phca_gilroy_bold.otf');
                    font-weight: bold;
                }
                @font-face {
                    font-family: 'NotoSerif';
                    src: url('file:///android_res/font/google_noto_serif.ttf');
                    font-weight: normal;
                    font-style: normal;
                }
                @font-face {
                    font-family: 'NotoSerif';
                    src: url('file:///android_res/font/google_noto_serif_medium.ttf');
                    font-weight: 500;
                    font-style: normal;
                }
                @font-face {
                    font-family: 'NotoSerif';
                    src: url('file:///android_res/font/google_noto_serif_bold.ttf');
                    font-weight: bold;
                    font-style: normal;
                }
                @font-face {
                    font-family: 'NotoSerif';
                    src: url('file:///android_res/font/google_noto_serif_italic.ttf');
                    font-weight: normal;
                    font-style: italic;
                }
                @font-face {
                    font-family: 'NotoSerif';
                    src: url('file:///android_res/font/google_noto_serif_medium_italic.ttf');
                    font-weight: 500;
                    font-style: italic;
                }
                @font-face {
                    font-family: 'NotoSerif';
                    src: url('file:///android_res/font/google_noto_serif_bold_italic.ttf');
                    font-weight: bold;
                    font-style: italic;
                }

                * {
                    box-sizing: border-box;
                    max-width: 100%;
                }
                body {
                    font-family: 'NotoSerif', serif;
                    color: ${if (isDarkTheme) "#FFFFFF" else "#000000"};
                    background-color: ${if (isDarkTheme) "#121212" else "#FFFFFF"};
                    line-height: 1.6;
                    padding: 16px;
                    margin: 0;
                    word-wrap: break-word;
                    -webkit-text-size-adjust: 100%;
                }
                
                .header-container {
                    margin-bottom: 24px;
                }
                
                .featured-image-container {
                    position: relative;
                    width: 100%;
                    margin-bottom: 16px;
                }
                
                .featured-image {
                    width: 100% !important;
                    height: auto;
                    border-radius: 8px;
                    margin: 0 !important;
                    display: block;
                }
                
                .category-badge {
                    position: absolute;
                    top: 12px;
                    left: 12px;
                    background-color: #F1F1F1;
                    color: #025CA1;
                    padding: 6px 12px;
                    border-radius: 4px;
                    font-family: 'Gilroy', sans-serif;
                    font-weight: bold;
                    font-size: 0.8rem;
                    text-transform: uppercase;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                
                .tags-container {
                    margin-bottom: 8px;
                    display: flex;
                    flex-wrap: wrap;
                    gap: 8px;
                }
                
                .tag {
                    background-color: #111111;
                    color: #FFFFFF;
                    padding: 4px 10px;
                    border-radius: 4px;
                    font-family: 'Gilroy', sans-serif;
                    font-weight: bold;
                    font-size: 0.75rem;
                    cursor: pointer;
                    display: inline-block;
                }
                
                .publish-date {
                    font-family: 'Gilroy', sans-serif;
                    font-weight: 500;
                    font-size: 0.75rem;
                    color: ${if (isDarkTheme) "#AAAAAA" else "#666666"};
                    margin-top: 8px;
                    text-transform: uppercase;
                }
                
                h1 {
                    font-family: 'Gilroy', sans-serif;
                    font-weight: bold;
                    font-size: 1.5rem;
                    line-height: 1.2;
                    margin-bottom: 12px;
                    margin-top: 0;
                }
                /* Images scaling */
                img, figure, .wp-block-image {
                    height: auto !important;
                    display: block;
                    margin: 16px auto !important;
                    max-width: 100% !important;
                }
                /* Force images to fill width if they are meant to be large */
                .wp-block-image img, 
                .wp-block-image.size-full img, 
                .wp-block-image.aligncenter img,
                img.size-full, img.aligncenter {
                    width: 100% !important;
                    object-fit: contain;
                }
                
                img {
                    border-radius: 8px;
                }
                
                img.emoji {
                    width: 1em !important;
                    display: inline !important;
                    margin: 0 !important;
                    border-radius: 0 !important;
                }
                
                /* Video containers */
                .video-wrapper {
                    position: relative;
                    width: 100%;
                    margin: 16px 0;
                    background: #000;
                    border-radius: 8px;
                    overflow: hidden;
                    clear: both;
                }
                
                /* Default 16:9 Aspect Ratio */
                .aspect-ratio-16-9 {
                    padding-bottom: 56.25%; /* 16:9 */
                    height: 0;
                }
                
                /* Vertical 9:16 Aspect Ratio */
                .aspect-ratio-9-16 {
                    padding-bottom: 177.77%; /* 9:16 */
                    height: 0;
                }
                
                .video-wrapper iframe,
                .video-wrapper object,
                .video-wrapper embed {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100% !important;
                    height: 100% !important;
                    border: 0;
                }
                
                /* WordPress block embeds alignment fix */
                .wp-block-embed, .wp-block-embed-youtube, .wp-block-embed-facebook {
                    margin: 16px 0 !important;
                    width: 100% !important;
                    padding: 0 !important;
                }
                
                /* Remove any fixed widths from containers */
                [style*="width"] {
                    max-width: 100% !important;
                }
                
                .offline-message {
                    background: ${if (isDarkTheme) "#333333" else "#f0f0f0"};
                    color: ${if (isDarkTheme) "#AAAAAA" else "#666666"};
                    padding: 20px;
                    border-radius: 8px;
                    text-align: center;
                    margin: 16px 0;
                    border: 1px dashed ${if (isDarkTheme) "#444444" else "#cccccc"};
                    font-style: italic;
                }
            </style>
        </head>
        <body>
            <div class="header-container">
                ${if (post.imageUrl != null || post.localImageUrl != null) """
                <div class="featured-image-container">
                    <img src="${if (post.localImageUrl != null) "file://" + post.localImageUrl else post.imageUrl}" class="featured-image">
                    ${if (post.category != null) """<div class="category-badge">${post.category}</div>""" else ""}
                </div>
                """ else ""}
                <h1>${post.plainTitle}</h1>
                ${if (post.tags.isNotEmpty()) """
                <div class="tags-container">
                    ${post.tags.joinToString("") { """<span class="tag" onclick="window.location.href='app://tag/$it'">$it</span>""" }}
                </div>
                """ else ""}
                <div class="publish-date">PUBLISHED ${post.formattedDate.uppercase()}</div>
            </div>

            <div id="content-container">
                ${post.content}
            </div>
            
            <script>
                (function() {
                    function wrapIframes() {
                        var iframes = document.querySelectorAll('iframe');
                        iframes.forEach(function(iframe) {
                            var src = iframe.src || "";
                            
                            // Check if it's a video/embed we want to handle
                            var isEmbed = src.includes('youtube') || 
                                          src.includes('facebook') || 
                                          src.includes('vimeo') || 
                                          src.includes('reels') || 
                                          src.includes('shorts') || 
                                          src.includes('tiktok') ||
                                          src.includes('instagram') ||
                                          src.includes('twitter') ||
                                          src.includes('threads');
                                          
                            if (!isEmbed) return;
                            
                            if (${isOffline}) {
                                var msg = document.createElement('div');
                                msg.className = 'offline-message';
                                msg.innerText = 'Embedded content is not available on offline reading';
                                iframe.parentNode.replaceChild(msg, iframe);
                                return;
                            }
                            
                            // Check if already wrapped
                            var parent = iframe.parentElement;
                            if (parent.classList.contains('video-wrapper')) {
                                // Update aspect ratio if needed
                                updateRatio(parent, iframe, src);
                                return;
                            }
                            
                            var wrapper = document.createElement('div');
                            wrapper.className = 'video-wrapper';
                            
                            updateRatio(wrapper, iframe, src);
                            
                            iframe.parentNode.insertBefore(wrapper, iframe);
                            wrapper.appendChild(iframe);
                        });
                    }
                    
                    function handleSocialPlaceholders() {
                        if (!${isOffline}) return;
                        
                        // Handle blockquotes that WordPress/Social platforms use as placeholders before JS loads
                        var selector = 'blockquote.instagram-media, blockquote.twitter-tweet, blockquote.tiktok-embed, .wp-block-embed';
                        var placeholders = document.querySelectorAll(selector);
                        
                        placeholders.forEach(function(el) {
                            var msg = document.createElement('div');
                            msg.className = 'offline-message';
                            msg.innerText = 'Embedded content is not available on offline reading';
                            el.parentNode.replaceChild(msg, el);
                        });
                    }
                    
                    function updateRatio(wrapper, iframe, src) {
                        var width = parseInt(iframe.width) || 16;
                        var height = parseInt(iframe.height) || 9;
                        var isVertical = src.includes('reels') || 
                                         src.includes('shorts') || 
                                         src.includes('tiktok') ||
                                         (height / width > 1.2);
                        
                        wrapper.classList.remove('aspect-ratio-16-9', 'aspect-ratio-9-16');
                        if (isVertical) {
                            wrapper.classList.add('aspect-ratio-9-16');
                        } else {
                            wrapper.classList.add('aspect-ratio-16-9');
                        }
                    }
                    
                    // Initial run
                    wrapIframes();
                    handleSocialPlaceholders();
                    
                    // Run again after images/other content might have loaded
                    window.addEventListener('load', function() {
                        wrapIframes();
                        handleSocialPlaceholders();
                    });
                    
                    // Periodic check for dynamic content
                    var count = 0;
                    var interval = setInterval(function() {
                        wrapIframes();
                        handleSocialPlaceholders();
                        if (++count > 5) clearInterval(interval);
                    }, 1000);
                })();
            </script>
        </body>
        </html>
    """

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                        allowContentAccess = true
                        
                        // Use false for more predictable mobile rendering of our custom HTML
                        useWideViewPort = false
                        loadWithOverviewMode = false
                        
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString()
                            if (url?.startsWith("app://tag/") == true) {
                                val tagName = Uri.decode(url.substringAfter("app://tag/"))
                                onTagClick(tagName)
                                return true
                            }
                            return false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customView = null
                            customViewCallback = null
                        }
                    }
                }
            },
            update = { webView ->
                // Using null base URL or "file:///" can help with local file access and avoid mixed content issues
                val baseUrl = if (isOffline) "file:///" else "https://drpsphca.com"
                webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "utf-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for fullscreen video
        customView?.let { view ->
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        addView(view, FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
