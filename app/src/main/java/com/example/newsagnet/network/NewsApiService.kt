package com.example.newsagnet.network

import com.example.newsagnet.network.dto.GeoFeatureCollectionDto
import com.example.newsagnet.network.dto.NewsArticleDto
import com.example.newsagnet.network.dto.NewsListDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit REST API interface for GlobalNewsAI backend service.
 * Supports both suspend coroutine functions and synchronous/asynchronous Retrofit Call methods.
 */
interface NewsApiService {

    @GET("news")
    suspend fun getNews(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("source") source: String? = null,
        @Query("keyword") keyword: String? = null
    ): NewsListDto

    @GET("news")
    fun getNewsCall(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("source") source: String? = null,
        @Query("keyword") keyword: String? = null
    ): Call<NewsListDto>

    @GET("news/geo")
    suspend fun getGeoNews(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 100
    ): GeoFeatureCollectionDto

    @GET("news/geo")
    fun getGeoNewsCall(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 100
    ): Call<GeoFeatureCollectionDto>

    @GET("news/{id}")
    suspend fun getNewsDetail(
        @Path("id") id: String
    ): NewsArticleDto

    @GET("news/{id}")
    fun getNewsDetailCall(
        @Path("id") id: String
    ): Call<NewsArticleDto>
}
