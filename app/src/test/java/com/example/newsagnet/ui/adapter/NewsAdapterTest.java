package com.example.newsagnet.ui.adapter;

import static org.junit.Assert.assertEquals;
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
}
