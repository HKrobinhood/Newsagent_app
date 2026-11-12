package com.example.newsagnet.data.prefs;

import androidx.annotation.NonNull;

/**
 * Enumeration of the four supported global font scale presets.
 *
 * <p>Conforms to COMP7506 Ch3 (Java Enum & Encapsulation).
 */
public enum FontScaleOption {
  SMALL("small", "Small", 0.90f),
  MEDIUM("medium", "Medium", 1.00f),
  LARGE("large", "Large", 1.15f),
  EXTRA_LARGE("extra_large", "Extra Large", 1.30f);

  private final String key;
  private final String displayName;
  private final float scale;

  FontScaleOption(@NonNull String key, @NonNull String displayName, float scale) {
    this.key = key;
    this.displayName = displayName;
    this.scale = scale;
  }

  @NonNull
  public String getKey() {
    return key;
  }

  @NonNull
  public String getDisplayName() {
    return displayName;
  }

  public float getScale() {
    return scale;
  }

  /**
   * Resolves the closest {@link FontScaleOption} for a given numeric scale factor.
   *
   * @param scale Scale factor (e.g. 0.90f, 1.00f, 1.15f, 1.30f).
   * @return Matching {@link FontScaleOption}, defaulting to {@link #MEDIUM}.
   */
  @NonNull
  public static FontScaleOption fromScale(float scale) {
    if (Math.abs(scale - SMALL.scale) < 0.04f) {
      return SMALL;
    } else if (Math.abs(scale - LARGE.scale) < 0.04f) {
      return LARGE;
    } else if (Math.abs(scale - EXTRA_LARGE.scale) < 0.04f) {
      return EXTRA_LARGE;
    } else {
      return MEDIUM;
    }
  }
}
