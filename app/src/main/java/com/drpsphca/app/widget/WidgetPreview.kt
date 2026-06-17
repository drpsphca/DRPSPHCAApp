package com.drpsphca.app.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.Preview
import androidx.glance.GlanceTheme
import androidx.glance.preview.ExperimentalGlancePreviewApi

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 290, heightDp = 210)
@Composable
fun BlogPostsWidgetPreview() {
    GlanceTheme {
        BlogPostsWidget().PreviewContentView()
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 290, heightDp = 210)
@Composable
fun NewsletterWidgetPreview() {
    GlanceTheme {
        NewsletterWidget().PreviewContentView()
    }
}
