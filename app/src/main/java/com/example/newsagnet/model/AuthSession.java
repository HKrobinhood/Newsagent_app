package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Domain entity representing an active authenticated user session.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation & Value Objects).
 */
public final class AuthSession implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String accessToken;
  private final String tokenType;
  private final String expiresAt;
  private final UserProfile userProfile;

  public AuthSession(
      @NonNull String accessToken,
      @NonNull String tokenType,
      @NonNull String expiresAt,
      @NonNull UserProfile userProfile) {
    this.accessToken = Objects.requireNonNull(accessToken);
    this.tokenType = Objects.requireNonNull(tokenType);
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.userProfile = Objects.requireNonNull(userProfile);
  }

  @NonNull
  public String getAccessToken() {
    return accessToken;
  }

  @NonNull
  public String getTokenType() {
    return tokenType;
  }

  @NonNull
  public String getExpiresAt() {
    return expiresAt;
  }

  @NonNull
  public UserProfile getUserProfile() {
    return userProfile;
  }

  @NonNull
  public String getAuthorizationHeader() {
    return "Bearer " + accessToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuthSession)) {
      return false;
    }
    AuthSession that = (AuthSession) o;
    return Objects.equals(accessToken, that.accessToken)
        && Objects.equals(userProfile, that.userProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, userProfile);
  }
}
