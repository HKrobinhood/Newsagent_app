package com.example.newsagnet

import com.example.newsagnet.model.ChatMessage
import com.example.newsagnet.model.MessageRole
import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.repository.AgentChatResult
import com.example.newsagnet.repository.ChatRepository
import com.example.newsagnet.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeChatRepository(
        var shouldFail: Boolean = false,
        var customReply: String = "Based on verified coverage from Geneva today, delegates reached agreement.",
        var returnedSessionId: String = "session-abc-123"
    ) : ChatRepository {

        var lastMessageSent: String? = null
        var lastSessionIdSent: String? = null

        override suspend fun sendMessage(
            message: String,
            sessionId: String?,
            mode: String
        ): Result<AgentChatResult> {
            lastMessageSent = message
            lastSessionIdSent = sessionId

            if (shouldFail) {
                return Result.failure(IOException("Failed to connect to agent"))
            }

            val chatMessage = ChatMessage.Builder()
                .setRole(MessageRole.ASSISTANT)
                .setContent(customReply)
                .setTimestamp("12:00")
                .setModeUsed("title_search")
                .setSources(
                    listOf(
                        NewsArticle(
                            "ref-1",
                            "Global Climate Accord Finalized",
                            "bbc",
                            "BBC News",
                            "2026-11-03T16:30:00Z",
                            "https://www.bbc.com/news/1",
                            "world",
                            "Geneva",
                            null,
                            null
                        )
                    )
                )
                .build()

            return Result.success(
                AgentChatResult(
                    returnedSessionId,
                    chatMessage,
                    0.95
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasWelcomeMessageFromAssistant() = runTest {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(repository = fakeRepo)
        val state = viewModel.uiState.value

        assertEquals(1, state.messages.size)
        assertEquals(MessageRole.ASSISTANT, state.messages.first().role)
        assertTrue(state.messages.first().content.contains("GlobalNewsAI"))
        assertFalse(state.isGenerating)
        assertEquals("", state.inputText)
        assertNull(state.sessionId)
        assertNull(state.errorMessage)
    }

    @Test
    fun onInputTextChanged_updatesState() = runTest {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(repository = fakeRepo)
        viewModel.onInputTextChanged("What is happening in Geneva?")

        assertEquals("What is happening in Geneva?", viewModel.uiState.value.inputText)
    }

    @Test
    fun sendMessage_blankInput_doesNotSend() = runTest {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(repository = fakeRepo)
        viewModel.onInputTextChanged("   ")
        viewModel.sendMessage()
        advanceUntilIdle()

        // Message list should still only contain the welcome message
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertNull(fakeRepo.lastMessageSent)
    }

    @Test
    fun sendMessage_success_updatesSessionIdAndAppendsAssistantMessage() = runTest {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(repository = fakeRepo)
        viewModel.onInputTextChanged("Tell me about climate accord")
        viewModel.sendMessage()

        // Immediately, user message should be added, input cleared, isGenerating true
        assertEquals("", viewModel.uiState.value.inputText)
        assertTrue(viewModel.uiState.value.isGenerating)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals(MessageRole.USER, viewModel.uiState.value.messages[1].role)

        // Advance coroutines to complete network invocation
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isGenerating)
        assertEquals(3, finalState.messages.size)
        assertEquals("session-abc-123", finalState.sessionId)
        assertNull(finalState.errorMessage)

        val assistantReply = finalState.messages[2]
        assertEquals(MessageRole.ASSISTANT, assistantReply.role)
        assertTrue(assistantReply.content.contains("Geneva"))
        assertTrue(assistantReply.sources.isNotEmpty())
        assertEquals("bbc", assistantReply.sources.first().sourceId)
        assertEquals("title_search", assistantReply.modeUsed)
        assertFalse(assistantReply.isError)
    }

    @Test
    fun sendMessage_failure_appendsErrorMessageAndSetsErrorState() = runTest {
        val fakeRepo = FakeChatRepository(shouldFail = true)
        val viewModel = ChatViewModel(repository = fakeRepo)
        viewModel.onInputTextChanged("Will fail query")
        viewModel.sendMessage()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isGenerating)
        assertNotNull(finalState.errorMessage)
        assertEquals(3, finalState.messages.size)

        val failureMessage = finalState.messages[2]
        assertEquals(MessageRole.ASSISTANT, failureMessage.role)
        assertTrue(failureMessage.isError)
        assertTrue(failureMessage.content.contains("Could not reach intelligence agent"))
    }

    @Test
    fun resetSession_clearsHistoryAndRestoresWelcomeMessage() = runTest {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(repository = fakeRepo)
        viewModel.onInputTextChanged("Hello agent")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.messages.size)
        assertEquals("session-abc-123", viewModel.uiState.value.sessionId)

        viewModel.resetSession()
        val resetState = viewModel.uiState.value
        assertEquals(1, resetState.messages.size)
        assertNull(resetState.sessionId)
        assertFalse(resetState.isGenerating)
        assertEquals("", resetState.inputText)
    }
}
