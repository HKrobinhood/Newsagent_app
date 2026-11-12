package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for `POST /api/v1/ai/test-connection`.
 * The personal API key is transmitted via the `X-LLM-Api-Key` HTTP header.
 */
data class KeyTestRequestDto(
    @SerializedName("provider")
    val provider: String
)
