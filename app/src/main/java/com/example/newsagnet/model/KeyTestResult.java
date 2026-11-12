package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import java.util.Objects;

/**
 * Immutable domain value object representing the result of an LLM API Key connection test.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation & Value Objects).
 */
public final class KeyTestResult {

  public static final String STATUS_SUCCESS = "Success";
  public static final String STATUS_AUTH_FAILED = "Authentication Failed";
  public static final String STATUS_TIMEOUT = "Timeout";
  public static final String STATUS_PROVIDER_ERROR = "Provider Error";

  private final String status;
  private final String provider;
  private final String message;

  public KeyTestResult(
      @NonNull String status, @NonNull String provider, @NonNull String message) {
    this.status = Objects.requireNonNull(status);
    this.provider = Objects.requireNonNull(provider);
    this.message = Objects.requireNonNull(message);
  }

  @NonNull
  public String getStatus() {
    return status;
  }

  @NonNull
  public String getProvider() {
    return provider;
  }

  @NonNull
  public String getMessage() {
    return message;
  }

  public boolean isSuccess() {
    return STATUS_SUCCESS.equalsIgnoreCase(status);
  }
}
