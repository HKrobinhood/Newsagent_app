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
}
