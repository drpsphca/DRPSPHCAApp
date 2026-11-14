package com.drpsphca.app.ui.screens.pbb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.drpsphca.app.PostItem
import com.drpsphca.app.data.pbb.*
import com.drpsphca.app.ui.WindowSize
import com.drpsphca.app.ui.theme.Gilroy
import com.drpsphca.app.ui.viewmodel.LiveSubTab
import com.drpsphca.app.ui.viewmodel.PbbSection
import com.drpsphca.app.ui.viewmodel.PbbViewModel
import com.drpsphca.app.ui.viewmodel.PostItemUiModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PbbUpdatesScreen(
    pbbViewModel: PbbViewModel,
    navController: NavController,
    windowSize: WindowSize,
    isDarkMode: Boolean,
    onAnnouncementClick: (AnnouncementPost) -> Unit
) {
    val uiState by pbbViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        PbbSectionSelector(
            selectedSection = uiState.selectedSection,
            onSectionSelected = { pbbViewModel.selectSection(it) },
            isDarkMode = isDarkMode
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { pbbViewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFBBA00C))
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error!!,
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { pbbViewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBA00C))
                        ) {
                            Text("Retry", fontFamily = Gilroy, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
                uiState.selectedSection == PbbSection.LIVE_UPDATES -> {
                    PbbLiveUpdatesView(
                        pbbViewModel = pbbViewModel,
                        windowSize = windowSize,
                        isDarkMode = isDarkMode
                    )
                }
                else -> {
                    PbbAnnouncementsView(
                        announcements = uiState.announcements,
                        navController = navController,
                        windowSize = windowSize,
                        isDarkMode = isDarkMode,
                        onAnnouncementClick = onAnnouncementClick
                    )
                }
            }
        }
    }
}

@Composable
fun PbbSectionSelector(
    selectedSection: PbbSection,
    onSectionSelected: (PbbSection) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PbbFilterChip(
            selected = selectedSection == PbbSection.LIVE_UPDATES,
            onClick = { onSectionSelected(PbbSection.LIVE_UPDATES) },
            label = "Live Updates",
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
        PbbFilterChip(
            selected = selectedSection == PbbSection.ANNOUNCEMENTS,
            onClick = { onSectionSelected(PbbSection.ANNOUNCEMENTS) },
            label = "Announcements",
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PbbFilterChip(
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
        color = if (selected) Color(0xFFBBA00C) else if (isDarkMode) Color.DarkGray else Color.LightGray,
        contentColor = if (selected) Color.Black else if (isDarkMode) Color.White else Color.Black,
        modifier = modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = Gilroy,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PbbLiveUpdatesView(
    pbbViewModel: PbbViewModel,
    windowSize: WindowSize,
    isDarkMode: Boolean
) {
    val uiState by pbbViewModel.uiState.collectAsState()
    val seasonPack = uiState.currentSeasonPack
    var expandedDropdown by remember { mutableStateOf(false) }

    val currentSeasonLabel = uiState.availableSeasons.find { it.id == uiState.selectedSeasonId }?.label
        ?: uiState.selectedSeasonId ?: "Select Season"

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val isTablet = windowSize != WindowSize.COMPACT || maxWidth >= 600.dp

        val gridColumns = when {
            isTablet && isLandscape -> 4
            isTablet && !isLandscape -> 3
            !isTablet && isLandscape -> 3
            else -> 2
        }

        val enlargedAvatarSize = if (isTablet) 120.dp else 96.dp

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Season Dropdown Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF2F2F2)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PBB Season",
                                    fontFamily = Gilroy,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color.LightGray else Color.Gray
                                )
                                Text(
                                    text = currentSeasonLabel,
                                    fontFamily = Gilroy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Season",
                                tint = Color(0xFFBBA00C)
                            )
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            uiState.availableSeasons.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.label,
                                            fontFamily = Gilroy,
                                            fontWeight = if (item.id == uiState.selectedSeasonId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        pbbViewModel.selectSeason(item.id)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Live Sub Tabs
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LiveSubTab.values()) { tab ->
                        val isSelected = uiState.selectedLiveSubTab == tab
                        val tabLabel = when (tab) {
                            LiveSubTab.OVERVIEW -> "Overview"
                            LiveSubTab.HOUSEMATES -> "Housemates"
                            LiveSubTab.NOMINATIONS -> "Nominations"
                            LiveSubTab.WEEKLY_TASKS -> "Weekly Tasks"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { pbbViewModel.selectLiveSubTab(tab) },
                            label = {
                                Text(
                                    text = tabLabel,
                                    fontFamily = Gilroy,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFBBA00C),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Content rendering based on selected sub-tab
            if (seasonPack == null) {
                item {
                    PbbComingSoonCard(isDarkMode = isDarkMode)
                }
            } else {
                when (uiState.selectedLiveSubTab) {
                    LiveSubTab.OVERVIEW -> {
                        item { PbbSeasonHeaderCard(seasonPack = seasonPack, isDarkMode = isDarkMode) }
                        item { PbbEvictionDeskCard(seasonPack = seasonPack, pbbViewModel = pbbViewModel, gridColumns = gridColumns, avatarSize = enlargedAvatarSize, isDarkMode = isDarkMode) }
                        item { PbbLatestNominationCard(seasonPack = seasonPack, pbbViewModel = pbbViewModel, gridColumns = gridColumns, avatarSize = enlargedAvatarSize, isDarkMode = isDarkMode) }
                        item { PbbLatestWeeklyTasksCard(weeklyTasks = seasonPack.weeklyTasks, isDarkMode = isDarkMode) }
                        item { PbbHousematesPreviewCard(seasonPack = seasonPack, pbbViewModel = pbbViewModel, isDarkMode = isDarkMode) }
                    }
                    LiveSubTab.HOUSEMATES -> {
                        if (seasonPack.housemates.isEmpty()) {
                            item { PbbComingSoonCard(isDarkMode = isDarkMode) }
                        } else {
                            val sortedHousemates = seasonPack.housemates.sortedWith(
                                compareBy(nullsLast()) { it.place ?: Int.MAX_VALUE }
                            )
                            items(sortedHousemates) { housemate ->
                                PbbHousemateItemCard(
                                    housemate = housemate,
                                    photoUrl = pbbViewModel.getPhotoUrl(housemate.id),
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                    LiveSubTab.NOMINATIONS -> {
                        if (seasonPack.nominationTallies.isEmpty() && seasonPack.evictions.isEmpty()) {
                            item { PbbComingSoonCard(isDarkMode = isDarkMode) }
                        } else {
                            val sortedTallies = seasonPack.nominationTallies.sortedByDescending { it.week ?: it.id ?: 0 }
                            val sortedEvictions = seasonPack.evictions.sortedByDescending { it.week ?: 0 }

                            if (sortedTallies.isNotEmpty()) {
                                item(key = "nomination_tallies_heading") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Nomination Tallies",
                                            fontFamily = Gilroy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        )
                                        TextButton(onClick = {
                                            coroutineScope.launch {
                                                listState.scrollToItem(3 + sortedTallies.size)
                                            }
                                        }) {
                                            Text(
                                                text = "Jump to Eviction Results",
                                                fontFamily = Gilroy,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFFBBA00C)
                                            )
                                        }
                                    }
                                }
                                items(sortedTallies, key = { "tally_${it.id ?: it.week}" }) { tally ->
                                    PbbNominationTallySectionCard(
                                        seasonPack = seasonPack,
                                        pbbViewModel = pbbViewModel,
                                        tally = tally,
                                        gridColumns = gridColumns,
                                        avatarSize = enlargedAvatarSize,
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            if (sortedEvictions.isNotEmpty()) {
                                item(key = "eviction-results") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Eviction Results",
                                            fontFamily = Gilroy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        )
                                        TextButton(onClick = {
                                            coroutineScope.launch {
                                                listState.scrollToItem(2)
                                            }
                                        }) {
                                            Text(
                                                text = "Jump to Nomination Tallies",
                                                fontFamily = Gilroy,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFFBBA00C)
                                            )
                                        }
                                    }
                                }
                                items(sortedEvictions, key = { "eviction_${it.week}" }) { eviction ->
                                    PbbEvictionResultSectionCard(
                                        seasonPack = seasonPack,
                                        pbbViewModel = pbbViewModel,
                                        eviction = eviction,
                                        gridColumns = gridColumns,
                                        avatarSize = enlargedAvatarSize,
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }
                        }
                    }
                    LiveSubTab.WEEKLY_TASKS -> {
                        if (seasonPack.weeklyTasks.isEmpty()) {
                            item { PbbComingSoonCard(isDarkMode = isDarkMode) }
                        } else {
                            val sortedTasks = seasonPack.weeklyTasks.sortedByDescending { it.week ?: 0 }
                            items(sortedTasks) { task ->
                                PbbWeeklyTaskCard(task = task, isDarkMode = isDarkMode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HousemateAvatar(
    photoUrl: String?,
    contentDescription: String?,
    size: Dp
) {
    if (!photoUrl.isNullOrEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
fun PbbComingSoonCard(isDarkMode: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF2F2F2)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Coming Soon",
                tint = Color(0xFFBBA00C),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Coming Soon",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Data for this season will be available upon official announcement.",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = if (isDarkMode) Color.LightGray else Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PbbSeasonHeaderCard(seasonPack: SeasonPack, isDarkMode: Boolean) {
    val seasonInfo = seasonPack.season
    val winnerId = seasonInfo?.winner
    val winnerHousemate = if (!winnerId.isNullOrEmpty()) {
        seasonPack.housemates.find { it.id == winnerId }
    } else null
    val winnerName = if (winnerHousemate != null) {
        formatHousemateFullName(winnerHousemate.name, winnerHousemate.displayName, winnerHousemate.id)
    } else if (!winnerId.isNullOrEmpty()) {
        formatHousemateFullName(null, null, winnerId)
    } else ""

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E2D22) else Color(0xFFE8F5E9)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = seasonInfo?.title ?: "Pinoy Big Brother",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF1B5E20),
                    modifier = Modifier.weight(1f)
                )

                val statusText = statusLabel(seasonInfo?.status)
                val statusColor = when (seasonInfo?.status?.lowercase()) {
                    "live" -> Color(0xFF2E7D32)
                    "finished" -> Color(0xFF0D47A1)
                    else -> Color(0xFFE65100)
                }

                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (statusText.isEmpty()) "UPCOMING" else statusText,
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val premiere = seasonInfo?.premiere ?: "TBA"
            val finale = seasonInfo?.finale ?: "Ongoing"
            Text(
                text = "Schedule: $premiere — $finale",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isDarkMode) Color.White else Color.DarkGray
            )

            if (seasonInfo?.currentWeek != null || seasonInfo?.currentDay != null) {
                val weekText = seasonInfo.currentWeek?.let { "Week $it" } ?: ""
                val dayText = seasonInfo.currentDay?.let { "Day $it" } ?: ""
                Text(
                    text = listOf(weekText, dayText).filter { it.isNotEmpty() }.joinToString(" • "),
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFBBA00C)
                )
            }

            if (winnerName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Winner: $winnerName",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun PbbEvictionDeskCard(
    seasonPack: SeasonPack,
    pbbViewModel: PbbViewModel,
    gridColumns: Int,
    avatarSize: Dp,
    isDarkMode: Boolean
) {
    val votingOpen = seasonPack.season?.votingOpen == true
    val latestEviction = seasonPack.evictions.lastOrNull()
    val deskState = latestEviction?.deskState?.lowercase(Locale.ROOT) ?: ""
    val placeholderPhoto = "https://pbbupdates.drpsphca.com/assets/img/misc/misc_hm.png"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Eviction Desk",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val hasEvictionNominees = latestEviction != null && latestEviction.nominated.isNotEmpty()

            if (hasEvictionNominees && (votingOpen || deskState.isNotEmpty())) {
                val sectionTitle = when (deskState) {
                    "closed_revealed" -> "Voting is CLOSED. Final votes tally:"
                    else -> "Voting is OPEN! Current vote standings:"
                }

                Text(
                    text = sectionTitle,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFC62828)
                )

                if ((deskState == "partial" || deskState == "eviction_night") && !latestEviction.resultsAsOf.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Voting is OPEN! Voting results as of ${latestEviction.resultsAsOf}:",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF222222)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CardGrid(items = latestEviction.nominated, columnCount = gridColumns) { nominee ->
                    val housemate = seasonPack.housemates.find { it.id == nominee.id }

                    val photoUrl = if (deskState == "closed_revealed") {
                        nominee.id?.let { pbbViewModel.getPhotoUrl(it) }
                    } else {
                        placeholderPhoto
                    }

                    val name = if (deskState == "closed_revealed") {
                        housemate?.let { formatHousemateName(it.name, it.displayName, it.id) }
                            ?: formatHousemateName(null, null, nominee.id ?: "")
                    } else {
                        nominee.label?.ifBlank { null } ?: "Housemate"
                    }

                    val percentText = if (deskState == "started") {
                        "0.00%"
                    } else {
                        formatEvictionPercent(nominee.percent)
                    }

                    val resultText = if (deskState == "closed_revealed") nominee.result else null

                    EnlargedEvictionNomineeCard(
                        photoUrl = photoUrl,
                        name = name,
                        percentText = percentText,
                        result = resultText,
                        avatarSize = avatarSize,
                        isDarkMode = isDarkMode
                    )
                }
            } else {
                Text(
                    text = "No ongoing eviction voting",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF222222)
                )
                val winnerId = seasonPack.season?.winner
                val winnerHousemate = if (!winnerId.isNullOrEmpty()) {
                    seasonPack.housemates.find { it.id == winnerId }
                } else null
                val winnerName = if (winnerHousemate != null) {
                    formatHousemateFullName(winnerHousemate.name, winnerHousemate.displayName, winnerHousemate.id)
                } else if (!winnerId.isNullOrEmpty()) {
                    formatHousemateFullName(null, null, winnerId)
                } else ""

                if (winnerName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Winner: $winnerName",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun PbbLatestNominationCard(
    seasonPack: SeasonPack,
    pbbViewModel: PbbViewModel,
    gridColumns: Int,
    avatarSize: Dp,
    isDarkMode: Boolean
) {
    val lastTally = seasonPack.nominationTallies.lastOrNull() ?: return
    // Filter entries where state == "nominated"
    val nominatedEntries = lastTally.entries.filter { it.state.equals("nominated", ignoreCase = true) }
    if (nominatedEntries.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE1F5FE)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Latest Nomination Tally: ${lastTally.title ?: "Week ${lastTally.week ?: 1}"}",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color(0xFF81D4FA) else Color(0xFF0277BD)
            )

            if (!lastTally.note.isNullOrEmpty()) {
                Text(
                    text = lastTally.note,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF555555)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CardGrid(items = nominatedEntries, columnCount = gridColumns) { entry ->
                val housemate = seasonPack.housemates.find { it.id == entry.id }
                val photoUrl = entry.id?.let { pbbViewModel.getPhotoUrl(it) }
                val hName = housemate?.let { formatHousemateName(it.name, it.displayName, it.id) }
                    ?: formatHousemateName(entry.name, null, entry.id ?: "")

                EnlargedNominationEntryCard(
                    photoUrl = photoUrl,
                    name = hName,
                    label = entry.label,
                    state = entry.state,
                    avatarSize = avatarSize,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
fun PbbLatestWeeklyTasksCard(
    weeklyTasks: List<WeeklyTask>,
    isDarkMode: Boolean
) {
    val latestTasks = weeklyTasks.takeLast(4)
    if (latestTasks.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3E5F5)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Tasks",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color(0xFFCE93D8) else Color(0xFF6A1B9A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            latestTasks.forEach { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title ?: "Task Week ${task.week ?: ""}",
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDarkMode) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.week != null) {
                            Text(
                                text = "Week ${task.week}",
                                fontFamily = Gilroy,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF555555)
                            )
                        }
                    }
                    val statusBg = getStatusColor(task.status)
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusLabel(task.status).ifEmpty { "PENDING" },
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PbbHousematesPreviewCard(
    seasonPack: SeasonPack,
    pbbViewModel: PbbViewModel,
    isDarkMode: Boolean
) {
    val allHousemates = seasonPack.housemates
    if (allHousemates.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF2F2F2)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Housemates",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = "See Details",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFBBA00C),
                    modifier = Modifier.clickable { pbbViewModel.selectLiveSubTab(LiveSubTab.HOUSEMATES) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(allHousemates) { hm ->
                    val photoUrl = pbbViewModel.getPhotoUrl(hm.id)
                    val fullName = formatHousemateFullName(hm.name, hm.displayName, hm.id)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(80.dp)
                    ) {
                        HousemateAvatar(
                            photoUrl = photoUrl,
                            contentDescription = fullName,
                            size = 64.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fullName,
                            fontFamily = Gilroy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDarkMode) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        if (!hm.moniker.isNullOrEmpty()) {
                            Text(
                                text = hm.moniker,
                                fontFamily = Gilroy,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF555555),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PbbHousemateItemCard(
    housemate: Housemate,
    photoUrl: String?,
    isDarkMode: Boolean
) {
    val fullName = housemate.name ?: formatHousemateFullName(null, housemate.displayName, housemate.id)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF262626) else Color(0xFFF9F9F9)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HousemateAvatar(
                photoUrl = photoUrl,
                contentDescription = fullName,
                size = 64.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                if (!housemate.moniker.isNullOrEmpty()) {
                    Text(
                        text = housemate.moniker,
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFFBBA00C)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                if (!housemate.type.isNullOrEmpty()) {
                    Text(
                        text = "Type: ${housemate.type}",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF666666)
                    )
                }
                val daysInfo = listOfNotNull(
                    housemate.dayEntered?.let { "Entered: Day $it" },
                    housemate.dayExited?.let { "Exited: Day $it" }
                ).joinToString(" • ")
                if (daysInfo.isNotEmpty()) {
                    Text(
                        text = daysInfo,
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF666666)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val statusText = statusLabel(housemate.status)
                val (statusBg, statusTextColor) = getStatusColors(housemate.status, isDarkMode)
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText.ifEmpty { "ACTIVE" },
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (housemate.place != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Place #${housemate.place}",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFBBA00C)
                    )
                }
                if (housemate.nominationPoints != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${housemate.nominationPoints} pts",
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PbbNominationTallySectionCard(
    seasonPack: SeasonPack,
    pbbViewModel: PbbViewModel,
    tally: NominationTally,
    gridColumns: Int,
    avatarSize: Dp,
    isDarkMode: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = tally.title ?: "Week ${tally.week ?: ""}",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color.White else Color.Black
            )
            val subInfo = listOfNotNull(
                tally.week?.let { "Week $it" },
                tally.method?.let { "Method: $it" }
            ).joinToString(" • ")
            if (subInfo.isNotEmpty()) {
                Text(
                    text = subInfo,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFBBA00C)
                )
            }
            if (!tally.note.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tally.note,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF555555)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            CardGrid(items = tally.entries, columnCount = gridColumns) { entry ->
                val housemate = seasonPack.housemates.find { it.id == entry.id }
                val photoUrl = entry.id?.let { pbbViewModel.getPhotoUrl(it) }
                val hName = housemate?.let { formatHousemateName(it.name, it.displayName, it.id) }
                    ?: formatHousemateName(entry.name, null, entry.id ?: "")

                EnlargedNominationEntryCard(
                    photoUrl = photoUrl,
                    name = hName,
                    label = entry.label,
                    state = entry.state,
                    avatarSize = avatarSize,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
fun PbbEvictionResultSectionCard(
    seasonPack: SeasonPack,
    pbbViewModel: PbbViewModel,
    eviction: Eviction,
    gridColumns: Int,
    avatarSize: Dp,
    isDarkMode: Boolean
) {
    val deskState = eviction.deskState?.lowercase(Locale.ROOT) ?: ""
    val placeholderPhoto = "https://pbbupdates.drpsphca.com/assets/img/misc/misc_hm.png"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Eviction: Week ${eviction.week ?: ""} (Day ${eviction.day ?: ""})",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFC62828)
            )
            val subInfo = listOfNotNull(
                eviction.date,
                eviction.voteType?.let { "Vote Type: ${formatVoteType(it)}" },
                eviction.resultsAsOf?.let { "Results as of $it" }
            ).joinToString(" • ")
            if (subInfo.isNotEmpty()) {
                Text(
                    text = subInfo,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF444444)
                )
            }
            if (!eviction.note.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eviction.note,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF555555)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            CardGrid(items = eviction.nominated, columnCount = gridColumns) { nominee ->
                val housemate = seasonPack.housemates.find { it.id == nominee.id }

                val photoUrl = if (deskState == "closed_revealed" || deskState.isEmpty()) {
                    nominee.id?.let { pbbViewModel.getPhotoUrl(it) }
                } else {
                    placeholderPhoto
                }

                val name = if (deskState.isNotEmpty() && deskState != "closed_revealed") {
                    nominee.label?.ifBlank { null } ?: "Housemate"
                } else {
                    housemate?.let { formatHousemateName(it.name, it.displayName, it.id) }
                        ?: formatHousemateName(null, null, nominee.id ?: "")
                }

                val percentText = if (deskState == "started") {
                    "0.00%"
                } else {
                    formatEvictionPercent(nominee.percent)
                }

                EnlargedEvictionNomineeCard(
                    photoUrl = photoUrl,
                    name = name,
                    percentText = percentText,
                    result = nominee.result,
                    avatarSize = avatarSize,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
fun EnlargedNominationEntryCard(
    photoUrl: String?,
    name: String,
    label: String?,
    state: String?,
    avatarSize: Dp,
    isDarkMode: Boolean
) {
    val cardColor = if (isDarkMode) Color(0xFF262626) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val labelColor = if (isDarkMode) Color(0xFF81D4FA) else Color(0xFF0277BD)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HousemateAvatar(
                photoUrl = photoUrl,
                contentDescription = name,
                size = avatarSize
            )
            if (name.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (!label.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = labelColor
                )
            }
            if (!state.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val stateColor = getNominationStateColor(state)
                Surface(
                    color = stateColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel(state).ifEmpty { "NOMINATED" },
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnlargedEvictionNomineeCard(
    photoUrl: String?,
    name: String,
    percentText: String,
    result: String?,
    avatarSize: Dp,
    isDarkMode: Boolean
) {
    val cardColor = if (isDarkMode) Color(0xFF262626) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val percentColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HousemateAvatar(
                photoUrl = photoUrl,
                contentDescription = name,
                size = avatarSize
            )
            if (name.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (percentText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = percentText,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = percentColor
                )
            }
            if (!result.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val resColor = getEvictionResultColor(result)
                Surface(
                    color = resColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel(result),
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PbbWeeklyTaskCard(
    task: WeeklyTask,
    isDarkMode: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title ?: "Weekly Task",
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                val statusText = statusLabel(task.status)
                val statusBg = getStatusColor(task.status)
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText.ifEmpty { "PENDING" },
                        fontFamily = Gilroy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (task.week != null || task.day != null) {
                Text(
                    text = listOfNotNull(task.week?.let { "Week $it" }, task.day?.let { "Day $it" }).joinToString(" • "),
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFBBA00C)
                )
            }

            if (!task.summary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.summary,
                    fontFamily = Gilroy,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF333333)
                )
            }
        }
    }
}

@Composable
fun PbbAnnouncementsView(
    announcements: List<AnnouncementPost>,
    navController: NavController,
    windowSize: WindowSize,
    isDarkMode: Boolean,
    onAnnouncementClick: (AnnouncementPost) -> Unit
) {
    if (announcements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No announcements available.",
                fontFamily = Gilroy,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) Color.White else Color.Black
            )
        }
    } else {
        val isCompact = windowSize == WindowSize.COMPACT
        if (isCompact) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(announcements, key = { it.url ?: it.hashCode().toString() }) { post ->
                    val itemUiModel = post.toItemUiModel()
                    PostItem(
                        post = itemUiModel,
                        navController = navController,
                        isDarkMode = isDarkMode,
                        onPostClick = { onAnnouncementClick(post) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(announcements, key = { it.url ?: it.hashCode().toString() }) { post ->
                    val itemUiModel = post.toItemUiModel()
                    PostItem(
                        post = itemUiModel,
                        navController = navController,
                        isDarkMode = isDarkMode,
                        onPostClick = { onAnnouncementClick(post) }
                    )
                }
            }
        }
    }
}

fun AnnouncementPost.toItemUiModel(): PostItemUiModel {
    return PostItemUiModel(
        id = url.hashCode(),
        formattedDate = formatCardPublishedDate(date),
        plainTitle = title ?: "PBB Announcement",
        plainExcerpt = excerpt ?: "",
        imageUrl = featuredImage,
        tags = emptyList(),
        category = "PBB Updates"
    )
}

@Composable
fun <T> CardGrid(
    items: List<T>,
    columnCount: Int = 2,
    content: @Composable (T) -> Unit
) {
    val rows = items.chunked(columnCount)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                repeat(columnCount - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

fun formatHousemateName(name: String?, displayName: String?, id: String): String {
    if (!displayName.isNullOrBlank()) return displayName
    if (!name.isNullOrBlank()) return name
    if (id.isBlank()) return "Housemate"
    return id.replace('-', ' ').split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}

fun formatHousemateFullName(name: String?, displayName: String?, id: String): String {
    if (!name.isNullOrBlank()) return name
    if (!displayName.isNullOrBlank()) return displayName
    if (id.isBlank()) return "Housemate"
    return id.replace('-', ' ').split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}

fun statusLabel(status: String?): String {
    val clean = status.orEmpty().replace('_', ' ').trim()
    if (clean.equals("houseguest", ignoreCase = true) || clean.equals("house guest", ignoreCase = true)) {
        return "HOUSE GUEST"
    }
    return clean.uppercase(Locale.ROOT)
}

fun formatEvictionPercent(percent: Double?): String {
    return if (percent != null) {
        "%.2f%%".format(Locale.US, percent)
    } else {
        "0.00%"
    }
}

fun formatCardPublishedDate(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return ""
    val cleanDateStr = rawDate.take(10)

    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    )

    for (format in inputFormats) {
        try {
            val date = format.parse(cleanDateStr) ?: format.parse(rawDate)
            if (date != null) {
                val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                return outputFormat.format(date)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    return rawDate
}

fun formatPbbPublishedDate(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return ""
    val cleanDateStr = rawDate.take(10)

    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    )

    for (format in inputFormats) {
        try {
            val date = format.parse(cleanDateStr) ?: format.parse(rawDate)
            if (date != null) {
                val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                return outputFormat.format(date).uppercase(Locale.ROOT)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    return rawDate.uppercase(Locale.ROOT)
}

fun getStatusColor(status: String?): Color {
    val clean = status?.lowercase(Locale.ROOT)?.replace('_', ' ')?.trim() ?: ""
    return when {
        clean == "houseguest" || clean == "house guest" -> Color(0xFF5E35B1)
        clean in listOf("safe", "saved", "passed", "success", "winner") -> Color(0xFF2E7D32) // safe green
        clean in listOf("nominated", "yellow", "pending") -> Color(0xFFF57F17) // nominated yellow
        clean in listOf("evicted", "red", "failed") -> Color(0xFFC62828) // evicted red
        clean in listOf("not_nominated", "blue") -> Color(0xFF0D47A1) // blue
        else -> Color(0xFF0277BD)
    }
}

fun getStatusColors(status: String?, isDarkMode: Boolean): Pair<Color, Color> {
    val clean = status?.lowercase(Locale.ROOT)?.replace('_', ' ')?.trim() ?: ""
    if (clean == "houseguest" || clean == "house guest") {
        return if (isDarkMode) {
            Color(0xFF1A1228) to Color(0xFFB39DDB)
        } else {
            Color(0xFFE8E0F5) to Color(0xFF5E35B1)
        }
    }
    return getStatusColor(status) to Color.White
}

fun getNominationStateColor(state: String?): Color {
    return when (state?.lowercase()) {
        "nominated" -> Color(0xFFC62828) // red
        "not_nominated" -> Color(0xFF0D47A1) // blue
        "ineligible" -> Color(0xFF757575) // gray
        else -> Color(0xFF0277BD)
    }
}

fun getEvictionResultColor(result: String?): Color {
    return when (result?.lowercase()) {
        "saved" -> Color(0xFF2E7D32) // green
        "evicted" -> Color(0xFFC62828) // red
        "winner" -> Color(0xFF0D47A1) // blue
        else -> Color(0xFF0277BD)
    }
}

fun formatVoteType(voteType: String?): String {
    return when (voteType?.lowercase(Locale.ROOT)) {
        "save_evict" -> "Save or Evict Voting"
        "save" -> "Save Voting"
        "finale" -> "Finale"
        else -> voteType?.replace('_', ' ')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: ""
    }
}
