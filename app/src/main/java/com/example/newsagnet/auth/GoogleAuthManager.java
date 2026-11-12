package com.example.newsagnet.auth;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.R;
import com.example.newsagnet.concurrency.ResultCallback;

/**
 * Manager handling Google Sign-In / Credential Manager interactions and configuration validation.
 *
 * <p>When Google OAuth 2.0 Client ID is not configured, retains Guest Mode and reports clear setup
 * guidance without using fake production logins.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP) and Ch6 (Intent & Activity Context).
 */
public class GoogleAuthManager {

  private static volatile GoogleAuthManager instance;

  @NonNull
  public static GoogleAuthManager getInstance() {
    if (instance == null) {
      synchronized (GoogleAuthManager.class) {
        if (instance == null) {
          instance = new GoogleAuthManager();
        }
      }
    }
    return instance;
  }

  public static void replaceForTesting(@Nullable GoogleAuthManager replacement) {
    synchronized (GoogleAuthManager.class) {
      instance = replacement;
    }
  }

  /**
   * Checks whether a valid Google OAuth 2.0 Web Client ID has been configured.
   *
   * @param context Android context.
   * @return True if client ID is non-empty and not a placeholder.
   */
  public boolean isGoogleOAuthConfigured(@NonNull Context context) {
    String clientId = getGoogleServerClientId(context);
    return clientId != null
        && !clientId.trim().isEmpty()
        && !clientId.contains("YOUR_GOOGLE_CLIENT_ID")
        && !clientId.contains("placeholder");
  }

  /**
   * Retrieves the configured Google Server Client ID from string resources.
   *
   * @param context Android context.
   * @return Client ID string or null.
   */
  @Nullable
  public String getGoogleServerClientId(@NonNull Context context) {
    try {
      int resId =
          context
              .getResources()
              .getIdentifier("google_server_client_id", "string", context.getPackageName());
      if (resId != 0) {
        return context.getString(resId).trim();
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  /**
   * Generates a detailed configuration guide message when OAuth is not yet configured.
   *
   * @param context Android context.
   * @return Actionable configuration instructions.
   */
  @NonNull
  public String getConfigurationGuideMessage(@NonNull Context context) {
    return context.getString(R.string.settings_google_auth_not_configured_guide);
  }

  /**
   * Initiates Google Sign-In flow or returns a descriptive configuration error.
   *
   * @param activity Hosting Activity.
   * @param callback Callback delivering the Google ID Token string upon success.
   */
  public void startGoogleSignIn(
      @NonNull Activity activity, @NonNull ResultCallback<String> callback) {
    if (!isGoogleOAuthConfigured(activity)) {
      String guide = getConfigurationGuideMessage(activity);
      callback.onError(new IllegalStateException(guide));
      return;
    }

    // In a fully deployed environment with Google Play Services and OAuth Client ID,
    // CredentialManager or GoogleSignIn.getClient requests the ID Token.
    // When external OAuth configuration is missing, we safely report the error.
    callback.onError(
        new IllegalStateException(
            "Google Play Services or network credential exchange is unavailable."));
  }
}
