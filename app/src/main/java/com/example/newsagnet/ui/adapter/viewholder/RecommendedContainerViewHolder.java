package com.example.newsagnet.ui.adapter.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.newsagnet.R;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.ui.adapter.NewsItemClickListener;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewHolder for the horizontal Recommended news carousel container.
 */
public class RecommendedContainerViewHolder extends RecyclerView.ViewHolder {

  private final RecyclerView rvRecommended;
  private final RecommendedAdapter innerAdapter;

  public RecommendedContainerViewHolder(@NonNull View itemView) {
    super(itemView);
    rvRecommended = itemView.findViewById(R.id.rv_recommended);
    rvRecommended.setLayoutManager(
        new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
    innerAdapter = new RecommendedAdapter();
    rvRecommended.setAdapter(innerAdapter);
  }

  public void bind(
      @NonNull List<NewsArticle> recommendedArticles,
      @Nullable NewsItemClickListener listener) {
    innerAdapter.setArticles(recommendedArticles, listener);
  }

  private static class RecommendedAdapter
      extends RecyclerView.Adapter<RecommendedCardViewHolder> {

    private final List<NewsArticle> items = new ArrayList<>();
    private NewsItemClickListener clickListener;

    public void setArticles(
        @NonNull List<NewsArticle> articles,
        @Nullable NewsItemClickListener listener) {
      this.items.clear();
      this.items.addAll(articles);
      this.clickListener = listener;
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecommendedCardViewHolder onCreateViewHolder(
        @NonNull ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_news_recommended_card, parent, false);
      return new RecommendedCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
        @NonNull RecommendedCardViewHolder holder, int position) {
      NewsArticle article = items.get(position);
      holder.bind(article, clickListener);
    }

    @Override
    public int getItemCount() {
      return items.size();
    }
  }

  private static class RecommendedCardViewHolder extends RecyclerView.ViewHolder {
    private final ImageView ivImage;
    private final TextView tvTitle;
    private final TextView tvSource;

    public RecommendedCardViewHolder(@NonNull View itemView) {
      super(itemView);
      ivImage = itemView.findViewById(R.id.iv_rec_image);
      tvTitle = itemView.findViewById(R.id.tv_rec_title);
      tvSource = itemView.findViewById(R.id.tv_rec_source);
    }

    public void bind(
        @NonNull NewsArticle article, @Nullable NewsItemClickListener listener) {
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
            .centerCrop()
            .into(ivImage);
      } else {
        ivImage.setImageResource(R.drawable.mock_img_placeholder);
      }

      itemView.setOnClickListener(
          v -> {
            if (listener != null) {
              listener.onNewsItemClick(article);
            }
          });
    }
  }
}
