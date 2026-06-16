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
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.drpsphca.app.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BlogPostsWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    companion object {
        val KEY_POSTS_DATA = stringPreferencesKey("posts_data")
        val KEY_INDEX = intPreferencesKey("index")
        val KEY_TOTAL = intPreferencesKey("total")
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                BlogWidgetContent()
            }
        }
    }
                        
    @Composable
    internal fun BlogWidgetContent() {
        val prefs = currentState<Preferences>()
        val postsData = prefs[KEY_POSTS_DATA]
        val index = prefs[KEY_INDEX] ?: 0
        val total = prefs[KEY_TOTAL] ?: 0

        if (postsData != null) {
            var currentTitle = "DRPS PHCA Blog Posts"
            var currentExcerpt = "Powered by DRPSPHCA.com"
            var postId: Int? = null
            var imagePath: String? = null
            var tags: List<String> = emptyList()

            try {
                val gson = Gson()
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val posts: List<Map<String, Any>> = gson.fromJson(postsData, type)
                if (posts.isNotEmpty() && index < posts.size) {
                    val post = posts[index]
                    currentTitle = post["title"] as? String ?: currentTitle
                    currentExcerpt = post["excerpt"] as? String ?: currentExcerpt
                    postId = (post["id"] as? Double)?.toInt() ?: (post["id"] as? Int)
                    imagePath = post["imageUrl"] as? String
                    tags = (post["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                }
            } catch (e: Exception) {
                // fallback
            }
            ActualContentView(currentTitle, currentExcerpt, postId, imagePath, tags, index, total)
        } else {
            PreviewContentView()
        }
    }
            
    @Composable
    private fun ActualContentView(title: String, excerpt: String, postId: Int?, imagePath: String?, tags: List<String>, index: Int, total: Int) {
        val clickAction = if (postId != null) {
            actionStartActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("drpsphca://post?id=$postId")
                )
            )
        } else {
            null
        }
        WidgetLayout(title, excerpt, imagePath, tags, index, total, clickAction)
    }

    
    @Composable
    internal fun PreviewContentView() {
        WidgetLayout(
            title = "DRPS PHCA Blog Posts",
            excerpt = "Powered by DRPSPHCA.com",
            imagePath = null,
            tags = emptyList(),
            index = 0,
            total = 1
        )
    }
    
    @Composable
    internal fun WidgetLayout(
        title: String,
        excerpt: String,
        imagePath: String?,
        tags: List<String>,
        index: Int,
        total: Int,
        clickAction: androidx.glance.action.Action? = null
    ) {
        val context = LocalContext.current
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density
        val widgetWidthPx = (size.width.value * density).toInt()
        val horizontalPaddingPx = (24 * density).toInt() // 12dp * 2
        val maxWidth = widgetWidthPx - horizontalPaddingPx
        
        // Use White text as requested
        val textColor = Color.White

        val titleBitmap = WidgetUtils.textToBitmap(
            context = context,
            text = title,
            fontSizeSp = 24f, // Enlarged
            color = textColor,
            fontResId = R.font.phca_gilroy_medium,
            maxWidthPx = maxWidth
        )

        val excerptBitmap = WidgetUtils.textToBitmap(
            context = context,
            text = excerpt,
            fontSizeSp = 16f, // Enlarged
            color = textColor,
            fontResId = R.font.phca_gilroy_regular,
            maxWidthPx = maxWidth
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
            // Top part (Image Area) - 50%
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

                // Logo on top left - Truly top-left
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.logo),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.width(140.dp)
                    )
                }
                // Navigation buttons: Left, Right, Refresh
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Button
                    Image(
                        provider = ImageProvider(R.drawable.phcaapp_left),
                        contentDescription = "Previous",
                        modifier = GlanceModifier
                            .size(36.dp)
                            .padding(4.dp)
                            .let { if (index > 0) it.clickable(actionRunCallback<PrevPostActionCallback>()) else it },
                        colorFilter = ColorFilter.tint(if (index > 0) WidgetColors.white else WidgetColors.whiteDisabled)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    // Right Button
                    Image(
                        provider = ImageProvider(R.drawable.phcaapp_right),
                        contentDescription = "Next",
                        modifier = GlanceModifier
                            .size(36.dp)
                            .padding(4.dp)
                            .let { if (index < total - 1) it.clickable(actionRunCallback<NextPostActionCallback>()) else it },
                        colorFilter = ColorFilter.tint(if (index < total - 1) WidgetColors.white else WidgetColors.whiteDisabled)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    // Refresh Button
                    Image(
                        provider = ImageProvider(R.drawable.phcaapp_refresh),
                        contentDescription = "Refresh",
                        modifier = GlanceModifier
                            .size(36.dp)
                            .padding(4.dp)
                            .clickable(actionRunCallback<BlogRefreshCallback>()),
                        colorFilter = ColorFilter.tint(WidgetColors.white)
                    )
                }
            }
            // Bottom part (Title + Excerpt + Tags) - 50%
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(Color(0xFF000000).copy(alpha = 0.6f)) // Darken to show white text
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Image(
                    provider = ImageProvider(titleBitmap),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxWidth()
                )
                Spacer(GlanceModifier.height(4.dp))
                Image(
                    provider = ImageProvider(excerptBitmap),
                    contentDescription = excerpt,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }
}

class BlogRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
    }
}

class NextPostActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val index = prefs[BlogPostsWidget.KEY_INDEX] ?: 0
            val total = prefs[BlogPostsWidget.KEY_TOTAL] ?: 1
            if (index < total - 1) {
                prefs.toMutablePreferences().apply {
                    this[BlogPostsWidget.KEY_INDEX] = index + 1
                }
            } else {
                prefs
            }
        }
        BlogPostsWidget().updateAll(context)
    }
}

class PrevPostActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val index = prefs[BlogPostsWidget.KEY_INDEX] ?: 0
            if (index > 0) {
                prefs.toMutablePreferences().apply {
                    this[BlogPostsWidget.KEY_INDEX] = index - 1
                }
            } else {
                prefs
            }
        }
        BlogPostsWidget().updateAll(context)
    }
}

class BlogPostsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BlogPostsWidget()
}