package com.drpsphca.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drpsphca.app.data.Post
import com.drpsphca.app.data.WordPressApi
import com.drpsphca.app.data.WordPressClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PostUiState {
    data class Success(val posts: List<Post>) : PostUiState
    data class PostSuccess(val post: Post) : PostUiState
    data class Error(val message: String) : PostUiState
    object Loading : PostUiState
}

class WordPressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                val categories = api.getCategories()
                val blogCategory = categories.find { it.slug == "blog" }
                if (blogCategory != null) {
                    _uiState.value = PostUiState.Success(api.getPosts(categories = blogCategory.id, perPage = 25))
                } else {
                    _uiState.value = PostUiState.Error("Blog category not found")
                }
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching posts", e)
                _uiState.value = PostUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchPost(id: Int) {
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            try {
                val api = WordPressClient.retrofit.create(WordPressApi::class.java)
                _uiState.value = PostUiState.PostSuccess(api.getPost(id))
            } catch (e: Exception) {
                Log.e("WordPressViewModel", "Error fetching post", e)
                _uiState.value = PostUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
