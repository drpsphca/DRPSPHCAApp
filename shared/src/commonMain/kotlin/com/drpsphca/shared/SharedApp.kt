package com.drpsphca.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import com.drpsphca.shared.resources.Res
import com.drpsphca.shared.resources.freeblog
import com.drpsphca.shared.resources.freenewsletter
import com.drpsphca.shared.resources.google_noto_serif
import com.drpsphca.shared.resources.google_noto_serif_bold
import com.drpsphca.shared.resources.google_noto_serif_bold_italic
import com.drpsphca.shared.resources.google_noto_serif_italic
import com.drpsphca.shared.resources.google_noto_serif_medium
import com.drpsphca.shared.resources.google_noto_serif_medium_italic
import com.drpsphca.shared.resources.logo
import com.drpsphca.shared.resources.pbbupd
import com.drpsphca.shared.resources.phcaapp_blog
import com.drpsphca.shared.resources.phcaapp_home
import com.drpsphca.shared.resources.phcaapp_newsletter
import com.drpsphca.shared.resources.phcaapp_pbb
import com.drpsphca.shared.resources.phcaapp_tval
import com.drpsphca.shared.resources.phca_gilroy_bold
import com.drpsphca.shared.resources.phca_gilroy_medium
import com.drpsphca.shared.resources.phca_gilroy_regular
import com.drpsphca.shared.resources.tag_background
import com.drpsphca.shared.resources.tvallogo
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private val PhcaNavy = Color(0xFF17345C)
private val PhcaGreen = Color(0xFF3C8E3E)
private val PhcaBlue = Color(0xFF025CA1)
private val PhcaGold = Color(0xFFBBA00C)
internal val LocalPhcaImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("The shared app image loader was not provided.")
}
private fun appTypography(gilroy: androidx.compose.ui.text.font.FontFamily) = Typography(
    bodyLarge = TextStyle(fontFamily = gilroy, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = gilroy, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = gilroy, fontSize = 12.sp, lineHeight = 16.sp),
    titleLarge = TextStyle(fontFamily = gilroy, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = gilroy, fontSize = 16.sp),
    headlineSmall = TextStyle(fontFamily = gilroy, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = gilroy, fontWeight = FontWeight.Bold, fontSize = 28.sp),
)

private data class AppSection(val title: String, val label: String)

private val sections = listOf(
    AppSection("Home", "Home"),
    AppSection("Blog", "Blog"),
    AppSection("Newsletter", "Newsletter"),
    AppSection("TV Ad Listings", "TVAL"),
    AppSection("PBB Updates", "PBB"),
)

private enum class PbbTab(val label: String) {
    OVERVIEW("Overview"),
    HOUSEMATES("Housemates"),
    NOMINATIONS("Nominations"),
    WEEKLY_TASKS("Weekly Tasks"),
}
private enum class PbbSection(val label: String) { LIVE("Live Updates"), ANNOUNCEMENTS("Announcements") }

@Composable
fun SharedApp() {
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        HttpClient(createPlatformHttpClientEngine()) {
                            defaultRequest {
                                header(HttpHeaders.UserAgent, "com.drpsphca.app")
                            }
                        },
                    ),
                )
            }
            .build()
    }

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var biteSized by rememberSaveable { mutableStateOf(false) }
    var pbbTab by rememberSaveable { mutableStateOf(PbbTab.OVERVIEW) }
    var pbbSection by rememberSaveable { mutableStateOf(PbbSection.LIVE) }
    val persistedPbbSeason = loadPersistedPbbSeason()
    var selectedSeason by rememberSaveable { mutableStateOf(persistedPbbSeason.orEmpty()) }
    var jumpToEvictions by remember { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchSubmitted by remember { mutableStateOf(false) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<Article>>(emptyList()) }
    var searchRequestId by remember { mutableIntStateOf(0) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var newsletterArticles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var pbbAnnouncements by remember { mutableStateOf<List<Article>>(emptyList()) }
    var board by remember { mutableStateOf<BoardIndex?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val repository = remember { ContentRepository(createPlatformHttpClientEngine()) }
    val section = sections[selectedSection]

    fun submitSearch() {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            searchSubmitted = true
            searchError = "Enter a search term."
            searchResults = emptyList()
            return
        }
        val requestId = ++searchRequestId
        searchSubmitted = true
        searchLoading = true
        searchError = null
        searchResults = emptyList()
        keyboardController?.hide()
        scope.launch {
            try {
                val results = repository.searchAllWordPress(query)
                if (requestId == searchRequestId) searchResults = results
            } catch (exception: Exception) {
                if (requestId == searchRequestId) {
                    searchError = exception.message
                        ?: "Unable to search posts. Check your connection and try again."
                }
            } finally {
                if (requestId == searchRequestId) searchLoading = false
            }
        }
    }

    fun loadContent(index: Int = selectedSection) {
        scope.launch {
            isLoading = true
            error = null
            if (index != 4) articles = emptyList()
            if (index == 0) newsletterArticles = emptyList()
            try {
                when (index) {
                    0 -> {
                        articles = repository.wordpressArticles(WordPressEndpoints.main, "blog", 10)
                        newsletterArticles = repository.wordpressArticles(WordPressEndpoints.main, "newsletter", 5)
                    }
                    1 -> articles = if (biteSized) {
                        repository.wordpressArticles(WordPressEndpoints.app, "app")
                    } else {
                        repository.wordpressArticles(WordPressEndpoints.main, "blog")
                    }
                    2 -> articles = repository.wordpressArticles(WordPressEndpoints.main, "newsletter")
                    3 -> articles = repository.wordpressArticles(WordPressEndpoints.tval)
                    4 -> {
                        val loadedBoard = repository.pbbBoard()
                        val apiPosts = repository.pbbAnnouncements()
                        val collab3Posts = repository.wordpressArticles(WordPressEndpoints.main, "collab3")
                        board = loadedBoard
                        pbbAnnouncements = mergePbbAnnouncements(apiPosts, collab3Posts)
                        val availableSeasonIds = loadedBoard.seasonList
                            .filter { it.available && it.id in loadedBoard.seasons }
                            .map { it.id }
                            .ifEmpty {
                                loadedBoard.seasons.filterValues { it.season?.available == true }.keys.toList()
                            }
                        if (selectedSeason !in availableSeasonIds) {
                            selectedSeason = availableSeasonIds.firstOrNull().orEmpty()
                            if (selectedSeason.isNotBlank()) persistPbbSeason(selectedSeason)
                        }
                    }
                }
            } catch (exception: Exception) {
                error = exception.message ?: "Unable to load content. Check your connection and try again."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedSection, refreshCount, if (selectedSection == 1) biteSized else null) {
        loadContent()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val payload = consumePendingNotificationRoute()
            if (payload != null) {
                try {
                    val route = Json { ignoreUnknownKeys = true }
                        .decodeFromString<PendingNotificationRoute>(payload)
                    val routeText = listOfNotNull(route.topic, route.type, route.channel)
                        .joinToString(" ")
                        .lowercase()
                    val channel = route.channel?.lowercase().orEmpty()
                    when {
                        "eviction" in routeText || "nomination" in routeText -> {
                            selectedSection = 4
                            pbbSection = PbbSection.LIVE
                            pbbTab = PbbTab.NOMINATIONS
                            jumpToEvictions = "eviction" in routeText
                            route.season?.let { season ->
                                selectedSeason = season
                                persistPbbSeason(season)
                            }
                        }
                        "announcement" in routeText || channel == "pbb" -> {
                            selectedSection = 4
                            pbbSection = PbbSection.ANNOUNCEMENTS
                            route.season?.let { season ->
                                selectedSeason = season
                                persistPbbSeason(season)
                            }
                        }
                        channel == "blog" -> {
                            selectedSection = 1
                            biteSized = false
                        }
                        channel == "newsletter" -> selectedSection = 2
                        channel == "tval" -> selectedSection = 3
                        channel == "app" -> {
                            selectedSection = 1
                            biteSized = true
                        }
                        else -> selectedSection = 4
                    }
                    searchVisible = false
                    searchSubmitted = false
                    selectedArticle = null
                    route.url?.let { postUrl ->
                        scope.launch {
                            try {
                                val article = when (channel) {
                                    "blog" -> repository.wordpressArticleByUrl(WordPressEndpoints.main, postUrl)
                                    "newsletter" -> repository.wordpressArticleByUrl(WordPressEndpoints.main, postUrl)
                                    "tval" -> repository.wordpressArticleByUrl(WordPressEndpoints.tval, postUrl)
                                    "app" -> repository.wordpressArticleByUrl(WordPressEndpoints.app, postUrl)
                                    "pbb" -> repository.pbbAnnouncements().firstOrNull { it.url == postUrl }
                                    else -> if ("pbbupdates.drpsphca.com" in postUrl) {
                                        repository.pbbAnnouncements().firstOrNull { it.url == postUrl }
                                    } else null
                                }
                                if (article != null) {
                                    selectedArticle = article
                                } else {
                                    error = "The notification post could not be found."
                                }
                            } catch (exception: Exception) {
                                error = exception.message
                                    ?: "Unable to open the notification. Check your connection and try again."
                            }
                        }
                    }
                } catch (exception: Exception) {
                    error = exception.message ?: "The notification could not be opened."
                }
            }
            delay(500)
        }
    }

    val currentArticles = if (selectedSection == 4 && pbbSection == PbbSection.ANNOUNCEMENTS) {
        pbbAnnouncements
    } else {
        articles
    }
    val darkMode = isSystemInDarkTheme()
    val gilroyFont = androidx.compose.ui.text.font.FontFamily(
        Font(Res.font.phca_gilroy_regular, FontWeight.Normal),
        Font(Res.font.phca_gilroy_medium, FontWeight.Medium),
        Font(Res.font.phca_gilroy_bold, FontWeight.Bold),
    )
    val notoSerifFont = androidx.compose.ui.text.font.FontFamily(
        Font(Res.font.google_noto_serif, FontWeight.Normal),
        Font(Res.font.google_noto_serif_medium, FontWeight.Medium),
        Font(Res.font.google_noto_serif_bold, FontWeight.Bold),
        Font(Res.font.google_noto_serif_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.google_noto_serif_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.google_noto_serif_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme(
            primary = Color(0xFF90CAF9),
            secondary = Color(0xFF9CCC9C),
            background = Color(0xFF121212),
            surface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFF292929),
        ) else androidx.compose.material3.lightColorScheme(
            primary = PhcaNavy,
            secondary = PhcaGreen,
            background = Color(0xFFF7F8FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFE6E6E6),
        ),
        typography = appTypography(gilroyFont),
    ) {
        CompositionLocalProvider(LocalPhcaImageLoader provides imageLoader) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.statusBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SectionLogo(selectedSection)
                            Box(Modifier.align(Alignment.CenterEnd)) {
                                TextButton(
                                    onClick = { moreMenuExpanded = true },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Menu",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                DropdownMenu(
                                    expanded = moreMenuExpanded,
                                    onDismissRequest = { moreMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        trailingIcon = {
                                            Icon(Icons.Filled.Refresh, contentDescription = null)
                                        },
                                        onClick = {
                                            refreshCount++
                                            moreMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Contact Us") },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.ContactSupport, contentDescription = null)
                                        },
                                        onClick = {
                                            selectedArticle = Article(
                                                title = "Contact Us",
                                                content = "Main Email Address: hello@drpsphca.com\n\n" +
                                                    "Press Contact: telltofree@drpsphca.com\n\n" +
                                                    "App Feedback: telltophcaapp@drpsphca.com\n\n" +
                                                    "DRPS PHCA TVAL: telltotval@drpsphca.com\n\n" +
                                                    "Phone (SMS/Viber): +63 993 733 7082\n\n" +
                                                    "App content is powered by DRPSPHCA.com.",
                                                contentHtml = "<p><strong>Main Email Address</strong><br>hello@drpsphca.com</p>" +
                                                    "<p><strong>Press Contact</strong><br>telltofree@drpsphca.com</p>" +
                                                    "<p><strong>App Feedback</strong><br>telltophcaapp@drpsphca.com</p>" +
                                                    "<p><strong>DRPS PHCA TVAL</strong><br>telltotval@drpsphca.com</p>" +
                                                    "<p><strong>Phone (SMS/Viber)</strong><br>+63 993 733 7082</p>" +
                                                    "<p>App content is powered by DRPSPHCA.com.</p>",
                                            )
                                            moreMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rate This App") },
                                        trailingIcon = {
                                            Icon(Icons.Filled.Star, contentDescription = null)
                                        },
                                        onClick = { moreMenuExpanded = false },
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    if (!searchVisible) {
                                        searchVisible = true
                                    } else if (searchQuery.isNotBlank()) {
                                        submitSearch()
                                    } else {
                                        searchVisible = false
                                        searchSubmitted = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search posts",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (searchVisible) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    searchRequestId++
                                    searchLoading = false
                                    searchSubmitted = false
                                    searchError = null
                                    searchResults = emptyList()
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                singleLine = true,
                                label = { Text("Search ${section.title}") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    sections.forEachIndexed { index, item ->
                        val selected = selectedSection == index
                        Column(
                            modifier = Modifier.weight(1f).height(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (selected) {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFE7D9FD), Color(0xFFC1C1C1)),
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Transparent),
                                        )
                                    },
                                )
                                .clickable {
                                selectedArticle = null
                                selectedSection = index
                                searchQuery = ""
                                searchVisible = false
                                searchSubmitted = false
                                searchRequestId++
                                searchLoading = false
                                searchError = null
                                searchResults = emptyList()
                            }.padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            SectionIcon(index, selected, darkMode)
                            Text(
                                item.label,
                                maxLines = 1,
                                color = if (selected) sectionSelectedColor(index) else {
                                    if (darkMode) Color.LightGray else Color(0xFF888888)
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                if (selectedArticle != null) {
                    ArticleDetail(
                        article = selectedArticle!!,
                        notoSerifFont = notoSerifFont,
                        onBack = { selectedArticle = null },
                    )
                } else if (searchSubmitted) {
                    Column(Modifier.fillMaxSize()) {
                        SectionHeading("Search Results", darkMode)
                        if (searchQuery.isNotBlank()) {
                            Text(
                                "Results for \"$searchQuery\"",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        ContentBody(
                            isLoading = searchLoading,
                            error = searchError,
                            articles = searchResults,
                            onRetry = { submitSearch() },
                            onArticleClick = { selectedArticle = it },
                            emptyMessage = "No posts found for \"$searchQuery\".",
                        )
                    }
                } else {
                    when {
                        selectedSection == 1 -> {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterButton("Blog", !biteSized) { biteSized = false }
                                FilterButton("Bite-sized", biteSized) { biteSized = true }
                            }
                            ContentBody(
                                isLoading, error, currentArticles,
                                onRetry = { refreshCount++ },
                                onArticleClick = { selectedArticle = it },
                                emptyMessage = "No posts found.",
                            )
                        }
                        selectedSection == 4 -> {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                PbbSection.values().forEach { tab ->
                                    PbbFilterButton(tab.label, pbbSection == tab) {
                                        pbbSection = tab
                                    }
                                    }
                            }
                            PbbContent(
                                tab = pbbTab,
                                section = pbbSection,
                                board = board,
                                selectedSeason = selectedSeason,
                                onSeasonSelected = {
                                    selectedSeason = it
                                    persistPbbSeason(it)
                                },
                                onTabSelected = { pbbTab = it },
                                jumpToEvictions = jumpToEvictions,
                                onEvictionJumpCompleted = { jumpToEvictions = false },
                                announcements = currentArticles,
                                isLoading = isLoading,
                                error = error,
                                onRetry = { refreshCount++ },
                                onArticleClick = { selectedArticle = it },
                            )
                        }
                        selectedSection == 0 -> HomeContent(
                            isLoading = isLoading,
                            error = error,
                            newsletters = newsletterArticles,
                            posts = currentArticles,
                            onRetry = { refreshCount++ },
                            onArticleClick = { selectedArticle = it },
                            isDarkMode = darkMode,
                        )
                        else -> ContentBody(
                            isLoading, error, currentArticles,
                            onRetry = { refreshCount++ },
                            onArticleClick = { selectedArticle = it },
                            emptyMessage = "No content found.",
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            Text(label, maxLines = 1)
        }
    }
}

@Composable
private fun PbbFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val darkMode = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PhcaGold else if (darkMode) Color.DarkGray else Color.LightGray,
        contentColor = if (selected) Color.Black else if (darkMode) Color.White else Color.Black,
        modifier = Modifier.height(36.dp),
    ) {
        Box(
            Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                maxLines = 1,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionLogo(index: Int) {
    val (image, description) = when (index) {
        1 -> Res.drawable.freeblog to "Free Blog"
        2 -> Res.drawable.freenewsletter to "Free Newsletter"
        3 -> Res.drawable.tvallogo to "TV Ad Listings"
        4 -> Res.drawable.pbbupd to "PBB Updates"
        else -> Res.drawable.logo to "DRPS PHCA"
    }
    Image(
        painter = painterResource(image),
        contentDescription = description,
        modifier = Modifier.height(40.dp).widthIn(max = 190.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun sectionSelectedColor(index: Int): Color = when (index) {
    1, 2 -> Color(0xFF0C7A0B)
    3 -> Color(0xFF0050DB)
    4 -> PhcaGold
    else -> Color(0xFF128FF1)
}

@Composable
private fun SectionIcon(index: Int, isSelected: Boolean, isDarkMode: Boolean) {
    val image = when (index) {
        1 -> Res.drawable.phcaapp_blog
        2 -> Res.drawable.phcaapp_newsletter
        3 -> Res.drawable.phcaapp_tval
        4 -> Res.drawable.phcaapp_pbb
        else -> Res.drawable.phcaapp_home
    }
    val tint = if (isSelected) {
        sectionSelectedColor(index)
    } else {
        if (isDarkMode) Color.LightGray else Color(0xFF888888)
    }
    androidx.compose.foundation.Image(
        painter = painterResource(image),
        contentDescription = sections[index].label,
        modifier = Modifier.size(if (index == 3) 34.dp else 24.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
private fun HomeContent(
    isLoading: Boolean,
    error: String?,
    newsletters: List<Article>,
    posts: List<Article>,
    onRetry: () -> Unit,
    onArticleClick: (Article) -> Unit,
    isDarkMode: Boolean,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionHeading("Latest Newsletter", isDarkMode, centered = true) }
        if (isLoading && newsletters.isEmpty()) {
            item { LoadingIndicator() }
        } else {
            item {
                newsletters.firstOrNull()?.let { ArticleCard(it, onArticleClick, showExcerpt = false) }
                    ?: Text("No newsletters available.", Modifier.padding(16.dp))
            }
        }
        item { SectionHeading("Latest Blog Posts", isDarkMode, centered = true) }
        if (error != null && posts.isEmpty()) {
            item { ErrorContent(error, onRetry) }
        } else {
            items(posts.take(10), key = { it.url }) { ArticleCard(it, onArticleClick) }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ContentBody(
    isLoading: Boolean,
    error: String?,
    articles: List<Article>,
    onRetry: () -> Unit,
    onArticleClick: (Article) -> Unit,
    emptyMessage: String,
) {
    when {
        isLoading && articles.isEmpty() -> LoadingIndicator()
        error != null && articles.isEmpty() -> ErrorContent(error, onRetry)
        articles.isEmpty() -> Text(emptyMessage, Modifier.padding(20.dp))
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(articles, key = { it.url }) { ArticleCard(it, onArticleClick) }
        }
    }
}

@Composable
private fun PbbContent(
    tab: PbbTab,
    section: PbbSection,
    board: BoardIndex?,
    selectedSeason: String,
    onSeasonSelected: (String) -> Unit,
    onTabSelected: (PbbTab) -> Unit,
    jumpToEvictions: Boolean,
    onEvictionJumpCompleted: () -> Unit,
    announcements: List<Article>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onArticleClick: (Article) -> Unit,
) {
    if (section == PbbSection.ANNOUNCEMENTS) {
        ContentBody(
            isLoading, error, announcements, onRetry, onArticleClick,
            emptyMessage = "No announcements found.",
        )
        return
    }
    if (isLoading && board == null) {
        LoadingIndicator()
        return
    }
    if (error != null && board == null) {
        ErrorContent(error, onRetry)
        return
    }
    val seasonKeys = board?.seasonList
        ?.filter { it.available && it.id in board?.seasons.orEmpty() }
        ?.map { it.id }
        ?.ifEmpty { null }
        ?: board?.seasons.orEmpty()
            .filterValues { it.season?.available == true }
            .keys.toList()
    val effectiveSeason = selectedSeason.takeIf { it in seasonKeys } ?: seasonKeys.firstOrNull()
    val pack = effectiveSeason?.let { board?.seasons?.get(it) }
    val sortedTallies = pack?.nominationTallies.orEmpty()
        .sortedWith(compareByDescending<NominationTally> { it.week ?: it.id ?: 0 })
    val sortedEvictions = pack?.evictions.orEmpty()
        .sortedWith(compareByDescending<Eviction> { it.week ?: 0 }.thenByDescending { it.day ?: 0 })
    val sortedTasks = pack?.weeklyTasks.orEmpty()
        .sortedWith(compareByDescending<WeeklyTask> { it.week ?: 0 }.thenByDescending { it.day ?: 0 })
    val latestEviction = sortedEvictions.firstOrNull()
    val latestTally = sortedTallies.firstOrNull()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var seasonMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(jumpToEvictions, tab, sortedTallies.size) {
        if (jumpToEvictions && tab == PbbTab.NOMINATIONS && sortedTallies.isNotEmpty()) {
            listState.animateScrollToItem(4 + sortedTallies.size)
            onEvictionJumpCompleted()
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item {
            if (effectiveSeason != null) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { seasonMenuExpanded = true },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant
                            else Color(0xFFF2F2F2),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "PBB Season",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    seasonLabel(effectiveSeason, pack?.season, board?.seasonList),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Season",
                                tint = PhcaGold,
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = seasonMenuExpanded,
                        onDismissRequest = { seasonMenuExpanded = false },
                    ) {
                        seasonKeys.forEach { season ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        seasonLabel(season, board?.seasons?.get(season)?.season, board?.seasonList),
                                        fontWeight = if (season == effectiveSeason) FontWeight.Bold
                                        else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onSeasonSelected(season)
                                    seasonMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            } else {
                Text("No PBB seasons are currently available.", Modifier.padding(20.dp))
            }
        }
        if (section == PbbSection.LIVE && effectiveSeason != null) {
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PbbTab.values().forEach { pbbTab ->
                        PbbFilterButton(pbbTab.label, tab == pbbTab) { onTabSelected(pbbTab) }
                    }
                }
            }
            item {
                pack?.season?.let { info ->
                    PbbPanel("Season Overview", Color(0xFFE8F5E9)) {
                        Text(
                            info.title ?: seasonLabel(effectiveSeason, info, board?.seasonList),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            listOfNotNull(
                                info.status?.let(::statusLabel),
                                info.currentWeek?.let { "Week $it" },
                                info.currentDay?.let { "Day $it" },
                            ).joinToString(" • ").ifBlank { "Season details" },
                        )
                        listOfNotNull(
                            "Schedule: ${info.premiere ?: "TBA"} — ${info.finale ?: "Ongoing"}",
                            info.days?.let { "$it days" },
                        ).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" • ")) }
                        info.winner?.let { Text("Winner: ${pack.housemates.findHousemateName(it)}") }
                    }
                }
            }
        }
        when (tab) {
            PbbTab.OVERVIEW -> {
                pack?.let { seasonPack ->
                    item {
                        PbbEvictionDeskCard(seasonPack, latestEviction, effectiveSeason)
                    }
                }
                latestTally?.let { tally ->
                    item {
                        PbbPanel(
                            "Latest Nomination Tally: ${tally.title ?: "Week ${tally.week ?: 1}"}",
                            Color(0xFFE1F5FE),
                        ) {
                            tally.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            tally.entries.filter { it.state.equals("nominated", ignoreCase = true) }
                                .forEach { entry ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            pack?.housemates.findHousemateName(entry.id)
                                                .ifBlank { entry.name ?: "Housemate" },
                                        )
                                        entry.label?.let {
                                            Text(it, color = PhcaGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                        }
                    }
                }
                if (sortedTasks.isNotEmpty()) {
                    item {
                        PbbPanel("Weekly Tasks", Color(0xFFF3E5F5)) {
                            sortedTasks.take(4).forEach { task ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            task.title ?: "Task Week ${task.week?.toString().orEmpty()}",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        task.week?.let { Text("Week $it", style = MaterialTheme.typography.bodySmall) }
                                    }
                                    PbbStatusChip(task.status ?: "pending")
                                }
                            }
                        }
                    }
                }
                pack?.let { seasonPack ->
                    item { SectionHeading("Housemates", centered = true) }
                    items(
                        seasonPack.housemates.sortedBy { it.place ?: Int.MAX_VALUE }.take(6),
                        key = { it.id },
                    ) { housemate ->
                        HousemateCard(
                            housemate,
                            effectiveSeason?.let {
                                housematePhotoUrl(it, seasonPack.photos[housemate.id])
                            },
                        )
                    }
                }
            }
            PbbTab.HOUSEMATES -> {
                item { SectionHeading("Housemates", centered = true) }
                if (pack?.housemates.isNullOrEmpty()) {
                    item { PbbEmptyState("Housemate details are coming soon.") }
                }
                items(pack?.housemates.orEmpty(), key = { it.id }) { housemate ->
                    HousemateCard(
                        housemate,
                        effectiveSeason?.let { housematePhotoUrl(it, pack?.photos?.get(housemate.id)) },
                    )
                }
            }
            PbbTab.NOMINATIONS -> {
                if (sortedTallies.isEmpty() && sortedEvictions.isEmpty()) {
                    item { PbbEmptyState("Coming Soon\nData for this season will be available upon official announcement.") }
                } else {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Nomination Tallies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            scope.launch { listState.animateScrollToItem(4 + sortedTallies.size) }
                        }) {
                            Text("Jump to Eviction Results", color = PhcaGold)
                        }
                    }
                }
                if (sortedTallies.isEmpty()) {
                    item { PbbEmptyState("Nomination results are not available yet.") }
                }
                items(sortedTallies, key = { "tally_${it.id ?: it.week}" }) { tally ->
                    PbbPanel(tally.title ?: "Week ${tally.week ?: "—"}", Color(0xFFE1F5FE)) {
                        tally.method?.let { Text("Method: $it") }
                        tally.entries.forEach { entry ->
                            PbbPersonRow(
                                name = pack?.housemates.findHousemateName(entry.id)
                                    ?.takeUnless { it == "Housemate" } ?: entry.name ?: "Housemate",
                                photoUrl = entry.id?.let { id ->
                                    effectiveSeason?.let { season ->
                                        housematePhotoUrl(season, pack?.photos?.get(id))
                                    }
                                },
                                subtitle = entry.label,
                                status = entry.state,
                                statusColor = nominationColor(entry.state),
                            )
                        }
                        tally.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Eviction Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { scope.launch { listState.animateScrollToItem(3) } }) {
                            Text("Jump to Nomination Tallies", color = PhcaGold)
                        }
                    }
                }
                if (sortedEvictions.isEmpty()) {
                    item { PbbEmptyState("Eviction results are not available yet.") }
                }
                items(sortedEvictions, key = { "eviction_${it.week}_${it.day}" }) { eviction ->
                    PbbPanel("Week ${eviction.week ?: "—"}", Color(0xFFFFF3E0)) {
                        eviction.date?.let { Text(it) }
                        eviction.voteType?.let { Text(voteTypeLabel(it), fontWeight = FontWeight.SemiBold) }
                        eviction.deskState?.let { PbbStatusChip(it) }
                        eviction.resultsAsOf?.let { Text("Results as of $it") }
                        eviction.nominated.forEach { nominee ->
                            PbbPersonRow(
                                name = pack?.housemates.findHousemateName(nominee.id)
                                    ?.takeUnless { it == "Housemate" }
                                    ?: nominee.label ?: "Housemate",
                                photoUrl = nominee.id?.let { id ->
                                    effectiveSeason?.let { season ->
                                        housematePhotoUrl(season, pack?.photos?.get(id))
                                    }
                                },
                                subtitle = nominee.percent?.let(::formatPercent),
                                status = nominee.result,
                                statusColor = evictionColor(nominee.result),
                            )
                        }
                        if (eviction.evicted.isNotEmpty()) {
                            Text(
                                "Evicted: ${eviction.evicted.joinToString {
                                    pack?.housemates.findHousemateName(it)
                                }}",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        eviction.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    }
                }
                }
            }
            PbbTab.WEEKLY_TASKS -> {
                item { SectionHeading("Weekly Tasks", centered = true) }
                if (sortedTasks.isEmpty()) {
                    item { PbbEmptyState("Coming Soon\nData for this season will be available upon official announcement.") }
                }
                items(sortedTasks, key = { "task_${it.week}_${it.day}" }) { task ->
                    PbbPanel("Week ${task.week ?: "—"}", Color(0xFFF3E5F5)) {
                        Text(task.title ?: "Weekly Task", fontWeight = FontWeight.Bold)
                        task.date?.let { Text(it) }
                        task.summary?.let { Text(it) }
                        task.status?.let { PbbStatusChip(it) }
                        task.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PbbPanel(
    title: String,
    backgroundColor: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val panelColor = if (isSystemInDarkTheme()) {
        when (backgroundColor) {
            Color(0xFFE8F5E9) -> Color(0xFF1E2D22)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    } else {
        backgroundColor
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = panelColor),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun PbbEmptyState(message: String) {
    Text(
        message,
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PbbStatusChip(status: String) {
    val color = statusColor(status)
    Surface(shape = RoundedCornerShape(8.dp), color = color) {
        Text(
            statusLabel(status),
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun PbbEvictionDeskCard(pack: SeasonPack, eviction: Eviction?, seasonId: String?) {
    val darkMode = isSystemInDarkTheme()
    val deskState = eviction?.deskState?.lowercase().orEmpty()
    val hasNominees = eviction?.nominated?.isNotEmpty() == true
    val votingOpen = pack.season?.votingOpen == true
    val showsDesk = hasNominees && (votingOpen || deskState in setOf(
        "started",
        "partial",
        "eviction_night",
        "closed_tally",
        "closed_revealed",
    ))
    PbbPanel("Eviction Desk", Color(0xFFFFF3E0)) {
        if (showsDesk) {
            val revealed = deskState == "closed_revealed"
            Text(
                when (deskState) {
                    "closed_revealed" -> "Voting is CLOSED. Final votes tally:"
                    else -> "Voting is OPEN! Current vote standings:"
                },
                color = if (darkMode) Color(0xFFFF8A80) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold,
            )
            if (deskState in setOf("partial", "eviction_night") &&
                !eviction?.resultsAsOf.isNullOrBlank()
            ) {
                Text(
                    "Voting results as of ${eviction?.resultsAsOf}:",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            eviction?.nominated.orEmpty().forEachIndexed { index, nominee ->
                val housemate = pack.housemates.firstOrNull { it.id == nominee.id }
                val name = when {
                    deskState == "started" -> "Housemate ${('A'.code + index).toChar()}"
                    revealed -> housemate?.name ?: nominee.id?.toDisplayName() ?: "Housemate"
                    else -> nominee.label?.takeIf { it.isNotBlank() } ?: "Housemate"
                }
                val photo = if (revealed && seasonId != null) {
                    housematePhotoUrl(seasonId, nominee.id?.let(pack.photos::get))
                } else {
                    "https://pbbupdates.drpsphca.com/assets/img/misc/misc_hm.png"
                }
                val percent = if (deskState == "started") "0.00%" else
                    nominee.percent?.let(::formatPercent) ?: "0.00%"
                PbbPersonRow(
                    name = name,
                    photoUrl = photo,
                    subtitle = percent,
                    status = nominee.result.takeIf { revealed },
                    statusColor = nominee.result?.let(::evictionColor),
                )
            }
        } else {
            Text("No ongoing eviction voting")
            pack.season?.winner?.let { winnerId ->
                Text(
                    "Winner: ${pack.housemates.findHousemateName(winnerId)}",
                    color = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HousemateCard(housemate: Housemate, photoUrl: String?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = housemate.name ?: housemate.id.toDisplayName(),
                    imageLoader = LocalPhcaImageLoader.current,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.Gray),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    housemate.name ?: housemate.id.toDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                housemate.moniker?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = PhcaGold, style = MaterialTheme.typography.bodySmall)
                }
                housemate.type?.let {
                    Text("Type: $it", style = MaterialTheme.typography.bodySmall)
                }
                listOfNotNull(
                    housemate.dayEntered?.let { "Entered: Day $it" },
                    housemate.dayExited?.let { "Exited: Day $it" },
                ).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PbbStatusChip(housemate.status ?: "active")
                housemate.place?.let {
                    Text("Place #$it", color = PhcaGold, style = MaterialTheme.typography.bodySmall)
                }
                housemate.nominationPoints?.let {
                    Text("$it pts", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PbbPersonRow(
    name: String,
    photoUrl: String?,
    subtitle: String?,
    status: String?,
    statusColor: Color?,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                imageLoader = LocalPhcaImageLoader.current,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color.Gray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
            }
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        if (status != null) {
            Surface(
                color = statusColor ?: Color(0xFF0277BD),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    statusLabel(status),
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun List<Housemate>?.findHousemateName(id: String?): String {
    if (id.isNullOrBlank()) return "Housemate"
    val housemate = this?.firstOrNull { it.id == id }
    return housemate?.displayName ?: housemate?.name ?: id.toDisplayName()
}

private fun String.toDisplayName(): String = replace('-', ' ').split(' ').joinToString(" ") {
    it.replaceFirstChar { first -> first.uppercase() }
}

private fun statusLabel(value: String): String = value.replace('_', ' ').trim().uppercase()

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "safe", "saved", "passed", "success", "winner" -> Color(0xFF2E7D32)
    "nominated", "yellow", "pending" -> Color(0xFFF57F17)
    "evicted", "red", "failed" -> Color(0xFFC62828)
    "not_nominated", "blue" -> Color(0xFF0D47A1)
    else -> Color(0xFF0277BD)
}

private fun nominationColor(state: String?): Color = when (state?.lowercase()) {
    "nominated" -> Color(0xFFC62828)
    "not_nominated" -> Color(0xFF0D47A1)
    "ineligible" -> Color(0xFF757575)
    else -> Color(0xFF0277BD)
}

private fun evictionColor(result: String?): Color = when (result?.lowercase()) {
    "saved" -> Color(0xFF2E7D32)
    "evicted" -> Color(0xFFC62828)
    "winner" -> Color(0xFF0D47A1)
    else -> Color(0xFF0277BD)
}

private fun voteTypeLabel(value: String): String = when (value.lowercase()) {
    "save_evict" -> "Save or Evict Voting"
    "save" -> "Save Voting"
    "finale" -> "Finale"
    else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun formatPercent(value: Double): String {
    val hundredths = (value * 100).roundToInt()
    return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}%"
}

@Composable
private fun ArticleDetail(
    article: Article,
    notoSerifFont: androidx.compose.ui.text.font.FontFamily,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) { Text("← Back") }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                if (article.image != null) {
                    PlatformArticleImage(
                        article = article,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        contentDescription = article.title,
                        onClick = null,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(article.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text(article.excerpt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                val html = article.contentHtml?.takeIf { it.isNotBlank() } ?: article.content
                if (html.isNotBlank()) {
                    HtmlArticleBody(html, notoSerifFont)
                } else {
                    Text(
                        article.contentText?.takeIf { it.isNotBlank() } ?: article.excerpt,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = notoSerifFont),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HtmlArticleBody(html: String, bodyFont: androidx.compose.ui.text.font.FontFamily) {
    val blocks = remember(html) { htmlArticleBlocks(html) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            val style = when (block.type) {
                HtmlBlockType.HEADING -> when (block.headingLevel) {
                    1 -> MaterialTheme.typography.headlineMedium
                    2 -> MaterialTheme.typography.headlineSmall
                    3 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                }.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HtmlBlockType.QUOTE -> MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HtmlBlockType.CODE -> MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                HtmlBlockType.BODY, HtmlBlockType.LIST_ITEM, HtmlBlockType.RULE ->
                    MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = bodyFont,
                        lineHeight = 26.sp,
                    )
            }
            Text(block.text, style = style)
        }
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    onClick: (Article) -> Unit,
    showExcerpt: Boolean = true,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick(article) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            if (article.image != null) {
                PlatformArticleImage(
                    article = article,
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    contentDescription = article.title,
                    onClick = { onClick(article) },
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else PhcaBlue,
                    fontWeight = FontWeight.Bold,
                )
                if (article.date.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(article.date, style = MaterialTheme.typography.bodySmall)
                }
                if (showExcerpt && article.excerpt.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        article.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (article.tags.isNotEmpty()) {
                    if (showExcerpt && article.excerpt.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        article.tags.take(4).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF0181E5),
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    centered: Boolean = false,
) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.headlineSmall,
        color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else PhcaGreen,
        fontWeight = FontWeight.Bold,
        textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center
        else androidx.compose.ui.text.style.TextAlign.Start,
    )
}

@Composable
internal fun articleImageRequest(article: Article): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(context, article.image, article.imageAuthorization) {
        ImageRequest.Builder(context)
            .data(article.image)
            .apply {
                val headers = NetworkHeaders.Builder()
                    .set("User-Agent", "com.drpsphca.app")
                val imageUrl = article.image.orEmpty()
                val authorization = article.imageAuthorization
                val origin = article.imageAuthorizationOrigin
                if (
                    !authorization.isNullOrBlank() &&
                    !origin.isNullOrBlank() &&
                    (imageUrl == origin || imageUrl.startsWith("$origin/"))
                ) {
                    headers.set("Authorization", authorization)
                }
                httpHeaders(headers.build())
            }
            .build()
    }
}

@Composable
private fun LoadingIndicator() {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Loading content…")
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

private fun seasonLabel(
    key: String,
    season: SeasonInfo?,
    seasonList: List<SeasonListItem>?,
): String = seasonList?.firstOrNull { it.id == key }?.label
    ?: when (key) {
        "gen11" -> "Gen 11"
        "collab3" -> "Collab 3.0"
        else -> season?.shortTitle ?: season?.title ?: key
    }

private fun housematePhotoUrl(seasonId: String, photo: String?): String? {
    if (photo.isNullOrBlank()) return null
    return when {
        photo.startsWith("http://") || photo.startsWith("https://") -> photo
        photo.startsWith("/") -> "https://pbbupdates.drpsphca.com$photo"
        else -> "https://pbbupdates.drpsphca.com/assets/img/$seasonId/$photo"
    }
}
