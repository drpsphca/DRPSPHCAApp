package com.drpsphca.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedSeason by rememberSaveable { mutableStateOf("") }
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
                        if (selectedSeason.isBlank()) {
                            selectedSeason = board?.seasons?.keys
                                ?.sortedWith(compareBy({ if (it == "collab3") 0 else if (it == "gen11") 1 else 2 }, { it }))
                                ?.firstOrNull().orEmpty()
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
                                        onClick = {
                                            refreshCount++
                                            moreMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Contact Us") },
                                        onClick = {
                                            selectedArticle = Article(
                                                title = "Contact Us",
                                                content = "Main Email Address: hello@drpsphca.com\n\n" +
                                                    "Press Contact: telltofree@drpsphca.com\n\n" +
                                                    "App Feedback: telltophcaapp@drpsphca.com\n\n" +
                                                    "DRPS PHCA TVAL: telltotval@drpsphca.com\n\n" +
                                                    "Phone (SMS/Viber): +63 993 733 7082\n\n" +
                                                    "App content is powered by DRPSPHCA.com.",
                                            )
                                            moreMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rate This App") },
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
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    sections.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedSection == index,
                            onClick = {
                                selectedArticle = null
                                selectedSection = index
                                searchQuery = ""
                                searchVisible = false
                                searchSubmitted = false
                                searchRequestId++
                                searchLoading = false
                                searchError = null
                                searchResults = emptyList()
                            },
                            icon = { SectionIcon(index, selectedSection == index, darkMode) },
                            label = {
                                Text(
                                    item.label,
                                    maxLines = 1,
                                    color = if (selectedSection == index) {
                                        sectionSelectedColor(index)
                                    } else {
                                        if (darkMode) Color.LightGray else Color(0xFF888888)
                                    },
                                )
                            },
                        )
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
                                    FilterButton(tab.label, pbbSection == tab) {
                                        pbbSection = tab
                                    }
                                    }
                            }
                            if (pbbSection == PbbSection.LIVE) {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    PbbTab.values().forEach { tab ->
                                        FilterButton(tab.label, pbbTab == tab) { pbbTab = tab }
                                    }
                                }
                            }
                                            PbbContent(
                                tab = pbbTab,
                                section = pbbSection,
                                board = board,
                                selectedSeason = selectedSeason,
                                onSeasonSelected = { selectedSeason = it },
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
    val pack = board?.seasons?.get(selectedSeason)
    if (isLoading && board == null) {
        LoadingIndicator()
        return
    }
    if (error != null && board == null) {
        ErrorContent(error, onRetry)
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                board?.seasons?.keys?.sorted()?.forEach { season ->
                    FilterButton(seasonLabel(season, board.seasons[season]?.season), season == selectedSeason) {
                        onSeasonSelected(season)
                    }
                }
            }
        }
        when (tab) {
            PbbTab.OVERVIEW -> {
                item {
                    SectionHeading(seasonLabel(selectedSeason, pack?.season))
                    pack?.season?.let { info ->
                        InfoCard(
                            listOfNotNull(
                                info.status?.let { "Status: $it" },
                                info.currentWeek?.let { "Week $it" },
                                info.currentDay?.let { "Day $it" },
                                info.winner?.let { "Winner: $it" },
                            ).joinToString("  •  ").ifBlank { "Season overview" },
                        )
                    }
                }
                item { SectionHeading("Housemates") }
                items(pack?.housemates.orEmpty().take(6), key = { it.id }) {
                    HousemateCard(it, housematePhotoUrl(selectedSeason, pack?.photos?.get(it.id)))
                }
                item { SectionHeading("Recent Announcements") }
                items(announcements.take(5), key = { it.url }) { ArticleCard(it, onArticleClick) }
            }
            PbbTab.HOUSEMATES -> {
                item { SectionHeading("Housemates") }
                items(pack?.housemates.orEmpty(), key = { it.id }) {
                    HousemateCard(it, housematePhotoUrl(selectedSeason, pack?.photos?.get(it.id)))
                }
            }
            PbbTab.NOMINATIONS -> {
                item { SectionHeading("Nomination Tallies") }
                items(pack?.nominationTallies.orEmpty()) { tally ->
                    InfoCard(
                        buildString {
                            append(tally.title ?: "Week ${tally.week ?: ""}")
                            tally.entries.forEach { entry ->
                                append("\n")
                                append(entry.name ?: entry.label ?: "Housemate")
                                entry.state?.let { append(": $it") }
                            }
                        },
                    )
                }
                item { SectionHeading("Evictions") }
                items(pack?.evictions.orEmpty()) { eviction ->
                    InfoCard(
                        buildString {
                            append("Week ${eviction.week ?: "—"}")
                            eviction.date?.let { append(" • $it") }
                            if (eviction.evicted.isNotEmpty()) append("\nEvicted: ${eviction.evicted.joinToString()}")
                            eviction.note?.let { append("\n$it") }
                        },
                    )
                }
            }
            PbbTab.WEEKLY_TASKS -> {
                item { SectionHeading("Weekly Tasks") }
                items(pack?.weeklyTasks.orEmpty()) { task ->
                    InfoCard(
                        buildString {
                            append(task.title ?: "Week ${task.week ?: ""}")
                            task.summary?.let { append("\n$it") }
                            task.status?.let { append("\nStatus: $it") }
                            task.date?.let { append("\n$it") }
                        },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
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
                    contentDescription = housemate.displayName ?: housemate.name,
                    imageLoader = LocalPhcaImageLoader.current,
                    modifier = Modifier.size(112.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    housemate.displayName ?: housemate.name ?: housemate.id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                housemate.moniker?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                listOfNotNull(housemate.status, housemate.hometown, housemate.place?.let { "Place: $it" })
                    .joinToString(" • ")
                    .takeIf { it.isNotBlank() }
                    ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
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
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
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

private fun seasonLabel(key: String, season: SeasonInfo?): String = when (key) {
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
