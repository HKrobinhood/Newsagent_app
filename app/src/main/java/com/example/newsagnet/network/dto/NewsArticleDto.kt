package com.example.newsagnet.network.dto

import com.example.newsagnet.model.NewsArticle
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for NewsArticle REST response.
 */
data class NewsArticleDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("source_id")
    val sourceId: String,
    @SerializedName("source_name")
    val sourceName: String,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("original_url")
    val originalUrl: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("location_name")
    val locationName: String? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
) {
    /**
     * Map DTO to UI/domain NewsArticle model.
     */
    fun toDomainModel(): NewsArticle {
        return NewsArticle(
            id,
            title,
            sourceId,
            sourceName,
            publishedAt,
            originalUrl,
            category,
            locationName,
            latitude,
            longitude
        )
    }
}
