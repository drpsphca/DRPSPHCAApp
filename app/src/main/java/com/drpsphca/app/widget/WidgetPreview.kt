package com.drpsphca.app.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.Preview
import androidx.glance.GlanceTheme
import androidx.glance.preview.ExperimentalGlancePreviewApi

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
fun BlogPostsWidgetPreview() {
    GlanceTheme {
        BlogPostsWidget().PreviewContentView()
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
fun NewsletterWidgetPreview() {
    GlanceTheme {
        NewsletterWidget().PreviewContentView()
    }
}
