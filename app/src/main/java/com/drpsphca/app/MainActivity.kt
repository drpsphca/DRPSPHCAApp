
package com.drpsphca.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.drpsphca.app.BuildConfig
import com.drpsphca.app.data.pbb.AnnouncementPost
import com.drpsphca.app.ui.WindowSize
import com.drpsphca.app.ui.rememberWindowSizeClass
import com.drpsphca.app.ui.screens.PostDetailScreen
import com.drpsphca.app.ui.screens.pbb.PbbUpdatesScreen
import com.drpsphca.app.ui.theme.DRPSPHCATheme
import com.drpsphca.app.ui.theme.Gilroy
import com.drpsphca.app.ui.theme.NotoSerif
import com.drpsphca.app.ui.viewmodel.NewsletterUiState
import com.drpsphca.app.ui.viewmodel.PbbViewModel
import com.drpsphca.app.ui.viewmodel.PostDetailUiModel
import com.drpsphca.app.ui.viewmodel.PostDetailUiState
import com.drpsphca.app.ui.viewmodel.PostItemUiModel
import com.drpsphca.app.ui.viewmodel.PostUiState
import com.drpsphca.app.ui.viewmodel.WordPressViewModel
import com.drpsphca.app.util.AppUpdateHelper
import com.drpsphca.app.util.AppUpdateHelperImpl
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateHelper: AppUpdateHelper

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
            subscribeToPbbTopics()
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

    private fun ensureFirebaseInitialized() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setProjectId("device-streaming-bafa1723")
                    .setApplicationId("1:210376071919:android:1cf1a5c506cd78d6a8191a")
                    .setApiKey("AIzaSyAFe5bXbQMeDVnNqnUjS6h1dyaAvedc0Bk")
                    .setStorageBucket("device-streaming-bafa1723.firebasestorage.app")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize FirebaseApp", e)
        }
    }

    private fun subscribeToPbbTopics() {
        ensureFirebaseInitialized()
        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
        FirebaseMessaging.getInstance().subscribeToTopic("nominations")
        FirebaseMessaging.getInstance().subscribeToTopic("evictions")
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "pbb_updates",
                "PBB Updates",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun schedulePbbBackgroundWorker() {
        val pbbWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.drpsphca.app.worker.PbbBackgroundWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "pbb_background_check",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            pbbWorkRequest
        )
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
        ensureFirebaseInitialized()

        createNotificationChannels()
        schedulePbbBackgroundWorker()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                subscribeToPbbTopics()
            }
        } else {
            subscribeToPbbTopics()
        }
        
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
        
        // Initialize App Update Check (flavor-specific)
        appUpdateHelper = AppUpdateHelperImpl(this)
        appUpdateHelper.checkForUpdates(this)
        
        setContent {
            val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
            val darkTheme = when (darkModeConfig) {
                WordPressViewModel.DarkModeConfig.ON -> true
                WordPressViewModel.DarkModeConfig.OFF -> false
                WordPressViewModel.DarkModeConfig.AUTO -> isSystemInDarkTheme()
            }
            
            DRPSPHCATheme(darkTheme = darkTheme) {
                appUpdateHelper.UpdateDialog(isDarkMode = darkTheme)
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
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onNotificationsToggle: () -> Unit
) {
    val isDarkMode = when (darkModeConfig) {
        WordPressViewModel.DarkModeConfig.ON -> true
        WordPressViewModel.DarkModeConfig.OFF -> false
        WordPressViewModel.DarkModeConfig.AUTO -> isSystemInDarkTheme()
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 0.dp),
        modifier = Modifier.padding(8.dp),
        containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
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
                    text = "Contact Us",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal
                )
            },
            onClick = {
                onContactUsClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phcaapp_contact),
                    contentDescription = "Contact Us",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = "Rate This App",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal
                )
            },
            onClick = {
                onRateAppClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phcaapp_star),
                    contentDescription = "Rate This App",
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
    showTvalLogo: Boolean = false,
    showPbbLogo: Boolean = false,
    darkModeConfig: WordPressViewModel.DarkModeConfig = WordPressViewModel.DarkModeConfig.AUTO,
    notificationsEnabled: Boolean = false,
    onDarkModeToggle: () -> Unit = {},
    onNotificationsToggle: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onContactUsClick: () -> Unit = {},
    onRateAppClick: () -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isOnline = remember(showMoreMenu) { isOnline(context) }

    CenterAlignedTopAppBar(
        title = {
            if (showTvalLogo) {
                Image(
                    painter = painterResource(id = R.drawable.tvallogo),
                    contentDescription = "TVAL Logo",
                    modifier = Modifier.height(40.dp)
                )
            } else if (showPbbLogo) {
                Image(
                    painter = painterResource(id = R.drawable.pbbupd),
                    contentDescription = "PBB Updates Logo",
                    modifier = Modifier.height(40.dp)
                )
            } else {
                title()
            }
        },
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
                    onContactUsClick = onContactUsClick,
                    onRateAppClick = onRateAppClick,
                    onDarkModeToggle = onDarkModeToggle,
                    onNotificationsToggle = onNotificationsToggle
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contact Us",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "DRPSPHCA.com Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DRPS PHCA App, created by the DRPS Philippine Commercial Archives (DRPS PHCA)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Main Email Address: hello@drpsphca.com",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hello@drpsphca.com")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Press Contact: telltofree@drpsphca.com",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:telltofree@drpsphca.com")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "App Feedback: telltophcaapp@drpsphca.com",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:telltophcaapp@drpsphca.com")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DRPS PHCA TVAL: telltotval@drpsphca.com",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:telltotval@drpsphca.com")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Phone (SMS/Viber): +63 993 733 7082",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:+639937337082")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            val annotatedString = buildAnnotatedString {
                append("App content are powered by ")
                withLink(
                    LinkAnnotation.Url(
                        url = "https://drpsphca.com",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append("DRPSPHCA.com")
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private fun requestReview(context: Context) {
    if (BuildConfig.FLAVOR == "googlePlay") {
        val manager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                if (context is Activity) {
                    val flow = manager.launchReviewFlow(context, reviewInfo)
                    flow.addOnCompleteListener { 
                        // Review flow finished
                    }
                }
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
            }
        }
    } else if (BuildConfig.FLAVOR == "huawei") {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("appmarket://details?id=${context.packageName}"))
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://appgallery.huawei.com/app/C${context.packageName}"))
            context.startActivity(intent)
        }
    }
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
    var isFullscreen by remember { mutableStateOf(false) }
    val pbbViewModel: PbbViewModel = viewModel()
    var selectedPbbAnnouncement by remember { mutableStateOf<AnnouncementPost?>(null) }

    val navigateToPostId by wordPressViewModel.navigateToPostId.collectAsState()
    LaunchedEffect(navigateToPostId) {
        navigateToPostId?.let { postId ->
            navController.navigate("postdetail/$postId")
            wordPressViewModel.onNotificationPostIdHandled()
        }
    }

    val navigateToPbb by wordPressViewModel.navigateToPbb.collectAsState()
    LaunchedEffect(navigateToPbb) {
        navigateToPbb?.let { seasonId ->
            if (seasonId != "default") {
                pbbViewModel.selectSeason(seasonId)
            }
            pbbViewModel.selectSection(com.drpsphca.app.ui.viewmodel.PbbSection.LIVE_UPDATES)
            pbbViewModel.selectLiveSubTab(com.drpsphca.app.ui.viewmodel.LiveSubTab.OVERVIEW)
            navController.navigate("pbb_updates")
            wordPressViewModel.onPbbNavigationHandled()
        }
    }

    val currentActivity = context as? Activity
    val currentIntent = currentActivity?.intent
    LaunchedEffect(currentIntent) {
        val fcmType = currentIntent?.getStringExtra("fcm_type")
        val fcmSeason = currentIntent?.getStringExtra("fcm_season")
        val fcmUrl = currentIntent?.getStringExtra("fcm_url")

        if (fcmType != null) {
            if (!fcmSeason.isNullOrEmpty()) {
                pbbViewModel.selectSeason(fcmSeason)
            }
            when (fcmType) {
                "nomination", "eviction" -> {
                    pbbViewModel.selectSection(com.drpsphca.app.ui.viewmodel.PbbSection.LIVE_UPDATES)
                    pbbViewModel.selectLiveSubTab(com.drpsphca.app.ui.viewmodel.LiveSubTab.NOMINATIONS)
                    navController.navigate("pbb_updates")
                }
                "announcement" -> {
                    pbbViewModel.selectSection(com.drpsphca.app.ui.viewmodel.PbbSection.ANNOUNCEMENTS)
                    navController.navigate("pbb_updates")
                }
            }
            currentIntent.removeExtra("fcm_type")
            currentIntent.removeExtra("fcm_season")
            currentIntent.removeExtra("fcm_url")
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = setOf(Screen.Home.route, Screen.Blog.route, Screen.Newsletter.route, Screen.TVAdListings.route, Screen.PbbUpdates.route)
    val windowSize = rememberWindowSizeClass()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute in bottomBarRoutes && !isFullscreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BottomNavigationBar(navController = navController, isDarkMode = isDarkMode)
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) },
            modifier = Modifier.padding(paddingValues)
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
                    },
                    onContactUsClick = {
                        navController.navigate("contactus")
                    },
                    onRateAppClick = {
                        requestReview(context)
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
                    },
                    onContactUsClick = {
                        navController.navigate("contactus")
                    },
                    onRateAppClick = {
                        requestReview(context)
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
                    },
                    onContactUsClick = {
                        navController.navigate("contactus")
                    },
                    onRateAppClick = {
                        requestReview(context)
                    }
                )
            }
            composable("tval") {
                TVAdListingsScreen(
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
                    },
                    onContactUsClick = {
                        navController.navigate("contactus")
                    },
                    onRateAppClick = {
                        requestReview(context)
                    }
                )
            }
            composable("pbb_updates") {
                PbbUpdatesMainScreen(
                    navController = navController,
                    pbbViewModel = pbbViewModel,
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
                                FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
                            }
                        } else {
                            wordPressViewModel.setNotificationsEnabled(false)
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_posts")
                        }
                    },
                    onContactUsClick = {
                        navController.navigate("contactus")
                    },
                    onRateAppClick = {
                        requestReview(context)
                    },
                    onAnnouncementClick = { announcement ->
                        selectedPbbAnnouncement = announcement
                        navController.navigate("pbb_announcement_detail")
                    }
                )
            }
            composable("pbb_announcement_detail") {
                PbbAnnouncementDetailRoute(
                    announcement = selectedPbbAnnouncement,
                    navController = navController,
                    isDarkMode = isDarkMode
                )
            }
            composable(
                "postdetail/{postId}?isTval={isTval}&isAppx={isAppx}",
                arguments = listOf(
                    navArgument("postId") { type = NavType.IntType },
                    navArgument("isTval") { type = NavType.BoolType; defaultValue = false },
                    navArgument("isAppx") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId")
                val isTval = backStackEntry.arguments?.getBoolean("isTval") ?: false
                val isAppx = backStackEntry.arguments?.getBoolean("isAppx") ?: false
                PostDetailRoute(
                    wordPressViewModel = wordPressViewModel,
                    navController = navController,
                    postId = postId,
                    isDarkMode = isDarkMode,
                    isTVAL = isTval,
                    isAppx = isAppx,
                    isFullscreen = isFullscreen,
                    onFullscreenToggle = { isFullscreen = it }
                )
            }
            composable("webview/{url}") { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: "about:blank"
                WebViewScreen(url = url)
            }
            composable("search?mode={mode}") { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "blog"
                SearchScreen(
                    navController = navController, 
                    wordPressViewModel = wordPressViewModel, 
                    windowSize = windowSize, 
                    isDarkMode = isDarkMode,
                    searchMode = mode
                )
            }
            composable("bookmarks") {
                BookmarksScreen(navController = navController, wordPressViewModel = wordPressViewModel, windowSize = windowSize, isDarkMode = isDarkMode)
            }
            composable("downloads") {
                DownloadsScreen(navController = navController, wordPressViewModel = wordPressViewModel, windowSize = windowSize, isDarkMode = isDarkMode)
            }
            composable("tag/{tagName}") { backStackEntry ->
                val tagName = backStackEntry.arguments?.getString("tagName") ?: ""
                TagScreen(tagName = tagName, navController = navController, wordPressViewModel = wordPressViewModel, windowSize = windowSize, isDarkMode = isDarkMode)
            }
            composable("contactus") {
                ContactUsScreen(onBackClick = { navController.popBackStack() })
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
    isDarkMode: Boolean,
    isTVAL: Boolean = false,
    isAppx: Boolean = false,
    isFullscreen: Boolean,
    onFullscreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(postId, isTVAL, isAppx) {
        if (postId != null) {
            wordPressViewModel.fetchPost(postId, isTval = isTVAL, isAppx = isAppx)
        }
    }
    val detailUiState by wordPressViewModel.detailUiState.collectAsState()
    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (isTVAL) "TV Ad Listing" else if (isAppx) "Bite-sized" else "Post",
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
                        if (!isTVAL && detailUiState is PostDetailUiState.Success) {
                            val post = (detailUiState as PostDetailUiState.Success).post
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
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)) {
            when (val state = detailUiState) {
                is PostDetailUiState.Loading, PostDetailUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is PostDetailUiState.Success -> {
                    val isOffline = !isOnline(context)
                    PostDetailScreen(
                        post = state.post, 
                        isDarkMode = isDarkMode, 
                        isOffline = isOffline,
                        onTagClick = { tagName ->
                            val encodedTag = Uri.encode(tagName)
                            navController.navigate("tag/$encodedTag")
                        },
                        onFullscreenToggle = onFullscreenToggle
                    )
                }

                is PostDetailUiState.Error -> {
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
    onNotificationsToggle: () -> Unit,
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit
) {
    val uiState by wordPressViewModel.uiState.collectAsState()
    val newsletterUiState by wordPressViewModel.newsletterUiState.collectAsState()
    val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    LaunchedEffect(Unit) {
        wordPressViewModel.fetchPosts(isRefreshing = false, page = 1, perPage = 10, forHome = true)
    }

    Column(modifier = Modifier) {
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
            onDownloadsClick = { navController.navigate("downloads") },
            onContactUsClick = onContactUsClick,
            onRateAppClick = onRateAppClick
        )
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    wordPressViewModel.fetchPosts(true, page = 1, perPage = 10, forHome = true)
                    wordPressViewModel.fetchNewsletters(perPage = 1)
                },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Banner Ad
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                // Newsletter Section
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Latest Newsletter",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gilroy,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF3C8E3E)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Latest Blog Posts",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gilroy,
                            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF3C8E3E)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                when (val state = uiState) {
                    is PostUiState.Loading -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is PostUiState.Success -> {
                        val displayedPosts = state.posts.take(10)
                        if (displayedPosts.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { 
                                Text("No blog posts available.", modifier = Modifier.padding(16.dp)) 
                            }
                        } else {
                            items(displayedPosts, key = { it.id }) { post ->
                                PostItem(post = post, navController = navController, isDarkMode = isDarkMode)
                            }
                        }
                    }
                    is PostUiState.Error -> {
                         item(span = { GridItemSpan(maxLineSpan) }) {
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
                    is PostUiState.Idle -> { /* Do Nothing */ }
                }

                // Footer Section
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Version ${com.drpsphca.app.BuildConfig.VERSION_NAME}",
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
                                    ),
                                    color = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF353535)
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
                                    ),
                                    color = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF353535)
                                )
                            }
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
    onNotificationsToggle: () -> Unit,
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit
) {
    val selectedSection by wordPressViewModel.selectedBlogSection.collectAsState()
    val uiState by wordPressViewModel.uiState.collectAsState()
    val appxUiState by wordPressViewModel.appxUiState.collectAsState()
    val isRefreshing by wordPressViewModel.isRefreshing.collectAsState()
    val appxIsRefreshing by wordPressViewModel.appxIsRefreshing.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    LaunchedEffect(selectedSection) {
        if (selectedSection == WordPressViewModel.BlogSection.BLOGS) {
            if (uiState !is PostUiState.Success) {
                wordPressViewModel.fetchPosts(isRefreshing = false, page = 1, perPage = 20)
            }
        } else {
            if (appxUiState !is PostUiState.Success) {
                wordPressViewModel.fetchAppxPosts(isRefreshing = false, page = 1, perPage = 20)
            }
        }
    }

    Column(modifier = Modifier) {
        TopBarWithActions(
            title = {
                Image(
                    painter = painterResource(id = R.drawable.freeblog),
                    contentDescription = "Free Blog Logo",
                    modifier = Modifier.height(40.dp)
                )
            },
            showSearch = true,
            darkModeConfig = darkModeConfig,
            notificationsEnabled = notificationsEnabled,
            onDarkModeToggle = { wordPressViewModel.toggleDarkMode() },
            onNotificationsToggle = onNotificationsToggle,
            onSearchClick = { 
                val mode = if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) "appx" else "blog"
                navController.navigate("search?mode=$mode") 
            },
            onBookmarksClick = { navController.navigate("bookmarks") },
            onDownloadsClick = { navController.navigate("downloads") },
            onContactUsClick = onContactUsClick,
            onRateAppClick = onRateAppClick
        )

        BlogSectionSelector(
            selectedSection = selectedSection,
            onSectionSelected = { wordPressViewModel.selectBlogSection(it) },
            isDarkMode = isDarkMode
        )

        val currentIsRefreshing = if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) appxIsRefreshing else isRefreshing
        val currentUiState = if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) appxUiState else uiState

        PullToRefreshBox(
            isRefreshing = currentIsRefreshing,
            onRefresh = {
                if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) {
                    wordPressViewModel.fetchAppxPosts(true, page = 1, perPage = 20)
                } else {
                    wordPressViewModel.fetchPosts(true, page = 1, perPage = 20)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = currentUiState) {
                is PostUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PostUiState.Success -> {
                    val emptyMsg = if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) "No Bite-sized posts available." else "No blog posts available."
                    if (state.posts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(emptyMsg, modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        if (isCompact) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                                }
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(
                                        post = post, 
                                        navController = navController, 
                                        isDarkMode = isDarkMode,
                                        onPostClick = if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) {
                                            { navController.navigate("postdetail/${post.id}?isAppx=true") }
                                        } else null
                                    )
                                }
                                item {
                                    if (state.hasMore) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Button(onClick = { 
                                                    if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) {
                                                        wordPressViewModel.fetchAppxNextPage()
                                                    } else {
                                                        wordPressViewModel.fetchNextPage()
                                                    }
                                                }) {
                                                    Text("Load More")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                                }
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(
                                        post = post, 
                                        navController = navController, 
                                        isDarkMode = isDarkMode,
                                        onPostClick = if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) {
                                            { navController.navigate("postdetail/${post.id}?isAppx=true") }
                                        } else null
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    if (state.hasMore) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Button(onClick = { 
                                                    if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) {
                                                        wordPressViewModel.fetchAppxNextPage()
                                                    } else {
                                                        wordPressViewModel.fetchNextPage()
                                                    }
                                                }) {
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
                        Button(onClick = { 
                            if (selectedSection == WordPressViewModel.BlogSection.BITE_SIZED) {
                                wordPressViewModel.fetchAppxPosts(false, page = 1, perPage = 20)
                            } else {
                                wordPressViewModel.fetchPosts(false, page = 1, perPage = 20)
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                is PostUiState.Idle -> { /* Do Nothing */ }
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
    onNotificationsToggle: () -> Unit,
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit
) {
    val newsletterUiState by wordPressViewModel.newsletterUiState.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    Column(modifier = Modifier) {
        TopBarWithActions(
            title = {
                Image(
                    painter = painterResource(id = R.drawable.freenewsletter),
                    contentDescription = "Free Newsletter Logo",
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
            onDownloadsClick = { navController.navigate("downloads") },
            onContactUsClick = onContactUsClick,
            onRateAppClick = onRateAppClick
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BannerAd(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            // ... same items as before

            Text(
                text = "Latest Newsletters",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = Gilroy
                ),
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
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(state.newsletters, key = { it.id }) { newsletter ->
                                    NewsletterItem(post = newsletter, navController = navController, windowSize = windowSize, isDarkMode = isDarkMode)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier.fillMaxSize()
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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                onPause()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }

    AndroidView(factory = { context ->
        WebView(context).apply {
            webViewRef = this
            // WARNING: Using setJavaScriptEnabled(true) can introduce XSS vulnerabilities.
            // Review carefully if JavaScript is strictly required for this WebView.
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val newUrl = request?.url?.toString() ?: return false
                    val uri = Uri.parse(newUrl)
                    if (uri.host == "drpsphca.com" || uri.host == "legal.drpsphca.com") {
                        // Keep drpsphca.com and legal.drpsphca.com links inside the WebView
                        return false
                    }
                    // If it's a different URL than the one we initially loaded and not our domain, open in browser
                    if (newUrl != url) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            // Fallback to internal loading if no app can handle it
                            return false
                        }
                    }
                    return false
                }
            }
            loadUrl(url)
        }
    }, update = { webView ->
        // Only load if the URL is different to avoid refreshing when the view updates
        // We use a tag to track the intended URL because webView.url might change due to redirects
        if (webView.tag != url) {
            webView.loadUrl(url)
            webView.tag = url
        }
    })
}

// Removed PostList function as it was never used.

@Composable
fun PostItem(
    post: PostItemUiModel, 
    navController: NavController, 
    highlightQuery: String? = null,
    showTags: Boolean = true,
    isDarkMode: Boolean = false,
    onPostClick: (() -> Unit)? = null
) {
    val imageSource = post.localImageUrl ?: post.imageUrl
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { 
                if (onPostClick != null) {
                    onPostClick()
                } else {
                    navController.navigate("postdetail/${post.id}") 
                }
            },
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
fun TVAdListingsScreen(
    navController: NavController,
    wordPressViewModel: WordPressViewModel,
    windowSize: WindowSize,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onNotificationsToggle: () -> Unit,
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit
) {
    val tvalUiState by wordPressViewModel.tvalUiState.collectAsState()
    val isRefreshing by wordPressViewModel.tvalIsRefreshing.collectAsState()
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()
    val selectedDate by wordPressViewModel.selectedTvalDate.collectAsState()
    val isCompact = windowSize == WindowSize.COMPACT

    LaunchedEffect(Unit) {
        if (tvalUiState !is PostUiState.Success) {
            wordPressViewModel.fetchTVALPosts(isRefreshing = false, page = 1)
        }
    }

    Column(modifier = Modifier) {
        TopBarWithActions(
            title = { Text("TV Ad Listings") }, // Will be overridden by logo due to showTvalLogo = true
            showTvalLogo = true,
            showSearch = true,
            darkModeConfig = darkModeConfig,
            notificationsEnabled = notificationsEnabled,
            onDarkModeToggle = { wordPressViewModel.toggleDarkMode() },
            onNotificationsToggle = onNotificationsToggle,
            onSearchClick = { navController.navigate("search?mode=tval") },
            onBookmarksClick = { navController.navigate("bookmarks") },
            onDownloadsClick = { navController.navigate("downloads") },
            onContactUsClick = onContactUsClick,
            onRateAppClick = onRateAppClick
        )

        DateSelector(
            dates = wordPressViewModel.getTvalDateRange(),
            selectedDate = selectedDate,
            onDateSelected = { wordPressViewModel.selectTvalDate(it) },
            isDarkMode = isDarkMode
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                wordPressViewModel.fetchTVALPosts(true, page = 1, date = selectedDate)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = tvalUiState) {
                is PostUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PostUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No TV Ad Listings available for this selection.", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        if (isCompact) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(
                                        post = post, 
                                        navController = navController, 
                                        isDarkMode = isDarkMode,
                                        onPostClick = { navController.navigate("postdetail/${post.id}?isTval=true") }
                                    )
                                }
                                item {
                                    if (state.hasMore) {
                                        Button(onClick = { wordPressViewModel.fetchTvalNextPage() }, modifier = Modifier.padding(16.dp)) {
                                            Text("Load More")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    PostItem(
                                        post = post, 
                                        navController = navController, 
                                        isDarkMode = isDarkMode,
                                        onPostClick = { navController.navigate("postdetail/${post.id}?isTval=true") }
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    if (state.hasMore) {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Button(onClick = { wordPressViewModel.fetchTvalNextPage() }, modifier = Modifier.padding(16.dp)) {
                                                Text("Load More")
                                            }
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
                        Button(onClick = { wordPressViewModel.fetchTVALPosts(false, page = 1, date = selectedDate) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PbbUpdatesMainScreen(
    navController: NavController,
    pbbViewModel: PbbViewModel,
    wordPressViewModel: WordPressViewModel,
    windowSize: WindowSize,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onNotificationsToggle: () -> Unit,
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit,
    onAnnouncementClick: (AnnouncementPost) -> Unit
) {
    val darkModeConfig by wordPressViewModel.darkModeConfig.collectAsState()

    Column(modifier = Modifier) {
        TopBarWithActions(
            title = {},
            showPbbLogo = true,
            showSearch = false,
            darkModeConfig = darkModeConfig,
            notificationsEnabled = notificationsEnabled,
            onDarkModeToggle = { wordPressViewModel.toggleDarkMode() },
            onNotificationsToggle = onNotificationsToggle,
            onBookmarksClick = { navController.navigate("bookmarks") },
            onDownloadsClick = { navController.navigate("downloads") },
            onContactUsClick = onContactUsClick,
            onRateAppClick = onRateAppClick
        )

        PbbUpdatesScreen(
            pbbViewModel = pbbViewModel,
            navController = navController,
            windowSize = windowSize,
            isDarkMode = isDarkMode,
            onAnnouncementClick = onAnnouncementClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PbbAnnouncementDetailRoute(
    announcement: AnnouncementPost?,
    navController: NavController,
    isDarkMode: Boolean
) {
    if (announcement == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Announcement not found.", fontFamily = Gilroy, fontWeight = FontWeight.Bold)
        }
        return
    }

    val context = LocalContext.current
    val formattedDateStr = remember(announcement.date) {
        com.drpsphca.app.ui.screens.pbb.formatPbbPublishedDate(announcement.date)
    }

    val detailUiModel = remember(announcement, formattedDateStr) {
        PostDetailUiModel(
            id = announcement.url.hashCode(),
            formattedDate = formattedDateStr,
            plainTitle = announcement.title ?: "PBB Announcement",
            plainExcerpt = announcement.excerpt ?: "",
            content = announcement.contentHtml ?: announcement.contentText ?: "",
            imageUrl = announcement.featuredImage,
            tags = emptyList(),
            link = announcement.url ?: "",
            category = "PBB Updates"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PBB Announcement",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.phcaapp_left),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sharePost(context, detailUiModel.plainTitle, detailUiModel.link)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.phcaapp_share),
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            PostDetailScreen(
                post = detailUiModel,
                isDarkMode = isDarkMode,
                isOffline = !isOnline(context)
            )
        }
    }
}

@Composable
fun BlogSectionSelector(
    selectedSection: WordPressViewModel.BlogSection,
    onSectionSelected: (WordPressViewModel.BlogSection) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BlogFilterChip(
            selected = selectedSection == WordPressViewModel.BlogSection.BLOGS,
            onClick = { onSectionSelected(WordPressViewModel.BlogSection.BLOGS) },
            label = "Blogs",
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
        BlogFilterChip(
            selected = selectedSection == WordPressViewModel.BlogSection.BITE_SIZED,
            onClick = { onSectionSelected(WordPressViewModel.BlogSection.BITE_SIZED) },
            label = "Bite-sized",
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BlogFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFF0C7A0B) else if (isDarkMode) Color.DarkGray else Color.LightGray,
        contentColor = if (selected) Color.White else if (isDarkMode) Color.White else Color.Black,
        modifier = modifier.height(32.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Gilroy,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
fun DateSelector(
    dates: List<Pair<String, String>>,
    selectedDate: String?,
    onDateSelected: (String?) -> Unit,
    isDarkMode: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedDate == null,
                onClick = { onDateSelected(null) },
                label = { Text("All Dates") },
                isDarkMode = isDarkMode
            )
        }
        items(dates) { (dateStr, displayStr) ->
            FilterChip(
                selected = selectedDate == dateStr,
                onClick = { onDateSelected(dateStr) },
                label = { Text(displayStr) },
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFF0050DB) else if (isDarkMode) Color.DarkGray else Color.LightGray,
        contentColor = if (selected) Color.White else if (isDarkMode) Color.White else Color.Black,
        modifier = Modifier.height(32.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            label()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController, 
    wordPressViewModel: WordPressViewModel, 
    windowSize: WindowSize, 
    isDarkMode: Boolean,
    searchMode: String = "blog"
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var searchPerformed by remember { mutableStateOf(false) }
    var activeBlogSection by remember {
        mutableStateOf(if (searchMode == "appx") WordPressViewModel.BlogSection.BITE_SIZED else WordPressViewModel.BlogSection.BLOGS)
    }

    val handleBack = {
        if (searchPerformed || appliedQuery.isNotEmpty()) {
            wordPressViewModel.reloadAllPosts()
        }
        navController.popBackStack()
    }

    BackHandler {
        handleBack()
    }

    val uiState by (
        when {
            searchMode == "tval" -> wordPressViewModel.tvalUiState
            activeBlogSection == WordPressViewModel.BlogSection.BITE_SIZED -> wordPressViewModel.appxUiState
            else -> wordPressViewModel.uiState
        }
    ).collectAsState()

    Column {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { 
                        Text(
                            text = when {
                                searchMode == "tval" -> "Search TV Ad Listings..."
                                activeBlogSection == WordPressViewModel.BlogSection.BITE_SIZED -> "Search Bite-sized Posts..."
                                else -> "Search Blog Posts..."
                            },
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
                        searchPerformed = true
                        when {
                            searchMode == "tval" -> wordPressViewModel.fetchTVALPosts(isRefreshing = true, page = 1, searchQuery = query)
                            activeBlogSection == WordPressViewModel.BlogSection.BITE_SIZED -> wordPressViewModel.fetchAppxPosts(isRefreshing = true, page = 1, searchQuery = query)
                            else -> wordPressViewModel.fetchPosts(isRefreshing = true, page = 1, searchQuery = query)
                        }
                    })
                )
            },
            navigationIcon = {
                IconButton(onClick = { handleBack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        )

        if (searchMode != "tval") {
            BlogSectionSelector(
                selectedSection = activeBlogSection,
                onSectionSelected = { section ->
                    activeBlogSection = section
                    if (appliedQuery.isNotEmpty()) {
                        if (section == WordPressViewModel.BlogSection.BITE_SIZED) {
                            wordPressViewModel.fetchAppxPosts(isRefreshing = true, page = 1, searchQuery = appliedQuery)
                        } else {
                            wordPressViewModel.fetchPosts(isRefreshing = true, page = 1, searchQuery = appliedQuery)
                        }
                    }
                },
                isDarkMode = isDarkMode
            )
        }

        // Results
        when (val state = uiState) {
            is PostUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is PostUiState.Success -> {
                if (windowSize == WindowSize.COMPACT) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.posts) { post ->
                            PostItem(
                                post = post, 
                                navController = navController, 
                                highlightQuery = appliedQuery,
                                showTags = false,
                                isDarkMode = isDarkMode,
                                onPostClick = {
                                    val suffix = when {
                                        searchMode == "tval" -> "?isTval=true"
                                        activeBlogSection == WordPressViewModel.BlogSection.BITE_SIZED -> "?isAppx=true"
                                        else -> ""
                                    }
                                    navController.navigate("postdetail/${post.id}$suffix")
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(state.posts) { post ->
                            PostItem(
                                post = post, 
                                navController = navController, 
                                highlightQuery = appliedQuery,
                                showTags = false,
                                isDarkMode = isDarkMode,
                                onPostClick = {
                                    val suffix = when {
                                        searchMode == "tval" -> "?isTval=true"
                                        activeBlogSection == WordPressViewModel.BlogSection.BITE_SIZED -> "?isAppx=true"
                                        else -> ""
                                    }
                                    navController.navigate("postdetail/${post.id}$suffix")
                                }
                            )
                        }
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
fun BookmarksScreen(navController: NavController, wordPressViewModel: WordPressViewModel, windowSize: WindowSize, isDarkMode: Boolean) {
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
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { padding ->
        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No bookmarks yet.")
            }
        } else {
            if (windowSize == WindowSize.COMPACT) {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(posts) { post ->
                        PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(posts) { post ->
                        PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(navController: NavController, wordPressViewModel: WordPressViewModel, windowSize: WindowSize, isDarkMode: Boolean) {
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
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { padding ->
         if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No downloads yet.")
            }
        } else {
            if (windowSize == WindowSize.COMPACT) {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(posts) { post ->
                        PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(posts) { post ->
                        PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(tagName: String, navController: NavController, wordPressViewModel: WordPressViewModel, windowSize: WindowSize, isDarkMode: Boolean) {
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
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is PostUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is PostUiState.Success -> {
                    if (windowSize == WindowSize.COMPACT) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.posts) { post ->
                                PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 300.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(state.posts) { post ->
                                PostItem(post = post, navController = navController, showTags = false, isDarkMode = isDarkMode)
                            }
                        }
                    }
                }
                is PostUiState.Error -> Text(state.message, modifier = Modifier.padding(16.dp))
                else -> {}
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, @param:DrawableRes val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.phcaapp_home)
    object Blog : Screen("blog", "Blog", R.drawable.phcaapp_blog)
    object Newsletter : Screen("newsletter", "Newsletter", R.drawable.phcaapp_newsletter)
    object TVAdListings : Screen("tval", "TVAL", R.drawable.phcaapp_tval)
    object PbbUpdates : Screen("pbb_updates", "PBB", R.drawable.phcaapp_pbb)
}

@Composable
fun BottomNavigationBar(navController: NavController, isDarkMode: Boolean) {
    val items = listOf(Screen.Home, Screen.Blog, Screen.Newsletter, Screen.TVAdListings, Screen.PbbUpdates)
    val selectedColors = mapOf(
        Screen.Home.route to Color(0xFF128FF1),
        Screen.Blog.route to Color(0xFF0C7A0B),
        Screen.Newsletter.route to Color(0xFF0C7A0B),
        Screen.TVAdListings.route to Color(0xFF0050DB),
        Screen.PbbUpdates.route to Color(0xFFBBA00C)
    )

    NavigationBar(
        containerColor = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFD4D4D4),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
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
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.label,
                        tint = tint,
                        modifier = Modifier.height(32.dp).padding(bottom = 0.dp)
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
