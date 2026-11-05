package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import com.example.newsagnet.model.ChatMessage;
import java.io.Serializable;
import java.util.Objects;

/**
 * Domain outcome of an AI agent chat inquiry.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP & Encapsulation).
 */
public class AgentChatResult implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String sessionId;
  private final ChatMessage message;
  private final double confidence;

  public AgentChatResult(
      @NonNull String sessionId, @NonNull ChatMessage message, double confidence) {
    this.sessionId = sessionId;
    this.message = message;
    this.confidence = confidence;
  }

  @NonNull
  public String getSessionId() {
    return sessionId;
  }

  @NonNull
  public ChatMessage getMessage() {
    return message;
  }

  public double getConfidence() {
    return confidence;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AgentChatResult)) {
      return false;
    }
    AgentChatResult that = (AgentChatResult) o;
    return Double.compare(that.confidence, confidence) == 0
        && Objects.equals(sessionId, that.sessionId)
        && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId, message, confidence);
  }
}
