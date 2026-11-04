package com.drpsphca.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
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
