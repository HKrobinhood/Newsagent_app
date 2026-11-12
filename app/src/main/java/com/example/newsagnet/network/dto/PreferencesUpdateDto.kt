package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for `PUT /api/v1/users/me/preferences`.
 */
data class PreferencesUpdateDto(
    @SerializedName("enabled_source_ids")
    val enabledSourceIds: List<String>,

    @SerializedName("font_scale")
    val fontScale: Float,

    @SerializedName("schema_version")
    val schemaVersion: Int = 1
)
