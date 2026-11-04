package com.drpsphca.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class Article(
    val title: String = "",
    val date: String = "",
    val url: String = "",
    val excerpt: String = "",
    val content: String = "",
    @SerialName("featured_image")
    val image: String? = null,
    @SerialName("content_html")
    val contentHtml: String? = null,
    @SerialName("content_text")
    val contentText: String? = null,
    val tags: List<String> = emptyList(),
    @Transient
    val imageAuthorization: String? = null,
    @Transient
    val imageAuthorizationOrigin: String? = null,
)

internal data class WordPressEndpoint(val apiUrl: String, val authorization: String) {
    fun requireAuthorization() {
        check(authorization.isNotBlank()) {
            "WordPress credentials are not configured for ${apiUrl.substringBefore("/wp-json")}"
        }
    }
}

internal object WordPressEndpoints {
    val main = WordPressEndpoint(
        WordPressAuthConfig.MAIN_API_URL,
        WordPressAuthConfig.MAIN_AUTHORIZATION,
    )
    val tval = WordPressEndpoint(
        WordPressAuthConfig.TVAL_API_URL,
        WordPressAuthConfig.TVAL_AUTHORIZATION,
    )
    val app = WordPressEndpoint(
        WordPressAuthConfig.APP_API_URL,
        WordPressAuthConfig.APP_AUTHORIZATION,
    )
}

@Serializable
data class BoardIndex(val seasons: Map<String, SeasonPack> = emptyMap())

@Serializable
data class SeasonPack(
    val season: SeasonInfo? = null,
    val housemates: List<Housemate> = emptyList(),
    val evictions: List<Eviction> = emptyList(),
    @SerialName("nomination_tallies") val nominationTallies: List<NominationTally> = emptyList(),
    @SerialName("weekly_tasks") val weeklyTasks: List<WeeklyTask> = emptyList(),
    val photos: Map<String, String> = emptyMap(),
)

@Serializable
data class SeasonInfo(
    val title: String? = null,
    @SerialName("short_title") val shortTitle: String? = null,
    val status: String? = null,
    val winner: String? = null,
    @SerialName("current_week") val currentWeek: Int? = null,
    @SerialName("current_day") val currentDay: Int? = null,
)

@Serializable
data class Housemate(
    val id: String,
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val moniker: String? = null,
    val status: String? = null,
    val hometown: String? = null,
    val place: Int? = null,
)

@Serializable
data class Eviction(
    val week: Int? = null,
    val date: String? = null,
    val note: String? = null,
    val evicted: List<String> = emptyList(),
)

@Serializable
data class NominationTally(
    val week: Int? = null,
    val title: String? = null,
    val entries: List<TallyEntry> = emptyList(),
)

@Serializable
data class TallyEntry(
    val name: String? = null,
    val label: String? = null,
    val state: String? = null,
)

@Serializable
data class WeeklyTask(
    val week: Int? = null,
    val date: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val status: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }

internal class ContentRepository(engine: HttpClientEngine) {
    private val client = HttpClient(engine) { expectSuccess = true }

    suspend fun wordpressArticles(
        endpoint: WordPressEndpoint,
        categorySlug: String? = null,
        limit: Int? = null,
    ): List<Article> {
        endpoint.requireAuthorization()
        val categoryId = categorySlug?.let { slug ->
            val categories = client.get("${endpoint.apiUrl.trimEnd('/')}/wp/v2/categories") {
                header(HttpHeaders.Authorization, endpoint.authorization)
                header(HttpHeaders.UserAgent, "com.drpsphca.app")
                parameter("slug", slug)
                parameter("per_page", 100)
            }.let { parseJsonArray(it.bodyAsText()) }
            categories.firstOrNull { it.jsonObject["slug"]?.stringValue() == slug }
                ?.jsonObject?.get("id")?.stringValue()
                ?: error("WordPress category '$slug' was not found.")
        }
        val pageSize = (limit ?: 100).coerceIn(1, 100)
        val posts = mutableListOf<JsonElement>()
        var page = 1
        var totalPages: Int? = null
        do {
            val response = client.get("${endpoint.apiUrl.trimEnd('/')}/wp/v2/posts") {
                header(HttpHeaders.Authorization, endpoint.authorization)
                header(HttpHeaders.UserAgent, "com.drpsphca.app")
                parameter("_embed", "1")
                parameter("per_page", pageSize)
                parameter("page", page)
                categoryId?.let { parameter("categories", it) }
            }
            val pagePosts = parseJsonArray(response.bodyAsText())
            posts.addAll(pagePosts)
            totalPages = response.headers["X-WP-TotalPages"]?.toIntOrNull()
                ?.coerceAtLeast(page) ?: totalPages
            if (limit != null && posts.size >= limit) break
            if (totalPages == null && pagePosts.size < pageSize) break
            page++
        } while (totalPages == null || page <= totalPages)
        val origin = Regex("""^https?://[^/]+""")
            .find(endpoint.apiUrl)?.value
            ?: error("Invalid WordPress API URL: ${endpoint.apiUrl}")
        return parseWordPressPosts(
            JsonArray(if (limit == null) posts else posts.take(limit)),
            endpoint.authorization,
            origin,
        )
    }

    suspend fun searchAllWordPress(query: String): List<Article> {
        require(query.isNotBlank()) { "Enter a search term." }
        return coroutineScope {
            listOf(WordPressEndpoints.main, WordPressEndpoints.tval, WordPressEndpoints.app)
                .map { endpoint -> async { searchWordPress(endpoint, query.trim()) } }
                .awaitAll()
                .flatten()
                .distinctBy { it.url }
                .sortedByDescending { it.date }
        }
    }

    private suspend fun searchWordPress(endpoint: WordPressEndpoint, query: String): List<Article> {
        endpoint.requireAuthorization()
        val origin = Regex("""^https?://[^/]+""")
            .find(endpoint.apiUrl)?.value
            ?: error("Invalid WordPress API URL: ${endpoint.apiUrl}")
        val posts = mutableListOf<JsonElement>()
        var page = 1
        var totalPages = 1
        do {
            val response: HttpResponse = client.get("${endpoint.apiUrl.trimEnd('/')}/wp/v2/posts") {
                header(HttpHeaders.Authorization, endpoint.authorization)
                header(HttpHeaders.UserAgent, "com.drpsphca.app")
                parameter("_embed", "1")
                parameter("search", query)
                parameter("per_page", 100)
                parameter("page", page)
            }
            totalPages = response.headers["X-WP-TotalPages"]?.toIntOrNull()?.coerceAtLeast(page) ?: page
            val pagePosts = parseJsonArray(response.bodyAsText())
            posts.addAll(pagePosts)
            page++
        } while (page <= totalPages && posts.isNotEmpty())
        return parseWordPressPosts(JsonArray(posts), endpoint.authorization, origin)
    }

    suspend fun pbbAnnouncements(): List<Article> =
        json.decodeFromString<List<Article>>(
            client.get("https://pbbupdates.drpsphca.com/api/posts.json").bodyAsText(),
        )

    suspend fun pbbBoard(): BoardIndex =
        json.decodeFromString(client.get("https://pbbupdates.drpsphca.com/api/index.json").bodyAsText())
}

internal fun parseJsonArray(responseBody: String): JsonArray =
    json.parseToJsonElement(responseBody).jsonArray

internal fun mergePbbAnnouncements(
    apiPosts: List<Article>,
    collab3Posts: List<Article>,
): List<Article> =
    (apiPosts + collab3Posts)
        .distinctBy { it.title.trim().lowercase() to it.date.take(10) }
        .sortedByDescending { it.date }

internal fun parseWordPressPosts(
    posts: JsonArray,
    authorization: String,
    authorizationOrigin: String,
): List<Article> =
    posts.mapNotNull { element ->
        val post = element as? JsonObject ?: return@mapNotNull null
        val title = post["title"].objectValue()?.get("rendered")?.stringValue()?.cleanHtml().orEmpty()
        val url = post["link"].stringValue().orEmpty()
        if (title.isBlank() || url.isBlank()) return@mapNotNull null
        val content = post["content"].objectValue()?.get("rendered")?.stringValue().orEmpty()
        val excerptHtml = post["excerpt"].objectValue()?.get("rendered")?.stringValue().orEmpty()
        val embedded = post["_embedded"].objectValue()
        val featuredMedia = embedded?.get("wp:featuredmedia")?.arrayValue()
            ?.firstOrNull()?.objectValue()
        val image = featuredMedia?.get("media_details").objectValue()
            ?.get("sizes").objectValue()
            ?.let { sizes ->
                sizes["medium_large"].objectValue()?.get("source_url")?.stringValue()
                    ?: sizes["large"].objectValue()?.get("source_url")?.stringValue()
            }
            ?: featuredMedia?.get("source_url")?.stringValue()
            ?: Regex("""<img[^>]+src=["']([^"']+)""", RegexOption.IGNORE_CASE)
                .find(content)?.groupValues?.get(1)
        val tags = embedded?.get("wp:term")?.arrayValue().orEmpty()
            .flatMap { it.arrayValue().orEmpty() }
            .filter { it.objectValue()?.get("taxonomy")?.stringValue() == "post_tag" }
            .mapNotNull { it.objectValue()?.get("name")?.stringValue() }
        Article(
            title = title,
            date = post["date"].stringValue().orEmpty(),
            url = url,
            excerpt = excerptHtml.cleanHtml(),
            content = content,
            image = image,
            tags = tags,
            imageAuthorization = authorization,
            imageAuthorizationOrigin = authorizationOrigin,
        )
    }

internal fun parseRss(xml: String): List<Article> {
    val itemPattern = Regex("<item\\b[^>]*>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
    return itemPattern.findAll(xml).mapNotNull { item ->
        val content = item.groupValues[1]
        val title = xmlValue(content, "title").cleanHtml()
        val url = xmlValue(content, "link").trim()
        if (title.isBlank() || url.isBlank()) return@mapNotNull null
        val html = xmlValue(content, "content:encoded")
            .ifBlank { xmlValue(content, "description") }
        Article(
            title = title,
            url = url,
            date = xmlValue(content, "pubDate").cleanHtml(),
            excerpt = xmlValue(content, "description").cleanHtml(),
            content = html,
            image = Regex("""<img[^>]+src=["']([^"']+)""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1),
            tags = Regex("<category(?:\\s[^>]*)?>(.*?)</category>", RegexOption.DOT_MATCHES_ALL)
                .findAll(content)
                .map { xmlValue(it.value, "category").cleanHtml() }
                .filter { it.isNotBlank() }
                .toList(),
        )
    }.toList()
}

private fun xmlValue(xml: String, tag: String): String {
    val escapedTag = Regex.escape(tag)
    val raw = Regex(
        "<$escapedTag(?:\\s[^>]*)?>(.*?)</$escapedTag>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    ).find(xml)?.groupValues?.get(1).orEmpty().trim()
    return if (raw.startsWith("<![CDATA[") && raw.endsWith("]]>")) {
        raw.removePrefix("<![CDATA[").removeSuffix("]]>").trim()
    } else {
        raw
    }
}

private fun String.cleanHtml(): String = replace(Regex("<[^>]*>"), " ")
    .decodeHtmlEntities()
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String.decodeHtmlEntities(): String = replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&apos;", "'")
    .replace("&#8217;", "'")
    .replace("&#8216;", "'")
    .replace("&#8220;", "\"")
    .replace("&#8221;", "\"")
    .replace("&#038;", "&")
    .replace("&nbsp;", " ")
    .replace("&#8211;", "–")
    .replace("&#8212;", "—")

private fun JsonElement?.objectValue(): JsonObject? = this as? JsonObject
private fun JsonElement?.arrayValue(): JsonArray? = this as? JsonArray
private fun JsonElement?.stringValue(): String? = (this as? JsonPrimitive)?.contentOrNull
