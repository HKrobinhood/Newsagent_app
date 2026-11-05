package com.example.newsagnet.repository

import com.example.newsagnet.model.ChatMessage
import com.example.newsagnet.model.MessageRole
import com.example.newsagnet.network.ApiClient
import com.example.newsagnet.network.ChatApiService
import com.example.newsagnet.network.dto.ChatRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Concrete ChatRepository communicating with FastAPI /ai/chat endpoints.
 */
class ChatRepositoryImpl(
    private val apiService: ChatApiService = ApiClient.chatApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChatRepository {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override suspend fun sendMessage(
        message: String,
        sessionId: String?,
        mode: String
    ): Result<AgentChatResult> = withContext(ioDispatcher) {
        try {
            val response = apiService.sendMessage(
                ChatRequestDto(
                    message = message,
                    sessionId = sessionId,
                    mode = mode
                )
            )

            val domainSources = response.sources.map { it.toDomain() }
            val chatMessage = ChatMessage.Builder()
                .setRole(MessageRole.ASSISTANT)
                .setContent(response.answer)
                .setTimestamp(timeFormat.format(Date()))
                .setSources(domainSources)
                .setModeUsed(response.modeUsed)
                .build()

            Result.success(
                AgentChatResult(
                    response.sessionId,
                    chatMessage,
                    response.confidence
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
