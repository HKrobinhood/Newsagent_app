package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.model.UserProfile;

/**
 * Abstraction interface for user authentication and session management.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Abstraction & Interfaces) and Ch7 (Asynchronous
 * Concurrency).
 */
public interface AuthRepository {

  /** Listener notified whenever user authentication state changes (e.g. login, logout). */
  interface AuthListener {
    void onAuthChanged(@NonNull UserProfile userProfile);
  }

  /**
   * Returns the current user profile (authenticated or guest).
   *
   * @return Current {@link UserProfile}.
   */
  @NonNull
  UserProfile getCurrentUser();

  /**
   * Returns true if user is currently signed in.
   *
   * @return True if authenticated.
   */
  boolean isLoggedIn();

  /**
   * Authenticates with the backend using a verified Google ID Token.
   *
   * @param idToken Google OAuth 2.0 ID Token.
   * @param callback Callback delivering the authenticated {@link UserProfile}.
   */
  void loginWithGoogle(@NonNull String idToken, @NonNull ResultCallback<UserProfile> callback);

  /**
   * Terminates active session with the backend and clears local credentials.
   *
   * @param callback Callback notified upon completion.
   */
  void logout(@NonNull ResultCallback<Void> callback);

  /**
   * Registers a listener for authentication state changes.
   *
   * @param listener Listener instance.
   */
  void addAuthListener(@NonNull AuthListener listener);

  /**
   * Unregisters a previously registered authentication listener.
   *
   * @param listener Listener instance.
   */
  void removeAuthListener(@NonNull AuthListener listener);
}
