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
import com.google.firebase.messaging.FirebaseMessaging
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
    data class Error(val message: String) : PostUiState
    object Loading : PostUiState
    object Idle : PostUiState
}

@Immutable
sealed interface PostDetailUiState {
    @Immutable
    data class Success(val post: PostDetailUiModel) : PostDetailUiState
    @Immutable
    data class Error(val message: String) : PostDetailUiState
    object Loading : PostDetailUiState
    object Idle : PostDetailUiState
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
    val localImageUrl: String? = null,
    val category: String? = null
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
    val localImageUrl: String? = null,
    val category: String? = null
) {
    fun toItemUiModel(): PostItemUiModel {
        return PostItemUiModel(
            id = id,
            formattedDate = formattedDate,
            plainTitle = plainTitle,
            plainExcerpt = plainExcerpt,
            imageUrl = imageUrl,
            tags = tags,
            localImageUrl = localImageUrl,
            category = category
        )
    }
}

data class NotificationChannelStates(
    val blogEnabled: Boolean = false,
    val newsletterEnabled: Boolean = false,
    val tvalEnabled: Boolean = false,
    val appEnabled: Boolean = false,
    val pbbEnabled: Boolean = true
)

data class NotificationTarget(
    val channel: String?,
    val url: String?,
    val postId: Int?
)

fun extractPostId(url: String?, postIdExtra: String?): Int? {
    if (!postIdExtra.isNullOrEmpty()) {
        postIdExtra.toIntOrNull()?.let { return it }
    }
    if (url.isNullOrEmpty()) return null
    try {
        val uri = android.net.Uri.parse(url)
        val p = uri.getQueryParameter("p") ?: uri.getQueryParameter("id") ?: uri.getQueryParameter("page_id")
        if (p != null) {
            p.toIntOrNull()?.let { return it }
        }
        val segments = uri.pathSegments
        for (segment in segments.reversed()) {
            val num = segment.toIntOrNull()
            if (num != null && num > 0) {
                return num
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
    return null
}

fun extractSlug(url: String?): String? {
    if (url.isNullOrEmpty()) return null
    try {
        val uri = android.net.Uri.parse(url)
        val segments = uri.pathSegments.filter { it.isNotEmpty() }
        val last = segments.lastOrNull()
        if (last != null && last.toIntOrNull() == null) {
            return last
        }
        return segments.getOrNull(segments.size - 2) ?: last
    } catch (e: Exception) {
        return null
    }
}

class WordPressViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("wp_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _detailUiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Idle)
    val detailUiState: StateFlow<PostDetailUiState> = _detailUiState.asStateFlow()

    private val _tvalUiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val tvalUiState: StateFlow<PostUiState> = _tvalUiState.asStateFlow()

    private val _newsletterUiState = MutableStateFlow<NewsletterUiState>(NewsletterUiState.Loading)
    val newsletterUiState: StateFlow<NewsletterUiState> = _newsletterUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _tvalIsRefreshing = MutableStateFlow(false)
    val tvalIsRefreshing: StateFlow<Boolean> = _tvalIsRefreshing.asStateFlow()

    private val _blogPosts = MutableStateFlow<List<PostItemUiModel>>(emptyList())
    private val _tvalPosts = MutableStateFlow<List<PostItemUiModel>>(emptyList())
    private val _currentPage = MutableStateFlow(1)
    private val _tvalCurrentPage = MutableStateFlow(1)

    private val _selectedTvalDate = MutableStateFlow<String?>(null)
    val selectedTvalDate: StateFlow<String?> = _selectedTvalDate.asStateFlow()

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

    enum class BlogSection { BLOGS, BITE_SIZED }
    private val _selectedBlogSection = MutableStateFlow(BlogSection.BLOGS)
    val selectedBlogSection: StateFlow<BlogSection> = _selectedBlogSection.asStateFlow()

    private val _appxUiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val appxUiState: StateFlow<PostUiState> = _appxUiState.asStateFlow()

    private val _appxIsRefreshing = MutableStateFlow(false)
    val appxIsRefreshing: StateFlow<Boolean> = _appxIsRefreshing.asStateFlow()

    private val _appxPosts = MutableStateFlow<List<PostItemUiModel>>(emptyList())
    private val _appxCurrentPage = MutableStateFlow(1)
    private val _appxCategories = MutableStateFlow<List<Category>>(emptyList())

    private val wordPressApi = WordPressClient.api
    private val tvalApi = WordPressClient.tvalApi
    private val appxApi = WordPressClient.appxApi
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

    fun selectBlogSection(section: BlogSection) {
        _selectedBlogSection.value = section
        if (section == BlogSection.BLOGS) {
            if (_blogPosts.value.isEmpty()) {
                fetchPosts(isRefreshing = false, page = 1, perPage = 20)
            }
        } else {
            if (_appxPosts.value.isEmpty()) {
                fetchAppxPosts(isRefreshing = false, page = 1, perPage = 20)
            }
        }
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

                try {
                    val appxCats = appxApi.getCategories()
                    _appxCategories.value = appxCats
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error fetching AppX categories", e)
                }
            }
        }
    }

    fun reloadAllPosts() {
        viewModelScope.launch {
            mutex.withLock {
                doFetchPosts(isRefreshing = true, page = 1, perPage = 20, forHome = false)
                doFetchAppxPosts(isRefreshing = true, page = 1, perPage = 20)
                doFetchTVALPosts(isRefreshing = true, page = 1, perPage = 20, searchQuery = null, date = _selectedTvalDate.value)
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

    fun fetchTVALPosts(isRefreshing: Boolean, page: Int = _tvalCurrentPage.value, perPage: Int = 20, searchQuery: String? = null, date: String? = null) {
        viewModelScope.launch {
            mutex.withLock {
                doFetchTVALPosts(isRefreshing, page, perPage, searchQuery, date)
            }
        }
    }

    private suspend fun doFetchTVALPosts(isRefreshing: Boolean, page: Int, perPage: Int, searchQuery: String?, date: String?) {
        if (isRefreshing) {
            _tvalIsRefreshing.value = true
            _tvalCurrentPage.value = 1
        } else if (page == 1 && _tvalPosts.value.isEmpty()) {
            _tvalUiState.update { PostUiState.Loading }
        }

        try {
            var modifiedAfter: String? = null
            var modifiedBefore: String? = null
            
            if (date != null) {
                // date is in "yyyy-MM-dd"
                modifiedAfter = "${date}T00:00:00"
                modifiedBefore = "${date}T23:59:59"
            }

            val posts = tvalApi.getPosts(
                page = page,
                perPage = perPage,
                search = searchQuery,
                modifiedAfter = modifiedAfter,
                modifiedBefore = modifiedBefore
            )

            var uiModels = posts.map { it.toUiModel(isTval = true) }
            
            if (searchQuery != null) {
                uiModels = uiModels.filter { it.plainTitle.contains(searchQuery, ignoreCase = true) }
            }

            val currentPosts = if (page == 1) emptyList() else _tvalPosts.value
            val newPosts = currentPosts + uiModels
            _tvalPosts.value = newPosts
            _tvalCurrentPage.value = page

            val hasMore = posts.size == perPage
            _tvalUiState.update { PostUiState.Success(newPosts, hasMore) }
        } catch (e: Exception) {
            Log.e("WordPressViewModel", "Error fetching TVAL posts", e)
            _tvalUiState.update { PostUiState.Error(e.message ?: "Unknown error") }
        } finally {
            if (isRefreshing) {
                _tvalIsRefreshing.value = false
            }
        }
    }

    fun selectTvalDate(date: String?) {
        _selectedTvalDate.value = date
        fetchTVALPosts(isRefreshing = true, page = 1, date = date)
    }

    fun getTvalDateRange(): List<Pair<String, String>> {
        val dates = mutableListOf<Pair<String, String>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        
        for (i in 0 until 35) {
            val dateStr = sdf.format(cal.time)
            val displayStr = displaySdf.format(cal.time)
            dates.add(dateStr to displayStr)
            cal.add(java.util.Calendar.DATE, -1)
        }
        return dates
    }

    fun fetchTvalNextPage() {
        viewModelScope.launch {
            mutex.withLock {
                val uiStateValue = _tvalUiState.value
                if (uiStateValue is PostUiState.Success && uiStateValue.hasMore) {
                    val nextPage = _tvalCurrentPage.value + 1
                    doFetchTVALPosts(isRefreshing = false, page = nextPage, perPage = 20, searchQuery = null, date = _selectedTvalDate.value)
                }
            }
        }
    }

    private suspend fun doFetchPosts(isRefreshing: Boolean, page: Int, perPage: Int, forHome: Boolean, searchQuery: String? = null, tag: String? = null) {
        if (isRefreshing) {
            _isRefreshing.value = true
            _currentPage.value = 1
        } else if (page == 1 && _blogPosts.value.isEmpty()) {
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
        val imgTagRegex = """<img[^>]+>""".toRegex()
        val srcRegex = """src=["']([^"']+)["']""".toRegex()
        val srcsetRegex = """srcset=["']([^"']+)["']""".toRegex()

        val matches = imgTagRegex.findAll(content).toList()
        
        // Process from end to beginning to avoid index issues if we were changing length 
        // (though we are doing string replacement here)
        for (i in matches.indices) {
            val imgMatch = matches[i]
            val fullTag = imgMatch.value
            var updatedTag = fullTag
            
            val srcMatch = srcRegex.find(fullTag)
            val srcUrl = srcMatch?.groups?.get(1)?.value
            
            if (srcUrl != null && srcUrl.startsWith("http")) {
                val extension = srcUrl.substringAfterLast(".", "jpg").substringBefore("?")
                val fileName = "post_${postId}_content_${i}.$extension"
                val localPath = downloadAndSaveImage(srcUrl, fileName)
                if (localPath != null) {
                    updatedTag = updatedTag.replace(srcUrl, "file://$localPath")
                }
            }
            
            // Remove srcset to force the browser to use the localized src
            updatedTag = updatedTag.replace(srcsetRegex, "")
            
            // Replace only this specific occurrence
            updatedContent = updatedContent.replaceFirst(fullTag, updatedTag)
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

    private val channelPrefs = application.getSharedPreferences("notification_channels", Context.MODE_PRIVATE)

    private val _channelStates = MutableStateFlow(
        NotificationChannelStates(
            blogEnabled = channelPrefs.getBoolean("blog", false),
            newsletterEnabled = channelPrefs.getBoolean("newsletter", false),
            tvalEnabled = channelPrefs.getBoolean("tval", false),
            appEnabled = channelPrefs.getBoolean("app", false),
            pbbEnabled = channelPrefs.getBoolean("pbb", true)
        )
    )
    val channelStates: StateFlow<NotificationChannelStates> = _channelStates.asStateFlow()

    fun toggleChannelNotification(channelKey: String, onPermissionNeeded: () -> Unit) {
        val currentStates = _channelStates.value
        when (channelKey) {
            "blog" -> {
                val next = !currentStates.blogEnabled
                if (next) onPermissionNeeded()
                setChannelState("blog", "phca-blog", next)
                _channelStates.update { it.copy(blogEnabled = next) }
            }
            "newsletter" -> {
                val next = !currentStates.newsletterEnabled
                if (next) onPermissionNeeded()
                setChannelState("newsletter", "phca-newsletter", next)
                _channelStates.update { it.copy(newsletterEnabled = next) }
            }
            "tval" -> {
                val next = !currentStates.tvalEnabled
                if (next) onPermissionNeeded()
                setChannelState("tval", "phca-tval", next)
                _channelStates.update { it.copy(tvalEnabled = next) }
            }
            "app" -> {
                val next = !currentStates.appEnabled
                if (next) onPermissionNeeded()
                setChannelState("app", "phca-app", next)
                _channelStates.update { it.copy(appEnabled = next) }
            }
            "pbb" -> {
                val next = !currentStates.pbbEnabled
                if (next) onPermissionNeeded()
                setPbbChannelState(next)
                _channelStates.update { it.copy(pbbEnabled = next) }
            }
        }
    }

    private fun setChannelState(prefKey: String, topic: String, enabled: Boolean) {
        channelPrefs.edit().putBoolean(prefKey, enabled).apply()
        val messaging = FirebaseMessaging.getInstance()
        if (enabled) {
            messaging.subscribeToTopic(topic)
        } else {
            messaging.unsubscribeFromTopic(topic)
        }
    }

    private fun setPbbChannelState(enabled: Boolean) {
        channelPrefs.edit().putBoolean("pbb", enabled).apply()
        val messaging = FirebaseMessaging.getInstance()
        if (enabled) {
            messaging.subscribeToTopic("announcements")
            messaging.subscribeToTopic("nominations")
            messaging.subscribeToTopic("evictions")
        } else {
            messaging.unsubscribeFromTopic("announcements")
            messaging.unsubscribeFromTopic("nominations")
            messaging.unsubscribeFromTopic("evictions")
        }
    }

    fun restoreActiveChannelSubscriptions() {
        val states = _channelStates.value
        val messaging = FirebaseMessaging.getInstance()
        if (states.blogEnabled) messaging.subscribeToTopic("phca-blog")
        if (states.newsletterEnabled) messaging.subscribeToTopic("phca-newsletter")
        if (states.tvalEnabled) messaging.subscribeToTopic("phca-tval")
        if (states.appEnabled) messaging.subscribeToTopic("phca-app")
        if (states.pbbEnabled) {
            messaging.subscribeToTopic("announcements")
            messaging.subscribeToTopic("nominations")
            messaging.subscribeToTopic("evictions")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        val messaging = FirebaseMessaging.getInstance()
        val topics = listOf("phca-blog", "phca-newsletter", "phca-tval", "phca-app", "announcements", "nominations", "evictions", "new_posts")
        if (enabled) {
            topics.forEach { messaging.subscribeToTopic(it) }
        } else {
            topics.forEach { messaging.unsubscribeFromTopic(it) }
        }
    }

    fun toggleNotifications() {
        val nextValue = !_notificationsEnabled.value
        setNotificationsEnabled(nextValue)
    }

    private val _navigateToPbb = MutableStateFlow<String?>(null)
    val navigateToPbb: StateFlow<String?> = _navigateToPbb.asStateFlow()

    private val _navigateToChannel = MutableStateFlow<String?>(null)
    val navigateToChannel: StateFlow<String?> = _navigateToChannel.asStateFlow()

    private val _navigateToUrl = MutableStateFlow<String?>(null)
    val navigateToUrl: StateFlow<String?> = _navigateToUrl.asStateFlow()

    fun onChannelNavigationHandled() {
        _navigateToChannel.value = null
    }

    fun onUrlNavigationHandled() {
        _navigateToUrl.value = null
    }

    private val _notificationTarget = MutableStateFlow<NotificationTarget?>(null)
    val notificationTarget: StateFlow<NotificationTarget?> = _notificationTarget.asStateFlow()

    fun onNotificationTargetHandled() {
        _notificationTarget.value = null
    }

    fun onPbbNavigationHandled() {
        _navigateToPbb.value = null
    }

    fun onNotificationPostIdHandled() {
        _navigateToPostId.value = null
    }

    fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val postIdExtra = intent.getStringExtra("post_id") ?: intent.data?.getQueryParameter("id")
        val fcmUrl = intent.getStringExtra("fcm_url")
        val fcmChannel = intent.getStringExtra("fcm_channel")
        val fcmSeason = intent.getStringExtra("fcm_season")
        val fcmType = intent.getStringExtra("fcm_type")

        val postId = extractPostId(fcmUrl, postIdExtra)

        if (!fcmChannel.isNullOrEmpty() || !fcmUrl.isNullOrEmpty() || postId != null) {
            _notificationTarget.value = NotificationTarget(
                channel = fcmChannel?.lowercase(Locale.ROOT),
                url = fcmUrl,
                postId = postId
            )
            intent.removeExtra("fcm_url")
            intent.removeExtra("fcm_channel")
            intent.removeExtra("post_id")
        }

        if (fcmType == "announcement" || fcmType == "nomination" || fcmType == "eviction" || fcmType == "pbb_updates" || !fcmSeason.isNullOrEmpty()) {
            _navigateToPbb.value = fcmSeason ?: "default"
            intent.removeExtra("fcm_type")
            intent.removeExtra("fcm_season")
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

    fun fetchAppxPosts(isRefreshing: Boolean, page: Int = _appxCurrentPage.value, perPage: Int = 20, searchQuery: String? = null, tag: String? = null) {
        viewModelScope.launch {
            mutex.withLock {
                doFetchAppxPosts(isRefreshing, page, perPage, searchQuery, tag)
            }
        }
    }

    private suspend fun doFetchAppxPosts(isRefreshing: Boolean, page: Int, perPage: Int, searchQuery: String? = null, tag: String? = null) {
        if (isRefreshing) {
            _appxIsRefreshing.value = true
            _appxCurrentPage.value = 1
        } else if (page == 1 && _appxPosts.value.isEmpty()) {
            _appxUiState.update { PostUiState.Loading }
        }

        try {
            if (_appxCategories.value.isEmpty()) {
                try {
                    _appxCategories.value = appxApi.getCategories()
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error fetching AppX categories", e)
                }
            }
            val appCategory = _appxCategories.value.find { it.slug == "app" }

            var tagIds: String? = null
            if (tag != null) {
                val tagsList = appxApi.getTags(search = tag)
                val matchingTag = tagsList.find { it.name.equals(tag, ignoreCase = true) }
                tagIds = matchingTag?.id?.toString()
                if (tagIds == null) {
                    _appxUiState.update { PostUiState.Success(emptyList(), false) }
                    return
                }
            }

            val posts = appxApi.getPosts(
                page = page,
                perPage = perPage,
                categories = appCategory?.id,
                search = searchQuery,
                tags = tagIds
            )

            var uiModels = posts.map { it.toUiModel() }

            if (searchQuery != null) {
                uiModels = uiModels.filter { it.plainTitle.contains(searchQuery, ignoreCase = true) }
            }

            val currentPosts = if (page == 1) emptyList() else _appxPosts.value
            val newPosts = currentPosts + uiModels
            _appxPosts.value = newPosts
            _appxCurrentPage.value = page

            val hasMore = posts.size == perPage
            _appxUiState.update { PostUiState.Success(newPosts, hasMore) }
        } catch (e: Exception) {
            Log.e("WordPressViewModel", "Error fetching AppX posts", e)
            _appxUiState.update { PostUiState.Error(e.message ?: "Unknown error") }
        } finally {
            if (isRefreshing) {
                _appxIsRefreshing.value = false
            }
        }
    }

    fun fetchAppxNextPage() {
        viewModelScope.launch {
            mutex.withLock {
                val uiStateValue = _appxUiState.value
                if (uiStateValue is PostUiState.Success && uiStateValue.hasMore) {
                    val nextPage = _appxCurrentPage.value + 1
                    doFetchAppxPosts(isRefreshing = false, page = nextPage, perPage = 20)
                }
            }
        }
    }

    fun fetchPost(id: Int, isTval: Boolean = false, isAppx: Boolean = false) {
        viewModelScope.launch {
            mutex.withLock {
                _detailUiState.update { PostDetailUiState.Loading }
                try {
                    // Check if already downloaded (Downloads are only for main blog for now)
                    if (!isTval && !isAppx) {
                        val downloadedPost = _downloadedPosts.value.find { it.id == id }
                        if (downloadedPost != null) {
                            _detailUiState.update { PostDetailUiState.Success(downloadedPost) }
                            return@withLock
                        }
                    }

                    val post = when {
                        isTval -> tvalApi.getPost(id)
                        isAppx -> appxApi.getPost(id)
                        else -> wordPressApi.getPost(id)
                    }
                    _detailUiState.update { PostDetailUiState.Success(post.toDetailUiModel(isTval = isTval || isAppx)) }
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error fetching post", e)
                    _detailUiState.update { PostDetailUiState.Error(e.message ?: "Unknown error") }
                }
            }
        }
    }

    fun fetchPostByUrl(url: String, isTval: Boolean = false, isAppx: Boolean = false) {
        viewModelScope.launch {
            mutex.withLock {
                _detailUiState.update { PostDetailUiState.Loading }
                try {
                    val uri = android.net.Uri.parse(url)
                    val host = uri.host?.lowercase(Locale.ROOT) ?: ""

                    val useTval = isTval || host.contains("tval.drpsphca.com")
                    val useAppx = isAppx || host.contains("app.drpsphca.com")

                    val slug = extractSlug(url)
                    val posts = when {
                        useTval -> tvalApi.getPosts(slug = slug, perPage = 1)
                        useAppx -> appxApi.getPosts(slug = slug, perPage = 1)
                        else -> wordPressApi.getPosts(slug = slug, perPage = 1)
                    }
                    val post = posts.firstOrNull()
                    if (post != null) {
                        _detailUiState.update { PostDetailUiState.Success(post.toDetailUiModel(isTval = useTval || useAppx)) }
                    } else {
                        val searchPosts = when {
                            useTval -> tvalApi.getPosts(search = url, perPage = 1)
                            useAppx -> appxApi.getPosts(search = url, perPage = 1)
                            else -> wordPressApi.getPosts(search = url, perPage = 1)
                        }
                        val searchPost = searchPosts.firstOrNull()
                        if (searchPost != null) {
                            _detailUiState.update { PostDetailUiState.Success(searchPost.toDetailUiModel(isTval = useTval || useAppx)) }
                        } else {
                            val recentPosts = when {
                                useTval -> tvalApi.getPosts(perPage = 10)
                                useAppx -> appxApi.getPosts(perPage = 10)
                                else -> emptyList()
                            }
                            val matchingPost = recentPosts.find { p ->
                                p.link == url || (slug != null && p.link.contains(slug))
                            } ?: recentPosts.firstOrNull()

                            if (matchingPost != null) {
                                _detailUiState.update { PostDetailUiState.Success(matchingPost.toDetailUiModel(isTval = useTval || useAppx)) }
                            } else if (useTval || useAppx) {
                                _detailUiState.update { PostDetailUiState.Success(Post(
                                    id = 0,
                                    date = "",
                                    modified = "",
                                    link = url,
                                    title = Rendered("Post"),
                                    content = Rendered("<p>Post available at <a href=\"$url\">$url</a></p>"),
                                    excerpt = Rendered(""),
                                    embedded = null
                                ).toDetailUiModel(isTval = useTval || useAppx)) }
                            } else {
                                _detailUiState.update { PostDetailUiState.Error("Post not found for URL") }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WordPressViewModel", "Error fetching post by URL", e)
                    val uri = android.net.Uri.parse(url)
                    val host = uri.host?.lowercase(Locale.ROOT) ?: ""
                    val useTval = isTval || host.contains("tval.drpsphca.com")
                    val useAppx = isAppx || host.contains("app.drpsphca.com")
                    if (useTval || useAppx) {
                        _detailUiState.update { PostDetailUiState.Success(Post(
                            id = 0,
                            date = "",
                            modified = "",
                            link = url,
                            title = Rendered("Post"),
                            content = Rendered("<p>Post available at <a href=\"$url\">$url</a></p>"),
                            excerpt = Rendered(""),
                            embedded = null
                        ).toDetailUiModel(isTval = useTval || useAppx)) }
                    } else {
                        _detailUiState.update { PostDetailUiState.Error(e.message ?: "Unknown error") }
                    }
                }
            }
        }
    }

    private fun Post.toUiModel(isTval: Boolean = false): PostItemUiModel {
        val dateToUse = if (isTval) modified else date
        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateToUse)?.let {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: dateToUse
        val plainTitle = Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val plainExcerpt = Html.fromHtml(excerpt.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val imageUrl = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
        val tags = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "post_tag" } }?.map {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: emptyList()
        val category = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "category" } }?.firstOrNull()?.let {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        }

        return PostItemUiModel(
            id = id,
            formattedDate = formattedDate,
            plainTitle = plainTitle,
            plainExcerpt = plainExcerpt,
            imageUrl = imageUrl,
            tags = tags,
            localImageUrl = null,
            category = category
        )
    }

    private fun Post.toDetailUiModel(isTval: Boolean = false): PostDetailUiModel {
        val dateToUse = if (isTval) modified else date
        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateToUse)?.let {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: dateToUse
        val plainTitle = Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val plainExcerpt = Html.fromHtml(excerpt.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val imageUrl = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
        val tags = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "post_tag" } }?.map {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: emptyList()
        val category = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "category" } }?.firstOrNull()?.let {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        }
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
            localImageUrl = null,
            category = category
        )
    }
}
