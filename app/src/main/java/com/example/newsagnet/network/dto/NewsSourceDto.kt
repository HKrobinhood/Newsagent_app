package com.example.newsagnet.network.dto

import com.example.newsagnet.model.NewsSourceItem
import com.google.gson.annotations.SerializedName

/**
 * DTO for a news media source returned by GET /api/v1/news/sources.
 */
data class NewsSourceDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("rss_url") val rssUrl: String? = "",
    @SerializedName("home_url") val homeUrl: String? = "",
    @SerializedName("is_active") val isActive: Boolean = true
) {
    fun toDomainModel(): NewsSourceItem {
        return NewsSourceItem(
            id,
            name,
            rssUrl ?: "",
            homeUrl ?: "",
            isActive
        )
    }
}
