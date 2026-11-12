package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Domain entity representing an authenticated or guest user profile.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation & Value Objects).
 */
public final class UserProfile implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String GUEST_ID = "guest";
  public static final String GUEST_EMAIL = "";
  public static final String PROVIDER_GUEST = "guest";
  public static final String PROVIDER_GOOGLE = "google";

  private final String id;
  private final String email;
  private final String displayName;
  private final String authProvider;

  public UserProfile(
      @NonNull String id,
      @NonNull String email,
      @Nullable String displayName,
      @NonNull String authProvider) {
    this.id = Objects.requireNonNull(id);
    this.email = Objects.requireNonNull(email);
    this.displayName = displayName;
    this.authProvider = Objects.requireNonNull(authProvider);
  }

  @NonNull
  public static UserProfile createGuest() {
    return new UserProfile(GUEST_ID, GUEST_EMAIL, "Guest User", PROVIDER_GUEST);
  }

  public boolean isGuest() {
    return GUEST_ID.equals(id) || PROVIDER_GUEST.equalsIgnoreCase(authProvider);
  }

  @NonNull
  public String getId() {
    return id;
  }

  @NonNull
  public String getEmail() {
    return email;
  }

  @Nullable
  public String getDisplayName() {
    return displayName;
  }

  @NonNull
  public String getAuthProvider() {
    return authProvider;
  }

  @NonNull
  public String getFormattedDisplayName() {
    if (displayName != null && !displayName.trim().isEmpty()) {
      return displayName.trim();
    }
    if (!email.trim().isEmpty()) {
      int atIdx = email.indexOf('@');
      return atIdx > 0 ? email.substring(0, atIdx) : email;
    }
    return "Guest User";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserProfile)) {
      return false;
    }
    UserProfile that = (UserProfile) o;
    return Objects.equals(id, that.id)
        && Objects.equals(email, that.email)
        && Objects.equals(authProvider, that.authProvider);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, email, authProvider);
  }

  @NonNull
  @Override
  public String toString() {
    return "UserProfile{"
        + "id='" + id + '\''
        + ", email='" + email + '\''
        + ", displayName='" + displayName + '\''
        + ", authProvider='" + authProvider + '\''
        + '}';
  }
}
