package com.example.newsagnet.network.dto

import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.network.ApiClient
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for media attachment in NewsArticle REST response.
 */
data class ArticleMediaDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("article_id")
    val articleId: String? = null,
    @SerializedName("media_type")
    val mediaType: String? = "image",
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerializedName("mime_type")
    val mimeType: String? = null,
    @SerializedName("width")
    val width: Int? = null,
    @SerializedName("height")
    val height: Int? = null,
    @SerializedName("credit")
    val credit: String? = null
)

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
    val createdAt: String? = null,
    @SerializedName("media")
    val media: ArticleMediaDto? = null,
    @SerializedName("media_list")
    val mediaList: List<ArticleMediaDto>? = null
) {
    /**
     * Map DTO to UI/domain NewsArticle model, resolving primary media image and credit.
     */
    fun toDomainModel(): NewsArticle {
        val article = NewsArticle(
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

        val primary = media ?: mediaList?.firstOrNull()
        if (primary != null) {
            val resolvedUrl = primary.imageUrl?.takeIf { it.isNotBlank() }
                ?: primary.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { resolveAbsoluteMediaUrl(it) }
            article.imageUrl = resolvedUrl
            article.imageCredit = primary.credit
        }

        return article
    }

    companion object {
        fun resolveAbsoluteMediaUrl(relativeOrAbsolute: String): String {
            if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
                return relativeOrAbsolute
            }
            val base = ApiClient.DEFAULT_BASE_URL
            val root = try {
                val uri = java.net.URI(base)
                val scheme = uri.scheme ?: "http"
                val host = uri.host ?: "10.0.2.2"
                val port = if (uri.port != -1) ":${uri.port}" else ""
                "$scheme://$host$port"
            } catch (e: Exception) {
                "http://10.0.2.2:8000"
            }
            return if (relativeOrAbsolute.startsWith("/")) "$root$relativeOrAbsolute" else "$root/$relativeOrAbsolute"
        }
    }
}
