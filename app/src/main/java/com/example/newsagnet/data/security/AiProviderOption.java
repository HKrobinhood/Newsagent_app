package com.example.newsagnet.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Enumeration of supported LLM providers for the AI Intelligence Agent.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Enumerations & Encapsulation).
 */
public enum AiProviderOption {
  SERVER_DEFAULT("server_default", "Server Default"),
  GEMINI("gemini", "Google Gemini"),
  OPENAI("openai", "OpenAI");

  private final String id;
  private final String displayName;

  AiProviderOption(@NonNull String id, @NonNull String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  @NonNull
  public String getId() {
    return id;
  }

  @NonNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Resolves a provider enum from its string identifier, defaulting to {@link #SERVER_DEFAULT}.
   *
   * @param rawId Provider string identifier.
   * @return Matching {@link AiProviderOption}.
   */
  @NonNull
  public static AiProviderOption fromId(@Nullable String rawId) {
    if (rawId == null || rawId.trim().isEmpty()) {
      return SERVER_DEFAULT;
    }
    String normalized = rawId.trim().toLowerCase(Locale.ROOT);
    for (AiProviderOption option : values()) {
      if (option.id.equals(normalized)) {
        return option;
      }
    }
    return SERVER_DEFAULT;
  }
}
