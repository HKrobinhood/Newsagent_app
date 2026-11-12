package com.example.newsagnet.network.dto

import com.example.newsagnet.model.KeyTestResult
import com.google.gson.annotations.SerializedName

/**
 * Response DTO from `POST /api/v1/ai/test-connection`.
 */
data class KeyTestResponseDto(
    @SerializedName("status")
    val status: String,

    @SerializedName("provider")
    val provider: String,

    @SerializedName("message")
    val message: String
) {
    fun toDomain(): KeyTestResult {
        return KeyTestResult(status, provider, message)
    }
}
