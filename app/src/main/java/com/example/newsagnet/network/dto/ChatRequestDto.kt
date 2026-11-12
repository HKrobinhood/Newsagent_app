package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload for conversational AI agent chat endpoint.
 */
data class ChatRequestDto @JvmOverloads constructor(
    @SerializedName("message") val message: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("mode") val mode: String = "hybrid",
    @SerializedName("source_ids") val sourceIds: List<String>? = null
)
