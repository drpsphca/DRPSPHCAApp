package com.drpsphca.app.data.pbb

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

interface PbbApi {
    @GET("api/index.json")
    suspend fun index(): BoardIndex

    @GET("api/posts.json")
    suspend fun posts(): List<AnnouncementPost>
}

data class BoardIndex(
    @Json(name = "posts_url") val postsUrl: String? = null,
    val seasons: Map<String, SeasonPack> = emptyMap()
)

data class SeasonPack(
    val season: SeasonInfo? = null,
    val housemates: List<Housemate> = emptyList(),
    val evictions: List<Eviction> = emptyList(),
    @Json(name = "nomination_tallies") val nominationTallies: List<NominationTally> = emptyList(),
    @Json(name = "weekly_tasks") val weeklyTasks: List<WeeklyTask> = emptyList(),
    val photos: Map<String, String> = emptyMap()
)

data class SeasonInfo(
    val id: String? = null,
    val title: String? = null,
    @Json(name = "short_title") val shortTitle: String? = null,
    val premiere: String? = null,
    val finale: String? = null,
    val days: Int? = null,
    val status: String? = null,
    @Json(name = "voting_open") val votingOpen: Boolean = false,
    @Json(name = "current_week") val currentWeek: Int? = null,
    @Json(name = "current_day") val currentDay: Int? = null,
    val winner: String? = null,
    val hosts: List<String> = emptyList()
)

data class Housemate(
    val id: String,
    val name: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    val moniker: String? = null,
    @Json(name = "age_on_entry") val ageOnEntry: Int? = null,
    val hometown: String? = null,
    val type: String? = null,
    @Json(name = "pair_id") val pairId: String? = null,
    @Json(name = "day_entered") val dayEntered: Int? = null,
    @Json(name = "day_exited") val dayExited: Int? = null,
    val status: String? = null,
    val place: Int? = null,
    @Json(name = "nomination_points") val nominationPoints: Int? = null,
    @Json(name = "vote_percent") val votePercent: Double? = null,
    @Json(name = "vote_percent_finale") val votePercentFinale: Double? = null
)

data class NominationTally(
    val id: Int? = null,
    val title: String? = null,
    val week: Int? = null,
    val method: String? = null,
    val note: String? = null,
    val entries: List<TallyEntry> = emptyList()
)

data class TallyEntry(
    val id: String? = null,
    val name: String? = null,
    val label: String? = null,
    val state: String? = null
)

data class Eviction(
    val week: Int? = null,
    val day: Int? = null,
    val date: String? = null,
    @Json(name = "vote_type") val voteType: String? = null,
    @Json(name = "desk_state") val deskState: String? = null,
    @Json(name = "results_as_of") val resultsAsOf: String? = null,
    val note: String? = null,
    val nominated: List<EvictionNominee> = emptyList(),
    val evicted: List<String> = emptyList()
)

data class EvictionNominee(
    val id: String? = null,
    val label: String? = null,
    val percent: Double? = null,
    val result: String? = null
)

data class WeeklyTask(
    val week: Int? = null,
    val day: Int? = null,
    val date: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val status: String? = null,
    val note: String? = null
)

data class AnnouncementPost(
    val title: String? = null,
    val date: String? = null,
    val url: String? = null,
    val excerpt: String? = null,
    @Json(name = "featured_image") val featuredImage: String? = null,
    @Json(name = "content_html") val contentHtml: String? = null,
    @Json(name = "content_text") val contentText: String? = null,
    val source: String = "PBB Updates"
)

object Network {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val api: PbbApi = Retrofit.Builder()
        .baseUrl("https://pbbupdates.drpsphca.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PbbApi::class.java)
}
