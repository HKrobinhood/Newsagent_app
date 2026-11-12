package com.example.newsagnet.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.example.newsagnet.model.NewsCategory;
import com.example.newsagnet.model.NewsSourceItem;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages user preferences and local persistent configuration via Android SharedPreferences.
 *
 * <p>Conforms to COMP7506 Ch6 (Storage, SharedPreferences, and Settings Persistence).
 */
public class NewsPreferences {

  private static final String PREF_NAME = "global_news_ai_prefs";

  public static final String KEY_DEFAULT_CATEGORY = "pref_key_default_category";
  public static final String KEY_LAST_REFRESH_TIME = "pref_key_last_refresh_time";
  public static final String KEY_DARK_MODE_ENABLED = "pref_key_dark_mode_enabled";
  public static final String KEY_THEME_MODE = "pref_key_theme_mode";
  public static final String KEY_PAGE_SIZE = "pref_key_page_size";

  public static final String THEME_MODE_LIGHT = "light";
  public static final String THEME_MODE_DARK = "dark";
  public static final String THEME_MODE_SYSTEM = "system";

  public static final String KEY_ENABLED_SOURCE_IDS = "pref_key_enabled_source_ids";
  public static final String KEY_KNOWN_SOURCE_IDS = "pref_key_known_source_ids";
  public static final String KEY_HAS_CUSTOM_SOURCES = "pref_key_has_custom_sources";
  public static final String KEY_FONT_SCALE = "pref_key_font_scale";
  public static final String KEY_SETTINGS_VERSION = "pref_key_settings_version";
  public static final String KEY_LAST_UPDATED = "pref_key_last_updated";

  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final float DEFAULT_FONT_SCALE = FontScaleOption.MEDIUM.getScale();
  public static final int CURRENT_SETTINGS_VERSION = 1;

  private final SharedPreferences sharedPreferences;

  public NewsPreferences(@NonNull Context context) {
    this(context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
  }

  public NewsPreferences(@NonNull SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
  }

  /**
   * Saves the user's preferred default news category.
   *
   * @param category Selected category.
   */
  public void setDefaultCategory(@NonNull NewsCategory category) {
    sharedPreferences.edit().putString(KEY_DEFAULT_CATEGORY, category.getKey()).apply();
  }

  /**
   * Retrieves the preferred default news category.
   *
   * @return Saved NewsCategory, defaults to {@link NewsCategory#ALL}.
   */
  @NonNull
  public NewsCategory getDefaultCategory() {
    String key = sharedPreferences.getString(KEY_DEFAULT_CATEGORY, NewsCategory.ALL.getKey());
    return NewsCategory.fromKey(key);
  }

  /**
   * Records the Unix timestamp of the last successful news refresh.
   *
   * @param timestamp Epoch milliseconds.
   */
  public void setLastRefreshTime(long timestamp) {
    sharedPreferences.edit().putLong(KEY_LAST_REFRESH_TIME, timestamp).apply();
  }

  /**
   * Retrieves the timestamp of the last successful refresh.
   *
   * @return Epoch milliseconds, or 0 if never refreshed.
   */
  public long getLastRefreshTime() {
    return sharedPreferences.getLong(KEY_LAST_REFRESH_TIME, 0L);
  }

  /**
   * Toggles the dark mode visual preference.
   *
   * @param enabled True if dark mode is active.
   */
  public void setDarkModeEnabled(boolean enabled) {
    sharedPreferences.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply();
  }

  /**
   * Checks if dark mode is enabled.
   *
   * @return True by default for tactical intelligence theme.
   */
  public boolean isDarkModeEnabled() {
    return sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, true);
  }

  /**
   * Saves the user's preferred visual theme mode (Light, Dark, or Follow System).
   *
   * @param mode Selected {@link ThemeMode}.
   */
  public void setThemeMode(@NonNull ThemeMode mode) {
    sharedPreferences.edit().putString(KEY_THEME_MODE, mode.getKey()).apply();
    setDarkModeEnabled(mode != ThemeMode.LIGHT);
  }

  /**
   * Retrieves the preferred visual theme mode.
   *
   * @return Saved {@link ThemeMode}, defaulting to {@link ThemeMode#SYSTEM}.
   */
  @NonNull
  public ThemeMode getThemeMode() {
    String key = sharedPreferences.getString(KEY_THEME_MODE, null);
    if (key != null) {
      return ThemeMode.fromKey(key);
    }
    if (sharedPreferences.contains(KEY_DARK_MODE_ENABLED)) {
      boolean isDark = sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, true);
      return isDark ? ThemeMode.DARK : ThemeMode.LIGHT;
    }
    return ThemeMode.SYSTEM;
  }

  /**
   * Updates preferred batch page size.
   *
   * @param pageSize Number of articles per query page.
   */
  public void setPageSize(int pageSize) {
    sharedPreferences.edit().putInt(KEY_PAGE_SIZE, pageSize).apply();
  }

  /**
   * Gets preferred batch page size.
   *
   * @return Configured page size or {@value #DEFAULT_PAGE_SIZE}.
   */
  public int getPageSize() {
    return sharedPreferences.getInt(KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE);
  }

  /**
   * Retrieves the set of enabled news media source IDs. Defaults to all default sources (`bbc`,
   * `cnn`, `aljazeera`, `fox`) when not yet customized.
   *
   * @return Ordered set of enabled `source_id` strings.
   */
  @NonNull
  public Set<String> getEnabledSourceIds() {
    boolean hasCustom = sharedPreferences.getBoolean(KEY_HAS_CUSTOM_SOURCES, false);
    if (!hasCustom && !sharedPreferences.contains(KEY_ENABLED_SOURCE_IDS)) {
      return NewsSourceItem.getDefaultSourceIds();
    }
    String raw = sharedPreferences.getString(KEY_ENABLED_SOURCE_IDS, null);
    if (raw == null) {
      return hasCustom ? new LinkedHashSet<>() : NewsSourceItem.getDefaultSourceIds();
    }
    return deserializeIds(raw);
  }

  /**
   * Persists the set of enabled news media source IDs and updates metadata timestamps.
   *
   * @param sourceIds Enabled source IDs (may be empty if the user deselected all sources).
   */
  public void setEnabledSourceIds(@NonNull Collection<String> sourceIds) {
    Set<String> normalized = normalizeIds(sourceIds);
    Set<String> known = getKnownSourceIds();
    known.addAll(normalized);
    long now = System.currentTimeMillis();

    sharedPreferences
        .edit()
        .putBoolean(KEY_HAS_CUSTOM_SOURCES, true)
        .putString(KEY_ENABLED_SOURCE_IDS, serializeIds(normalized))
        .putString(KEY_KNOWN_SOURCE_IDS, serializeIds(known))
        .putInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION)
        .putLong(KEY_LAST_UPDATED, now)
        .apply();
  }

  /**
   * Resets enabled news sources to the full default set (`bbc`, `cnn`, `aljazeera`, `fox`) plus any
   * additional known sources.
   *
   * @param availableSourceIds Available source IDs to enable.
   */
  public void resetSourcesToDefault(@NonNull Collection<String> availableSourceIds) {
    Set<String> defaults = normalizeIds(availableSourceIds);
    if (defaults.isEmpty()) {
      defaults = NewsSourceItem.getDefaultSourceIds();
    }
    long now = System.currentTimeMillis();
    sharedPreferences
        .edit()
        .putBoolean(KEY_HAS_CUSTOM_SOURCES, false)
        .putString(KEY_ENABLED_SOURCE_IDS, serializeIds(defaults))
        .putString(KEY_KNOWN_SOURCE_IDS, serializeIds(defaults))
        .putInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION)
        .putLong(KEY_LAST_UPDATED, now)
        .apply();
  }

  /**
   * Reconciles newly discovered media sources from the backend without corrupting existing user
   * choices. Previously unseen sources are enabled by default unless the user explicitly disabled
   * all sources.
   *
   * @param availableSourceIds Active source IDs returned by the backend.
   */
  public void reconcileAvailableSources(@NonNull Collection<String> availableSourceIds) {
    Set<String> available = normalizeIds(availableSourceIds);
    if (available.isEmpty()) {
      return;
    }
    Set<String> known = getKnownSourceIds();
    Set<String> enabled = getEnabledSourceIds();
    boolean hasCustom = sharedPreferences.getBoolean(KEY_HAS_CUSTOM_SOURCES, false);
    boolean allExplicitlyDisabled = hasCustom && enabled.isEmpty();

    boolean changed = false;
    for (String id : available) {
      if (!known.contains(id)) {
        known.add(id);
        if (!allExplicitlyDisabled) {
          enabled.add(id);
        }
        changed = true;
      }
    }

    if (changed) {
      sharedPreferences
          .edit()
          .putString(KEY_ENABLED_SOURCE_IDS, serializeIds(enabled))
          .putString(KEY_KNOWN_SOURCE_IDS, serializeIds(known))
          .apply();
    }
  }

  @NonNull
  private Set<String> getKnownSourceIds() {
    String raw = sharedPreferences.getString(KEY_KNOWN_SOURCE_IDS, null);
    if (raw == null || raw.trim().isEmpty()) {
      return NewsSourceItem.getDefaultSourceIds();
    }
    Set<String> known = deserializeIds(raw);
    known.addAll(NewsSourceItem.getDefaultSourceIds());
    return known;
  }

  /**
   * Retrieves the user's global font scale factor.
   *
   * @return Font scale factor (0.90f, 1.00f, 1.15f, or 1.30f), defaults to 1.00f.
   */
  public float getFontScale() {
    float raw = sharedPreferences.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE);
    return FontScaleOption.fromScale(raw).getScale();
  }

  /**
   * Saves the user's global font scale factor and updates metadata timestamps.
   *
   * @param fontScale Selected font scale factor.
   */
  public void setFontScale(float fontScale) {
    float normalized = FontScaleOption.fromScale(fontScale).getScale();
    long now = System.currentTimeMillis();
    sharedPreferences
        .edit()
        .putFloat(KEY_FONT_SCALE, normalized)
        .putInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION)
        .putLong(KEY_LAST_UPDATED, now)
        .apply();
  }

  /**
   * Returns the schema version of the stored user preferences.
   *
   * @return Schema version integer, defaults to {@value #CURRENT_SETTINGS_VERSION}.
   */
  public int getSettingsVersion() {
    return sharedPreferences.getInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION);
  }

  /**
   * Updates the schema version of the stored user preferences.
   *
   * @param version Schema version integer.
   */
  public void setSettingsVersion(int version) {
    sharedPreferences.edit().putInt(KEY_SETTINGS_VERSION, Math.max(1, version)).apply();
  }

  /**
   * Returns the epoch millisecond timestamp when preferences were last updated.
   *
   * @return Epoch milliseconds, or 0L if never updated.
   */
  public long getLastUpdated() {
    return sharedPreferences.getLong(KEY_LAST_UPDATED, 0L);
  }

  /**
   * Updates the epoch millisecond timestamp of the last preference change.
   *
   * @param timestamp Epoch milliseconds.
   */
  public void setLastUpdated(long timestamp) {
    sharedPreferences.edit().putLong(KEY_LAST_UPDATED, timestamp).apply();
  }

  /**
   * Applies cloud-synchronized preferences to local storage.
   *
   * @param sourceIds Enabled source IDs from cloud.
   * @param fontScale Font scale ratio from cloud.
   * @param schemaVersion Settings schema version.
   * @param lastUpdated Timestamp of last update.
   */
  public void applyRemotePreferences(
      @NonNull Collection<String> sourceIds,
      float fontScale,
      int schemaVersion,
      long lastUpdated) {
    Set<String> normalized = normalizeIds(sourceIds);
    sharedPreferences
        .edit()
        .putString(KEY_ENABLED_SOURCE_IDS, serializeIds(normalized))
        .putFloat(KEY_FONT_SCALE, FontScaleOption.fromScale(fontScale).getScale())
        .putInt(KEY_SETTINGS_VERSION, Math.max(1, schemaVersion))
        .putLong(KEY_LAST_UPDATED, lastUpdated > 0 ? lastUpdated : System.currentTimeMillis())
        .apply();
  }

  /**
   * Constructs an immutable {@link UserPreferences} snapshot of the current settings.
   *
   * @return Current {@link UserPreferences} snapshot.
   */
  @NonNull
  public UserPreferences getUserPreferences() {
    return new UserPreferences(
        getEnabledSourceIds(), getFontScale(), getSettingsVersion(), getLastUpdated());
  }

  @NonNull
  private static Set<String> normalizeIds(@NonNull Collection<String> ids) {
    Set<String> result = new LinkedHashSet<>();
    for (String id : ids) {
      if (id != null && !id.trim().isEmpty()) {
        result.add(id.trim().toLowerCase());
      }
    }
    return result;
  }

  @NonNull
  private static String serializeIds(@NonNull Collection<String> ids) {
    StringBuilder sb = new StringBuilder();
    for (String id : ids) {
      if (id != null && !id.trim().isEmpty()) {
        if (sb.length() > 0) {
          sb.append(',');
        }
        sb.append(id.trim().toLowerCase());
      }
    }
    return sb.toString();
  }

  @NonNull
  private static Set<String> deserializeIds(@NonNull String csv) {
    Set<String> result = new LinkedHashSet<>();
    if (csv.trim().isEmpty()) {
      return result;
    }
    String[] parts = csv.split(",");
    for (String part : parts) {
      String trimmed = part.trim().toLowerCase();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
  }

  /** Clears all stored preference values. */
  public void clear() {
    sharedPreferences.edit().clear().apply();
  }
}
