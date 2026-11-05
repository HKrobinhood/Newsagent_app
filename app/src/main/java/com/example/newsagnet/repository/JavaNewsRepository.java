package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.model.NewsArticle;
import java.util.List;

/**
 * Abstraction interface for querying news headlines and geographic situation datasets.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Abstraction & Interfaces) and Ch5 (Repository Pattern).
 */
public interface JavaNewsRepository {

  /**
   * Queries paginated news articles with optional filters.
   *
   * @param page 1-based page index.
   * @param limit Number of articles per page.
   * @param category Optional category filter key.
   * @param source Optional publisher source identifier.
   * @param keyword Optional search keyword.
   * @param callback Callback receiving {@link NewsPageResult} or error.
   */
  void getNews(
      int page,
      int limit,
      @Nullable String category,
      @Nullable String source,
      @Nullable String keyword,
      @NonNull NewsCallback<NewsPageResult> callback);

  /**
   * Retrieves single article details by ID.
   *
   * @param id Article identifier.
   * @param callback Callback receiving {@link NewsArticle} or error.
   */
  void getNewsDetail(@NonNull String id, @NonNull NewsCallback<NewsArticle> callback);

  /**
   * Retrieves geolocated news articles for 3D globe visualization.
   *
   * @param category Optional category filter.
   * @param callback Callback receiving list of geocoded {@link NewsArticle}s.
   */
  void getGeoNews(
      @Nullable String category, @NonNull NewsCallback<List<NewsArticle>> callback);
}
