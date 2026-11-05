package com.example.newsagnet.network.dto

import com.example.newsagnet.model.NewsArticle
import com.google.gson.annotations.SerializedName

/**
 * GeoJSON Point geometry payload.
 */
data class GeoGeometryDto(
    @SerializedName("type") val type: String = "Point",
    @SerializedName("coordinates") val coordinates: List<Double> = emptyList()
)

/**
 * GeoJSON properties payload containing news article metadata.
 */
data class GeoPropertiesDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("source_id") val sourceId: String,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("original_url") val originalUrl: String,
    @SerializedName("category") val category: String,
    @SerializedName("location_name") val locationName: String? = null
)

/**
 * GeoJSON Feature representing a single geolocated news item.
 */
data class GeoFeatureDto(
    @SerializedName("type") val type: String = "Feature",
    @SerializedName("geometry") val geometry: GeoGeometryDto,
    @SerializedName("properties") val properties: GeoPropertiesDto
) {
    /**
     * Converts network GeoJSON feature to client domain NewsArticle entity.
     */
    fun toDomain(): NewsArticle = NewsArticle(
        properties.id,
        properties.title,
        properties.sourceId,
        properties.sourceName,
        properties.publishedAt,
        properties.originalUrl,
        properties.category,
        properties.locationName,
        geometry.coordinates.getOrNull(1),
        geometry.coordinates.getOrNull(0)
    )
}

/**
 * Standard GeoJSON FeatureCollection container.
 */
data class GeoFeatureCollectionDto(
    @SerializedName("type") val type: String = "FeatureCollection",
    @SerializedName("features") val features: List<GeoFeatureDto> = emptyList()
)
