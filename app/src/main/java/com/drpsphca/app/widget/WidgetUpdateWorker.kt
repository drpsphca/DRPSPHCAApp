package com.drpsphca.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drpsphca.app.data.WordPressClient
import com.google.gson.Gson
import androidx.core.text.HtmlCompat

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private fun String.cleanHtml(): String {
        return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    override suspend fun doWork(): Result {
        Log.d("WidgetUpdateWorker", "Starting widget update work")
        return try {
            val api = WordPressClient.api
            
            val categories = try {
                api.getCategories(perPage = 100)
            } catch (e: Exception) {
                Log.e("WidgetUpdateWorker", "Failed to fetch categories", e)
                return Result.retry()
            }
            
            val manager = GlanceAppWidgetManager(context)

            // Update Newsletter Widget
            val newsletterCat = categories.find { 
                it.slug.equals("newsletter", ignoreCase = true) || 
                it.name.contains("Newsletter", ignoreCase = true) 
            }
            
            if (newsletterCat != null) {
                val posts = api.getPosts(categories = newsletterCat.id, perPage = 1)
                if (posts.isNotEmpty()) {
                    val post = posts[0]
                    val imageUrl = post.embedded?.featuredMedia?.firstOrNull()?.sourceUrl
                    val localImagePath = imageUrl?.let { 
                        WidgetUtils.downloadAndSaveImage(context, it, "newsletter_featured.jpg")
                    }

                    val glanceIds = manager.getGlanceIds(NewsletterWidget::class.java)
                    glanceIds.forEach { glanceId ->
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                set(NewsletterWidget.KEY_POST_ID, post.id)
                                set(NewsletterWidget.KEY_TITLE, post.title.rendered.cleanHtml())
                                set(NewsletterWidget.KEY_IMAGE_URL, localImagePath ?: "")
                            }
                        }
                    }
                    NewsletterWidget().updateAll(context)
                }
            }

            // Update Blog Posts Widget
            val blogCat = categories.find { 
                it.slug.equals("blog", ignoreCase = true) || 
                it.name.contains("Blog", ignoreCase = true) 
            }
            
            if (blogCat != null) {
                val posts = api.getPosts(categories = blogCat.id, perPage = 10)
                if (posts.isNotEmpty()) {
                    val gson = Gson()
                    val postsData = posts.mapIndexed { i, it -> 
                        val imageUrl = it.embedded?.featuredMedia?.firstOrNull()?.sourceUrl
                        val localPath = imageUrl?.let { url ->
                            WidgetUtils.downloadAndSaveImage(context, url, "blog_featured_$i.jpg")
                        }
                        val tags = it.embedded?.terms?.firstOrNull { terms -> 
                            terms.any { term -> term.taxonomy == "post_tag" } 
                        }?.map { term -> term.name.cleanHtml() } ?: emptyList<String>()

                        mapOf(
                            "id" to it.id,
                            "title" to it.title.rendered.cleanHtml(),
                            "excerpt" to it.excerpt.rendered.cleanHtml(),
                            "imageUrl" to (localPath ?: ""),
                            "tags" to tags
                        )
                    }
                    val postsJson = gson.toJson(postsData)
                    val glanceIds = manager.getGlanceIds(BlogPostsWidget::class.java)
                    glanceIds.forEach { glanceId ->
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                set(BlogPostsWidget.KEY_POSTS_DATA, postsJson)
                                set(BlogPostsWidget.KEY_INDEX, 0)
                                set(BlogPostsWidget.KEY_TOTAL, posts.size)
                            }
                        }
                    }
                    BlogPostsWidget().updateAll(context)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("WidgetUpdateWorker", "Error updating widgets", e)
            Result.retry()
        }
    }
}
