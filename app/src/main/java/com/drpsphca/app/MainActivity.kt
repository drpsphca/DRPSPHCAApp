package com.drpsphca.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import com.drpsphca.app.ui.screens.PostDetailScreen
import com.drpsphca.app.ui.theme.DRPSPHCATheme
import com.drpsphca.app.ui.viewmodel.PostItemUiModel
import com.drpsphca.app.ui.viewmodel.PostUiState
import com.drpsphca.app.ui.viewmodel.WordPressViewModel
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import android.net.Uri
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Calendar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.drpsphca.app.ui.viewmodel.NewsletterUiState
import com.drpsphca.app.BuildConfig


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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = setOf(Screen.Home.route, Screen.Blog.route, Screen.Newsletter.route)

    Scaffold {
        paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    HomeScreen(navController = navController, wordPressViewModel = wordPressViewModel)
                }
                composable("blog") {
                    BlogScreen(navController = navController, wordPressViewModel = wordPressViewModel)
                }
                composable("newsletter") {
                    NewsletterScreen(navController = navController, wordPressViewModel = wordPressViewModel)
                }
                composable(
                    "postdetail/{postId}",
                    arguments = listOf(navArgument("postId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getInt("postId")
                    PostDetailRoute(
                        wordPressViewModel = wordPressViewModel,
                        navController = navController,
                        postId = postId
                    )
                }
                composable("webview/{url}") { backStackEntry ->
                    val url = backStackEntry.arguments?.getString("url") ?: "about:blank"
                    WebViewScreen(url = url)
                }
            }
            
            AnimatedVisibility(
                visible = currentRoute in bottomBarRoutes,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailRoute(
    wordPressViewModel: WordPressViewModel,
    navController: NavController,
    postId: Int?
) {
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
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is PostUiState.Loading, is PostUiState.Success, PostUiState.Idle -> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, wordPressViewModel: WordPressViewModel) {
    val uiState by wordPressViewModel.uiState.collectAsState()
    val newsletterUiState by wordPressViewModel.newsletterUiState.collectAsState()
    val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        wordPressViewModel.fetchPosts(isRefreshing = false, page = 1, perPage = 10, forHome = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "DRPSPHCA Blog",
                        modifier = Modifier.height(40.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                wordPressViewModel.fetchPosts(true, page = 1, perPage = 10, forHome = true)
                wordPressViewModel.fetchNewsletters(perPage = 1)
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp), // Add padding to avoid overlap with floating bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Newsletter Section
                item {
                    Text(
                        text = "Latest Newsletter",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                item {
                     when (val state = newsletterUiState) {
                        is NewsletterUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is NewsletterUiState.Success -> {
                            val newsletter = state.newsletters.firstOrNull()
                            if (newsletter != null) {
                                NewsletterItem(post = newsletter, navController = navController)
                            } else {
                                Text("No newsletters available.", modifier = Modifier.padding(16.dp))
                            }
                        }
                        is NewsletterUiState.Error -> {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                        }
                        NewsletterUiState.Idle -> { /* Do Nothing */ }
                    }
                }

                // Blog Posts Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Latest Blog Posts",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                when (val state = uiState) {
                    is PostUiState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is PostUiState.Success -> {
                        val displayedPosts = state.posts.take(10)
                        if (displayedPosts.isEmpty()) {
                            item { Text("No blog posts available.", modifier = Modifier.padding(16.dp)) }
                        } else {
                            items(displayedPosts, key = { it.id }) { post ->
                                PostItem(post = post, navController = navController)
                            }
                        }
                    }
                    is PostUiState.Error -> {
                         item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = state.message)
                                Button(onClick = { wordPressViewModel.fetchPosts(false, page = 1, perPage = 10, forHome = true) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is PostUiState.PostSuccess, PostUiState.Idle -> { /* Do Nothing */ }
                }

                // Footer Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "© ${Calendar.getInstance().get(Calendar.YEAR)} DRPS PHCA",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                val encodedUrl = Uri.encode("https://drpsphca.com/terms")
                                navController.navigate("webview/$encodedUrl")
                            }) {
                                Text("Terms of Use")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            TextButton(onClick = {
                                val encodedUrl = Uri.encode("https://drpsphca.com/privacy")
                                navController.navigate("webview/$encodedUrl")
                            }) {
                                Text("Privacy Policy")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogScreen(navController: NavController, wordPressViewModel: WordPressViewModel) {
    val uiState by wordPressViewModel.uiState.collectAsState()
    val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        wordPressViewModel.fetchPosts(isRefreshing = false, page = 1, perPage = 20)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Blog") }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { wordPressViewModel.fetchPosts(true, page = 1, perPage = 20) },
                modifier = Modifier.weight(1f)
            ) {
                when (val state = uiState) {
                    is PostUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PostUiState.Success -> {
                        if (state.posts.isEmpty()) {
                            Text("No blog posts available.", modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(post = post, navController = navController)
                                }
                                item {
                                    if (state.hasMore) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { wordPressViewModel.fetchNextPage() }) {
                                            Text("Load More")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                    is PostUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message)
                            Button(onClick = { wordPressViewModel.fetchPosts(false, page = 1, perPage = 20) }) {
                                Text("Retry")
                            }
                        }
                    }
                    is PostUiState.PostSuccess, PostUiState.Idle -> { /* Do Nothing */ }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsletterScreen(navController: NavController, wordPressViewModel: WordPressViewModel) {
    val newsletterUiState by wordPressViewModel.newsletterUiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Newsletter") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Latest Newsletters",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(16.dp)
            )
            when (val state = newsletterUiState) {
                is NewsletterUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NewsletterUiState.Success -> {
                    if (state.newsletters.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(state.newsletters, key = { it.id }) { newsletter ->
                                PostItem(post = newsletter, navController = navController)
                            }
                        }
                    } else {
                        Text("No newsletters available.", modifier = Modifier.padding(16.dp))
                    }
                }
                is NewsletterUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message)
                        Button(onClick = { wordPressViewModel.fetchNewsletters(perPage = 5) }) {
                            Text("Retry")
                        }
                    }
                }
                NewsletterUiState.Idle -> { /* Do Nothing */ }
            }
        }
    }
}

@Composable
fun NewsletterItem(post: PostItemUiModel, navController: NavController) {
    Card(modifier = Modifier
        .padding(8.dp)
        .clickable { navController.navigate("postdetail/${post.id}") }) {
        Column {
            post.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = post.plainTitle,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.plainTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = post.formattedDate, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WebViewScreen(url: String) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl(url)
        }
    }, update = { webView ->
        webView.loadUrl(url)
    })
}

@Composable
fun PostList(posts: List<PostItemUiModel>, navController: NavController) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostItem(post = post, navController = navController)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostItem(post = post, navController = navController)
                }
            }
        }
    }
}

@Composable
fun PostItem(post: PostItemUiModel, navController: NavController) {
    Card(modifier = Modifier
        .padding(8.dp)
        .clickable { navController.navigate("postdetail/${post.id}") }) {
        Column {
            post.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = post.plainTitle,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.plainTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = post.formattedDate, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = post.plainExcerpt, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (post.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(post.tags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = tag,
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

sealed class Screen(val route: String, val label: String, @DrawableRes val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.phcaapp_home)
    object Blog : Screen("blog", "Blog", R.drawable.phcaapp_blog)
    object Newsletter : Screen("newsletter", "Newsletter", R.drawable.phcaapp_newsletter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Blog, Screen.Newsletter)
    val selectedColors = mapOf(
        Screen.Home.route to Color(0xFF128FF1),
        Screen.Blog.route to Color(0xFF734CC2),
        Screen.Newsletter.route to Color(0xFFDA048F)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        NavigationBar(
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            containerColor = Color.Gray
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val selectedColor = selectedColors[screen.route] ?: MaterialTheme.colorScheme.primary
                val tint = if (isSelected) selectedColor else Color.White

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    label = { Text(screen.label) },
                    icon = {
                        Image(
                            painter = painterResource(id = screen.icon),
                            contentDescription = screen.label,
                            colorFilter = ColorFilter.tint(tint)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = selectedColor,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
