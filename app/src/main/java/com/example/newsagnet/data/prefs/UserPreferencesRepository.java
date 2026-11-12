package com.example.newsagnet.data.prefs;

import androidx.annotation.NonNull;
import java.util.Collection;
import java.util.Set;

/**
 * Abstraction interface for reading and persisting user preferences (`enabled_source_ids`,
 * `font_scale`, `settings_version`, `last_updated`).
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Abstraction, Interfaces, Polymorphism) and Ch6 (Storage).
 */
public interface UserPreferencesRepository {

  /** Listener notified on the UI thread whenever user preferences change. */
  interface OnPreferencesChangedListener {
    void onPreferencesChanged(@NonNull UserPreferences updatedPreferences);
  }

  /**
   * Returns an immutable snapshot of the current user preferences.
   *
   * @return Current {@link UserPreferences}.
   */
  @NonNull
  UserPreferences getPreferences();

  /**
   * Returns the set of currently enabled news source IDs.
   *
   * @return Enabled `source_id` strings.
   */
  @NonNull
  Set<String> getEnabledSourceIds();

  /**
   * Replaces the set of enabled news source IDs.
   *
   * @param sourceIds Enabled `source_id` strings (empty if all sources are disabled).
   */
  void setEnabledSourceIds(@NonNull Collection<String> sourceIds);

  /**
   * Toggles a single news source on or off.
   *
   * @param sourceId Stable media `source_id` (e.g. `"bbc"`).
   * @param enabled True to enable, false to disable.
   */
  void setSourceEnabled(@NonNull String sourceId, boolean enabled);

  /**
   * Enables all provided news source IDs.
   *
   * @param allSourceIds Collection of all available `source_id`s.
   */
  void selectAllSources(@NonNull Collection<String> allSourceIds);

  /**
   * Resets media source selection to the default all-enabled configuration.
   *
   * @param defaultSourceIds Default `source_id`s.
   */
  void resetSourcesToDefault(@NonNull Collection<String> defaultSourceIds);

  /**
   * Reconciles newly discovered backend sources without overwriting existing user selections.
   *
   * @param availableSourceIds Active `source_id`s returned by the backend.
   */
  void reconcileAvailableSources(@NonNull Collection<String> availableSourceIds);

  /**
   * Returns the active global font scale factor (`0.90f`, `1.00f`, `1.15f`, `1.30f`).
   *
   * @return Font scale factor.
   */
  float getFontScale();

  /**
   * Updates the active global font scale factor.
   *
   * @param fontScale Selected font scale factor.
   */
  void setFontScale(float fontScale);

  /**
   * Returns the schema version of the preferences record.
   *
   * @return Settings schema version.
   */
  int getSettingsVersion();

  /**
   * Returns the epoch millisecond timestamp of the last update.
   *
   * @return Last updated timestamp.
   */
  long getLastUpdated();

  /**
   * Registers a listener for preference changes.
   *
   * @param listener Listener to register.
   */
  void addOnPreferencesChangedListener(@NonNull OnPreferencesChangedListener listener);

  /**
   * Unregisters a previously registered listener.
   *
   * @param listener Listener to remove.
   */
  void removeOnPreferencesChangedListener(@NonNull OnPreferencesChangedListener listener);
}
