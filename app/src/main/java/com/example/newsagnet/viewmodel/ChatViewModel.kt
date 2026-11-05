package com.example.newsagnet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsagnet.model.ChatMessage
import com.example.newsagnet.model.MessageRole
import com.example.newsagnet.repository.ChatRepository
import com.example.newsagnet.repository.ChatRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI State for the AI Intelligence Chat Screen.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val sessionId: String? = null
)

/**
 * ViewModel managing AI chat conversation and protocol integration with the backend.
 */
class ChatViewModel(
    private val repository: ChatRepository = ChatRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        resetSession()
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(
            MessageRole.USER,
            prompt,
            currentTimeString()
        )

        val currentSessionId = _uiState.value.sessionId

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isGenerating = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = repository.sendMessage(prompt, currentSessionId)
            result.fold(
                onSuccess = { agentResult ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + agentResult.message,
                            sessionId = agentResult.sessionId,
                            isGenerating = false
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.localizedMessage ?: "Network error communicating with agent"
                    val failureMessage = ChatMessage.Builder()
                        .setRole(MessageRole.ASSISTANT)
                        .setContent("Could not reach intelligence agent: $errorMsg. Please check backend connection.")
                        .setTimestamp(currentTimeString())
                        .setError(true)
                        .build()
                    _uiState.update {
                        it.copy(
                            messages = it.messages + failureMessage,
                            errorMessage = errorMsg,
                            isGenerating = false
                        )
                    }
                }
            )
        }
    }

    fun resetSession() {
        _uiState.update {
            it.copy(
                sessionId = null,
                errorMessage = null,
                inputText = "",
                isGenerating = false,
                messages = listOf(
                    ChatMessage(
                        MessageRole.ASSISTANT,
                        "Hello! I am GlobalNewsAI Intelligence Agent.\n\n" +
                                "You can ask me about recent international news, specific political statements, " +
                                "or cross-source verification (e.g. 'What did the climate summit conclude today?').",
                        currentTimeString()
                    )
                )
            )
        }
    }

    private fun currentTimeString(): String = timeFormat.format(Date())
}
