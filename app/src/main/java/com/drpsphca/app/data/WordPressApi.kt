package com.drpsphca.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WordPressApi {
    @GET("wp/v2/posts?_embed")
    suspend fun getPosts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("categories") categories: Int? = null,
        @Query("search") search: String? = null,
        @Query("tags") tags: String? = null
    ): List<Post>

    @GET("wp/v2/posts/{id}?_embed")
    suspend fun getPost(@Path("id") id: Int): Post

    @GET("wp/v2/categories")
    suspend fun getCategories(@Query("per_page") perPage: Int = 100): List<Category>

    @GET("wp/v2/tags")
    suspend fun getTags(@Query("search") search: String? = null, @Query("per_page") perPage: Int = 100): List<Tag>
}

data class Tag(
    val id: Int,
    val name: String,
    val slug: String
)
