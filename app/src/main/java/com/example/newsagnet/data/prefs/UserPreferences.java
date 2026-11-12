package com.example.newsagnet.data.prefs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsSourceItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable domain value object representing user preferences (`enabled_source_ids`, `font_scale`,
 * `settings_version`, `last_updated`).
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation, Immutability, Builder Pattern).
 */
public final class UserPreferences {

  public static final String NONE_SOURCES_QUERY_TOKEN = "__none__";
  public static final float DEFAULT_FONT_SCALE = FontScaleOption.MEDIUM.getScale();
  public static final int DEFAULT_SETTINGS_VERSION = 1;

  private final Set<String> enabledSourceIds;
  private final float fontScale;
  private final int settingsVersion;
  private final long lastUpdated;

  public UserPreferences(
      @NonNull Set<String> enabledSourceIds,
      float fontScale,
      int settingsVersion,
      long lastUpdated) {
    Set<String> normalized = new LinkedHashSet<>();
    for (String id : enabledSourceIds) {
      if (id != null && !id.trim().isEmpty()) {
        normalized.add(id.trim().toLowerCase());
      }
    }
    this.enabledSourceIds = Collections.unmodifiableSet(normalized);
    this.fontScale = FontScaleOption.fromScale(fontScale).getScale();
    this.settingsVersion = Math.max(1, settingsVersion);
    this.lastUpdated = lastUpdated;
  }

  @NonNull
  public Set<String> getEnabledSourceIds() {
    return enabledSourceIds;
  }

  public float getFontScale() {
    return fontScale;
  }

  @NonNull
  public FontScaleOption getFontScaleOption() {
    return FontScaleOption.fromScale(fontScale);
  }

  public int getSettingsVersion() {
    return settingsVersion;
  }

  public long getLastUpdated() {
    return lastUpdated;
  }

  public boolean isSourceEnabled(@Nullable String sourceId) {
    if (sourceId == null || sourceId.trim().isEmpty()) {
      return false;
    }
    return enabledSourceIds.contains(sourceId.trim().toLowerCase());
  }

  public boolean areAllSourcesDisabled() {
    return enabledSourceIds.isEmpty();
  }

  /**
   * Formats enabled source IDs as a deterministic query parameter for `/api/v1/news` and
   * `/api/v1/news/geo`.
   *
   * @return Comma-separated source IDs (e.g. `"aljazeera,bbc,cnn"`), or `"__none__"` if empty.
   */
  @NonNull
  public String toSourceIdsQueryParam() {
    if (enabledSourceIds.isEmpty()) {
      return NONE_SOURCES_QUERY_TOKEN;
    }
    Set<String> sorted = new TreeSet<>(enabledSourceIds);
    StringBuilder sb = new StringBuilder();
    for (String id : sorted) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(id);
    }
    return sb.toString();
  }

  /**
   * Filters a list of {@link NewsArticle}s so only articles from enabled sources remain.
   *
   * @param articles Input articles.
   * @return Filtered list of articles whose `source_id` is enabled.
   */
  @NonNull
  public List<NewsArticle> filterArticles(@Nullable Collection<NewsArticle> articles) {
    if (articles == null || articles.isEmpty() || enabledSourceIds.isEmpty()) {
      return new ArrayList<>();
    }
    List<NewsArticle> result = new ArrayList<>();
    for (NewsArticle article : articles) {
      if (article != null && isSourceAllowedForArticle(article)) {
        result.add(article);
      }
    }
    return result;
  }

  private boolean isSourceAllowedForArticle(@NonNull NewsArticle article) {
    String sourceId = article.getSourceId();
    if (sourceId != null && !sourceId.trim().isEmpty()) {
      return isSourceEnabled(sourceId);
    }
    String sourceName = article.getSourceName();
    if (sourceName != null) {
      String lower = sourceName.toLowerCase();
      if (lower.contains("bbc")) {
        return isSourceEnabled(NewsSourceItem.SOURCE_BBC);
      } else if (lower.contains("cnn")) {
        return isSourceEnabled(NewsSourceItem.SOURCE_CNN);
      } else if (lower.contains("al jazeera") || lower.contains("aljazeera")) {
        return isSourceEnabled(NewsSourceItem.SOURCE_ALJAZEERA);
      } else if (lower.contains("fox")) {
        return isSourceEnabled(NewsSourceItem.SOURCE_FOX);
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserPreferences)) {
      return false;
    }
    UserPreferences that = (UserPreferences) o;
    return Float.compare(that.fontScale, fontScale) == 0
        && settingsVersion == that.settingsVersion
        && Objects.equals(enabledSourceIds, that.enabledSourceIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabledSourceIds, fontScale, settingsVersion);
  }
}
