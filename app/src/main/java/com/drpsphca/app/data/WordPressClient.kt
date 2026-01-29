package com.drpsphca.app.data

import com.drpsphca.app.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WordPressClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "com.drpsphca.app")
                .header("Authorization", "Bearer ${BuildConfig.WORDPRESS_API_KEY}")
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    private val gson = GsonBuilder().create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.WORDPRESS_API_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: WordPressApi by lazy {
        retrofit.create(WordPressApi::class.java)
    }
}
