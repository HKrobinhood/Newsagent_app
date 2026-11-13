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
 * ViewHolder for Type B — Medium Card (1-column half width, suitable for 2 side-by-side).
 */
public class MediumNewsViewHolder extends RecyclerView.ViewHolder {

  public final ImageView ivCover;
  public final TextView tvTitle;
  public final TextView tvSource;

  public MediumNewsViewHolder(@NonNull View itemView) {
    super(itemView);
    ivCover = itemView.findViewById(R.id.iv_medium_cover);
    tvTitle = itemView.findViewById(R.id.tv_medium_title);
    tvSource = itemView.findViewById(R.id.tv_medium_source);
  }

  public void bind(@NonNull NewsArticle article, @Nullable NewsItemClickListener listener) {
    tvTitle.setText(article.getTitle());

    String timeStr = "";
    String rawDate = article.getPublishedAt();
    if (rawDate != null && rawDate.contains("T")) {
      timeStr = rawDate.substring(0, rawDate.indexOf('T'));
    }
    String meta = article.getSourceName();
    if (!timeStr.isEmpty()) {
      meta += " • " + timeStr;
    }
    tvSource.setText(meta);

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
