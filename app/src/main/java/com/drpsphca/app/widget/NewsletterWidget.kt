package com.drpsphca.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.color.ColorProvider
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.drpsphca.app.R

class NewsletterWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        val KEY_POST_ID = intPreferencesKey("post_id")
        val KEY_TITLE = stringPreferencesKey("title")
        val KEY_IMAGE_URL = stringPreferencesKey("image_url")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                NewsletterWidgetContent()
            }
        }
    }

    @Composable
    private fun NewsletterWidgetContent() {
        val prefs = currentState<Preferences>()
        val postId = prefs[KEY_POST_ID]
        val title = prefs[KEY_TITLE]
        val localImagePath = prefs[KEY_IMAGE_URL]

        if (postId != null && title != null) {
            ActualContentView(postId, title, localImagePath)
        } else {
            PreviewContentView()
        }
    }

    @Composable
    private fun ActualContentView(postId: Int, title: String, imagePath: String?) {
        val clickAction = actionStartActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("drpsphca://post?id=$postId")
            )
        )
        WidgetLayout(title = title, imagePath = imagePath, clickAction = clickAction)
    }

    @Composable
    private fun PreviewContentView() {
        WidgetLayout(title = "DRPS PHCA Newsletter")
    }

    @Composable
    private fun WidgetLayout(title: String, imagePath: String? = null, clickAction: androidx.glance.action.Action? = null) {
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        
        // Use White text as requested
        val textColor = Color.White
        
        // Calculate max width for text (widget is 4x3, roughly 320dp wide)
        val textWidthPx = (280 * density).toInt()

        val titleBitmap = WidgetUtils.textToBitmap(
            context = context,
            text = title,
            fontSizeSp = 28f, // Larger enough font
            color = textColor,
            fontResId = R.font.phca_gilroy_medium,
            maxWidthPx = textWidthPx
        )

        val featuredImageProvider = if (!imagePath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) ImageProvider(bitmap) else null
            } catch (e: Exception) { null }
        } else null

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.background)
                .let { if (clickAction != null) it.clickable(clickAction) else it }
        ) {
            // Top part (Featured Image area) - 50%
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(WidgetColors.featuredPlaceholder)
            ) {
                // Featured Image background
                featuredImageProvider?.let {
                    Image(
                        provider = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }

                // Logo on top left - Truly top-left corner
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = GlanceModifier
                            .width(200.dp)
                    )
                }
                
                // Refresh button on top right
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.phcaapp_refresh),
                        contentDescription = "Refresh",
                        modifier = GlanceModifier
                            .size(36.dp)
                            .padding(4.dp)
                            .clickable(actionRunCallback<NewsletterRefreshCallback>()),
                        colorFilter = ColorFilter.tint(WidgetColors.white)
                    )
                }
            }
            
            // Bottom part (Title) - 50%
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(if (textColor == Color.White) ColorProvider(day = Color(0xFF000000).copy(alpha = 0.6f), night = Color(0xFF000000).copy(alpha = 0.6f)) else WidgetColors.background)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Image(
                    provider = ImageProvider(titleBitmap),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }
}

class NewsletterRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
    }
}

class NewsletterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsletterWidget()
}
