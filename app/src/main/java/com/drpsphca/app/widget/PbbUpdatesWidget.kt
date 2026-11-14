package com.drpsphca.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.drpsphca.app.MainActivity
import com.drpsphca.app.R
import com.drpsphca.app.data.pbb.SeasonPack
import com.google.gson.Gson
import java.util.Locale

class PbbUpdatesWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    companion object {
        val KEY_SELECTED_SEASON_ID = stringPreferencesKey("pbb_selected_season_id")
        val KEY_SEASON_TITLE = stringPreferencesKey("pbb_season_title")
        val KEY_SEASON_DATA = stringPreferencesKey("pbb_season_data")
        val KEY_SEASON_AVAILABLE = booleanPreferencesKey("pbb_season_available")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id)
        if (prefs[KEY_SEASON_DATA] == null && prefs[KEY_SELECTED_SEASON_ID] != null) {
            val workRequest = OneTimeWorkRequestBuilder<PbbWidgetWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        provideContent {
            GlanceTheme {
                PbbWidgetContent()
            }
        }
    }

    @Composable
    internal fun PbbWidgetContent() {
        val prefs = currentState<Preferences>()
        val selectedSeasonId = prefs[KEY_SELECTED_SEASON_ID]
        val seasonTitle = prefs[KEY_SEASON_TITLE]
        val seasonDataJson = prefs[KEY_SEASON_DATA]
        val isAvailable = prefs[KEY_SEASON_AVAILABLE] ?: false

        val context = LocalContext.current
        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val cardBg = if (isDarkMode) Color(0xFF262626) else Color(0xFFFFFFFF)
        val textColor = if (isDarkMode) Color.White else Color.Black

        var seasonPack: SeasonPack? = null
        if (!seasonDataJson.isNullOrEmpty()) {
            try {
                seasonPack = Gson().fromJson(seasonDataJson, SeasonPack::class.java)
            } catch (e: Exception) {
                // Ignore fallback
            }
        }

        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardBg)
                .padding(6.dp)
        ) {
            // Item 1: Header (Season Title / change season + Refresh button)
            item {
                PbbWidgetHeader(
                    seasonTitle = if (isAvailable && !seasonTitle.isNullOrEmpty()) seasonTitle else "Select Season",
                    isDarkMode = isDarkMode
                )
            }

            if (!isAvailable || selectedSeasonId.isNullOrEmpty() || seasonPack == null) {
                item {
                    SelectSeasonPromptView(textColor = textColor)
                }
            } else {
                val seasonInfo = seasonPack.season
                val votingOpen = seasonInfo?.votingOpen == true
                val latestEviction = seasonPack.evictions.lastOrNull()
                val latestTally = seasonPack.nominationTallies.lastOrNull()
                val latestTasks = seasonPack.weeklyTasks.sortedByDescending { it.week ?: 0 }.take(3)

                // Item 2: Season title, premiere to finale, current week and day, status badge
                item {
                    Spacer(GlanceModifier.height(4.dp))
                    val statusText = (seasonInfo?.status ?: "").replace('_', ' ').uppercase(Locale.ROOT)
                    val statusColor = when (seasonInfo?.status?.lowercase()) {
                        "live" -> Color(0xFF2E7D32)
                        "finished", "ended" -> Color(0xFF0D47A1)
                        else -> Color(0xFFE65100)
                    }
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(if (isDarkMode) Color(0xFF1E2D22) else Color(0xFFE8F5E9))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = seasonInfo?.title ?: "Pinoy Big Brother",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color(0xFF1B5E20), night = Color(0xFF81C784)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Box(
                                modifier = GlanceModifier
                                    .background(statusColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText.ifEmpty { "ACTIVE" },
                                    style = TextStyle(
                                        color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        val premiere = seasonInfo?.premiere ?: "TBA"
                        val finale = seasonInfo?.finale ?: "Ongoing"
                        Text(
                            text = "Schedule: $premiere — $finale",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                fontSize = 13.sp
                            )
                        )
                        if (seasonInfo?.currentWeek != null || seasonInfo?.currentDay != null) {
                            val weekText = seasonInfo.currentWeek?.let { "Week $it" } ?: ""
                            val dayText = seasonInfo.currentDay?.let { "Day $it" } ?: ""
                            Text(
                                text = listOf(weekText, dayText).filter { it.isNotEmpty() }.joinToString(" • "),
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color(0xFFBBA00C), night = Color(0xFFBBA00C)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Item 3: Eviction Desk
                item {
                    Spacer(GlanceModifier.height(4.dp))
                    val deskState = latestEviction?.deskState?.lowercase(Locale.ROOT) ?: ""
                    val showStandings = votingOpen && latestEviction != null && latestEviction.nominated.isNotEmpty() &&
                            listOf("started", "partial", "eviction_night", "closed_tally").contains(deskState)

                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(if (isDarkMode) Color(0xFF332222) else Color(0xFFFFF3E0))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Eviction Desk",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color(0xFFE65100), night = Color(0xFFE65100)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))

                        if (!votingOpen) {
                            Text(
                                text = "No ongoing eviction voting",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                    fontSize = 13.sp
                                )
                            )
                        } else if (deskState == "started") {
                            Text(
                                text = "Current vote standings:",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color(0xFFC62828), night = Color(0xFFFF8A80)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            latestEviction?.nominated?.forEach { nominee ->
                                val name = nominee.label ?: nominee.id ?: "Housemate"
                                Text(
                                    text = "$name: 0.00%",
                                    style = TextStyle(
                                        color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        } else if (deskState == "partial" || deskState == "eviction_night" || deskState == "closed_tally") {
                            Text(
                                text = "Current vote standings:",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color(0xFFC62828), night = Color(0xFFFF8A80)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            latestEviction?.nominated?.forEach { nominee ->
                                val name = nominee.label ?: nominee.id ?: "Housemate"
                                val percent = formatEvictionPercent(nominee.percent)
                                Text(
                                    text = "$name: $percent",
                                    style = TextStyle(
                                        color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        } else if (deskState == "closed_revealed") {
                            Text(
                                text = "Final votes tally:",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            latestEviction?.nominated?.forEach { nominee ->
                                val hm = seasonPack.housemates.find { it.id == nominee.id }
                                val name = hm?.displayName ?: hm?.name ?: nominee.id ?: "Housemate"
                                val percent = formatEvictionPercent(nominee.percent)
                                val result = nominee.result?.replace('_', ' ')?.uppercase(Locale.ROOT) ?: ""
                                Text(
                                    text = "$name: $percent" + if (result.isNotEmpty()) " ($result)" else "",
                                    style = TextStyle(
                                        color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = "No ongoing eviction voting",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                // Item 4: Latest Nomination Tally
                item {
                    Spacer(GlanceModifier.height(4.dp))
                    val nominatedEntries = latestTally?.entries?.filter { it.state.equals("nominated", ignoreCase = true) } ?: emptyList()
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(if (isDarkMode) Color(0xFF1E2A38) else Color(0xFFE1F5FE))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = latestTally?.title ?: "Latest Nomination Tally",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color(0xFF0277BD), night = Color(0xFF81D4FA)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        if (nominatedEntries.isEmpty()) {
                            Text(
                                text = "No active nomination tally",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                    fontSize = 13.sp
                                )
                            )
                        } else {
                            nominatedEntries.forEach { entry ->
                                val hm = seasonPack.housemates.find { it.id == entry.id }
                                val name = hm?.displayName ?: hm?.name ?: entry.name ?: entry.id ?: "Housemate"
                                val label = entry.label ?: ""
                                Text(
                                    text = "• $name" + if (label.isNotEmpty()) " — $label" else "",
                                    style = TextStyle(
                                        color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Item 5: Weekly Tasks (take 3)
                item {
                    Spacer(GlanceModifier.height(4.dp))
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(if (isDarkMode) Color(0xFF222222) else Color(0xFFF3E5F5))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Weekly Tasks",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color(0xFF6A1B9A), night = Color(0xFFCE93D8)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        if (latestTasks.isEmpty()) {
                            Text(
                                text = "No weekly tasks available",
                                style = TextStyle(
                                    color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                    fontSize = 13.sp
                                )
                            )
                        } else {
                            latestTasks.forEach { task ->
                                val statusText = (task.status ?: "PENDING").replace('_', ' ').uppercase(Locale.ROOT)
                                val statusColor = when (task.status?.lowercase()) {
                                    "passed", "success", "safe" -> Color(0xFF2E7D32)
                                    "failed", "red" -> Color(0xFFC62828)
                                    else -> Color(0xFFF57F17)
                                }
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "W${task.week ?: 1}: ${task.title ?: "Task"}",
                                        style = TextStyle(
                                            color = androidx.glance.color.ColorProvider(day = textColor, night = textColor),
                                            fontSize = 13.sp
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Box(
                                        modifier = GlanceModifier
                                            .background(statusColor)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = TextStyle(
                                                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 6: "See more in app" section card using accent color #BBA00C
                item {
                    Spacer(GlanceModifier.height(4.dp))
                    val mainAppIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("fcm_type", "pbb_updates")
                        if (!selectedSeasonId.isNullOrEmpty()) {
                            putExtra("fcm_season", selectedSeasonId)
                        }
                    }
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(Color(0xFFBBA00C))
                            .padding(vertical = 6.dp)
                            .clickable(actionStartActivity(mainAppIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "See more in app",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color.Black, night = Color.Black),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PbbWidgetHeader(
        seasonTitle: String,
        isDarkMode: Boolean
    ) {
        val context = LocalContext.current
        val selectSeasonIntent = Intent(context, PbbWidgetSelectSeasonActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(selectSeasonIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleBitmap = WidgetUtils.textToBitmap(
                    context = context,
                    text = seasonTitle,
                    fontSizeSp = 18f,
                    color = if (isDarkMode) Color(0xFFBBA00C) else Color(0xFF025CA1),
                    fontResId = R.font.phca_gilroy_bold,
                    maxWidthPx = 400
                )
                Image(
                    provider = ImageProvider(titleBitmap),
                    contentDescription = seasonTitle,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.height(24.dp)
                )
            }

            Image(
                provider = ImageProvider(R.drawable.phcaapp_refresh),
                contentDescription = "Refresh",
                modifier = GlanceModifier
                    .size(26.dp)
                    .clickable(actionRunCallback<PbbWidgetRefreshCallback>()),
                colorFilter = ColorFilter.tint(androidx.glance.color.ColorProvider(day = Color.Black, night = Color.White))
            )
        }
    }

    @Composable
    private fun SelectSeasonPromptView(textColor: Color) {
        val context = LocalContext.current
        val intent = Intent(context, PbbWidgetSelectSeasonActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    provider = ImageProvider(R.drawable.pbbupd),
                    contentDescription = "Select Season",
                    modifier = GlanceModifier.size(48.dp)
                )
                Spacer(GlanceModifier.height(6.dp))
                val promptBitmap = WidgetUtils.textToBitmap(
                    context = context,
                    text = "Select Season",
                    fontSizeSp = 16f,
                    color = textColor,
                    fontResId = R.font.phca_gilroy_bold,
                    maxWidthPx = 300
                )
                Image(
                    provider = ImageProvider(promptBitmap),
                    contentDescription = "Select Season",
                    modifier = GlanceModifier.height(20.dp)
                )
            }
        }
    }
}

fun formatEvictionPercent(percent: Double?): String {
    return if (percent != null) {
        "%.2f%%".format(Locale.US, percent)
    } else {
        "0.00%"
    }
}

class PbbWidgetRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val workRequest = OneTimeWorkRequestBuilder<PbbWidgetWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

class PbbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PbbUpdatesWidget()
}
