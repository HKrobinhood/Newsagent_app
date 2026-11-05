package com.example.newsagnet

import com.example.newsagnet.model.MessageRole
import com.example.newsagnet.network.ChatApiService
import com.example.newsagnet.network.dto.ChatRequestDto
import com.example.newsagnet.network.dto.ChatResponseDto
import com.example.newsagnet.network.dto.NewsSourceRefDto
import com.example.newsagnet.repository.ChatRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeChatApiService(
        var shouldFail: Boolean = false,
        var lastRequest: ChatRequestDto? = null
    ) : ChatApiService {
        override fun sendMessageCall(request: ChatRequestDto): retrofit2.Call<ChatResponseDto> {
            throw UnsupportedOperationException("Not implemented in fake")
        }

        override suspend fun sendMessage(request: ChatRequestDto): ChatResponseDto {
            lastRequest = request
            if (shouldFail) {
                throw IOException("Server unavailable")
            }
            return ChatResponseDto(
                sessionId = request.sessionId ?: "sess-12345",
                answer = "Verified diplomatic response.",
                modeUsed = "title_search",
                sources = listOf(
                    NewsSourceRefDto(
                        title = "Global Summit Accord",
                        sourceName = "BBC News",
                        originalUrl = "https://bbc.com/news/summit",
                        publishedAt = "2026-11-03T12:00:00Z",
                        locationName = "Geneva"
                    )
                ),
                confidence = 0.98
            )
        }
    }

    @Test
    fun sendMessage_success_returnsMappedDomainResult() = runTest(testDispatcher) {
        val fakeApi = FakeChatApiService()
        val repository = ChatRepositoryImpl(apiService = fakeApi, ioDispatcher = testDispatcher)

        val result = repository.sendMessage(
            message = "Tell me about the Geneva summit",
            sessionId = null
        )

        assertTrue(result.isSuccess)
        val agentResult = result.getOrThrow()
        assertEquals("sess-12345", agentResult.sessionId)
        assertEquals(MessageRole.ASSISTANT, agentResult.message.role)
        assertEquals("Verified diplomatic response.", agentResult.message.content)
        assertEquals("title_search", agentResult.message.modeUsed)
        assertEquals(1, agentResult.message.sources.size)
        assertEquals("BBC News", agentResult.message.sources.first().sourceName)
        assertEquals("https://bbc.com/news/summit", agentResult.message.sources.first().originalUrl)
    }

    @Test
    fun sendMessage_propagatesSessionId() = runTest(testDispatcher) {
        val fakeApi = FakeChatApiService()
        val repository = ChatRepositoryImpl(apiService = fakeApi, ioDispatcher = testDispatcher)

        val result = repository.sendMessage(
            message = "Follow up question",
            sessionId = "existing-sess-999"
        )

        assertTrue(result.isSuccess)
        assertEquals("existing-sess-999", fakeApi.lastRequest?.sessionId)
        assertEquals("existing-sess-999", result.getOrThrow().sessionId)
    }

    @Test
    fun sendMessage_networkError_returnsFailureResult() = runTest(testDispatcher) {
        val fakeApi = FakeChatApiService(shouldFail = true)
        val repository = ChatRepositoryImpl(apiService = fakeApi, ioDispatcher = testDispatcher)

        val result = repository.sendMessage(
            message = "Any query",
            sessionId = null
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
