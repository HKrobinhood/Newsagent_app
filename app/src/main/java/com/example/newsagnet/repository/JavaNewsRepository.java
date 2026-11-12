package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsSourceItem;
import java.util.List;

/**
 * Abstraction interface for querying news headlines, media sources, and geographic situation
 * datasets.
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
   * Queries paginated news articles with optional category, source set, and keyword filters.
   *
   * @param page 1-based page index.
   * @param limit Number of articles per page.
   * @param category Optional category filter key.
   * @param source Optional single publisher source identifier.
   * @param sourceIds Optional comma-separated allowed source IDs (`"bbc,cnn"`, or `"__none__"`).
   * @param keyword Optional search keyword.
   * @param callback Callback receiving {@link NewsPageResult} or error.
   */
  default void getNews(
      int page,
      int limit,
      @Nullable String category,
      @Nullable String source,
      @Nullable String sourceIds,
      @Nullable String keyword,
      @NonNull NewsCallback<NewsPageResult> callback) {
    getNews(page, limit, category, source, keyword, callback);
  }

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

  /**
   * Retrieves geolocated news articles filtered by category and allowed media source IDs.
   *
   * @param category Optional category filter.
   * @param sourceIds Optional comma-separated allowed source IDs (`"bbc,cnn"`, or `"__none__"`).
   * @param callback Callback receiving list of geocoded {@link NewsArticle}s.
   */
  default void getGeoNews(
      @Nullable String category,
      @Nullable String sourceIds,
      @NonNull NewsCallback<List<NewsArticle>> callback) {
    getGeoNews(category, callback);
  }

  /**
   * Retrieves the list of active news media sources from the backend.
   *
   * @param callback Callback receiving list of {@link NewsSourceItem}s.
   */
  default void getNewsSources(@NonNull NewsCallback<List<NewsSourceItem>> callback) {
    callback.onSuccess(NewsSourceItem.getDefaultSources());
  }
}
