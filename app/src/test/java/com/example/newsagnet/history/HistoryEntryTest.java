package com.example.newsagnet.history;

import static org.junit.Assert.*;

import com.example.newsagnet.model.NewsArticle;
import org.junit.Test;

public class HistoryEntryTest {
  @Test public void snapshotDoesNotChangeWhenOriginalModelIsMutated() {
    NewsArticle article = new NewsArticle("id", "Original title", "bbc", "BBC News", "date",
        "https://example.org/story", "world");
    HistoryEntry snapshot = HistoryEntry.fromArticle(article, 123L);
    article.setTitle("Changed title");
    assertEquals("Original title", snapshot.getTitle());
    assertEquals(123L, snapshot.getOpenedAt());
  }

  @Test public void citationWithMissingMetadataStillHasUsableHistory() {
    NewsArticle article = new NewsArticle();
    article.setOriginalUrl(" https://example.org/story ");
    HistoryEntry snapshot = HistoryEntry.fromArticle(article, 123L);
    assertEquals("https://example.org/story", snapshot.getUrl());
    assertEquals("Unknown source", snapshot.getSourceName());
    assertEquals("Untitled article", snapshot.getTitle());
    assertEquals("", snapshot.getPublishedAt());
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonWebIntentIsNotStoredAsNews() {
    NewsArticle article = new NewsArticle();
    article.setOriginalUrl("file:///private/file");
    HistoryEntry.fromArticle(article, 0);
  }
}
