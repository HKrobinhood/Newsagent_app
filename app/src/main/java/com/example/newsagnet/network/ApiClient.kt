package com.example.newsagnet.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton factory creating and configuring Retrofit API clients.
 */
object ApiClient {

    /**
     * Standard Android emulator loopback host targeting machine localhost:8000.
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/v1/"

    private var customBaseUrl: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val newsApiService: NewsApiService by lazy {
        createService(customBaseUrl ?: DEFAULT_BASE_URL)
    }

    val chatApiService: ChatApiService by lazy {
        createChatService(customBaseUrl ?: DEFAULT_BASE_URL)
    }

    val authApiService: AuthApiService by lazy {
        createAuthService(customBaseUrl ?: DEFAULT_BASE_URL)
    }

    fun setBaseUrl(url: String) {
        customBaseUrl = url
    }

    fun createService(baseUrl: String = DEFAULT_BASE_URL): NewsApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }

    fun createChatService(baseUrl: String = DEFAULT_BASE_URL): ChatApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApiService::class.java)
    }

    fun createAuthService(baseUrl: String = DEFAULT_BASE_URL): AuthApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}
