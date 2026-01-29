package com.drpsphca.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WordPressApi {
    @GET("wp/v2/posts?_embed")
    suspend fun getPosts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("categories") categories: Int? = null
    ): List<Post>

    @GET("wp/v2/posts/{id}?_embed")
    suspend fun getPost(@Path("id") id: Int): Post

    @GET("wp/v2/categories")
    suspend fun getCategories(@Query("per_page") perPage: Int = 100): List<Category>

    @GET("wp/v2/media/{id}")
    suspend fun getMedia(@Path("id") id: Int): Media
}
