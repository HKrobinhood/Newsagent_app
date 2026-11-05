package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for paginated news response.
 */
data class NewsListDto(
    @SerializedName("items")
    val items: List<NewsArticleDto>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)
