package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.UserPreferences;

/**
 * Abstraction interface for cloud preferences synchronization and conflict resolution.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Abstraction & Interfaces) and Ch7 (Asynchronous
 * Concurrency).
 */
public interface RemotePreferencesRepository {

  /**
   * Pulls preferences from the remote backend and updates local storage.
   *
   * @param callback Callback delivering the updated {@link UserPreferences}.
   */
  void syncFromRemote(@NonNull ResultCallback<UserPreferences> callback);

  /**
   * Pushes current local preferences to the remote backend.
   *
   * @param localPreferences Preferences snapshot to upload.
   * @param callback Optional callback delivering updated {@link UserPreferences}.
   */
  void syncToRemote(
      @NonNull UserPreferences localPreferences,
      @Nullable ResultCallback<UserPreferences> callback);

  /**
   * Reconciles cloud and local preferences upon successful user sign-in.
   *
   * @param callback Callback delivering the reconciled {@link UserPreferences}.
   */
  void reconcileOnLogin(@NonNull ResultCallback<UserPreferences> callback);
}
