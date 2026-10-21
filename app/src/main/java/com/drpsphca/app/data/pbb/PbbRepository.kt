package com.drpsphca.app.data.pbb

import android.content.Context
import android.content.SharedPreferences
import com.drpsphca.app.data.WordPressClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PbbRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pbb_prefs", Context.MODE_PRIVATE)

    private var cachedBoardIndex: BoardIndex? = null

    suspend fun getBoardIndex(forceRefresh: Boolean = false): BoardIndex = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedBoardIndex != null) {
            return@withContext cachedBoardIndex!!
        }
        val result = Network.api.index()
        cachedBoardIndex = result
        result
    }

    fun getSeasonLabel(seasonKey: String, seasonPack: SeasonPack?): String {
        return when (seasonKey.lowercase(Locale.ROOT)) {
            "gen11" -> "Gen 11"
            "collab3" -> "Collab 3.0"
            else -> seasonPack?.season?.shortTitle
                ?: seasonPack?.season?.title
                ?: seasonKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    fun getSortedSeasonKeys(seasons: Map<String, SeasonPack>): List<String> {
        val availableSeasons = seasons.filter { (_, pack) -> pack.season?.available == true }
        val keys = availableSeasons.keys.toList()
        // Order: collab3, then gen11, then any others
        return keys.sortedWith(Comparator { k1, k2 ->
            val rank1 = when (k1.lowercase(Locale.ROOT)) {
                "collab3" -> 1
                "gen11" -> 2
                else -> 3
            }
            val rank2 = when (k2.lowercase(Locale.ROOT)) {
                "collab3" -> 1
                "gen11" -> 2
                else -> 3
            }
            if (rank1 != rank2) rank1.compareTo(rank2) else k1.compareTo(k2)
        })
    }

    fun getAvailableSeasonKeys(seasons: Map<String, SeasonPack>): List<String> {
        return getSortedSeasonKeys(seasons)
    }

    fun determineDefaultSeason(seasons: Map<String, SeasonPack>): String? {
        if (seasons.isEmpty()) return null

        // Restore saved season if present
        val savedSeason = prefs.getString("selected_season_id", null)
        if (!savedSeason.isNullOrEmpty() && seasons.containsKey(savedSeason)) {
            return savedSeason
        }

        // 1. Prefer a season whose status is live or voting_open is true
        val liveOrVoting = seasons.entries.firstOrNull { (_, pack) ->
            pack.season?.status?.equals("live", ignoreCase = true) == true || pack.season?.votingOpen == true
        }
        if (liveOrVoting != null) {
            return liveOrVoting.key
        }

        // 2. Pick the season with the latest premiere date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sortedByPremiere = seasons.entries.sortedByDescending { (_, pack) ->
            val premiereStr = pack.season?.premiere
            if (!premiereStr.isNullOrEmpty()) {
                try {
                    dateFormat.parse(premiereStr)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } else 0L
        }

        val topSeason = sortedByPremiere.firstOrNull()
        if (topSeason != null && (topSeason.value.season?.premiere?.isNotEmpty() == true)) {
            return topSeason.key
        }

        // 3. Fallback preference: collab3, then gen11
        if (seasons.containsKey("collab3")) return "collab3"
        if (seasons.containsKey("gen11")) return "gen11"

        return seasons.keys.firstOrNull()
    }

    fun saveSelectedSeason(seasonId: String) {
        prefs.edit().putString("selected_season_id", seasonId).apply()
    }

    fun getPhotoUrl(seasonId: String, seasonPack: SeasonPack?, housemateId: String): String? {
        val file = seasonPack?.photos?.get(housemateId)
        return if (file.isNullOrBlank()) {
            null
        } else {
            resolvePhotoUrl(seasonId, file)
        }
    }

    fun resolvePhotoUrl(seasonId: String, photoPathOrFilename: String?): String {
        if (photoPathOrFilename.isNullOrBlank()) {
            return "https://pbbupdates.drpsphca.com/assets/img/misc/misc_hm.png"
        }
        return when {
            photoPathOrFilename.startsWith("http://") || photoPathOrFilename.startsWith("https://") -> photoPathOrFilename
            photoPathOrFilename.startsWith("/") -> "https://pbbupdates.drpsphca.com$photoPathOrFilename"
            else -> "https://pbbupdates.drpsphca.com/assets/img/$seasonId/$photoPathOrFilename"
        }
    }

    suspend fun getMergedAnnouncements(): List<AnnouncementPost> = coroutineScope {
        val jekyllDeferred = async(Dispatchers.IO) {
            try {
                Network.api.posts()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val wpDeferred = async(Dispatchers.IO) {
            try {
                fetchCollab3WordPressPosts()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val jekyllPosts = jekyllDeferred.await()
        val wpPosts = wpDeferred.await()

        val merged = (jekyllPosts + wpPosts).distinctBy { it.title to it.date }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        merged.sortedByDescending { post ->
            val dStr = post.date
            if (!dStr.isNullOrEmpty()) {
                try {
                    // Try yyyy-MM-dd or fallback parse
                    val cleanDate = dStr.take(10)
                    dateFormat.parse(cleanDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } else 0L
        }
    }

    private suspend fun fetchCollab3WordPressPosts(): List<AnnouncementPost> {
        val categories = try {
            WordPressClient.api.getCategories(perPage = 100)
        } catch (e: Exception) {
            emptyList()
        }

        val collab3Cat = categories.find { 
            it.slug.equals("collab3", ignoreCase = true) || it.name.contains("collab3", ignoreCase = true) 
        }

        val posts = if (collab3Cat != null) {
            WordPressClient.api.getPosts(categories = collab3Cat.id, perPage = 20)
        } else {
            WordPressClient.api.getPosts(search = "collab3", perPage = 20)
        }

        return posts.map { wpPost ->
            val imageUrl = wpPost.embedded?.featuredMedia?.firstOrNull()?.sourceUrl
            val cleanTitle = cleanHtmlText(wpPost.title.rendered)
            val cleanExcerpt = cleanHtmlText(wpPost.excerpt.rendered)
            val cleanContent = cleanHtmlText(wpPost.content.rendered)

            AnnouncementPost(
                title = cleanTitle,
                date = wpPost.date,
                url = wpPost.link,
                excerpt = cleanExcerpt,
                featuredImage = imageUrl,
                contentHtml = wpPost.content.rendered,
                contentText = cleanContent,
                source = "WordPress"
            )
        }
    }

    private fun cleanHtmlText(html: String): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html).toString().trim()
            }
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "").trim()
        }
    }
}
