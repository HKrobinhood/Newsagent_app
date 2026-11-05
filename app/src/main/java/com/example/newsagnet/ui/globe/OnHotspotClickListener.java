package com.example.newsagnet.ui.globe;

import androidx.annotation.NonNull;
import com.example.newsagnet.model.NewsArticle;
import java.util.List;

/**
 * Callback interface invoked when a user taps on a spatial news hotspot or cluster on the 3D Globe.
 *
 * <p>Conforms to COMP7506 Ch5 & Ch6 (Event Listener Callback Pattern).
 */
public interface OnHotspotClickListener {

  /**
   * Invoked when a single news hotspot marker is selected.
   *
   * @param article The selected news article.
   */
  void onSingleHotspotClick(@NonNull NewsArticle article);

  /**
   * Invoked when a multi-article cluster marker is selected.
   *
   * @param clusterArticles The list of articles aggregated within this cluster.
   */
  void onClusterHotspotClick(@NonNull List<NewsArticle> clusterArticles);
}
