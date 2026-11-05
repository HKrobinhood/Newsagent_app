package com.example.newsagnet.network.dto

import com.example.newsagnet.model.NewsArticle
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Citation reference linking an AI answer statement to a verified news article.
 */
data class NewsSourceRefDto(
    @SerializedName("title") val title: String,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("original_url") val originalUrl: String,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("location_name") val locationName: String? = null
) {
    /**
     * Converts network citation reference to client domain NewsArticle entity.
     */
    fun toDomain(): NewsArticle = NewsArticle(
        UUID.randomUUID().toString(),
        title,
        sourceName.lowercase().replace(" ", "-"),
        sourceName,
        publishedAt,
        originalUrl,
        "general",
        locationName,
        null,
        null
    )
}
