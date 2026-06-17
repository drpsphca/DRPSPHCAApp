package com.drpsphca.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.text.Html
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drpsphca.app.data.Category
import com.drpsphca.app.data.Post
import com.drpsphca.app.data.Rendered
import com.drpsphca.app.data.WordPressApi
import com.drpsphca.app.data.WordPressClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.Locale

@Immutable
sealed interface PostUiState {
    @Immutable
    data class Success(val posts: List<PostItemUiModel>, val hasMore: Boolean) : PostUiState
    @Immutable
    data class PostSuccess(val post: PostDetailUiModel) : PostUiState
    @Immutable
    data class Error(val message: String) : PostUiState
    object Loading : PostUiState
    object Idle : PostUiState
}

@Immutable
sealed interface NewsletterUiState {
    @Immutable
    data class Success(val newsletters: List<PostItemUiModel>) : NewsletterUiState
    @Immutable
    data class Error(val message: String) : NewsletterUiState
    object Loading : NewsletterUiState
    object Idle : NewsletterUiState
}

@Immutable
data class PostItemUiModel(
    val id: Int,
    val formattedDate: String,
    val plainTitle: String,
    val plainExcerpt: String,
    val imageUrl: String?,
    val tags: List<String>,
    val localImageUrl: String? = null
)

@Immutable
data class PostDetailUiModel(
    val id: Int,
    val formattedDate: String,
    val plainTitle: String,
    val plainExcerpt: String,
    val content: String,
    val imageUrl: String?,
    val tags: List<String>,
    val link: String,
    val localImageUrl: String? = null
) {
    fun toItemUiModel(): PostItemUiModel {
        return PostItemUiModel(
            id = id,
            formattedDate = formattedDate,
            plainTitle = plainTitle,
            plainExcerpt = plainExcerpt,
            imageUrl = imageUrl,
            tags = tags,
            localImageUrl = localImageUrl
        )
    }
}

class WordPressViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("wp_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _newsletterUiState = MutableStateFlow<NewsletterUiState>(NewsletterUiState.Loading)
    val newsletterUiState: StateFlow<NewsletterUiState> = _newsletterUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _blogPosts = MutableStateFlow<List<PostItemUiModel>>(emptyList())
    private val _currentPage = MutableStateFlow(1)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    
    private val _bookmarkedPosts = MutableStateFlow<List<PostItemUiModel>>(emptyList())
    val bookmarkedPosts: StateFlow<List<PostItemUiModel>> = _bookmarkedPosts.asStateFlow()
    
    private val _downloadedPosts = MutableStateFlow<List<PostDetailUiModel>>(emptyList())
    val downloadedPosts: StateFlow<List<PostItemUiModel>> = _downloadedPosts
        .map { list -> list.map { it.toItemUiModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadingPostIds = MutableStateFlow<Set<Int>>(emptySet())
    val downloadingPostIds: StateFlow<Set<Int>> = _downloadingPostIds.asStateFlow()
    
    private val _bookmarkingPostIds = MutableStateFlow<Set<Int>>(emptySet())
    val bookmarkingPostIds: StateFlow<Set<Int>> = _bookmarkingPostIds.asStateFlow()

    enum class DarkModeConfig { ON, OFF, AUTO }
    private val _darkModeConfig = MutableStateFlow(
        DarkModeConfig.valueOf(prefs.getString("dark_mode_config", DarkModeConfig.AUTO.name) ?: DarkModeConfig.AUTO.name)
    )
    val darkModeConfig: StateFlow<DarkModeConfig> = _darkModeConfig.asStateFlow()

    // Keep this for legacy or simple checks if needed, but UI will mostly use darkModeConfig
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _navigateToPostId = MutableStateFlow<String?>(null)
    val navigateToPostId: StateFlow<String?> = _navigateToPostId.asStateFlow()

    private val wordPressApi = WordPressClient.api
    private val mutex = Mutex()

    init {
        loadBookmarks()
        loadDownloads()
        fetchCategories()
    }

    private fun loadBookmarks() {
        val json = prefs.getString("bookmarks", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<PostItemUiModel>>() {}.type
                val bookmarks: List<PostItemUiModel> = gson.fromJson(json, type)
                _bookmarkedPosts.value = bookmarks
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error loading bookmarks", e)
            }
        }
    }

    private fun saveBookmarks(bookmarks: List<PostItemUiModel>) {
        val json = gson.toJson(bookmarks)
        prefs.edit().putString("bookmarks", json).apply()
    }

    private fun loadDownloads() {
        val json = prefs.getString("downloads", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<PostDetailUiModel>>() {}.type
                val downloads: List<PostDetailUiModel> = gson.fromJson(json, type)
                _downloadedPosts.value = downloads
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error loading downloads", e)
            }
        }
    }

    private fun saveDownloads(downloads: List<PostDetailUiModel>) {
        val json = gson.toJson(downloads)
        prefs.edit().putString("downloads", json).apply()
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val categories = wordPressApi.getCategories()
                    _categories.value = categories

                    doFetchPosts(isRefreshing = false, page = 1, perPage = 20, forHome = true)
                    doFetchNewsletters(perPage = 5)
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error fetching categories", e)
                    _uiState.update { PostUiState.Error(e.message ?: "Unknown error") }
                    _newsletterUiState.update { NewsletterUiState.Error(e.message ?: "Unknown error") }
                }
            }
        }
    }

    fun fetchPosts(isRefreshing: Boolean, page: Int = _currentPage.value, perPage: Int = 20, forHome: Boolean = false, searchQuery: String? = null, tag: String? = null) {
        viewModelScope.launch {
            mutex.withLock {
                doFetchPosts(isRefreshing, page, perPage, forHome, searchQuery, tag)
            }
        }
    }

    private suspend fun doFetchPosts(isRefreshing: Boolean, page: Int, perPage: Int, forHome: Boolean, searchQuery: String? = null, tag: String? = null) {
        if (isRefreshing) {
            _isRefreshing.value = true
            _currentPage.value = 1
        } else if (page == 1) {
            _uiState.update { PostUiState.Loading }
        }

        try {
            val blogCategory = _categories.value.find { it.slug == "blog" }
            if (blogCategory == null && searchQuery == null && tag == null) {
                _uiState.update { PostUiState.Error("Blog category not found") }
                return
            }

            var tagIds: String? = null
            if (tag != null) {
                val tagsList = wordPressApi.getTags(search = tag)
                val matchingTag = tagsList.find { it.name.equals(tag, ignoreCase = true) }
                tagIds = matchingTag?.id?.toString()
                if (tagIds == null) {
                    _uiState.update { PostUiState.Success(emptyList(), false) }
                    return
                }
            }

            val posts = wordPressApi.getPosts(
                page = page, 
                perPage = perPage, 
                categories = blogCategory?.id,
                search = searchQuery,
                tags = tagIds
            )

            var uiModels = posts.map { it.toUiModel() }
            
            // Filter to strictly match title if searchQuery is present
            if (searchQuery != null) {
                uiModels = uiModels.filter { it.plainTitle.contains(searchQuery, ignoreCase = true) }
            }

            val currentPosts = if (page == 1 || forHome) emptyList() else _blogPosts.value
            val newPosts = currentPosts + uiModels
            _blogPosts.value = newPosts
            _currentPage.value = page

            val hasMore = posts.size == perPage
            _uiState.update { PostUiState.Success(newPosts, hasMore) }
        } catch (e: Exception) {
            Log.e("WordPressViewModel", "Error fetching posts", e)
            _uiState.update { PostUiState.Error(e.message ?: "Unknown error") }
        } finally {
            if (isRefreshing) {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleBookmark(post: PostItemUiModel, onComplete: (Boolean) -> Unit) {
        val isCurrentlyBookmarked = _bookmarkedPosts.value.any { it.id == post.id }
        if (isCurrentlyBookmarked) {
            viewModelScope.launch {
                _bookmarkingPostIds.update { it + post.id }
                kotlinx.coroutines.delay(1000) // Simulate processing
                _bookmarkedPosts.update { list -> 
                    val newList = list.filter { it.id != post.id }
                    saveBookmarks(newList)
                    newList
                }
                _bookmarkingPostIds.update { it - post.id }
                onComplete(false)
            }
        } else {
            viewModelScope.launch {
                _bookmarkingPostIds.update { it + post.id }
                kotlinx.coroutines.delay(1000) // Simulate processing
                _bookmarkedPosts.update { list -> 
                    val newList = list + post
                    saveBookmarks(newList)
                    newList
                }
                _bookmarkingPostIds.update { it - post.id }
                onComplete(true)
            }
        }
    }
    
    fun toggleDownload(post: PostItemUiModel, onComplete: (Boolean) -> Unit) {
        val downloadedPost = _downloadedPosts.value.find { it.id == post.id }
        if (downloadedPost != null) {
            viewModelScope.launch {
                _downloadingPostIds.update { it + post.id }
                deletePostFiles(downloadedPost)
                _downloadedPosts.update { list -> 
                    val newList = list.filter { it.id != post.id }
                    saveDownloads(newList)
                    newList
                }
                _downloadingPostIds.update { it - post.id }
                onComplete(false)
            }
        } else {
            viewModelScope.launch {
                _downloadingPostIds.update { it + post.id }
                try {
                    var detail = wordPressApi.getPost(post.id).toDetailUiModel()
                    
                    // Download featured image
                    detail.imageUrl?.let { url ->
                        val localPath = downloadAndSaveImage(url, "post_${post.id}_featured.jpg")
                        if (localPath != null) {
                            detail = detail.copy(localImageUrl = localPath)
                        }
                    }
                    
                    // Download content images
                    val updatedContent = processContentImages(detail.content, post.id)
                    detail = detail.copy(content = updatedContent)

                    _downloadedPosts.update { list -> 
                        val newList = list + detail
                        saveDownloads(newList)
                        newList
                    }
                    onComplete(true)
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error downloading post", e)
                    onComplete(false)
                } finally {
                    _downloadingPostIds.update { it - post.id }
                }
            }
        }
    }

    fun toggleDownload(post: PostDetailUiModel, onComplete: (Boolean) -> Unit) {
        val downloadedPost = _downloadedPosts.value.find { it.id == post.id }
        if (downloadedPost != null) {
            viewModelScope.launch {
                _downloadingPostIds.update { it + post.id }
                deletePostFiles(downloadedPost)
                _downloadedPosts.update { list -> 
                    val newList = list.filter { it.id != post.id }
                    saveDownloads(newList)
                    newList
                }
                _downloadingPostIds.update { it - post.id }
                onComplete(false)
            }
        } else {
            viewModelScope.launch {
                _downloadingPostIds.update { it + post.id }
                try {
                    var detail = post
                    
                    // Download featured image
                    detail.imageUrl?.let { url ->
                        val localPath = downloadAndSaveImage(url, "post_${post.id}_featured.jpg")
                        if (localPath != null) {
                            detail = detail.copy(localImageUrl = localPath)
                        }
                    }
                    
                    // Download content images
                    val updatedContent = processContentImages(detail.content, post.id)
                    detail = detail.copy(content = updatedContent)

                    _downloadedPosts.update { list -> 
                        val newList = list + detail
                        saveDownloads(newList)
                        newList
                    }
                    onComplete(true)
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error downloading post", e)
                    onComplete(false)
                } finally {
                    _downloadingPostIds.update { it - post.id }
                }
            }
        }
    }

    private suspend fun downloadAndSaveImage(url: String, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.doInput = true
                connection.connect()
                val input = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(input) ?: return@withContext null
                
                val file = File(getApplication<Application>().filesDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                file.absolutePath
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error saving image: $url", e)
                null
            }
        }
    }

    private suspend fun processContentImages(content: String, postId: Int): String {
        var updatedContent = content
        val imgRegex = """<img[^>]+src=["']([^"']+)["'][^>]*>""".toRegex()
        val matches = imgRegex.findAll(content)
        
        matches.forEachIndexed { index, matchResult ->
            val url = matchResult.groups[1]?.value
            if (url != null && url.startsWith("http")) {
                val extension = url.substringAfterLast(".", "jpg").substringBefore("?")
                val fileName = "post_${postId}_content_${index}.$extension"
                val localPath = downloadAndSaveImage(url, fileName)
                if (localPath != null) {
                    updatedContent = updatedContent.replace(url, "file://$localPath")
                }
            }
        }
        return updatedContent
    }

    private fun deletePostFiles(post: PostDetailUiModel) {
        viewModelScope.launch(Dispatchers.IO) {
            post.localImageUrl?.let { File(it).delete() }
            val imgRegex = """file://([^"'\s>]+)""".toRegex()
            imgRegex.findAll(post.content).forEach { match ->
                val path = match.groups[1]?.value
                if (path != null) {
                    File(path).delete()
                }
            }
        }
    }

    fun toggleDarkMode() {
        val nextConfig = when (_darkModeConfig.value) {
            DarkModeConfig.AUTO -> DarkModeConfig.ON
            DarkModeConfig.ON -> DarkModeConfig.OFF
            DarkModeConfig.OFF -> DarkModeConfig.AUTO
        }
        _darkModeConfig.value = nextConfig
        prefs.edit().putString("dark_mode_config", nextConfig.name).apply()
        
        // Update the legacy boolean for compatibility where used
        val isDark = when (nextConfig) {
            DarkModeConfig.ON -> true
            DarkModeConfig.OFF -> false
            DarkModeConfig.AUTO -> false // Default fallback
        }
        _isDarkMode.value = isDark
        prefs.edit().putBoolean("dark_mode", isDark).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun toggleNotifications() {
        val nextValue = !_notificationsEnabled.value
        setNotificationsEnabled(nextValue)
    }

    fun onNotificationPostIdHandled() {
        _navigateToPostId.value = null
    }

    fun handleNotificationIntent(intent: Intent?) {
        val postId = intent?.getStringExtra("post_id") ?: intent?.data?.getQueryParameter("id")
        if (postId != null) {
            _navigateToPostId.value = postId
            intent?.removeExtra("post_id")
        }
    }

    fun fetchNextPage() {
        // ...
        viewModelScope.launch {
            mutex.withLock {
                val uiStateValue = _uiState.value
                if (uiStateValue is PostUiState.Success && uiStateValue.hasMore) {
                    val nextPage = _currentPage.value + 1
                    doFetchPosts(isRefreshing = false, page = nextPage, perPage = 20, forHome = false)
                }
            }
        }
    }

    fun fetchNewsletters(perPage: Int = 5) {
        viewModelScope.launch {
            mutex.withLock {
                doFetchNewsletters(perPage)
            }
        }
    }

    private suspend fun doFetchNewsletters(perPage: Int) {
        _newsletterUiState.update { NewsletterUiState.Loading }
        try {
            val newsletterCategory = _categories.value.find { it.slug == "newsletter" }

            if (newsletterCategory == null) {
                _newsletterUiState.update { NewsletterUiState.Error("Newsletter category not found. Assuming newsletters are standard posts under a 'newsletter' category slug.") }
                return
            }
            val newsletters = wordPressApi.getPosts(categories = newsletterCategory.id, perPage = perPage)
            _newsletterUiState.update { NewsletterUiState.Success(newsletters.map { it.toUiModel() }) }
        } catch (e: Exception) {
            Log.e("WordPressViewModel", "Error fetching newsletters", e)
            _newsletterUiState.update { NewsletterUiState.Error(e.message ?: "Unknown error") }
        }
    }

    fun fetchPost(id: Int) {
        viewModelScope.launch {
            mutex.withLock {
                _uiState.update { PostUiState.Loading }
                try {
                    // Check if already downloaded
                    val downloadedPost = _downloadedPosts.value.find { it.id == id }
                    if (downloadedPost != null) {
                        _uiState.update { PostUiState.PostSuccess(downloadedPost) }
                        return@withLock
                    }

                    val post = wordPressApi.getPost(id)
                    _uiState.update { PostUiState.PostSuccess(post.toDetailUiModel()) }
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error fetching post", e)
                    _uiState.update { PostUiState.Error(e.message ?: "Unknown error") }
                }
            }
        }
    }

    private fun Post.toUiModel(): PostItemUiModel {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(date)?.let {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: date
        val plainTitle = Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val plainExcerpt = Html.fromHtml(excerpt.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val imageUrl = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
        val tags = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "post_tag" } }?.map {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: emptyList()

        return PostItemUiModel(
            id = id,
            formattedDate = formattedDate,
            plainTitle = plainTitle,
            plainExcerpt = plainExcerpt,
            imageUrl = imageUrl,
            tags = tags,
            localImageUrl = null
        )
    }

    private fun Post.toDetailUiModel(): PostDetailUiModel {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(date)?.let {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: date
        val plainTitle = Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val plainExcerpt = Html.fromHtml(excerpt.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val imageUrl = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
        val tags = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "post_tag" } }?.map {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: emptyList()
        val contentAsString = content.rendered

        return PostDetailUiModel(
            id = id,
            formattedDate = formattedDate,
            plainTitle = plainTitle,
            plainExcerpt = plainExcerpt,
            content = contentAsString,
            imageUrl = imageUrl,
            tags = tags,
            link = link,
            localImageUrl = null
        )
    }
}
