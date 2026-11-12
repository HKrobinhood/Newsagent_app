package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Domain model representing a selectable news media source.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation & Value Objects).
 */
public final class NewsSourceItem {

  public static final String SOURCE_BBC = "bbc";
  public static final String SOURCE_CNN = "cnn";
  public static final String SOURCE_ALJAZEERA = "aljazeera";
  public static final String SOURCE_FOX = "fox";

  private static final List<NewsSourceItem> DEFAULT_SOURCES =
      Collections.unmodifiableList(
          Arrays.asList(
              new NewsSourceItem(
                  SOURCE_BBC,
                  "BBC News",
                  "http://feeds.bbci.co.uk/news/world/rss.xml",
                  "https://www.bbc.com/news",
                  true),
              new NewsSourceItem(
                  SOURCE_CNN,
                  "CNN",
                  "http://rss.cnn.com/rss/edition_world.rss",
                  "https://edition.cnn.com",
                  true),
              new NewsSourceItem(
                  SOURCE_ALJAZEERA,
                  "Al Jazeera",
                  "https://www.aljazeera.com/xml/rss/all.xml",
                  "https://www.aljazeera.com",
                  true),
              new NewsSourceItem(
                  SOURCE_FOX,
                  "Fox News",
                  "https://moxie.foxnews.com/google-publisher/world.xml",
                  "https://www.foxnews.com",
                  true)));

  private final String id;
  private final String name;
  private final String rssUrl;
  private final String homeUrl;
  private final boolean active;

  public NewsSourceItem(
      @NonNull String id,
      @NonNull String name,
      @NonNull String rssUrl,
      @NonNull String homeUrl,
      boolean active) {
    this.id = Objects.requireNonNull(id).trim().toLowerCase();
    this.name = Objects.requireNonNull(name).trim();
    this.rssUrl = rssUrl != null ? rssUrl : "";
    this.homeUrl = homeUrl != null ? homeUrl : "";
    this.active = active;
  }

  @NonNull
  public String getId() {
    return id;
  }

  @NonNull
  public String getName() {
    return name;
  }

  @NonNull
  public String getRssUrl() {
    return rssUrl;
  }

  @NonNull
  public String getHomeUrl() {
    return homeUrl;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Returns the canonical list of supported default news sources.
   *
   * @return Unmodifiable list of default {@link NewsSourceItem}s.
   */
  @NonNull
  public static List<NewsSourceItem> getDefaultSources() {
    return DEFAULT_SOURCES;
  }

  /**
   * Returns the ordered set of default source IDs (`bbc`, `cnn`, `aljazeera`, `fox`).
   *
   * @return Ordered set of default source IDs.
   */
  @NonNull
  public static Set<String> getDefaultSourceIds() {
    Set<String> ids = new LinkedHashSet<>();
    for (NewsSourceItem item : DEFAULT_SOURCES) {
      ids.add(item.getId());
    }
    return ids;
  }

  /**
   * Extracts source IDs from a collection of {@link NewsSourceItem}s.
   *
   * @param items Source items.
   * @return Ordered list of source IDs.
   */
  @NonNull
  public static List<String> extractIds(@NonNull List<NewsSourceItem> items) {
    List<String> ids = new ArrayList<>(items.size());
    for (NewsSourceItem item : items) {
      ids.add(item.getId());
    }
    return ids;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NewsSourceItem)) {
      return false;
    }
    NewsSourceItem that = (NewsSourceItem) o;
    return active == that.active
        && Objects.equals(id, that.id)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, active);
  }
}
