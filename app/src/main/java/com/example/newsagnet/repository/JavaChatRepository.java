package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.ResultCallback;

/**
 * Abstraction interface for communicating with the AI News Intelligence Agent.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Abstraction & Interfaces) and Ch7 (Asynchronous Concurrency).
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
}
