package com.drpsphca.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DRPSPHCATheme {
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
                Scaffold(
                    topBar = { TopAppBar(title = { Text("DRPSPHCA Blog") }) }
                ) {
                    innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
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
                                    Button(onClick = { wordPressViewModel.fetchPosts() }) {
                                        Text("Retry")
                                    }
                                }
                            }

                            is PostUiState.PostSuccess -> { /* Do Nothing */ }
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
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) {
                    innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (val state = uiState) {
                            is PostUiState.Loading -> {
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

                            is PostUiState.Success -> { /* Do Nothing */ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostList(posts: List<Post>, navController: NavController) {
    LazyColumn(modifier = Modifier.padding(8.dp)) {
        items(posts) {
            post -> PostItem(post = post, navController = navController)
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
                Text(text = post.title.rendered, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(post.date)?.let {
                    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
                } ?: post.date
                Text(text = formattedDate, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = android.text.Html.fromHtml(post.excerpt.rendered, android.text.Html.FROM_HTML_MODE_COMPACT).toString(), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                post.embedded?.terms?.getOrNull(0)?.let { tags ->
                    Row {
                        tags.forEach { tag ->
                            Text(
                                text = tag.name,
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
