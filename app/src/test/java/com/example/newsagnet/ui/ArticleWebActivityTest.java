package com.example.newsagnet.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.newsagnet.ui.activity.ArticleWebActivity;
import org.junit.Test;

/**
 * Unit tests for ArticleWebActivity helper and URL normalization logic.
 */
public class ArticleWebActivityTest {

  @Test
  public void testNormalizeUrl_withHttps_keepsIntact() {
    String input = "https://www.bbc.com/news/world";
    String normalized = ArticleWebActivity.normalizeUrl(input);
    assertEquals("https://www.bbc.com/news/world", normalized);
  }

  @Test
  public void testNormalizeUrl_withHttp_keepsIntact() {
    String input = "http://edition.cnn.com/article";
    String normalized = ArticleWebActivity.normalizeUrl(input);
    assertEquals("http://edition.cnn.com/article", normalized);
  }

  @Test
  public void testNormalizeUrl_withoutScheme_prependsHttps() {
    String input = "aljazeera.com/news/live";
    String normalized = ArticleWebActivity.normalizeUrl(input);
    assertEquals("https://aljazeera.com/news/live", normalized);
  }

  @Test
  public void testNormalizeUrl_withTrimmingWhitespace() {
    String input = "   www.bbc.co.uk/news   ";
    String normalized = ArticleWebActivity.normalizeUrl(input);
    assertEquals("https://www.bbc.co.uk/news", normalized);
  }

  @Test
  public void testConstants_areDefined() {
    assertNotNull(ArticleWebActivity.EXTRA_URL);
    assertNotNull(ArticleWebActivity.EXTRA_TITLE);
    assertNotNull(ArticleWebActivity.EXTRA_SOURCE);
  }
}
