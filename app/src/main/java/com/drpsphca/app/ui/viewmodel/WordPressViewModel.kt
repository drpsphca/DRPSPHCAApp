package com.drpsphca.app.ui.viewmodel

import android.text.Html
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drpsphca.app.data.Category
import com.drpsphca.app.data.Post
import com.drpsphca.app.data.WordPressApi
import com.drpsphca.app.data.WordPressClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val tags: List<String>
)

@Immutable
data class PostDetailUiModel(
    val formattedDate: String,
    val plainTitle: String,
    val content: String,
    val imageUrl: String?,
    val tags: List<String>
)

class WordPressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _newsletterUiState = MutableStateFlow<NewsletterUiState>(NewsletterUiState.Loading)
    val newsletterUiState: StateFlow<NewsletterUiState> = _newsletterUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _blogPosts = MutableStateFlow<List<PostItemUiModel>>(emptyList())
    private val _currentPage = MutableStateFlow(1)
    private val _totalPages = MutableStateFlow(1)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    init {
        fetchCategories()
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                _categories.value = api.getCategories()
                fetchPosts(isRefreshing = false, page = 1, perPage = 10, forHome = true)
                fetchNewsletters(perPage = 5)
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching categories", e)
                _uiState.value = PostUiState.Error(e.message ?: "Unknown error")
                _newsletterUiState.value = NewsletterUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchPosts(isRefreshing: Boolean, page: Int = _currentPage.value, perPage: Int = 10, forHome: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshing) {
                _isRefreshing.value = true
                _currentPage.value = 1 // Reset page for refresh
            } else if (page == 1) {
                _uiState.value = PostUiState.Loading
            }

            val blogCategory = _categories.value.find { it.slug == "blog" }
            if (blogCategory == null) {
                _uiState.value = PostUiState.Error("Blog category not found")
                _isRefreshing.value = false
                return@launch
            }

            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                val posts = api.getPosts(page = page, perPage = perPage, categories = blogCategory.id)

                val currentPosts = if (page == 1 || forHome) emptyList() else _blogPosts.value
                val newPosts = currentPosts + posts.map { it.toUiModel() }
                _blogPosts.value = newPosts
                _currentPage.value = page

                val hasMore = posts.size == perPage
                _uiState.value = PostUiState.Success(_blogPosts.value, hasMore)
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching posts", e)
                _uiState.value = PostUiState.Error(e.message ?: "Unknown error")
            }
            _isRefreshing.value = false
        }
    }

    fun fetchNextPage() {
        if (_uiState.value is PostUiState.Success && (_uiState.value as PostUiState.Success).hasMore) {
            val nextPage = _currentPage.value + 1
            fetchPosts(isRefreshing = false, page = nextPage, perPage = 20)
        }
    }

    fun fetchNewsletters(perPage: Int = 5) {
        viewModelScope.launch {
            _newsletterUiState.value = NewsletterUiState.Loading
            val newsletterCategory = _categories.value.find { it.slug == "newsletter" }

            if (newsletterCategory == null) {
                _newsletterUiState.value = NewsletterUiState.Error("Newsletter category not found. Assuming newsletters are standard posts under a 'newsletter' category slug.")
                return@launch
            }
            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                val newsletters = api.getPosts(categories = newsletterCategory.id, perPage = perPage)
                _newsletterUiState.value = NewsletterUiState.Success(newsletters.map { it.toUiModel() })
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching newsletters", e)
                _newsletterUiState.value = NewsletterUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchPost(id: Int) {
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                val post = api.getPost(id)
                _uiState.value = PostUiState.PostSuccess(post.toDetailUiModel())
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching post", e)
                _uiState.value = PostUiState.Error(e.message ?: "Unknown error")
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
            tags = tags
        )
    }

    private fun Post.toDetailUiModel(): PostDetailUiModel {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(date)?.let {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: date
        val plainTitle = Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_COMPACT).toString()
        val imageUrl = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
        val tags = embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "post_tag" } }?.map {
            Html.fromHtml(it.name, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: emptyList()

        return PostDetailUiModel(
            formattedDate = formattedDate,
            plainTitle = plainTitle,
            content = content.rendered,
            imageUrl = imageUrl,
            tags = tags
        )
    }
}
