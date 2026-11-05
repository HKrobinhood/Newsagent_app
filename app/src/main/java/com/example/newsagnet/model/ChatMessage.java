package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an individual chat message in the AI News Intelligence screen.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP, Encapsulation, Constructor Overloading, and Builder Pattern).
 */
public class ChatMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private MessageRole role;
  private String content;
  private String timestamp;
  private List<NewsArticle> sources;
  private boolean isThinking;
  private boolean isError;
  private String modeUsed;

  public ChatMessage() {
    this.id = UUID.randomUUID().toString();
    this.sources = new ArrayList<>();
  }

  public ChatMessage(@NonNull MessageRole role, @NonNull String content, @NonNull String timestamp) {
    this(
        UUID.randomUUID().toString(),
        role,
        content,
        timestamp,
        new ArrayList<>(),
        false,
        false,
        null);
  }

  public ChatMessage(
      @NonNull String id,
      @NonNull MessageRole role,
      @NonNull String content,
      @NonNull String timestamp,
      @Nullable List<NewsArticle> sources,
      boolean isThinking,
      boolean isError,
      @Nullable String modeUsed) {
    this.id = id;
    this.role = role;
    this.content = content;
    this.timestamp = timestamp;
    this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
    this.isThinking = isThinking;
    this.isError = isError;
    this.modeUsed = modeUsed;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public MessageRole getRole() {
    return role;
  }

  public void setRole(MessageRole role) {
    this.role = role;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  @NonNull
  public List<NewsArticle> getSources() {
    return sources != null ? Collections.unmodifiableList(sources) : Collections.emptyList();
  }

  public void setSources(@Nullable List<NewsArticle> sources) {
    this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
  }

  public boolean isThinking() {
    return isThinking;
  }

  public void setThinking(boolean thinking) {
    isThinking = thinking;
  }

  public boolean isError() {
    return isError;
  }

  public void setError(boolean error) {
    isError = error;
  }

  @Nullable
  public String getModeUsed() {
    return modeUsed;
  }

  public void setModeUsed(@Nullable String modeUsed) {
    this.modeUsed = modeUsed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChatMessage)) {
      return false;
    }
    ChatMessage that = (ChatMessage) o;
    return isThinking == that.isThinking
        && isError == that.isError
        && Objects.equals(id, that.id)
        && role == that.role
        && Objects.equals(content, that.content)
        && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(sources, that.sources)
        && Objects.equals(modeUsed, that.modeUsed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, role, content, timestamp, sources, isThinking, isError, modeUsed);
  }

  @NonNull
  @Override
  public String toString() {
    return "ChatMessage{"
        + "id='"
        + id
        + '\''
        + ", role="
        + role
        + ", contentLength="
        + (content != null ? content.length() : 0)
        + ", timestamp='"
        + timestamp
        + '\''
        + ", isThinking="
        + isThinking
        + ", isError="
        + isError
        + ", modeUsed='"
        + modeUsed
        + '\''
        + '}';
  }

  /** Builder pattern for ChatMessage. */
  public static class Builder {
    private String id = UUID.randomUUID().toString();
    private MessageRole role = MessageRole.USER;
    private String content = "";
    private String timestamp = "";
    private List<NewsArticle> sources = new ArrayList<>();
    private boolean isThinking = false;
    private boolean isError = false;
    private String modeUsed = null;

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setRole(MessageRole role) {
      this.role = role;
      return this;
    }

    public Builder setContent(String content) {
      this.content = content;
      return this;
    }

    public Builder setTimestamp(String timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setSources(List<NewsArticle> sources) {
      this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
      return this;
    }

    public Builder setThinking(boolean thinking) {
      this.isThinking = thinking;
      return this;
    }

    public Builder setError(boolean error) {
      this.isError = error;
      return this;
    }

    public Builder setModeUsed(String modeUsed) {
      this.modeUsed = modeUsed;
      return this;
    }

    public ChatMessage build() {
      return new ChatMessage(
          id, role, content, timestamp, sources, isThinking, isError, modeUsed);
    }
  }
}
