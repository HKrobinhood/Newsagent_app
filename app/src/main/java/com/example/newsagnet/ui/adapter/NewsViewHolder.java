package com.example.newsagnet.ui.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.R;
import com.example.newsagnet.model.NewsArticle;

/**
 * ViewHolder caching view references for an item in the news RecyclerView.
 *
 * <p>Conforms to COMP7506 Ch5 (RecyclerView, ViewHolder pattern) and Ch6A (ImageView & Drawables).
 */
public class NewsViewHolder extends RecyclerView.ViewHolder {

  private final TextView tvSource;
  private final TextView tvPublishedAt;
  private final TextView tvTitle;
  private final TextView tvCategory;
  private final LinearLayout llLocation;
  private final TextView tvLocation;
  private final ImageView ivOpenIcon;

  public NewsViewHolder(@NonNull View itemView) {
    super(itemView);
    tvSource = itemView.findViewById(R.id.tv_source);
    tvPublishedAt = itemView.findViewById(R.id.tv_published_at);
    tvTitle = itemView.findViewById(R.id.tv_title);
    tvCategory = itemView.findViewById(R.id.tv_category);
    llLocation = itemView.findViewById(R.id.ll_location);
    tvLocation = itemView.findViewById(R.id.tv_location);
    ivOpenIcon = itemView.findViewById(R.id.iv_open_icon);
  }

  /**
   * Binds article domain data to cached view elements.
   *
   * @param article The article model to display.
   * @param listener Callback for click events.
   */
  public void bind(@NonNull NewsArticle article, @Nullable NewsItemClickListener listener) {
    tvSource.setText(article.getSourceName());

    String rawDate = article.getPublishedAt();
    if (rawDate != null && rawDate.contains("T")) {
      tvPublishedAt.setText(rawDate.substring(0, rawDate.indexOf('T')));
    } else {
      tvPublishedAt.setText(rawDate != null ? rawDate : "");
    }

    tvTitle.setText(article.getTitle());

    if (article.getCategory() != null) {
      tvCategory.setText(article.getCategory().toUpperCase());
      tvCategory.setVisibility(View.VISIBLE);
    } else {
      tvCategory.setVisibility(View.GONE);
    }

    String location = article.getLocationName();
    if (location != null && !location.trim().isEmpty()) {
      tvLocation.setText(location);
      llLocation.setVisibility(View.VISIBLE);
    } else {
      llLocation.setVisibility(View.GONE);
    }

    itemView.setOnClickListener(v -> {
      if (listener != null) {
        listener.onNewsItemClick(article);
      }
    });
  }
}
