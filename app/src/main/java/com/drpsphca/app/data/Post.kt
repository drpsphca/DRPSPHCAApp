package com.drpsphca.app.data

import com.google.gson.annotations.SerializedName

data class Post(
    val id: Int,
    val date: String,
    val title: Rendered,
    val content: Rendered,
    val excerpt: Rendered,
    @SerializedName("_embedded") val embedded: Embedded?
)

data class Rendered(
    val rendered: String
)

data class Embedded(
    @SerializedName("wp:featuredmedia") val featuredMedia: List<FeaturedMedia>?,
    @SerializedName("wp:term") val terms: List<List<Term>>?
)

data class FeaturedMedia(
    @SerializedName("source_url") val sourceUrl: String
)

data class Term(
    val id: Int,
    val name: String,
    val taxonomy: String
)
