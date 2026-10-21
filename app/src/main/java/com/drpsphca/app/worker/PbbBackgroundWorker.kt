package com.drpsphca.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drpsphca.app.MainActivity
import com.drpsphca.app.R
import com.drpsphca.app.data.pbb.EvictionNominee
import com.drpsphca.app.data.pbb.PbbRepository
import com.drpsphca.app.data.pbb.SeasonPack
import java.util.Locale

class PbbBackgroundWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val PREFS_NAME = "pbb_background_worker_prefs"
        const val KEY_FIRST_RUN = "pbb_first_run"
        const val KEY_SEEN_ANNOUNCEMENT_URLS = "seen_announcement_urls"
        const val CHANNEL_ID = "pbb_updates"
        const val CHANNEL_NAME = "PBB Updates"
    }

    override suspend fun doWork(): Result {
        Log.d("PbbBackgroundWorker", "Running periodic background check")
        return try {
            val repository = PbbRepository(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true)

            val mergedAnnouncements = repository.getMergedAnnouncements()
            val boardIndex = repository.getBoardIndex(forceRefresh = true)
            val availableSeasonKeys = repository.getSortedSeasonKeys(boardIndex.seasons)

            val currentAnnouncementUrls = mergedAnnouncements.mapNotNull { it.url }.toSet()

            if (isFirstRun) {
                // Initialize DataStore/SharedPreferences without notifying
                val editor = prefs.edit()
                editor.putStringSet(KEY_SEEN_ANNOUNCEMENT_URLS, currentAnnouncementUrls)

                availableSeasonKeys.forEach { seasonKey ->
                    val pack = boardIndex.seasons[seasonKey] ?: return@forEach
                    val lastTally = pack.nominationTallies.lastOrNull()
                    if (lastTally?.id != null) {
                        editor.putInt("last_seen_tally_id_$seasonKey", lastTally.id)
                    }
                    editor.putBoolean("last_seen_voting_open_$seasonKey", pack.season?.votingOpen == true)

                    val lastEviction = pack.evictions.lastOrNull()
                    if (lastEviction?.week != null) {
                        editor.putInt("last_seen_eviction_week_$seasonKey", lastEviction.week)
                    }
                    if (lastEviction?.deskState != null) {
                        editor.putString("last_seen_desk_state_$seasonKey", lastEviction.deskState)
                    }
                }

                editor.putBoolean(KEY_FIRST_RUN, false)
                editor.apply()

                Log.d("PbbBackgroundWorker", "First run completed and state saved.")
                return Result.success()
            }

            // --- SUBSEQUENT RUNS ---

            // 1. Announcements check
            val seenUrls = prefs.getStringSet(KEY_SEEN_ANNOUNCEMENT_URLS, emptySet()) ?: emptySet()
            val newAnnouncements = mergedAnnouncements.filter { post ->
                !post.url.isNullOrEmpty() && !seenUrls.contains(post.url)
            }

            newAnnouncements.forEach { post ->
                val title = post.title ?: "PBB Announcement"
                val text = post.excerpt?.takeIf { it.isNotBlank() } ?: post.contentText ?: ""
                if (text.isNotBlank()) {
                    showNotification(
                        title = title,
                        text = text,
                        type = "announcement",
                        seasonId = null,
                        url = post.url
                    )
                }
            }

            // Update seen announcement URLs
            val updatedSeenUrls = seenUrls.toMutableSet().apply {
                addAll(currentAnnouncementUrls)
            }
            prefs.edit().putStringSet(KEY_SEEN_ANNOUNCEMENT_URLS, updatedSeenUrls).apply()

            // 2. Season updates check (Nominations & Evictions)
            availableSeasonKeys.forEach { seasonKey ->
                val pack = boardIndex.seasons[seasonKey] ?: return@forEach
                if (pack.season?.available == false) return@forEach

                // Nomination results check
                checkNominationResults(seasonKey, pack, prefs)

                // Eviction results check
                checkEvictionResults(seasonKey, pack, prefs)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PbbBackgroundWorker", "Error in periodic check", e)
            Result.retry()
        }
    }

    private fun checkNominationResults(
        seasonKey: String,
        pack: SeasonPack,
        prefs: android.content.SharedPreferences
    ) {
        val lastTally = pack.nominationTallies.lastOrNull() ?: return
        val previousTallyId = prefs.getInt("last_seen_tally_id_$seasonKey", -1)
        val previousVotingOpen = prefs.getBoolean("last_seen_voting_open_$seasonKey", false)
        val currentVotingOpen = pack.season?.votingOpen == true

        val tallyChanged = lastTally.id != null && lastTally.id != previousTallyId
        val votingOpenedNow = currentVotingOpen && !previousVotingOpen

        if (tallyChanged || votingOpenedNow) {
            val nominatedEntries = lastTally.entries.filter { it.state.equals("nominated", ignoreCase = true) }
            if (nominatedEntries.isNotEmpty()) {
                val ordinal = getTallyOrdinal(lastTally.title, lastTally.week)
                val title = "$ordinal Nomination Night - Voting is now OPEN!"

                val formattedEntries = nominatedEntries.joinToString(", ") { entry ->
                    val housemate = pack.housemates.find { it.id == entry.id }
                    val name = housemate?.displayName ?: housemate?.name ?: entry.name ?: entry.id ?: "Housemate"
                    val label = entry.label ?: ""
                    if (label.isNotBlank()) "$name - $label" else name
                }

                val text = "Nominated housemates this week: $formattedEntries"

                if (text.isNotBlank()) {
                    showNotification(
                        title = title,
                        text = text,
                        type = "nomination",
                        seasonId = seasonKey,
                        url = null
                    )
                }
            }

            // Save new state
            val editor = prefs.edit()
            if (lastTally.id != null) {
                editor.putInt("last_seen_tally_id_$seasonKey", lastTally.id)
            }
            editor.putBoolean("last_seen_voting_open_$seasonKey", currentVotingOpen)
            editor.apply()
        }
    }

    private fun checkEvictionResults(
        seasonKey: String,
        pack: SeasonPack,
        prefs: android.content.SharedPreferences
    ) {
        val latestEviction = pack.evictions.lastOrNull() ?: return
        val previousEvictionWeek = prefs.getInt("last_seen_eviction_week_$seasonKey", -1)
        val previousDeskState = prefs.getString("last_seen_desk_state_$seasonKey", "")
        val currentDeskState = latestEviction.deskState?.lowercase(Locale.ROOT) ?: ""

        val weekChanged = latestEviction.week != null && latestEviction.week != previousEvictionWeek
        val closedRevealedNow = currentDeskState == "closed_revealed" && previousDeskState != "closed_revealed"

        if (weekChanged || closedRevealedNow) {
            if (latestEviction.nominated.isNotEmpty()) {
                val ordinal = toOrdinal(latestEviction.week ?: 1)
                val title = "$ordinal Eviction Night Results:"

                val formattedNominees = latestEviction.nominated.joinToString(", ") { nominee ->
                    val housemate = pack.housemates.find { it.id == nominee.id }
                    val name = housemate?.displayName ?: housemate?.name ?: nominee.label ?: nominee.id ?: "Housemate"
                    val resultText = formatResultText(nominee.result)
                    val percentText = formatEvictionPercent(nominee.percent)
                    "$name: $resultText ($percentText)"
                }

                val text = "Eviction Results: $formattedNominees"

                if (text.isNotBlank()) {
                    showNotification(
                        title = title,
                        text = text,
                        type = "eviction",
                        seasonId = seasonKey,
                        url = null
                    )
                }
            }

            // Save new state
            val editor = prefs.edit()
            if (latestEviction.week != null) {
                editor.putInt("last_seen_eviction_week_$seasonKey", latestEviction.week)
            }
            if (latestEviction.deskState != null) {
                editor.putString("last_seen_desk_state_$seasonKey", latestEviction.deskState)
            }
            editor.apply()
        }
    }

    private fun showNotification(
        title: String,
        text: String,
        type: String,
        seasonId: String?,
        url: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fcm_type", type)
            if (!seasonId.isNullOrEmpty()) {
                putExtra("fcm_season", seasonId)
            }
            if (!url.isNullOrEmpty()) {
                putExtra("fcm_url", url)
            }
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.phcaapp_notificationsicon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun formatResultText(rawResult: String?): String {
        if (rawResult.isNullOrBlank()) return "Nominated"
        val clean = rawResult.replace('_', ' ').trim()
        return clean.split(' ').joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    private fun formatEvictionPercent(percent: Double?): String {
        return if (percent != null) {
            "%.2f%%".format(Locale.US, percent)
        } else {
            "0.00%"
        }
    }

    private fun toOrdinal(n: Int): String {
        val hundredRemainder = n % 100
        val tenRemainder = n % 10
        if (hundredRemainder in 11..13) {
            return "${n}th"
        }
        return when (tenRemainder) {
            1 -> "${n}st"
            2 -> "${n}nd"
            3 -> "${n}rd"
            else -> "${n}th"
        }
    }

    private fun getTallyOrdinal(tallyTitle: String?, week: Int?): String {
        if (!tallyTitle.isNullOrEmpty()) {
            val regex = Regex("(\\d+(st|nd|rd|th))", RegexOption.IGNORE_CASE)
            val match = regex.find(tallyTitle)
            if (match != null) {
                return match.value
            }
        }
        return toOrdinal(week ?: 1)
    }
}
