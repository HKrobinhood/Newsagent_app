package com.example.newsagnet.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.example.newsagnet.model.NewsCategory;

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
  public static final String KEY_PAGE_SIZE = "pref_key_page_size";

  public static final int DEFAULT_PAGE_SIZE = 20;

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

  /** Clears all stored preference values. */
  public void clear() {
    sharedPreferences.edit().clear().apply();
  }
}
