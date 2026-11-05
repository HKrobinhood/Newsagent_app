package com.example.newsagnet.network

import com.example.newsagnet.network.dto.ChatRequestDto
import com.example.newsagnet.network.dto.ChatResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface defining AI Agent communication endpoints.
 * Supports both suspend coroutine and synchronous/asynchronous Retrofit Call methods.
 */
interface ChatApiService {

    /**
     * Sends user inquiry to backend AI intelligence agent and retrieves response with citations.
     */
    @POST("ai/chat")
    suspend fun sendMessage(
        @Body request: ChatRequestDto
    ): ChatResponseDto

    @POST("ai/chat")
    fun sendMessageCall(
        @Body request: ChatRequestDto
    ): Call<ChatResponseDto>
}
