
package com.drpsphca.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.drpsphca.app.ui.theme.Gilroy
import com.drpsphca.app.ui.theme.NotoSerif
import com.drpsphca.app.ui.viewmodel.PostItemUiModel
import com.drpsphca.app.ui.viewmodel.PostUiState
import com.drpsphca.app.ui.viewmodel.WordPressViewModel
import java.util.Calendar
import com.drpsphca.app.ui.viewmodel.NewsletterUiState
import com.drpsphca.app.BuildConfig
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.drpsphca.app.ui.WindowSize
import com.drpsphca.app.ui.rememberWindowSizeClass
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.drpsphca.app.ui.viewmodel.PostDetailUiModel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val wordPressViewModel: WordPressViewModel by lazy {
            androidx.lifecycle.ViewModelProvider(this)[WordPressViewModel::class.java]
        }
        if (isGranted) {
            wordPressViewModel.setNotificationsEnabled(true)
            // Auto-subscribe once permission is granted (Android 13+)
            FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
        } else {
            wordPressViewModel.setNotificationsEnabled(false)
            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_posts")
        }
    }

    fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val wordPressViewModel: WordPressViewModel by lazy { 
            androidx.lifecycle.ViewModelProvider(this)[WordPressViewModel::class.java]
        }
        wordPressViewModel.handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Schedule widget updates
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.drpsphca.app.widget.WidgetUpdateWorker>(
            1, java.util.concurrent.TimeUnit.HOURS
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_update",
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        val wordPressViewModel: WordPressViewModel by lazy { 
            androidx.lifecycle.ViewModelProvider(this)[WordPressViewModel::class.java]
        }
        wordPressViewModel.handleNotificationIntent(intent)
        
        // Initialize Ads SDK (flavor-specific)
        initAds(this)
        
        setContent {
            val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
            val darkTheme = when (darkModeConfig) {
                WordPressViewModel.DarkModeConfig.ON -> true
                WordPressViewModel.DarkModeConfig.OFF -> false
                WordPressViewModel.DarkModeConfig.AUTO -> isSystemInDarkTheme()
            }
            
            DRPSPHCATheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(), 
                    color = MaterialTheme.colorScheme.surface
                ) {
                    WordPressApp(wordPressViewModel)
                }
            }
        }
    }
}

@Composable
fun MorePopupMenu(
    expanded: Boolean,
    isOnline: Boolean,
    darkModeConfig: WordPressViewModel.DarkModeConfig,
    notificationsEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onBookmarksClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onNotificationsToggle: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 0.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        DropdownMenuItem(
            text = { 
                Text(
                    text = "Bookmarks",
                    color = if (isOnline) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal
                ) 
            },
            onClick = {
                onBookmarksClick()
                onDismissRequest()
            },
            enabled = isOnline,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phcaapp_bookmarks),
                    contentDescription = "Bookmarks",
                    modifier = Modifier.size(24.dp),
                    tint = if (isOnline) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        )
        DropdownMenuItem(
            text = { 
                Text(
                    text = "Downloads",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal
                ) 
            },
            onClick = {
                onDownloadsClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phcaapp_download),
                    contentDescription = "Downloads",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
        DropdownMenuItem(
            text = { 
                Text(
                    text = when (darkModeConfig) {
                        WordPressViewModel.DarkModeConfig.ON -> "Dark Mode: ON"
                        WordPressViewModel.DarkModeConfig.OFF -> "Dark Mode: OFF"
                        WordPressViewModel.DarkModeConfig.AUTO -> "Dark Mode: AUTO"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal
                ) 
            },
            onClick = {
                onDarkModeToggle()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phcaapp_darkmode),
                    contentDescription = "Dark Mode",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = if (notificationsEnabled) "Notifications: ON" else "Notifications: OFF",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal
                )
            },
            onClick = {
                onNotificationsToggle()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phcaapp_bell),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithActions(
    title: @Composable () -> Unit,
    showSearch: Boolean = false,
    darkModeConfig: WordPressViewModel.DarkModeConfig = WordPressViewModel.DarkModeConfig.AUTO,
    notificationsEnabled: Boolean = false,
    onDarkModeToggle: () -> Unit = {},
    onNotificationsToggle: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isOnline = remember(showMoreMenu) { isOnline(context) }

    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = {
            if (showSearch) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.phcaapp_search),
                        contentDescription = "Search",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.phcaapp_more),
                        contentDescription = "More",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                MorePopupMenu(
                    expanded = showMoreMenu,
                    isOnline = isOnline,
                    darkModeConfig = darkModeConfig,
                    notificationsEnabled = notificationsEnabled,
                    onDismissRequest = { showMoreMenu = false },
                    onBookmarksClick = onBookmarksClick,
                    onDownloadsClick = onDownloadsClick,
                    onDarkModeToggle = onDarkModeToggle,
                    onNotificationsToggle = onNotificationsToggle
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressApp(wordPressViewModel: WordPressViewModel = viewModel()) {
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val notificationsEnabled by wordPressViewModel.notificationsEnabled.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkMode = when (darkModeConfig) {
        WordPressViewModel.DarkModeConfig.ON -> true
        WordPressViewModel.DarkModeConfig.OFF -> false
        WordPressViewModel.DarkModeConfig.AUTO -> isSystemDark
    }

    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
        }
    }
    val navController = rememberNavController()

    val navigateToPostId by wordPressViewModel.navigateToPostId.collectAsState()
    LaunchedEffect(navigateToPostId) {
        navigateToPostId?.let { postId ->
            navController.navigate("postdetail/$postId")
            wordPressViewModel.onNotificationPostIdHandled()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = setOf(Screen.Home.route, Screen.Blog.route, Screen.Newsletter.route)
    val windowSize = rememberWindowSizeClass()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute in bottomBarRoutes,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BottomNavigationBar(navController = navController, isDarkMode = isDarkMode)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    navController = navController, 
                    wordPressViewModel = wordPressViewModel, 
                    windowSize = windowSize, 
                    isDarkMode = isDarkMode,
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsToggle = {
                        val activity = context as? MainActivity
                        if (!notificationsEnabled) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                activity?.requestNotificationPermission()
                            } else {
                                wordPressViewModel.setNotificationsEnabled(true)
                                // Auto-subscribe on older devices (no permission prompt needed)
                                FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
                            }
                        } else {
                            wordPressViewModel.setNotificationsEnabled(false)
                            // Auto-unsubscribe when toggling OFF
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_posts")
                        }
                    }
                )
            }
            composable("blog") {
                BlogScreen(
                    navController = navController, 
                    wordPressViewModel = wordPressViewModel, 
                    windowSize = windowSize, 
                    isDarkMode = isDarkMode,
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsToggle = {
                        val activity = context as? MainActivity
                        if (!notificationsEnabled) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                activity?.requestNotificationPermission()
                            } else {
                                wordPressViewModel.setNotificationsEnabled(true)
                                // Auto-subscribe on older devices (no permission prompt needed)
                                FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
                            }
                        } else {
                            wordPressViewModel.setNotificationsEnabled(false)
                            // Auto-unsubscribe when toggling OFF
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_posts")
                        }
                    }
                )
            }
            composable("newsletter") {
                NewsletterScreen(
                    navController = navController, 
                    wordPressViewModel = wordPressViewModel, 
                    windowSize = windowSize, 
                    isDarkMode = isDarkMode,
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsToggle = {
                        val activity = context as? MainActivity
                        if (!notificationsEnabled) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                activity?.requestNotificationPermission()
                            } else {
                                wordPressViewModel.setNotificationsEnabled(true)
                                // Auto-subscribe on older devices (no permission prompt needed)
                                FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
                            }
                        } else {
                            wordPressViewModel.setNotificationsEnabled(false)
                            // Auto-unsubscribe when toggling OFF
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_posts")
                        }
                    }
                )
            }
            composable(
                "postdetail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.IntType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId")
                PostDetailRoute(
                    wordPressViewModel = wordPressViewModel,
                    navController = navController,
                    postId = postId,
                    isDarkMode = isDarkMode
                )
            }
            composable("webview/{url}") { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: "about:blank"
                WebViewScreen(url = url)
            }
            composable("search") {
                SearchScreen(navController = navController, wordPressViewModel = wordPressViewModel, isDarkMode = isDarkMode)
            }
            composable("bookmarks") {
                BookmarksScreen(navController = navController, wordPressViewModel = wordPressViewModel, isDarkMode = isDarkMode)
            }
            composable("downloads") {
                DownloadsScreen(navController = navController, wordPressViewModel = wordPressViewModel, isDarkMode = isDarkMode)
            }
            composable("tag/{tagName}") { backStackEntry ->
                val tagName = backStackEntry.arguments?.getString("tagName") ?: ""
                TagScreen(tagName = tagName, navController = navController, wordPressViewModel = wordPressViewModel, isDarkMode = isDarkMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailRoute(
    wordPressViewModel: WordPressViewModel,
    navController: NavController,
    postId: Int?,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(postId) {
        if (postId != null) {
            wordPressViewModel.fetchPost(postId)
        }
    }
    val uiState by wordPressViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Post",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is PostUiState.PostSuccess) {
                        val post = (uiState as PostUiState.PostSuccess).post
                        val bookmarkedPosts by wordPressViewModel.bookmarkedPosts.collectAsState()
                        val isBookmarked = bookmarkedPosts.any { it.id == post.id }
                        
                        val downloadedPosts by wordPressViewModel.downloadedPosts.collectAsState()
                        val isDownloaded = downloadedPosts.any { it.id == post.id }
                        
                        val downloadingIds by wordPressViewModel.downloadingPostIds.collectAsState()
                        val isDownloading = post.id in downloadingIds
                        
                        val bookmarkingIds by wordPressViewModel.bookmarkingPostIds.collectAsState()
                        val isBookmarking = post.id in bookmarkingIds
                        val isOnline = isOnline(context)
                        
                        if (isBookmarking) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            IconButton(
                                onClick = { 
                                    wordPressViewModel.toggleBookmark(post.toItemUiModel()) { added ->
                                        if (added) {
                                            Toast.makeText(context, "Post added to bookmarks", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Bookmark removed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = isOnline
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.phcaapp_bookmarks),
                                    contentDescription = "Bookmark",
                                    tint = if (!isOnline) Color.Gray else if (isBookmarked) Color(0xFF128FF1) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        if (isDownloading) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            IconButton(
                                onClick = { 
                                    val wasDownloaded = isDownloaded
                                    wordPressViewModel.toggleDownload(post) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Post downloaded and saved to device", Toast.LENGTH_SHORT).show()
                                        } else if (wasDownloaded) {
                                            Toast.makeText(context, "Download deleted from device.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = isOnline
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.phcaapp_download),
                                    contentDescription = "Download",
                                    tint = if (!isOnline) Color.Gray else if (isDownloaded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(onClick = { 
                            sharePost(context, post.plainTitle, post.link)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.phcaapp_share),
                                contentDescription = "Share",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
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
                    val isOffline = !isOnline(context)
                    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
                    val isSystemDark = isSystemInDarkTheme()
                    val isDarkMode = when (darkModeConfig) {
                        WordPressViewModel.DarkModeConfig.ON -> true
                        WordPressViewModel.DarkModeConfig.OFF -> false
                        WordPressViewModel.DarkModeConfig.AUTO -> isSystemDark
                    }
                    PostDetailScreen(
                        post = state.post, 
                        isDarkMode = isDarkMode, 
                        isOffline = isOffline,
                        onTagClick = { tagName ->
                            val encodedTag = Uri.encode(tagName)
                            navController.navigate("tag/$encodedTag")
                        }
                    )
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

fun sharePost(context: Context, title: String, url: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "$title: $url")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
        else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
        else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    wordPressViewModel: WordPressViewModel,
    windowSize: WindowSize,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onNotificationsToggle: () -> Unit
) {
    val uiState by wordPressViewModel.uiState.collectAsState()
    val newsletterUiState by wordPressViewModel.newsletterUiState.collectAsState()
    val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    LaunchedEffect(Unit) {
        wordPressViewModel.fetchPosts(isRefreshing = false, page = 1, perPage = 10, forHome = true)
    }

    Column {
        TopBarWithActions(
            title = {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "DRPSPHCA Blog",
                    modifier = Modifier.height(40.dp)
                )
            },
            showSearch = true,
            darkModeConfig = darkModeConfig,
            notificationsEnabled = notificationsEnabled,
            onDarkModeToggle = { wordPressViewModel.toggleDarkMode() },
            onNotificationsToggle = onNotificationsToggle,
            onSearchClick = { navController.navigate("search") },
            onBookmarksClick = { navController.navigate("bookmarks") },
            onDownloadsClick = { navController.navigate("downloads") }
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                wordPressViewModel.fetchPosts(true, page = 1, perPage = 10, forHome = true)
                wordPressViewModel.fetchNewsletters(perPage = 1)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner Ad
                item {
                    BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                // Newsletter Section
                item {
                    Text(
                        text = "Latest Newsletter",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gilroy,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF3C8E3E)
                        ),
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
                                NewsletterItem(post = newsletter, navController = navController, windowSize = windowSize, isDarkMode = isDarkMode)
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
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gilroy,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF3C8E3E)
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                when (val state = uiState) {
                    is PostUiState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is PostUiState.Success -> {
                        val displayedPosts = state.posts.take(10)
                        if (displayedPosts.isEmpty()) {
                            item { Text("No blog posts available.", modifier = Modifier.padding(16.dp)) }
                        } else {
                            if (isCompact) {
                                items(displayedPosts, key = { it.id }) { post ->
                                    PostItem(post = post, navController = navController, isDarkMode = isDarkMode)
                                }
                            } else {
                                // Replaced LazyVerticalGrid with custom PostGrid composable for non-compact layout
                                item {
                                    PostGrid(posts = displayedPosts, navController = navController, columns = 2, isDarkMode = isDarkMode)
                                }
                            }
                        }
                    }
                    is PostUiState.Error -> {
                         item {
                            Column(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = Gilroy
                            ),
                            color = Color.Gray
                        )
                        Text(
                            text = "© ${Calendar.getInstance().get(Calendar.YEAR)} DRPS PHCA",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = Gilroy
                            ),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                val encodedUrl = Uri.encode("https://legal.drpsphca.com/terms")
                                navController.navigate("webview/$encodedUrl")
                            }) {
                                Text(
                                    text = "Terms of Use",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Gilroy
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            TextButton(onClick = {
                                val encodedUrl = Uri.encode("https://legal.drpsphca.com/privacy")
                                navController.navigate("webview/$encodedUrl")
                            }) {
                                Text(
                                    text = "Privacy Policy",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Gilroy
                                    )
                                )
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
fun BlogScreen(
    navController: NavController, 
    wordPressViewModel: WordPressViewModel, 
    windowSize: WindowSize, 
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onNotificationsToggle: () -> Unit
) {
    val uiState by wordPressViewModel.uiState.collectAsState()
    val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    LaunchedEffect(Unit) {
        wordPressViewModel.fetchPosts(isRefreshing = false, page = 1, perPage = 20)
    }

    Column {
        TopBarWithActions(
            title = { 
                Text(
                    text = "Blog",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold
                    )
                ) 
            },
            showSearch = true,
            darkModeConfig = darkModeConfig,
            notificationsEnabled = notificationsEnabled,
            onDarkModeToggle = { wordPressViewModel.toggleDarkMode() },
            onNotificationsToggle = onNotificationsToggle,
            onSearchClick = { navController.navigate("search") },
            onBookmarksClick = { navController.navigate("bookmarks") },
            onDownloadsClick = { navController.navigate("downloads") }
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                wordPressViewModel.fetchPosts(true, page = 1, perPage = 20)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                // ... same states as before
                is PostUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PostUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Text("No blog posts available.", modifier = Modifier.padding(16.dp))
                    } else {
                        if (isCompact) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                                }
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(post = post, navController = navController, isDarkMode = isDarkMode)
                                }
                                item {
                                    if (state.hasMore) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Button(onClick = { wordPressViewModel.fetchNextPage() }) {
                                                    Text("Load More")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                         else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                                }
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(post = post, navController = navController, isDarkMode = isDarkMode)
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    if (state.hasMore) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Button(onClick = { wordPressViewModel.fetchNextPage() }) {
                                                    Text("Load More")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsletterScreen(
    navController: NavController, 
    wordPressViewModel: WordPressViewModel, 
    windowSize: WindowSize, 
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onNotificationsToggle: () -> Unit
) {
    val newsletterUiState by wordPressViewModel.newsletterUiState.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    Column {
        TopBarWithActions(
            title = { 
                Text(
                    text = "Newsletter",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold
                    )
                ) 
            },
            showSearch = true,
            darkModeConfig = darkModeConfig,
            notificationsEnabled = notificationsEnabled,
            onDarkModeToggle = { wordPressViewModel.toggleDarkMode() },
            onNotificationsToggle = onNotificationsToggle,
            onSearchClick = { navController.navigate("search") },
            onBookmarksClick = { navController.navigate("bookmarks") },
            onDownloadsClick = { navController.navigate("downloads") }
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            // ... same items as before

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
                        if (isCompact) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(state.newsletters, key = { it.id }) { newsletter ->
                                    NewsletterItem(post = newsletter, navController = navController, windowSize = windowSize, isDarkMode = isDarkMode)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.newsletters, key = { it.id }) { newsletter ->
                                    NewsletterItem(post = newsletter, navController = navController, windowSize = windowSize, isDarkMode = isDarkMode)
                                }
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
fun NewsletterItem(post: PostItemUiModel, navController: NavController, windowSize: WindowSize, isDarkMode: Boolean = false) {
    val isCompact = windowSize == WindowSize.COMPACT
    val imageSource = post.localImageUrl ?: post.imageUrl
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { navController.navigate("postdetail/${post.id}") },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE6E6E6)
        )
    ) {
        if (isCompact) {
            Column {
                if (!imageSource.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = post.plainTitle,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.plainTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gilroy,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF025CA1)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post.formattedDate, 
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (!imageSource.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = post.plainTitle,
                        modifier = Modifier.width(250.dp).height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.plainTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gilroy,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF025CA1)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post.formattedDate, 
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(url: String) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            // WARNING: Using setJavaScriptEnabled(true) can introduce XSS vulnerabilities.
            // Review carefully if JavaScript is strictly required for this WebView.
            // settings.javaScriptEnabled = true // Removed to fix warning
            webViewClient = WebViewClient()
            loadUrl(url)
        }
    }, update = { webView ->
        webView.loadUrl(url)
    })
}

// Removed PostList function as it was never used.

@Composable
fun PostItem(
    post: PostItemUiModel, 
    navController: NavController, 
    highlightQuery: String? = null,
    showTags: Boolean = true,
    isDarkMode: Boolean = false
) {
    val imageSource = post.localImageUrl ?: post.imageUrl
    Card(
        modifier = Modifier
            .padding(8.dp)
            .widthIn(max = 500.dp)
            .clickable { navController.navigate("postdetail/${post.id}") },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE6E6E6)
        )
    ) {
        Column {
            if (!imageSource.isNullOrEmpty()) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = post.plainTitle,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                val annotatedTitle = if (highlightQuery.isNullOrEmpty()) {
                    buildAnnotatedString { append(post.plainTitle) }
                } else {
                    getHighlightedText(post.plainTitle, highlightQuery)
                }

                Text(
                    text = annotatedTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = Gilroy,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF025CA1)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.formattedDate, 
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.plainExcerpt, 
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Normal
                    )
                )
                
                if (showTags && post.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(post.tags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0181E5),
                                modifier = Modifier.clickable { navController.navigate("tag/${tag}") }
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = Gilroy,
                                        fontWeight = FontWeight.Medium
                                    ),
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

fun getHighlightedText(text: String, query: String): AnnotatedString {
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var currentIndex = 0
        while (currentIndex < text.length) {
            val index = lowerText.indexOf(lowerQuery, currentIndex)
            if (index == -1) {
                append(text.substring(currentIndex))
                break
            } else {
                append(text.substring(currentIndex, index))
                withStyle(style = SpanStyle(background = Color(0xFFFF6B2D), color = Color.White)) {
                    append(text.substring(index, index + query.length))
                }
                currentIndex = index + query.length
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, wordPressViewModel: WordPressViewModel, isDarkMode: Boolean) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    val uiState by wordPressViewModel.uiState.collectAsState()

    Column {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { 
                        Text(
                            text = "Search Blog Posts...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = Gilroy,
                                fontWeight = FontWeight.Normal
                            )
                        ) 
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Normal
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        appliedQuery = query
                        wordPressViewModel.fetchPosts(isRefreshing = true, page = 1, searchQuery = query)
                    })
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        // Results
        when (val state = uiState) {
            is PostUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is PostUiState.Success -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.posts) { post ->
                        PostItem(
                            post = post, 
                            navController = navController, 
                            highlightQuery = appliedQuery,
                            showTags = false,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
            is PostUiState.Error -> Text(state.message, modifier = Modifier.padding(16.dp))
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(navController: NavController, wordPressViewModel: WordPressViewModel, isDarkMode: Boolean) {
    val posts by wordPressViewModel.bookmarkedPosts.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Bookmarks",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No bookmarks yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(posts) { post ->
                    PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(navController: NavController, wordPressViewModel: WordPressViewModel, isDarkMode: Boolean) {
    val posts by wordPressViewModel.downloadedPosts.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
         if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No downloads yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(posts) { post ->
                    PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(tagName: String, navController: NavController, wordPressViewModel: WordPressViewModel, isDarkMode: Boolean) {
    val uiState by wordPressViewModel.uiState.collectAsState()
    LaunchedEffect(tagName) {
        wordPressViewModel.fetchPosts(isRefreshing = true, page = 1, tag = tagName)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Tag: $tagName",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is PostUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is PostUiState.Success -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.posts) { post ->
                            PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                        }
                    }
                }
                is PostUiState.Error -> Text(state.message, modifier = Modifier.padding(16.dp))
                else -> {}
            }
        }
    }
}

@Composable
fun PostGrid(posts: List<PostItemUiModel>, navController: NavController, columns: Int, isDarkMode: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        posts.chunked(columns).forEach { rowPosts ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPosts.forEach { post ->
                    Box(modifier = Modifier.weight(1f)) {
                        PostItem(post = post, navController = navController, isDarkMode = isDarkMode)
                    }
                }
                if (rowPosts.size < columns) {
                    repeat(columns - rowPosts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, @param:DrawableRes val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.phcaapp_home)
    object Blog : Screen("blog", "Blog", R.drawable.phcaapp_blog)
    object Newsletter : Screen("newsletter", "Newsletter", R.drawable.phcaapp_newsletter)
}

@Composable
fun BottomNavigationBar(navController: NavController, isDarkMode: Boolean) {
    val items = listOf(Screen.Home, Screen.Blog, Screen.Newsletter)
    val selectedColors = mapOf(
        Screen.Home.route to Color(0xFF128FF1),
        Screen.Blog.route to Color(0xFF734CC2),
        Screen.Newsletter.route to Color(0xFFDA048F)
    )

    NavigationBar(
        containerColor = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFD4D4D4),
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val selectedColor = selectedColors[screen.route] ?: MaterialTheme.colorScheme.primary
            val tint = if (isSelected) selectedColor else if (isDarkMode) Color.LightGray else Color(0xFF888888)

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
                label = { 
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (isSelected) selectedColor else if (isDarkMode) Color.LightGray else Color(0xFF888888)
                    ) 
                },
                icon = {
                    Image(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.label,
                        colorFilter = ColorFilter.tint(tint),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = selectedColor,
                    unselectedTextColor = if (isDarkMode) Color.LightGray else Color(0xFF888888),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
