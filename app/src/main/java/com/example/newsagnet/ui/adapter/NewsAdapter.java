package com.example.newsagnet.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.newsagnet.R;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.ui.adapter.viewholder.CompactNewsViewHolder;
import com.example.newsagnet.ui.adapter.viewholder.FeaturedNewsViewHolder;
import com.example.newsagnet.ui.adapter.viewholder.LargeNewsViewHolder;
import com.example.newsagnet.ui.adapter.viewholder.MediumNewsViewHolder;
import com.example.newsagnet.ui.adapter.viewholder.RecommendedContainerViewHolder;
import com.example.newsagnet.ui.adapter.viewholder.TextOnlyNewsViewHolder;
import com.example.newsagnet.ui.theme.FontScaleManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-ViewType RecyclerView Adapter managing the editorial magazine-style layout:
 * Featured Headline (Cover story), Recommended horizontal carousel, and Asymmetric Latest
 * news grid (Large, Medium, Compact, Text-Only cards).
 *
 * <p>Conforms to COMP7506 Ch5 (RecyclerView.Adapter pattern and View Recycling).
 */
public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  public static final int VIEW_TYPE_STANDARD = -1;
  public static final int VIEW_TYPE_FEATURED = 0;
  public static final int VIEW_TYPE_RECOMMENDED = 1;
  public static final int VIEW_TYPE_LARGE = 2;
  public static final int VIEW_TYPE_MEDIUM = 3;
  public static final int VIEW_TYPE_COMPACT = 4;
  public static final int VIEW_TYPE_TEXT_ONLY = 5;

  private final List<NewsArticle> rawArticles = new ArrayList<>();
  private final List<NewsArticle> recommendedArticles = new ArrayList<>();
  private final List<NewsArticle> latestArticles = new ArrayList<>();
  private NewsArticle featuredArticle;

  private NewsItemClickListener clickListener;
  private float fontScale = UserPreferences.DEFAULT_FONT_SCALE;
  private boolean isEditorialLayout = true;
  private boolean hasRecommendedSection = false;

  public NewsAdapter() {}

  public NewsAdapter(@Nullable NewsItemClickListener clickListener) {
    this.clickListener = clickListener;
  }

  public NewsAdapter(@Nullable NewsItemClickListener clickListener, boolean isEditorialLayout) {
    this.clickListener = clickListener;
    this.isEditorialLayout = isEditorialLayout;
  }

  public void setOnNewsItemClickListener(@Nullable NewsItemClickListener listener) {
    this.clickListener = listener;
  }

  public void setEditorialLayout(boolean editorialLayout) {
    if (this.isEditorialLayout != editorialLayout) {
      this.isEditorialLayout = editorialLayout;
      partitionDataset();
      notifyDataSetChanged();
    }
  }

  public boolean isEditorialLayout() {
    return isEditorialLayout;
  }

  public void setFontScale(float fontScale) {
    if (Float.compare(this.fontScale, fontScale) != 0) {
      this.fontScale = fontScale;
      try {
        notifyDataSetChanged();
      } catch (Exception ignored) {
        // Ignored in host JVM unit tests
      }
    }
  }

  public float getFontScale() {
    return fontScale;
  }

  /**
   * Updates article dataset and partitions items into editorial magazine sections.
   *
   * @param newArticles Updated list of news articles.
   */
  public void submitList(@NonNull List<NewsArticle> newArticles) {
    rawArticles.clear();
    rawArticles.addAll(newArticles);
    partitionDataset();
    try {
      notifyDataSetChanged();
    } catch (Exception ignored) {
      // In host JVM unit tests where the adapter is unattached and Android Observable is stubbed
    }
  }

  private void partitionDataset() {
    recommendedArticles.clear();
    latestArticles.clear();
    featuredArticle = null;
    hasRecommendedSection = false;

    if (rawArticles.isEmpty()) {
      return;
    }

    if (!isEditorialLayout || rawArticles.size() < 4) {
      // Fallback: direct 1-to-1 card layout for small datasets (<4) or standard mode
      return;
    }

    // Editorial partition:
    // Index 0: Featured Cover Story
    featuredArticle = rawArticles.get(0);

    // Indices 1..4: Recommended carousel (up to 4 items)
    int recEnd = Math.min(5, rawArticles.size());
    for (int i = 1; i < recEnd; i++) {
      recommendedArticles.add(rawArticles.get(i));
    }
    hasRecommendedSection = !recommendedArticles.isEmpty();

    // Indices 5..N: Latest News Asymmetric Grid
    for (int i = recEnd; i < rawArticles.size(); i++) {
      latestArticles.add(rawArticles.get(i));
    }
  }

  /**
   * Returns the column span required for GridLayoutManager (2 columns total).
   */
  public int getSpanSize(int position) {
    int viewType = getItemViewType(position);
    if (viewType == VIEW_TYPE_MEDIUM) {
      return 1; // Half width (two cards side-by-side)
    }
    return 2; // All other types (Featured, Recommended, Large, Compact, TextOnly) span full width
  }

  @Override
  public int getItemViewType(int position) {
    if (!isEditorialLayout) {
      return VIEW_TYPE_STANDARD;
    }

    if (hasRecommendedSection) {
      if (position == 0) {
        return (featuredArticle != null && featuredArticle.hasImage())
            ? VIEW_TYPE_FEATURED
            : VIEW_TYPE_TEXT_ONLY;
      }
      if (position == 1) {
        return VIEW_TYPE_RECOMMENDED;
      }

      int latestIndex = position - 2;
      if (latestIndex >= 0 && latestIndex < latestArticles.size()) {
        NewsArticle article = latestArticles.get(latestIndex);
        if (!article.hasImage()) {
          return VIEW_TYPE_TEXT_ONLY;
        }
        int pattern = latestIndex % 5;
        if (pattern == 0 || pattern == 1) {
          return VIEW_TYPE_MEDIUM;
        } else if (pattern == 2) {
          return VIEW_TYPE_LARGE;
        } else {
          return VIEW_TYPE_COMPACT;
        }
      }
      return VIEW_TYPE_COMPACT;
    }

    // When hasRecommendedSection is false (<4 items)
    if (position == 0 && !rawArticles.isEmpty()) {
      return rawArticles.get(0).hasImage() ? VIEW_TYPE_FEATURED : VIEW_TYPE_TEXT_ONLY;
    }
    if (position < rawArticles.size()) {
      NewsArticle article = rawArticles.get(position);
      if (!article.hasImage()) {
        return VIEW_TYPE_TEXT_ONLY;
      }
      return (position % 2 == 0) ? VIEW_TYPE_LARGE : VIEW_TYPE_COMPACT;
    }
    return VIEW_TYPE_STANDARD;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == VIEW_TYPE_FEATURED) {
      View view = inflater.inflate(R.layout.item_news_featured, parent, false);
      return new FeaturedNewsViewHolder(view);
    } else if (viewType == VIEW_TYPE_RECOMMENDED) {
      View view = inflater.inflate(R.layout.item_news_recommended_container, parent, false);
      return new RecommendedContainerViewHolder(view);
    } else if (viewType == VIEW_TYPE_LARGE) {
      View view = inflater.inflate(R.layout.item_news_large, parent, false);
      return new LargeNewsViewHolder(view);
    } else if (viewType == VIEW_TYPE_MEDIUM) {
      View view = inflater.inflate(R.layout.item_news_medium, parent, false);
      return new MediumNewsViewHolder(view);
    } else if (viewType == VIEW_TYPE_COMPACT) {
      View view = inflater.inflate(R.layout.item_news_compact, parent, false);
      return new CompactNewsViewHolder(view);
    } else if (viewType == VIEW_TYPE_TEXT_ONLY) {
      View view = inflater.inflate(R.layout.item_news_text_only, parent, false);
      return new TextOnlyNewsViewHolder(view);
    } else {
      View view = inflater.inflate(R.layout.item_news, parent, false);
      return new NewsViewHolder(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof FeaturedNewsViewHolder) {
      NewsArticle article = getItem(position);
      if (article != null) {
        ((FeaturedNewsViewHolder) holder).bind(article, clickListener);
      }
    } else if (holder instanceof RecommendedContainerViewHolder) {
      ((RecommendedContainerViewHolder) holder).bind(recommendedArticles, clickListener);
    } else if (holder instanceof LargeNewsViewHolder) {
      NewsArticle article = getItem(position);
      if (article != null) {
        ((LargeNewsViewHolder) holder).bind(article, clickListener);
      }
    } else if (holder instanceof MediumNewsViewHolder) {
      NewsArticle article = getItem(position);
      if (article != null) {
        ((MediumNewsViewHolder) holder).bind(article, clickListener);
      }
    } else if (holder instanceof CompactNewsViewHolder) {
      NewsArticle article = getItem(position);
      if (article != null) {
        ((CompactNewsViewHolder) holder).bind(article, clickListener);
      }
    } else if (holder instanceof TextOnlyNewsViewHolder) {
      NewsArticle article = getItem(position);
      if (article != null) {
        ((TextOnlyNewsViewHolder) holder).bind(article, clickListener);
      }
    } else if (holder instanceof NewsViewHolder) {
      NewsArticle article = getItem(position);
      if (article != null) {
        ((NewsViewHolder) holder).bind(article, clickListener);
      }
    }
    FontScaleManager.applyFontScale(holder.itemView, fontScale);
  }

  @Override
  public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
    super.onViewRecycled(holder);
    try {
      if (holder instanceof FeaturedNewsViewHolder) {
        Glide.with(holder.itemView.getContext())
            .clear(((FeaturedNewsViewHolder) holder).ivCover);
      } else if (holder instanceof LargeNewsViewHolder) {
        Glide.with(holder.itemView.getContext())
            .clear(((LargeNewsViewHolder) holder).ivCover);
      } else if (holder instanceof MediumNewsViewHolder) {
        Glide.with(holder.itemView.getContext())
            .clear(((MediumNewsViewHolder) holder).ivCover);
      } else if (holder instanceof CompactNewsViewHolder) {
        Glide.with(holder.itemView.getContext())
            .clear(((CompactNewsViewHolder) holder).ivCover);
      }
    } catch (Exception ignored) {
      // Host context destroyed
    }
  }

  @Override
  public int getItemCount() {
    if (rawArticles.isEmpty()) {
      return 0;
    }
    if (!isEditorialLayout || !hasRecommendedSection) {
      return rawArticles.size();
    }
    // 1 (Featured) + 1 (Recommended container) + latestArticles.size()
    return 2 + latestArticles.size();
  }

  @Nullable
  public NewsArticle getItem(int position) {
    if (position < 0) {
      return null;
    }

    if (!isEditorialLayout || !hasRecommendedSection) {
      if (position < rawArticles.size()) {
        return rawArticles.get(position);
      }
      return null;
    }

    if (position == 0) {
      return featuredArticle;
    }
    if (position == 1) {
      return !recommendedArticles.isEmpty() ? recommendedArticles.get(0) : null;
    }
    int latestIndex = position - 2;
    if (latestIndex >= 0 && latestIndex < latestArticles.size()) {
      return latestArticles.get(latestIndex);
    }
    return null;
  }
}
