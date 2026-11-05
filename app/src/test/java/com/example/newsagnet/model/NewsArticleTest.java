package com.example.newsagnet.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for NewsArticle and NewsCategory OOP models.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP, Encapsulation, Builder Pattern, and Enum types).
 */
public class NewsArticleTest {

  @Test
  public void testFullConstructor_andGettersSetters() {
    NewsArticle article =
        new NewsArticle(
            "id-100",
            "UN Security Council Resolution Passed",
            "reuters",
            "Reuters World",
            "2026-11-04T10:00:00Z",
            "https://reuters.com/news/100",
            "world",
            "New York, USA",
            40.7128,
            -74.0060);

    assertEquals("id-100", article.getId());
    assertEquals("UN Security Council Resolution Passed", article.getTitle());
    assertEquals("reuters", article.getSourceId());
    assertEquals("Reuters World", article.getSourceName());
    assertEquals("2026-11-04T10:00:00Z", article.getPublishedAt());
    assertEquals("https://reuters.com/news/100", article.getOriginalUrl());
    assertEquals("world", article.getCategory());
    assertEquals("New York, USA", article.getLocationName());
    assertEquals(Double.valueOf(40.7128), article.getLatitude());
    assertEquals(Double.valueOf(-74.0060), article.getLongitude());

    // Test setters
    article.setTitle("Updated Title");
    assertEquals("Updated Title", article.getTitle());

    article.setCategory("politics");
    assertEquals("politics", article.getCategory());
  }

  @Test
  public void testOverloadedConstructor_withoutCoordinates() {
    NewsArticle article =
        new NewsArticle(
            "id-200",
            "Global Semiconductor Supply Chain Report",
            "bloomberg",
            "Bloomberg Tech",
            "2026-11-04T11:00:00Z",
            "https://bloomberg.com/news/200",
            "technology");

    assertEquals("id-200", article.getId());
    assertEquals("technology", article.getCategory());
    assertNull(article.getLocationName());
    assertNull(article.getLatitude());
    assertNull(article.getLongitude());
  }

  @Test
  public void testBuilderPattern() {
    NewsArticle article =
        new NewsArticle.Builder()
            .setId("id-300")
            .setTitle("Fusion Energy Milestone Reached")
            .setSourceId("bbc")
            .setSourceName("BBC Science")
            .setPublishedAt("2026-11-04T12:00:00Z")
            .setOriginalUrl("https://bbc.com/science/300")
            .setCategory("science")
            .setLocationName("Oxford, UK")
            .setLatitude(51.7520)
            .setLongitude(-1.2577)
            .build();

    assertNotNull(article);
    assertEquals("id-300", article.getId());
    assertEquals("Fusion Energy Milestone Reached", article.getTitle());
    assertEquals("Oxford, UK", article.getLocationName());
    assertEquals(Double.valueOf(51.7520), article.getLatitude());
  }

  @Test
  public void testEqualsAndHashCode() {
    NewsArticle article1 =
        new NewsArticle(
            "id-1", "Title", "src", "Source", "2026-11-04", "https://url.com", "all");
    NewsArticle article2 =
        new NewsArticle(
            "id-1", "Title", "src", "Source", "2026-11-04", "https://url.com", "all");
    NewsArticle article3 =
        new NewsArticle(
            "id-2", "Other", "src", "Source", "2026-11-04", "https://url.com", "all");

    assertEquals(article1, article2);
    assertEquals(article1.hashCode(), article2.hashCode());
    assertFalse(article1.equals(article3));
    assertFalse(article1.equals(null));
  }

  @Test
  public void testNewsCategory_propertiesAndResolution() {
    assertEquals("all", NewsCategory.ALL.getKey());
    assertEquals("All", NewsCategory.ALL.getDisplayName());

    assertEquals("world", NewsCategory.WORLD.getKey());
    assertEquals("technology", NewsCategory.TECHNOLOGY.getKey());
    assertEquals("business", NewsCategory.BUSINESS.getKey());
    assertEquals("science", NewsCategory.SCIENCE.getKey());
    assertEquals("health", NewsCategory.HEALTH.getKey());

    assertEquals(NewsCategory.TECHNOLOGY, NewsCategory.fromKey("technology"));
    assertEquals(NewsCategory.TECHNOLOGY, NewsCategory.fromKey("TechNology"));
    assertEquals(NewsCategory.WORLD, NewsCategory.fromKey("WORLD"));
    assertEquals(NewsCategory.ALL, NewsCategory.fromKey("unknown_category"));
    assertEquals(NewsCategory.ALL, NewsCategory.fromKey(null));
  }
}
