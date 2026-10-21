package com.drpsphca.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.drpsphca.app.data.pbb.PbbRepository
import com.drpsphca.app.ui.theme.DRPSPHCATheme
import com.drpsphca.app.ui.theme.Gilroy
import kotlinx.coroutines.launch

class PbbWidgetSelectSeasonActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        val repository = PbbRepository(this)

        setContent {
            DRPSPHCATheme {
                var isLoading by remember { mutableStateOf(true) }
                var error by remember { mutableStateOf<String?>(null) }
                var availableSeasons by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

                LaunchedEffect(Unit) {
                    try {
                        val index = repository.getBoardIndex(forceRefresh = true)
                        val sortedKeys = repository.getSortedSeasonKeys(index.seasons)
                        val items = sortedKeys
                            .filter { key -> index.seasons[key]?.season?.available == true }
                            .map { key ->
                                key to repository.getSeasonLabel(key, index.seasons[key])
                            }
                        availableSeasons = items
                        isLoading = false
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: "Failed to load seasons."
                        isLoading = false
                    }
                }

                Dialog(onDismissRequest = { finish() }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Select Season",
                                fontFamily = Gilroy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            when {
                                isLoading -> {
                                    CircularProgressIndicator(
                                        color = Color(0xFFBBA00C),
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                                error != null -> {
                                    Text(
                                        text = error ?: "Error loading seasons",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { finish() }) {
                                        Text("Close")
                                    }
                                }
                                availableSeasons.isEmpty() -> {
                                    Text(
                                        text = "No available seasons found.",
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { finish() }) {
                                        Text("Close")
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(availableSeasons) { (id, label) ->
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectSeason(id, label)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontFamily = Gilroy,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        }
    }

    private fun selectSeason(seasonId: String, seasonTitle: String) {
        lifecycleScope.launch {
            val context = this@PbbWidgetSelectSeasonActivity
            val manager = GlanceAppWidgetManager(context)

            val glanceIds = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                listOf(manager.getGlanceIdBy(appWidgetId))
            } else {
                manager.getGlanceIds(PbbUpdatesWidget::class.java)
            }

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[PbbUpdatesWidget.KEY_SELECTED_SEASON_ID] = seasonId
                        this[PbbUpdatesWidget.KEY_SEASON_TITLE] = seasonTitle
                        this[PbbUpdatesWidget.KEY_SEASON_AVAILABLE] = true
                    }
                }
            }

            val workRequest = OneTimeWorkRequestBuilder<PbbWidgetWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
