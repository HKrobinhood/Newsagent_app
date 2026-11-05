package com.example.newsagnet.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Structured response payload from AI agent containing answer, citations, and metadata.
 */
data class ChatResponseDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("mode_used") val modeUsed: String,
    @SerializedName("sources") val sources: List<NewsSourceRefDto> = emptyList(),
    @SerializedName("confidence") val confidence: Double = 1.0
)
