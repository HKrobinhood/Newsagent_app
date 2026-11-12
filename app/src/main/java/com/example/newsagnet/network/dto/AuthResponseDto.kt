package com.example.newsagnet.network.dto

import com.example.newsagnet.model.AuthSession
import com.google.gson.annotations.SerializedName

/**
 * Authentication response from `POST /api/v1/auth/google`.
 */
data class AuthResponseDto(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_at")
    val expiresAt: String,

    @SerializedName("user")
    val user: UserDto
) {
    fun toDomain(): AuthSession {
        return AuthSession(accessToken, tokenType, expiresAt, user.toDomain())
    }
}
