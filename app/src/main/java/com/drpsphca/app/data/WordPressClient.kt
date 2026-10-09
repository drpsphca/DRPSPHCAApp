package com.drpsphca.app.data

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WordPressClient {
    private val gson = GsonBuilder().create()

    private fun getOkHttpClient(apiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "com.drpsphca.app")
                    .header("Authorization", "Bearer $apiKey")
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
    }

    private fun getRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: WordPressApi by lazy {
        val apiKey = com.drpsphca.app.BuildConfig.WORDPRESS_API_KEY
        val baseUrl = com.drpsphca.app.BuildConfig.WORDPRESS_API_URL
        val okClient = getOkHttpClient(apiKey)
        getRetrofit(baseUrl, okClient).create(WordPressApi::class.java)
    }

    val tvalApi: WordPressApi by lazy {
        val apiKey = com.drpsphca.app.BuildConfig.TVAL_API_KEY
        val baseUrl = com.drpsphca.app.BuildConfig.TVAL_API_URL
        val okClient = getOkHttpClient(apiKey)
        getRetrofit(baseUrl, okClient).create(WordPressApi::class.java)
    }

    val appxApi: WordPressApi by lazy {
        val apiKey = com.drpsphca.app.BuildConfig.APPX_API_KEY
        val baseUrl = com.drpsphca.app.BuildConfig.APPX_API_URL
        val okClient = getOkHttpClient(apiKey)
        getRetrofit(baseUrl, okClient).create(WordPressApi::class.java)
    }
}
