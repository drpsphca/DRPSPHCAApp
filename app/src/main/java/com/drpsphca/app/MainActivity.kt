package com.drpsphca.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.drpsphca.app.data.Post
import com.drpsphca.app.ui.screens.PostDetailScreen
import com.drpsphca.app.ui.theme.DRPSPHCATheme
import com.drpsphca.app.ui.viewmodel.PostUiState
import com.drpsphca.app.ui.viewmodel.WordPressViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DRPSPHCATheme {
                val view = LocalView.current
                val darkTheme = isSystemInDarkTheme()
                val backgroundColor = MaterialTheme.colorScheme.background
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = backgroundColor.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WordPressApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressApp(wordPressViewModel: WordPressViewModel = viewModel()) {
    val navController = rememberNavController()
    Scaffold {
        paddingValues ->
        NavHost(navController = navController, startDestination = "postlist", modifier = Modifier.padding(paddingValues)) {
            composable("postlist") {
                val uiState by wordPressViewModel.uiState.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val isCurrentDestination = navBackStackEntry?.destination?.route == "postlist"

                LaunchedEffect(uiState, isCurrentDestination) {
                    if (isCurrentDestination && uiState is PostUiState.PostSuccess) {
                        wordPressViewModel.fetchPosts(false)
                    }
                }
                Scaffold(
                    topBar = { 
                        CenterAlignedTopAppBar(
                            title = {
                                AsyncImage(
                                    model = "https://drpsphca.com/wp-content/uploads/2025/11/DRPS-PHCA-Website-2025-Logo-Gen-11-Theme-Main-CC-scaled.png",
                                    contentDescription = "DRPSPHCA Blog",
                                    modifier = Modifier.height(40.dp)
                                )
                            } 
                        )
                    }
                ) {
                    innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()

                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { wordPressViewModel.fetchPosts(true) }
                        ) {
                            when (val state = uiState) {
                                is PostUiState.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }

                                is PostUiState.Success -> {
                                    PostList(posts = state.posts, navController = navController)
                                }

                                is PostUiState.Error -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = state.message)
                                        Button(onClick = { wordPressViewModel.fetchPosts(false) }) {
                                            Text("Retry")
                                        }
                                    }
                                }

                                is PostUiState.PostSuccess -> { /* Do Nothing while reloading */ }
                            }
                        }
                    }
                }
            }
            composable(
                "postdetail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.IntType })
            ) {
                backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId")
                LaunchedEffect(postId) {
                    if (postId != null) {
                        wordPressViewModel.fetchPost(postId)
                    }
                }
                val uiState by wordPressViewModel.uiState.collectAsState()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Post") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) {
                    innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (val state = uiState) {
                            is PostUiState.Loading, is PostUiState.Success -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }

                            is PostUiState.PostSuccess -> {
                                PostDetailScreen(post = state.post)
                            }

                            is PostUiState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = state.message)
                                    Button(onClick = { postId?.let { wordPressViewModel.fetchPost(it) } }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostList(posts: List<Post>, navController: NavController) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts) { post ->
                    PostItem(post = post, navController = navController)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts) { post ->
                    PostItem(post = post, navController = navController)
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, navController: NavController) {
    Card(modifier = Modifier
        .padding(8.dp)
        .clickable { navController.navigate("postdetail/${post.id}") }) {
        Column {
            post.embedded?.featuredMedia?.firstOrNull()?.sourceUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = post.title.rendered,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = android.text.Html.fromHtml(post.title.rendered, android.text.Html.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(post.date)?.let {
                    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
                } ?: post.date
                Text(text = formattedDate, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = android.text.Html.fromHtml(post.excerpt.rendered, android.text.Html.FROM_HTML_MODE_COMPACT).toString(), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val tags = post.embedded?.terms?.firstOrNull { it.any { term -> term.taxonomy == "post_tag" } }
                if (tags != null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = android.text.Html.fromHtml(tag.name, android.text.Html.FROM_HTML_MODE_COMPACT).toString(),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
