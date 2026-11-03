package com.drpsphca.app.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drpsphca.app.ui.theme.DRPSPHCATheme
import com.drpsphca.app.ui.theme.Gilroy
import com.drpsphca.app.ui.viewmodel.WordPressViewModel
import com.drpsphca.app.ui.viewmodel.PostDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
class PostDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val postUrl = intent.getStringExtra("url") ?: intent.getStringExtra("post_url")
        val channel = intent.getStringExtra("fcm_channel")?.lowercase()
        val isTval = channel == "tval"
        val isAppx = channel == "app"

        setContent {
            val wordPressViewModel: WordPressViewModel = viewModel()
            val isDark = isSystemInDarkTheme()
            val detailUiState by wordPressViewModel.detailUiState.collectAsState()

            LaunchedEffect(postUrl) {
                if (!postUrl.isNullOrEmpty()) {
                    wordPressViewModel.fetchPostByUrl(postUrl, isTval = isTval, isAppx = isAppx)
                }
            }

            DRPSPHCATheme(darkTheme = isDark) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (isTval) "TV Ad Listing" else if (isAppx) "Bite-sized" else "Post",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = Gilroy,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        when (val state = detailUiState) {
                            is PostDetailUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is PostDetailUiState.Success -> {
                                PostDetailScreen(
                                    post = state.post,
                                    isDarkMode = isDark,
                                    isOffline = false,
                                    onTagClick = { },
                                    onFullscreenToggle = { }
                                )
                            }
                            is PostDetailUiState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            is PostDetailUiState.Idle -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Loading post...")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
