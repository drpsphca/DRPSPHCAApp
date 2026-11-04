package com.drpsphca.shared

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration

internal enum class HtmlBlockType {
    BODY,
    HEADING,
    QUOTE,
    LIST_ITEM,
    CODE,
    RULE,
}

internal data class HtmlBlock(
    val type: HtmlBlockType,
    val text: AnnotatedString,
    val headingLevel: Int = 0,
)

private data class InlineTag(val name: String, val value: String? = null)
private data class ListContext(val ordered: Boolean, var itemNumber: Int = 0)

private val htmlTokens = Regex("""(?s)<[^>]*>|[^<]+""")
private val tagName = Regex("""^</?\s*([a-zA-Z0-9]+)""")
private val hrefAttribute = Regex("""(?i)\bhref\s*=\s*["']([^"']+)["']""")

internal fun htmlArticleBlocks(html: String): List<HtmlBlock> {
    val blocks = mutableListOf<HtmlBlock>()
    var text = AnnotatedString.Builder()
    var type = HtmlBlockType.BODY
    var headingLevel = 0
    var suppressedTag: String? = null
    val inlineTags = mutableListOf<InlineTag>()
    val lists = mutableListOf<ListContext>()

    fun flush() {
        val content = text.toAnnotatedString()
        if (content.text.isNotBlank()) {
            blocks += HtmlBlock(type, content, headingLevel)
        }
        text = AnnotatedString.Builder()
        type = HtmlBlockType.BODY
        headingLevel = 0
    }

    fun appendText(raw: String) {
        if (suppressedTag != null) return
        val normalized = decodeHtmlText(raw)
            .replace(Regex("""[\t\r\n ]+"""), " ")
            .let { if (text.length == 0) it.trimStart() else it }
        if (normalized.isBlank()) return

        if (type == HtmlBlockType.LIST_ITEM && text.length == 0) {
            val list = lists.lastOrNull()
            val marker = if (list?.ordered == true) "${++list.itemNumber}. " else "• "
            text.append(marker)
        }
        val bold = type == HtmlBlockType.HEADING ||
            inlineTags.any { it.name in setOf("b", "strong") }
        val italic = type == HtmlBlockType.QUOTE ||
            inlineTags.any { it.name in setOf("i", "em") }
        val decoration = when {
            inlineTags.any { it.name in setOf("u", "ins") } -> TextDecoration.Underline
            inlineTags.any { it.name in setOf("s", "del", "strike") } -> TextDecoration.LineThrough
            inlineTags.any { it.name == "a" } -> TextDecoration.Underline
            else -> null
        }
        val link = inlineTags.lastOrNull { it.name == "a" }?.value
        val style = SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            fontFamily = if (type == HtmlBlockType.CODE || inlineTags.any { it.name == "code" }) {
                FontFamily.Monospace
            } else {
                null
            },
            textDecoration = decoration,
            color = if (!link.isNullOrBlank()) Color(0xFF1565C0) else Color.Unspecified,
        )
        if (!link.isNullOrBlank()) text.pushStringAnnotation("URL", link)
        text.pushStyle(style)
        text.append(normalized)
        text.pop()
        if (!link.isNullOrBlank()) text.pop()
    }

    for (token in htmlTokens.findAll(html).map { it.value }) {
        if (!token.startsWith('<')) {
            appendText(token)
            continue
        }
        val name = tagName.find(token)?.groupValues?.getOrNull(1)?.lowercase() ?: continue
        val closing = token.startsWith("</")
        val selfClosing = token.endsWith("/>")

        if (suppressedTag != null) {
            if (closing && name == suppressedTag) suppressedTag = null
            continue
        }
        if (!closing && name in setOf("script", "style", "noscript")) {
            suppressedTag = name
            continue
        }

        when (name) {
            "p", "div", "section", "article", "main", "header", "footer" -> {
                if (closing) flush() else flush()
            }
            "blockquote" -> {
                if (closing) flush() else {
                    flush()
                    type = HtmlBlockType.QUOTE
                }
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                if (closing) {
                    flush()
                } else {
                    flush()
                    type = HtmlBlockType.HEADING
                    headingLevel = name.last().digitToInt()
                }
            }
            "pre" -> {
                if (closing) flush() else {
                    flush()
                    type = HtmlBlockType.CODE
                }
            }
            "ul", "ol" -> {
                if (closing) {
                    flush()
                    if (lists.isNotEmpty()) lists.removeAt(lists.lastIndex)
                } else {
                    flush()
                    lists += ListContext(ordered = name == "ol")
                }
            }
            "li" -> {
                if (closing) flush() else {
                    flush()
                    type = HtmlBlockType.LIST_ITEM
                }
            }
            "br" -> text.append("\n")
            "hr" -> {
                flush()
                blocks += HtmlBlock(HtmlBlockType.RULE, AnnotatedString("—"))
            }
            "b", "strong", "i", "em", "u", "ins", "s", "del", "strike", "code", "a" -> {
                if (closing) {
                    val index = inlineTags.indexOfLast { it.name == name }
                    if (index >= 0) inlineTags.removeAt(index)
                } else if (!selfClosing) {
                    val value = if (name == "a") hrefAttribute.find(token)?.groupValues?.get(1) else null
                    inlineTags += InlineTag(name, value)
                }
            }
        }
    }
    flush()
    return blocks
}

private fun decodeHtmlText(value: String): String = value
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&#39;", "'")
    .replace("&nbsp;", " ")
    .replace("&#8217;", "’")
    .replace("&#8216;", "‘")
    .replace("&#8220;", "“")
    .replace("&#8221;", "”")
    .replace("&#8211;", "–")
    .replace("&#8212;", "—")
    .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
        match.groupValues[1].toIntOrNull(16)?.let(::codePointString) ?: match.value
    }
    .replace(Regex("&#([0-9]+);")) { match ->
        match.groupValues[1].toIntOrNull()?.let(::codePointString) ?: match.value
    }

private fun codePointString(codePoint: Int): String =
    if (codePoint in 0..0xFFFF && codePoint !in 0xD800..0xDFFF) codePoint.toChar().toString()
    else if (codePoint in 0x10000..0x10FFFF) {
        val value = codePoint - 0x10000
        "${(0xD800 + (value shr 10)).toChar()}${(0xDC00 + (value and 0x3FF)).toChar()}"
    } else {
        "\uFFFD"
    }
