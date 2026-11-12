package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Representation of user preferences from `GET /api/v1/users/me/preferences`.
 */
data class UserPreferencesDto(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("enabled_source_ids")
    val enabledSourceIds: List<String>,

    @SerializedName("font_scale")
    val fontScale: Float,

    @SerializedName("schema_version")
    val schemaVersion: Int,

    @SerializedName("updated_at")
    val updatedAt: String
)
