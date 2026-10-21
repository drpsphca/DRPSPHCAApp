package com.drpsphca.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drpsphca.app.data.pbb.PbbRepository
import com.google.gson.Gson

class PbbWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("PbbWidgetWorker", "Starting PBB Widget Update Worker")
        return try {
            val repository = PbbRepository(context)
            val index = repository.getBoardIndex(forceRefresh = true)
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(PbbUpdatesWidget::class.java)
            val gson = Gson()

            glanceIds.forEach { glanceId ->
                val prefs = getAppWidgetState<androidx.datastore.preferences.core.Preferences>(
                    context, PreferencesGlanceStateDefinition, glanceId
                )
                val selectedSeasonId = prefs[PbbUpdatesWidget.KEY_SELECTED_SEASON_ID]

                if (!selectedSeasonId.isNullOrEmpty() && index.seasons.containsKey(selectedSeasonId)) {
                    val pack = index.seasons[selectedSeasonId]

                    if (pack?.season?.available == true) {
                        val seasonTitle = repository.getSeasonLabel(selectedSeasonId, pack)
                        val packJson = gson.toJson(pack)

                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { mutablePrefs ->
                            mutablePrefs.toMutablePreferences().apply {
                                this[PbbUpdatesWidget.KEY_SEASON_TITLE] = seasonTitle
                                this[PbbUpdatesWidget.KEY_SEASON_DATA] = packJson
                                this[PbbUpdatesWidget.KEY_SEASON_AVAILABLE] = true
                            }
                        }
                    } else {
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { mutablePrefs ->
                            mutablePrefs.toMutablePreferences().apply {
                                this[PbbUpdatesWidget.KEY_SEASON_AVAILABLE] = false
                                remove(PbbUpdatesWidget.KEY_SELECTED_SEASON_ID)
                            }
                        }
                    }
                } else {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { mutablePrefs ->
                        mutablePrefs.toMutablePreferences().apply {
                            this[PbbUpdatesWidget.KEY_SEASON_AVAILABLE] = false
                        }
                    }
                }
            }

            PbbUpdatesWidget().updateAll(context)
            Result.success()
        } catch (e: Exception) {
            Log.e("PbbWidgetWorker", "Error updating PBB widget", e)
            Result.retry()
        }
    }
}
