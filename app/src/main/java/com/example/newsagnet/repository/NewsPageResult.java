package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import com.example.newsagnet.model.NewsArticle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data container representing a paginated news query result.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP & Encapsulation).
 */
public class NewsPageResult {

  private final List<NewsArticle> articles;
  private final int total;
  private final int page;
  private final boolean hasMore;

  public NewsPageResult(
      @NonNull List<NewsArticle> articles, int total, int page, boolean hasMore) {
    this.articles = new ArrayList<>(articles);
    this.total = total;
    this.page = page;
    this.hasMore = hasMore;
  }

  @NonNull
  public List<NewsArticle> getArticles() {
    return Collections.unmodifiableList(articles);
  }

  public int getTotal() {
    return total;
  }

  public int getPage() {
    return page;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public boolean getHasMore() {
    return hasMore;
  }

  @NonNull
  @Override
  public String toString() {
    return "NewsPageResult{"
        + "articlesCount="
        + articles.size()
        + ", total="
        + total
        + ", page="
        + page
        + ", hasMore="
        + hasMore
        + '}';
  }
}
