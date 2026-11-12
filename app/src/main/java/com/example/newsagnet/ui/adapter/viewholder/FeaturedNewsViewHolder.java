package com.example.newsagnet.ui.adapter.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.newsagnet.R;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.ui.adapter.NewsItemClickListener;

/**
 * ViewHolder for the Editorial Featured Cover Story card (16:9 large cover image).
 */
public class FeaturedNewsViewHolder extends RecyclerView.ViewHolder {

  public final ImageView ivCover;
  public final TextView tvTitle;
  public final TextView tvSource;
  public final TextView tvCategory;
  public final TextView tvTime;
  public final TextView tvCredit;
  public final ImageView ivOpen;

  public FeaturedNewsViewHolder(@NonNull View itemView) {
    super(itemView);
    ivCover = itemView.findViewById(R.id.iv_featured_cover);
    tvTitle = itemView.findViewById(R.id.tv_featured_title);
    tvSource = itemView.findViewById(R.id.tv_featured_source);
    tvCategory = itemView.findViewById(R.id.tv_featured_category);
    tvTime = itemView.findViewById(R.id.tv_featured_time);
    tvCredit = itemView.findViewById(R.id.tv_featured_credit);
    ivOpen = itemView.findViewById(R.id.iv_featured_open);
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

    if (article.getImageCredit() != null && !article.getImageCredit().trim().isEmpty()) {
      tvCredit.setText(article.getImageCredit());
      tvCredit.setVisibility(View.VISIBLE);
    } else {
      tvCredit.setVisibility(View.GONE);
    }

    Object imageModel =
        article.getMockDrawableRes() != null
            ? article.getMockDrawableRes()
            : article.getImageUrl();

    if (imageModel != null) {
      Glide.with(itemView.getContext())
          .load(imageModel)
          .placeholder(R.drawable.mock_img_placeholder)
          .error(R.drawable.mock_img_placeholder)
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .transition(DrawableTransitionOptions.withCrossFade())
          .centerCrop()
          .into(ivCover);
    } else {
      ivCover.setImageResource(R.drawable.mock_img_placeholder);
    }

    itemView.setOnClickListener(
        v -> {
          if (listener != null) {
            listener.onNewsItemClick(article);
          }
        });
  }
}
