package com.drpsphca.app.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WordPressClient {
    private const val BASE_URL = "https://drpsphca.com/wp-json/wp/v2/"
    private const val AUTH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjEsIm5hbWUiOiJkcnBzcGhjYSIsImlhdCI6MTc2OTQyOTMzNCwiZXhwIjoxOTI3MTA5MzM0fQ.-nKP0IFtPUgyvWb2hIsuFep424Xx2CYYCpITHR_GJoA"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "com.drpsphca.app")
                .header("Authorization", "Bearer $AUTH_TOKEN")
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
