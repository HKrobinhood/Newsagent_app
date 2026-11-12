package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for `POST /api/v1/auth/google`.
 */
data class AuthRequestDto(
    @SerializedName("id_token")
    val idToken: String
)
