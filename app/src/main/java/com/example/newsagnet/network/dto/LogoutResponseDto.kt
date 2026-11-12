package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from `POST /api/v1/auth/logout`.
 */
data class LogoutResponseDto(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String
)
