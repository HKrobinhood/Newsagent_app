package com.example.newsagnet.history;

import com.example.newsagnet.model.NewsArticle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/** An immutable, local snapshot of an opened news link; it does not contain article bodies. */
public final class HistoryEntry {
  private final String url;
  private final String title;
  private final String sourceName;
  private final String publishedAt;
  private final long openedAt;
  private final String note;

  public HistoryEntry(String url, String title, String sourceName, String publishedAt,
      long openedAt, String note) {
    this.url = url;
    this.title = title;
    this.sourceName = sourceName;
    this.publishedAt = publishedAt;
    this.openedAt = openedAt;
    this.note = note;
  }

  public static HistoryEntry fromArticle(NewsArticle article, long openedAt) {
    String url = text(article.getOriginalUrl());
    try {
      URI uri = new URI(url);
      if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme())
          || "http".equalsIgnoreCase(uri.getScheme()))) {
        throw new IllegalArgumentException("History requires an HTTP(S) news link");
      }
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid news link", e);
    }
    return new HistoryEntry(url, fallback(article.getTitle(), "Untitled article"),
        fallback(article.getSourceName(), "Unknown source"), text(article.getPublishedAt()),
        openedAt, "");
  }

  private static String text(String value) { return value == null ? "" : value.trim(); }
  private static String fallback(String value, String fallback) {
    String cleaned = text(value);
    return cleaned.isEmpty() ? fallback : cleaned;
  }

  public String getUrl() { return url; }
  public String getTitle() { return title; }
  public String getSourceName() { return sourceName; }
  public String getPublishedAt() { return publishedAt; }
  public long getOpenedAt() { return openedAt; }
  public String getNote() { return note; }

  public NewsArticle toArticle() {
    return new NewsArticle(url, title, "", sourceName, publishedAt, url, "general");
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof HistoryEntry)) return false;
    HistoryEntry that = (HistoryEntry) other;
    return openedAt == that.openedAt && Objects.equals(url, that.url)
        && Objects.equals(title, that.title) && Objects.equals(sourceName, that.sourceName)
        && Objects.equals(publishedAt, that.publishedAt) && Objects.equals(note, that.note);
  }

  @Override public int hashCode() {
    return Objects.hash(url, title, sourceName, publishedAt, openedAt, note);
  }
}
