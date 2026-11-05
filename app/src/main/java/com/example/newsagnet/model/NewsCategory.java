package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Standard news categories supported by GlobalNewsAI.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP & Enum Types with Encapsulation).
 */
public enum NewsCategory {
  ALL("all", "All"),
  WORLD("world", "World"),
  TECHNOLOGY("technology", "Tech"),
  BUSINESS("business", "Business"),
  SCIENCE("science", "Science"),
  HEALTH("health", "Health");

  private final String key;
  private final String displayName;

  NewsCategory(@NonNull String key, @NonNull String displayName) {
    this.key = key;
    this.displayName = displayName;
  }

  @NonNull
  public String getKey() {
    return key;
  }

  @NonNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Resolves a NewsCategory from a case-insensitive key string.
   *
   * @param key The category identifier key.
   * @return Matching NewsCategory, or {@link #ALL} if not found or null.
   */
  @NonNull
  public static NewsCategory fromKey(@Nullable String key) {
    if (key == null || key.trim().isEmpty()) {
      return ALL;
    }
    for (NewsCategory category : values()) {
      if (category.key.equalsIgnoreCase(key.trim())) {
        return category;
      }
    }
    return ALL;
  }

  @NonNull
  @Override
  public String toString() {
    return "NewsCategory{" + "key='" + key + '\'' + ", displayName='" + displayName + '\'' + '}';
  }
}
