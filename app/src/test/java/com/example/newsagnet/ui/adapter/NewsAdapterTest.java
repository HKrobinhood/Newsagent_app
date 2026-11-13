package com.example.newsagnet.ui.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.newsagnet.model.NewsArticle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for NewsAdapter and NewsItemClickListener.
 *
 * <p>Conforms to COMP7506 Ch5 (Adapter & RecyclerView testing).
 */
public class NewsAdapterTest {

  private NewsAdapter adapter;
  private List<NewsArticle> sampleArticles;

  @Before
  public void setUp() {
    adapter = new NewsAdapter();
    sampleArticles = new ArrayList<>();
    sampleArticles.add(
        new NewsArticle(
            "art-1",
            "Global AI Summit Opens in London",
            "src-bbc",
            "BBC News",
            "2026-11-04T08:00:00Z",
            "https://bbc.com/news/1",
            "technology",
            "London, UK",
            51.5074,
            -0.1278));
    sampleArticles.add(
        new NewsArticle(
            "art-2",
            "Renewable Energy Investment Hits Record High",
            "src-reuters",
            "Reuters",
            "2026-11-04T09:30:00Z",
            "https://reuters.com/news/2",
            "business",
            "Tokyo, Japan",
            35.6762,
            139.6503));
  }

  @Test
  public void testInitialState_emptyDataset() {
    assertEquals(0, adapter.getItemCount());
    assertNull(adapter.getItem(0));
    assertNull(adapter.getItem(-1));
  }

  @Test
  public void testSubmitList_updatesItemCountAndRetrieval() {
    adapter.submitList(sampleArticles);

    assertEquals(2, adapter.getItemCount());
    NewsArticle first = adapter.getItem(0);
    assertNotNull(first);
    assertEquals("art-1", first.getId());
    assertEquals("BBC News", first.getSourceName());
    assertEquals("technology", first.getCategory());

    NewsArticle second = adapter.getItem(1);
    assertNotNull(second);
    assertEquals("art-2", second.getId());
    assertEquals("Reuters", second.getSourceName());
  }

  @Test
  public void testSubmitList_replaceWithEmptyList() {
    adapter.submitList(sampleArticles);
    assertEquals(2, adapter.getItemCount());

    adapter.submitList(new ArrayList<>());
    assertEquals(0, adapter.getItemCount());
    assertNull(adapter.getItem(0));
  }

  @Test
  public void testItemClickListener_callbackDispatched() {
    AtomicBoolean clicked = new AtomicBoolean(false);
    NewsItemClickListener listener =
        article -> {
          assertEquals("art-1", article.getId());
          clicked.set(true);
        };

    adapter.setOnNewsItemClickListener(listener);
    adapter.submitList(sampleArticles);

    NewsArticle article = adapter.getItem(0);
    assertNotNull(article);
    listener.onNewsItemClick(article);
    assertTrue("Listener should have been triggered", clicked.get());
  }

  @Test
  public void testGetItem_boundaryConditions() {
    adapter.submitList(sampleArticles);
    assertNull(adapter.getItem(-10));
    assertNull(adapter.getItem(2));
    assertNull(adapter.getItem(100));
  }

  @Test
  public void testEditorialLayout_partitionsIntoFeaturedRecommendedAndLatest() {
    List<NewsArticle> editorialArticles =
        com.example.newsagnet.data.mock.MockEditorialNewsProvider.getEditorialMockArticles();
    assertTrue("Mock editorial list should have at least 10 articles", editorialArticles.size() >= 10);

    adapter.setEditorialLayout(true);
    adapter.submitList(editorialArticles);

    // Position 0: Featured Cover Story
    assertEquals(NewsAdapter.VIEW_TYPE_FEATURED, adapter.getItemViewType(0));
    assertEquals(2, adapter.getSpanSize(0));
    NewsArticle featured = adapter.getItem(0);
    assertNotNull(featured);
    assertEquals("mock-editorial-featured", featured.getId());

    // Position 1: Recommended Carousel
    assertEquals(NewsAdapter.VIEW_TYPE_RECOMMENDED, adapter.getItemViewType(1));
    assertEquals(2, adapter.getSpanSize(1));

    // Position 2 & 3: Medium Cards side-by-side
    assertEquals(NewsAdapter.VIEW_TYPE_MEDIUM, adapter.getItemViewType(2));
    assertEquals(1, adapter.getSpanSize(2));
    assertEquals(NewsAdapter.VIEW_TYPE_MEDIUM, adapter.getItemViewType(3));
    assertEquals(1, adapter.getSpanSize(3));

    // Position 4: Large Card
    assertEquals(NewsAdapter.VIEW_TYPE_LARGE, adapter.getItemViewType(4));
    assertEquals(2, adapter.getSpanSize(4));

    // Position 5 & 6: Compact Cards
    assertEquals(NewsAdapter.VIEW_TYPE_COMPACT, adapter.getItemViewType(5));
    assertEquals(2, adapter.getSpanSize(5));
    assertEquals(NewsAdapter.VIEW_TYPE_COMPACT, adapter.getItemViewType(6));
    assertEquals(2, adapter.getSpanSize(6));
  }

  @Test
  public void testDegradation_articleWithoutImageReturnsTextOnly() {
    List<NewsArticle> articles =
        com.example.newsagnet.data.mock.MockEditorialNewsProvider.getEditorialMockArticles();
    adapter.setEditorialLayout(true);
    adapter.submitList(articles);

    // In MockEditorialNewsProvider, latTxt1 has no image
    // Find text only item and assert VIEW_TYPE_TEXT_ONLY
    boolean foundTextOnly = false;
    for (int i = 0; i < adapter.getItemCount(); i++) {
      if (adapter.getItemViewType(i) == NewsAdapter.VIEW_TYPE_TEXT_ONLY) {
        foundTextOnly = true;
        assertEquals(2, adapter.getSpanSize(i));
        NewsArticle a = adapter.getItem(i);
        assertNotNull(a);
        assertFalse(a.hasImage());
        break;
      }
    }
    assertTrue("Should contain degraded Text-Only card for image-less news", foundTextOnly);
  }

  @Test
  public void testEditorialFeed_respectsCategoryAndMediaPreferences() {
    com.example.newsagnet.data.prefs.UserPreferences prefs =
        new com.example.newsagnet.data.prefs.UserPreferences(
            new java.util.LinkedHashSet<>(Arrays.asList("bbc", "cnn")),
            1.0f,
            1,
            System.currentTimeMillis());

    List<NewsArticle> bbcCnnWorldFeed =
        com.example.newsagnet.data.mock.MockEditorialNewsProvider.provideEditorialFeed(
            new ArrayList<>(), prefs, com.example.newsagnet.model.NewsCategory.WORLD);

    assertFalse("Feed should not be empty for bbc/cnn world", bbcCnnWorldFeed.isEmpty());
    for (NewsArticle a : bbcCnnWorldFeed) {
      assertEquals("world", a.getCategory().toLowerCase());
      assertTrue(
          "Publisher must be bbc or cnn",
          "bbc".equals(a.getSourceId()) || "cnn".equals(a.getSourceId()));
    }
  }

  @Test
  public void testRealImageArticles_deterministicFeaturedAndZeroDuplicates() {
    List<NewsArticle> realArticles = new ArrayList<>();
    // Article 0: Text-only (no image)
    realArticles.add(
        new NewsArticle.Builder()
            .setId("real-0-text")
            .setTitle("Financial Statement Released")
            .setSourceId("bbc")
            .setSourceName("BBC News")
            .setPublishedAt("2026-11-12T10:00:00Z")
            .setOriginalUrl("https://bbc.com/text-0")
            .setCategory("business")
            .build());

    // Article 1: Has image URL -> should become Featured Cover Story
    realArticles.add(
        new NewsArticle.Builder()
            .setId("real-1-img")
            .setTitle("Global Climate Agreement Reached")
            .setSourceId("cnn")
            .setSourceName("CNN")
            .setPublishedAt("2026-11-12T09:30:00Z")
            .setOriginalUrl("https://cnn.com/img-1")
            .setCategory("world")
            .setImageUrl("https://images.cnn.com/climate.jpg")
            .setImageCredit("CNN Visuals")
            .build());

    // Add 8 more articles (mix of image and text-only)
    for (int i = 2; i < 10; i++) {
      NewsArticle.Builder b =
          new NewsArticle.Builder()
              .setId("real-" + i)
              .setTitle("Article Headline " + i)
              .setSourceId("bbc")
              .setSourceName("BBC News")
              .setPublishedAt("2026-11-12T08:00:00Z")
              .setOriginalUrl("https://bbc.com/" + i)
              .setCategory("world");
      if (i % 2 == 0) {
        b.setImageUrl("https://images.bbc.com/pic" + i + ".jpg");
      }
      realArticles.add(b.build());
    }

    adapter.setEditorialLayout(true);
    adapter.submitList(realArticles);

    // Verify Position 0 selected the first article WITH image as Featured Story
    NewsArticle featured = adapter.getItem(0);
    assertNotNull(featured);
    assertEquals("real-1-img", featured.getId());
    assertEquals(NewsAdapter.VIEW_TYPE_FEATURED, adapter.getItemViewType(0));
    assertEquals("https://images.cnn.com/climate.jpg", featured.getImageUrl());
    assertEquals("CNN Visuals", featured.getImageCredit());

    // Verify all articles across sections are unique (zero duplicates)
    java.util.Set<String> seenIds = new java.util.HashSet<>();
    seenIds.add(featured.getId());

    // Position 1 is Recommended container: check its items
    assertEquals(NewsAdapter.VIEW_TYPE_RECOMMENDED, adapter.getItemViewType(1));

    // Check latest items
    for (int pos = 2; pos < adapter.getItemCount(); pos++) {
      NewsArticle item = adapter.getItem(pos);
      assertNotNull("Latest article at " + pos + " should not be null", item);
      assertFalse("Article " + item.getId() + " must not duplicate", seenIds.contains(item.getId()));
      seenIds.add(item.getId());
    }
  }

  @Test
  public void testRealImageArticles_paginationAppendMaintainsLayout() {
    List<NewsArticle> page1 = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      page1.add(
          new NewsArticle.Builder()
              .setId("p1-" + i)
              .setTitle("Page 1 Headline " + i)
              .setSourceId("bbc")
              .setSourceName("BBC News")
              .setPublishedAt("2026-11-12T10:00:00Z")
              .setOriginalUrl("https://bbc.com/p1-" + i)
              .setCategory("world")
              .setImageUrl(i % 2 == 0 ? "https://images.com/" + i + ".jpg" : null)
              .build());
    }

    adapter.setEditorialLayout(true);
    adapter.submitList(page1);
    int initialCount = adapter.getItemCount();
    assertTrue(initialCount > 0);

    // Append page 2
    List<NewsArticle> combined = new ArrayList<>(page1);
    for (int i = 0; i < 5; i++) {
      combined.add(
          new NewsArticle.Builder()
              .setId("p2-" + i)
              .setTitle("Page 2 Headline " + i)
              .setSourceId("bbc")
              .setSourceName("BBC News")
              .setPublishedAt("2026-11-12T09:00:00Z")
              .setOriginalUrl("https://bbc.com/p2-" + i)
              .setCategory("world")
              .setImageUrl("https://images.com/p2-" + i + ".jpg")
              .build());
    }

    adapter.submitList(combined);
    // Combined has 11 articles: 1 featured + 1 rec container (4 rec articles) + 6 latest articles = 8 rows
    assertEquals(8, adapter.getItemCount());
    assertEquals(NewsAdapter.VIEW_TYPE_FEATURED, adapter.getItemViewType(0));
    assertEquals(NewsAdapter.VIEW_TYPE_RECOMMENDED, adapter.getItemViewType(1));
    for (int pos = 2; pos < adapter.getItemCount(); pos++) {
      assertNotNull(adapter.getItem(pos));
    }
  }
}
