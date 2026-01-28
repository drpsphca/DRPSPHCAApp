package com.drpsphca.app.ui.viewmodel

import android.text.Html
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drpsphca.app.data.Post
import com.drpsphca.app.data.WordPressApi
import com.drpsphca.app.data.WordPressClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface PostUiState {
    data class Success(val posts: List<PostItemUiModel>) : PostUiState
    data class PostSuccess(val post: PostDetailUiModel) : PostUiState
    data class Error(val message: String) : PostUiState
    object Loading : PostUiState
}

data class PostItemUiModel(
    val id: Int,
    val formattedDate: String,
    val plainTitle: String,
    val plainExcerpt: String,
    val imageUrl: String?,
    val tags: List<String>
)

data class PostDetailUiModel(
    val formattedDate: String,
    val plainTitle: String,
    val content: String,
    val imageUrl: String?,
    val tags: List<String>
)

class WordPressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchPosts(isRefreshing = false)
    }

    fun fetchPosts(isRefreshing: Boolean) {
        viewModelScope.launch {
            if (isRefreshing) {
                _isRefreshing.value = true
            } else {
                _uiState.value = PostUiState.Loading
            }
            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                val categories = api.getCategories()
                val blogCategory = categories.find { it.slug == "blog" }
                if (blogCategory != null) {
                    val posts = api.getPosts(categories = blogCategory.id, perPage = 25)
                    _uiState.value = PostUiState.Success(posts.map { it.toUiModel() })
                } else {
                    _uiState.value = PostUiState.Error("Blog category not found")
                }
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching posts", e)
                _uiState.value = PostUiState.Error(e.message ?: "Unknown error")
            }
            _isRefreshing.value = false
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
