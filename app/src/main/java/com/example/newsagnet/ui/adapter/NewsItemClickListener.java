package com.example.newsagnet.ui.adapter;

import com.example.newsagnet.model.NewsArticle;

/**
 * Callback interface for news item user interactions.
 *
 * <p>Conforms to COMP7506 Ch5 (Event-Driven Architecture & MVC/MVVM patterns).
 */
public interface NewsItemClickListener {

  /**
   * Invoked when an article card is clicked by the user.
   *
   * @param article The selected NewsArticle instance.
   */
  void onNewsItemClick(NewsArticle article);
}
