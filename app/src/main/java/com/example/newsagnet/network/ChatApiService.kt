package com.example.newsagnet.network

import com.example.newsagnet.network.dto.ChatRequestDto
import com.example.newsagnet.network.dto.ChatResponseDto
import com.example.newsagnet.network.dto.KeyTestRequestDto
import com.example.newsagnet.network.dto.KeyTestResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface defining AI Agent communication and API key verification endpoints.
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

    @POST("ai/chat")
    fun sendMessageWithKeyCall(
        @Header("X-LLM-Provider") llmProvider: String?,
        @Header("X-LLM-Api-Key") llmApiKey: String?,
        @Body request: ChatRequestDto
    ): Call<ChatResponseDto> {
        return sendMessageCall(request)
    }

    @POST("ai/test-connection")
    fun testConnectionCall(
        @Header("X-LLM-Api-Key") llmApiKey: String?,
        @Body request: KeyTestRequestDto
    ): Call<KeyTestResponseDto> {
        throw UnsupportedOperationException("testConnectionCall not implemented")
    }
}
