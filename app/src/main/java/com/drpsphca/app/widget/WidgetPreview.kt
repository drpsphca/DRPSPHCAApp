package com.drpsphca.app.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.Preview
import androidx.glance.GlanceTheme
import androidx.glance.preview.ExperimentalGlancePreviewApi

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 210, heightDp = 280)
@Composable
fun BlogPostsWidgetPreview() {
    GlanceTheme {
        BlogPostsWidget().PreviewContentView()
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 210, heightDp = 280)
@Composable
fun NewsletterWidgetPreview() {
    GlanceTheme {
        NewsletterWidget().PreviewContentView()
    }
}
