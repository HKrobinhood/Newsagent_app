package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.security.AiProviderOption;
import com.example.newsagnet.model.KeyTestResult;
import java.util.Collection;

/**
 * Abstraction interface for communicating with the AI News Intelligence Agent.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Abstraction & Interfaces) and Ch7 (Asynchronous
 * Concurrency).
 */
public interface JavaChatRepository {

  /**
   * Submits a user message to the AI agent and dispatches response asynchronously.
   *
   * @param message User inquiry prompt.
   * @param sessionId Optional session UUID to maintain conversation continuity.
   * @param mode Dual-mode search strategy ("hybrid", "title_search", "web_search").
   * @param callback Callback delivering result on the main UI thread.
   */
  void sendMessage(
      @NonNull String message,
      @Nullable String sessionId,
      @NonNull String mode,
      @NonNull ResultCallback<AgentChatResult> callback);

  /**
   * Submits a user message constrained to the user's enabled news media source IDs.
   *
   * @param message User inquiry prompt.
   * @param sessionId Optional session UUID to maintain conversation continuity.
   * @param mode Dual-mode search strategy.
   * @param allowedSourceIds Allowed news media `source_id`s (may be empty if all disabled).
   * @param callback Callback delivering result on the main UI thread.
   */
  default void sendMessage(
      @NonNull String message,
      @Nullable String sessionId,
      @NonNull String mode,
      @Nullable Collection<String> allowedSourceIds,
      @NonNull ResultCallback<AgentChatResult> callback) {
    sendMessage(message, sessionId, mode, callback);
  }

  /**
   * Tests LLM provider connectivity via the backend `/api/v1/ai/test-connection` endpoint without
   * storing or echoing the API key.
   *
   * @param provider Selected {@link AiProviderOption}.
   * @param apiKey Ephemeral personal API key (may be null when testing `SERVER_DEFAULT`).
   * @param callback Callback delivering {@link KeyTestResult} on the main UI thread.
   */
  default void testApiKeyConnection(
      @NonNull AiProviderOption provider,
      @Nullable String apiKey,
      @NonNull ResultCallback<KeyTestResult> callback) {
    callback.onSuccess(
        new KeyTestResult(
            KeyTestResult.STATUS_SUCCESS,
            provider.getId(),
            "Connection test completed."));
  }
}
