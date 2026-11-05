package com.example.newsagnet.ui.globe;

import androidx.annotation.NonNull;
import com.example.newsagnet.model.NewsArticle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates an aggregated spatial cluster or single news marker projected onto 2D screen space.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation and Domain Modeling).
 */
public class HotspotCluster implements Serializable {

  private static final long serialVersionUID = 1L;

  private final List<NewsArticle> articles;
  private final float screenX;
  private final float screenY;
  private final float depthZ;
  private final String primaryCategory;
  private final boolean isCluster;

  public HotspotCluster(
      @NonNull List<NewsArticle> articles,
      float screenX,
      float screenY,
      float depthZ,
      @NonNull String primaryCategory) {
    this.articles = Collections.unmodifiableList(new ArrayList<>(articles));
    this.screenX = screenX;
    this.screenY = screenY;
    this.depthZ = depthZ;
    this.primaryCategory = primaryCategory;
    this.isCluster = articles.size() > 1;
  }

  @NonNull
  public List<NewsArticle> getArticles() {
    return articles;
  }

  public float getScreenX() {
    return screenX;
  }

  public float getScreenY() {
    return screenY;
  }

  public float getDepthZ() {
    return depthZ;
  }

  @NonNull
  public String getPrimaryCategory() {
    return primaryCategory;
  }

  public boolean isCluster() {
    return isCluster;
  }

  @NonNull
  public NewsArticle getPrimaryArticle() {
    return articles.get(0);
  }

  public int getCount() {
    return articles.size();
  }
}
