package com.example.newsagnet.ui.adapter.viewholder;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.R;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.ui.adapter.NewsItemClickListener;

/**
 * ViewHolder for Type D — Text Only Card (editorial fallback for articles without images).
 */
public class TextOnlyNewsViewHolder extends RecyclerView.ViewHolder {

  public final TextView tvTitle;
  public final TextView tvSource;
  public final TextView tvCategory;
  public final TextView tvTime;

  public TextOnlyNewsViewHolder(@NonNull View itemView) {
    super(itemView);
    tvTitle = itemView.findViewById(R.id.tv_text_title);
    tvSource = itemView.findViewById(R.id.tv_text_source);
    tvCategory = itemView.findViewById(R.id.tv_text_category);
    tvTime = itemView.findViewById(R.id.tv_text_time);
  }

  public void bind(@NonNull NewsArticle article, @Nullable NewsItemClickListener listener) {
    tvTitle.setText(article.getTitle());
    tvSource.setText(article.getSourceName());

    String rawDate = article.getPublishedAt();
    if (rawDate != null && rawDate.contains("T")) {
      tvTime.setText(rawDate.substring(0, rawDate.indexOf('T')));
    } else {
      tvTime.setText(rawDate != null ? rawDate : "");
    }

    if (article.getCategory() != null && !article.getCategory().trim().isEmpty()) {
      tvCategory.setText(article.getCategory().toUpperCase());
      tvCategory.setVisibility(View.VISIBLE);
    } else {
      tvCategory.setVisibility(View.GONE);
    }

    itemView.setOnClickListener(
        v -> {
          if (listener != null) {
            listener.onNewsItemClick(article);
          }
        });
  }
}
