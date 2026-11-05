package com.example.newsagnet.repository

/**
 * Repository interface managing communication with the backend AI news intelligence agent.
 */
interface ChatRepository {

    /**
     * Dispatches user inquiry to the backend agent and returns verified assistant response.
     */
    suspend fun sendMessage(
        message: String,
        sessionId: String?,
        mode: String = "hybrid"
    ): Result<AgentChatResult>
}
