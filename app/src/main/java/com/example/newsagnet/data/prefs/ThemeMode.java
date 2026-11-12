package com.example.newsagnet.data.prefs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Visual appearance theme mode preference (Light, Dark, or System default).
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Enums) and Material Design theme standards.
 */
public enum ThemeMode {
  LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
  DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
  SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

  private final String key;
  @AppCompatDelegate.NightMode private final int nightMode;

  ThemeMode(@NonNull String key, @AppCompatDelegate.NightMode int nightMode) {
    this.key = key;
    this.nightMode = nightMode;
  }

  @NonNull
  public String getKey() {
    return key;
  }

  @AppCompatDelegate.NightMode
  public int getNightMode() {
    return nightMode;
  }

  /**
   * Resolves a {@link ThemeMode} from its persistent key string.
   *
   * @param key Stored preference key string.
   * @return Matching {@link ThemeMode}, defaulting to {@link #SYSTEM}.
   */
  @NonNull
  public static ThemeMode fromKey(@Nullable String key) {
    if (key == null) {
      return SYSTEM;
    }
    for (ThemeMode mode : values()) {
      if (mode.key.equalsIgnoreCase(key.trim())) {
        return mode;
      }
    }
    return SYSTEM;
  }
}
