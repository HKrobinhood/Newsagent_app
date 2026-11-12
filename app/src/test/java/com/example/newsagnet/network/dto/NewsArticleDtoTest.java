package com.example.newsagnet.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.newsagnet.model.NewsArticle;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for NewsArticleDto and ArticleMediaDto JSON parsing and domain mapping (Milestone N3).
 */
public class NewsArticleDtoTest {

  private Gson gson;

  @Before
  public void setUp() {
    gson = new Gson();
  }

  @Test
  public void testParseArticleWithDirectMediaUrl() {
    String json =
        "{\n"
            + "  \"id\": \"news-101\",\n"
            + "  \"title\": \"Major Scientific Discovery in Deep Sea Trench\",\n"
            + "  \"source_id\": \"bbc\",\n"
            + "  \"source_name\": \"BBC News\",\n"
            + "  \"published_at\": \"2026-11-12T10:00:00Z\",\n"
            + "  \"original_url\": \"https://www.bbc.com/news/deep-sea-101\",\n"
            + "  \"category\": \"science\",\n"
            + "  \"media\": {\n"
            + "    \"media_type\": \"image\",\n"
            + "    \"image_url\": \"https://images.bbci.co.uk/deepsea.jpg\",\n"
            + "    \"thumbnail_url\": \"/api/v1/media/media-101/thumbnail\",\n"
            + "    \"width\": 1600,\n"
            + "    \"height\": 900,\n"
            + "    \"credit\": \"BBC Ocean Science / Jane Doe\"\n"
            + "  }\n"
            + "}";

    NewsArticleDto dto = gson.fromJson(json, NewsArticleDto.class);
    assertNotNull(dto);
    assertEquals("news-101", dto.getId());
    assertNotNull(dto.getMedia());
    assertEquals("https://images.bbci.co.uk/deepsea.jpg", dto.getMedia().getImageUrl());

    NewsArticle domain = dto.toDomainModel();
    assertNotNull(domain);
    assertEquals("news-101", domain.getId());
    assertEquals("https://images.bbci.co.uk/deepsea.jpg", domain.getImageUrl());
    assertEquals("BBC Ocean Science / Jane Doe", domain.getImageCredit());
    assertTrue(domain.hasImage());
  }

  @Test
  public void testParseArticleWithRelativeThumbnailUrlFallback() {
    String json =
        "{\n"
            + "  \"id\": \"news-102\",\n"
            + "  \"title\": \"Local Community Restores Ancient Forest\",\n"
            + "  \"source_id\": \"cnn\",\n"
            + "  \"source_name\": \"CNN\",\n"
            + "  \"published_at\": \"2026-11-12T11:00:00Z\",\n"
            + "  \"original_url\": \"https://www.cnn.com/forest-102\",\n"
            + "  \"category\": \"world\",\n"
            + "  \"media\": {\n"
            + "    \"media_type\": \"image\",\n"
            + "    \"thumbnail_url\": \"/api/v1/media/media-102/thumbnail\",\n"
            + "    \"credit\": \"Forestry Network\"\n"
            + "  }\n"
            + "}";

    NewsArticleDto dto = gson.fromJson(json, NewsArticleDto.class);
    assertNotNull(dto);
    NewsArticle domain = dto.toDomainModel();
    assertNotNull(domain);
    assertNotNull(domain.getImageUrl());
    assertTrue("Thumbnail URL should be resolved to absolute host", domain.getImageUrl().startsWith("http"));
    assertTrue("Thumbnail URL should point to media endpoint", domain.getImageUrl().contains("/api/v1/media/media-102/thumbnail"));
    assertEquals("Forestry Network", domain.getImageCredit());
    assertTrue(domain.hasImage());
  }

  @Test
  public void testParseArticleWithoutMedia_degradesGracefully() {
    String json =
        "{\n"
            + "  \"id\": \"news-103\",\n"
            + "  \"title\": \"Treasury Issues Quarterly Bond Guidance\",\n"
            + "  \"source_id\": \"reuters\",\n"
            + "  \"source_name\": \"Reuters\",\n"
            + "  \"published_at\": \"2026-11-12T12:00:00Z\",\n"
            + "  \"original_url\": \"https://www.reuters.com/bond-guidance-103\",\n"
            + "  \"category\": \"business\"\n"
            + "}";

    NewsArticleDto dto = gson.fromJson(json, NewsArticleDto.class);
    assertNotNull(dto);
    assertNull(dto.getMedia());

    NewsArticle domain = dto.toDomainModel();
    assertNotNull(domain);
    assertNull(domain.getImageUrl());
    assertNull(domain.getImageCredit());
    assertNull(domain.getMockDrawableRes());
    assertFalse(domain.hasImage());
  }
}
