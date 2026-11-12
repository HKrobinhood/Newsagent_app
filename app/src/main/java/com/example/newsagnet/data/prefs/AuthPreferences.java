package com.example.newsagnet.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.model.AuthSession;
import com.example.newsagnet.model.UserProfile;
import java.util.Objects;

/**
 * SharedPreferences storage managing authenticated user sessions and profile credentials.
 *
 * <p>Conforms to COMP7506 Ch6 (SharedPreferences Persistence & Session State).
 */
public class AuthPreferences {

  public static final String PREF_NAME = "global_news_ai_auth_prefs";

  public static final String KEY_ACCESS_TOKEN = "auth_access_token";
  public static final String KEY_TOKEN_TYPE = "auth_token_type";
  public static final String KEY_EXPIRES_AT = "auth_expires_at";
  public static final String KEY_USER_ID = "auth_user_id";
  public static final String KEY_USER_EMAIL = "auth_user_email";
  public static final String KEY_USER_NAME = "auth_user_display_name";
  public static final String KEY_AUTH_PROVIDER = "auth_provider";

  private static volatile AuthPreferences sInstance;

  private final SharedPreferences preferences;

  public static AuthPreferences getInstance(@NonNull Context context) {
    if (sInstance == null) {
      synchronized (AuthPreferences.class) {
        if (sInstance == null) {
          sInstance = new AuthPreferences(context.getApplicationContext());
        }
      }
    }
    return sInstance;
  }

  public AuthPreferences(@NonNull Context context) {
    this(context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
  }

  public AuthPreferences(@NonNull SharedPreferences preferences) {
    this.preferences = Objects.requireNonNull(preferences);
  }

  /**
   * Persists an active authenticated session.
   *
   * @param session Active session instance.
   */
  public void saveSession(@NonNull AuthSession session) {
    UserProfile profile = session.getUserProfile();
    preferences
        .edit()
        .putString(KEY_ACCESS_TOKEN, session.getAccessToken())
        .putString(KEY_TOKEN_TYPE, session.getTokenType())
        .putString(KEY_EXPIRES_AT, session.getExpiresAt())
        .putString(KEY_USER_ID, profile.getId())
        .putString(KEY_USER_EMAIL, profile.getEmail())
        .putString(KEY_USER_NAME, profile.getDisplayName())
        .putString(KEY_AUTH_PROVIDER, profile.getAuthProvider())
        .apply();
  }

  /**
   * Retrieves the currently stored authenticated session, or null if unauthenticated.
   *
   * @return Active {@link AuthSession}, or null.
   */
  @Nullable
  public AuthSession getSession() {
    String token = preferences.getString(KEY_ACCESS_TOKEN, null);
    String userId = preferences.getString(KEY_USER_ID, null);
    String email = preferences.getString(KEY_USER_EMAIL, null);
    if (token == null || token.isEmpty() || userId == null || email == null) {
      return null;
    }
    String tokenType = preferences.getString(KEY_TOKEN_TYPE, "bearer");
    String expiresAt = preferences.getString(KEY_EXPIRES_AT, "");
    String name = preferences.getString(KEY_USER_NAME, null);
    String provider = preferences.getString(KEY_AUTH_PROVIDER, UserProfile.PROVIDER_GOOGLE);

    UserProfile profile = new UserProfile(userId, email, name, provider);
    return new AuthSession(token, tokenType, expiresAt, profile);
  }

  /**
   * Returns current user profile (authenticated or guest).
   *
   * @return {@link UserProfile}.
   */
  @NonNull
  public UserProfile getUserProfile() {
    AuthSession session = getSession();
    return session != null ? session.getUserProfile() : UserProfile.createGuest();
  }

  /**
   * Returns true if user is signed in with a valid access token.
   *
   * @return True if authenticated.
   */
  public boolean isLoggedIn() {
    return getSession() != null;
  }

  /** Clears stored session tokens and credentials, resetting back to Guest mode. */
  public void clearSession() {
    preferences
        .edit()
        .remove(KEY_ACCESS_TOKEN)
        .remove(KEY_TOKEN_TYPE)
        .remove(KEY_EXPIRES_AT)
        .remove(KEY_USER_ID)
        .remove(KEY_USER_EMAIL)
        .remove(KEY_USER_NAME)
        .remove(KEY_AUTH_PROVIDER)
        .apply();
  }
}
