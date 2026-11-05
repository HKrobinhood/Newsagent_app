package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload for conversational AI agent chat endpoint.
 */
data class ChatRequestDto(
    @SerializedName("message") val message: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("mode") val mode: String = "hybrid"
)
