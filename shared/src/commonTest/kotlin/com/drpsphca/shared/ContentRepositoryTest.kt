package com.drpsphca.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ContentRepositoryTest {
    @Test
    fun parsesWordPressJsonArrayFromResponseText() {
        val posts = parseJsonArray(
            """[{"title":{"rendered":"Sample"},"link":"https://drpsphca.com/sample"}]""",
        )

        assertEquals(1, posts.size)
        assertEquals("Sample", posts.single().jsonObject["title"]?.jsonObject?.get("rendered")?.jsonPrimitive?.content)
    }

    @Test
    fun reportsEmptyWordPressResponseWithEndpointContext() {
        val error = assertFailsWith<IllegalArgumentException> {
            parseJsonArray("", "posts from https://drpsphca.com/wp-json/ (page 1)")
        }

        assertTrue(error.message.orEmpty().contains("https://drpsphca.com/wp-json/"))
        assertTrue(error.message.orEmpty().contains("empty JSON response"))
    }

    @Test
    fun parsesWordPressRssArticle() {
        val articles = parseRss(
            """
                <rss><channel><item>
                  <title><![CDATA[News &amp; Updates]]></title>
                  <link>https://drpsphca.com/blog/sample</link>
                  <pubDate>Tue, 03 Nov 2026 06:34:06 +0000</pubDate>
                  <description><![CDATA[<p>A short &amp; useful summary.</p>]]></description>
                  <content:encoded><![CDATA[<p>Article body</p><img src="https://example.com/image.jpg"/>]]></content:encoded>
                </item></channel></rss>
            """.trimIndent(),
        )

        assertEquals(1, articles.size)
        assertEquals("News & Updates", articles.single().title)
        assertEquals("A short & useful summary.", articles.single().excerpt)
        assertEquals("https://example.com/image.jpg", articles.single().image)
        assertTrue(articles.single().content.contains("Article body"))
    }

    @Test
    fun parsesAuthenticatedWordPressFeaturedImageAndTags() {
        val posts = Json.parseToJsonElement(
            """
                [{
                  "date": "2026-11-04T10:00:00",
                  "link": "https://drpsphca.com/blog/sample",
                  "title": {"rendered": "Sample post"},
                  "excerpt": {"rendered": "<p>Sample summary</p>"},
                  "content": {"rendered": "<p>Sample body</p>"},
                  "_embedded": {
                    "wp:featuredmedia": [{
                      "source_url": "https://drpsphca.com/wp-content/uploads/sample.jpg",
                      "media_details": {
                        "sizes": {
                          "large": {"source_url": "https://drpsphca.com/wp-content/uploads/sample-large.jpg"}
                        }
                      }
                    }],
                    "wp:term": [[{"taxonomy": "post_tag", "name": "Updates"}]]
                  }
                }]
            """.trimIndent(),
        ).let { it as kotlinx.serialization.json.JsonArray }

        val article = parseWordPressPosts(
            posts,
            authorization = "Bearer test-token",
            authorizationOrigin = "https://drpsphca.com",
        ).single()

        assertEquals("Sample post", article.title)
        assertEquals("Sample summary", article.excerpt)
        assertEquals(
            "https://drpsphca.com/wp-content/uploads/sample-large.jpg",
            article.image,
        )
        assertEquals(listOf("Updates"), article.tags)
        assertEquals("Bearer test-token", article.imageAuthorization)
        assertEquals("https://drpsphca.com", article.imageAuthorizationOrigin)
    }

    @Test
    fun combinesPbbApiAndCollab3AnnouncementsWithoutDuplicates() {
        val apiPost = Article(
            title = "Collab 3 announcement",
            date = "2026-11-03T10:00:00",
            url = "https://pbbupdates.drpsphca.com/posts/collab3",
        )
        val duplicate = apiPost.copy(url = "https://drpsphca.com/collab3/announcement")
        val newerPost = Article(
            title = "New collab3 update",
            date = "2026-11-04T10:00:00",
            url = "https://drpsphca.com/collab3/new-update",
        )

        val merged = mergePbbAnnouncements(listOf(apiPost), listOf(duplicate, newerPost))

        assertEquals(listOf(newerPost, apiPost), merged)
    }

    @Test
    fun parsesPbbSeasonDetailsForIosBoard() {
        val index = Json.decodeFromString<BoardIndex>(
            """
                {
                  "posts_url": "https://pbbupdates.drpsphca.com/api/posts.json",
                  "season_list": [{
                    "id": "collab3",
                    "label": "Collab 3.0",
                    "available": true
                  }],
                  "seasons": {
                    "collab3": {
                      "season": {
                        "id": "collab3",
                        "title": "Pinoy Big Brother: Collab 3.0",
                        "short_title": "PBB Collab 3.0",
                        "premiere": "2026-10-24",
                        "days": 99,
                        "status": "live",
                        "voting_open": true,
                        "current_week": 2,
                        "current_day": 12,
                        "hosts": ["Host One"],
                        "winner": "bela-arcal"
                      },
                      "housemates": [{
                        "id": "bela-arcal",
                        "name": "Bela Arcal",
                        "display_name": "Bela",
                        "age_on_entry": 29,
                        "type": "Kapuso",
                        "day_entered": 1,
                        "status": "safe",
                        "nomination_points": 3,
                        "vote_percent": 20.5
                      }],
                      "evictions": [{
                        "week": 2,
                        "day": 12,
                        "vote_type": "save_evict",
                        "desk_state": "closed",
                        "results_as_of": "2026-10-30",
                        "nominated": [{
                          "id": "bela-arcal",
                          "label": "Bela",
                          "percent": 42.5,
                          "result": "saved"
                        }],
                        "evicted": []
                      }],
                      "nomination_tallies": [{
                        "id": 1,
                        "week": 2,
                        "title": "Nominations",
                        "method": "House vote",
                        "entries": [{
                          "id": "bela-arcal",
                          "label": "3 pts",
                          "state": "not_nominated"
                        }]
                      }],
                      "weekly_tasks": [{
                        "week": 2,
                        "day": 12,
                        "title": "Task",
                        "status": "passed",
                        "note": "Task note"
                      }]
                    }
                  }
                }
            """.trimIndent(),
        )
        val season = index.seasons.getValue("collab3")
        val info = requireNotNull(season.season)

        assertTrue(info.available)
        assertTrue(info.votingOpen)
        assertEquals("Host One", info.hosts.single())
        assertTrue(index.postsUrl?.endsWith("posts.json") == true)
        assertEquals("Collab 3.0", index.seasonList.single().label)
        assertEquals(29, season.housemates.single().ageOnEntry)
        assertEquals("Kapuso", season.housemates.single().type)
        assertEquals(20.5, season.housemates.single().votePercent)
        assertEquals(42.5, season.evictions.single().nominated.single().percent)
        assertEquals("closed", season.evictions.single().deskState)
        assertEquals("Bela", season.evictions.single().nominated.single().label)
        assertEquals("House vote", season.nominationTallies.single().method)
        assertEquals("Task note", season.weeklyTasks.single().note)
    }

    @Test
    fun selectsFirstAvailablePbbSeasonAndRestoresPersistedChoice() {
        val index = Json.decodeFromString<BoardIndex>(
            """
                {
                  "season_list": [
                    {"id":"unavailable","label":"Unavailable","available":false},
                    {"id":"gen11","label":"Gen 11","available":true},
                    {"id":"collab3","label":"Collab 3.0","available":true}
                  ],
                  "seasons": {
                    "unavailable": {"season":{}},
                    "gen11": {"season":{}},
                    "collab3": {"season":{}}
                  }
                }
            """.trimIndent(),
        )

        assertEquals(listOf("gen11", "collab3"), availablePbbSeasonIds(index))
        assertEquals("gen11", resolvePbbSeason(index, null))
        assertEquals("collab3", resolvePbbSeason(index, "collab3"))
        assertEquals("gen11", resolvePbbSeason(index, "unavailable"))
    }

    @Test
    fun parsesPbbAnnouncementContentFromPostsEndpoint() {
        val posts = Json.decodeFromString<List<Article>>(
            """
                [{
                  "title":"PBB announcement",
                  "date":"2026-11-04",
                  "url":"https://pbbupdates.drpsphca.com/announcements/sample",
                  "excerpt":"Short summary",
                  "featured_image":"https://example.com/featured.png",
                  "content_html":"<p>Full announcement body.</p>",
                  "content_text":"Full announcement body."
                }]
            """.trimIndent(),
        )

        assertEquals("Short summary", posts.single().excerpt)
        assertEquals("https://example.com/featured.png", posts.single().image)
        assertEquals("<p>Full announcement body.</p>", posts.single().contentHtml)
    }

    @Test
    fun preservesHtmlHeadingsParagraphsInlineStylesLinksAndLists() {
        val blocks = htmlArticleBlocks(
            """
                <h2>Section &amp; title</h2>
                <p>Body with <strong>bold</strong>, <em>emphasis</em>, and <a href="https://example.com">a link</a>.</p>
                <ul><li>First item</li><li>Second item</li></ul>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                HtmlBlockType.HEADING,
                HtmlBlockType.BODY,
                HtmlBlockType.LIST_ITEM,
                HtmlBlockType.LIST_ITEM,
            ),
            blocks.map { it.type },
        )
        assertEquals("Section & title", blocks.first().text.text)
        assertEquals(2, blocks.first().headingLevel)
        assertTrue(blocks[1].text.text.contains("bold"))
        assertTrue(blocks[1].text.getStringAnnotations("URL", 0, blocks[1].text.length)
            .any { it.item == "https://example.com" })
        assertEquals("• First item", blocks[2].text.text)
        assertEquals("• Second item", blocks[3].text.text)
    }
}
