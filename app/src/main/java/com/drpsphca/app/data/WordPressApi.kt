package com.drpsphca.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WordPressApi {
    @GET("posts")
    suspend fun getPosts(
        @Query("_embed") embed: Boolean = true,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("categories") categories: Int? = null
    ): List<Post>

    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("posts/{id}")
    suspend fun getPost(
        @Path("id") id: Int,
        @Query("_embed") embed: Boolean = true
    ): Post
}
