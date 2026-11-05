package com.example.newsagnet.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.R;
import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.NewsArticle;
import java.util.List;

/**
 * ViewHolder for AI Agent responses, thinking states, mode tags, and citation source cards.
 *
 * <p>Conforms to COMP7506 Ch5 (Multi-Type ViewHolder & Dynamic View Inflation) and Ch6A (Drawables
 * & Images).
 */
public class AiMessageViewHolder extends RecyclerView.ViewHolder {

  private final TextView tvAiContent;
  private final TextView tvAiTimestamp;
  private final TextView tvAiMode;
  private final ProgressBar progressThinking;
  private final TextView tvSourcesHeader;
  private final LinearLayout layoutSourcesContainer;

  public AiMessageViewHolder(@NonNull View itemView) {
    super(itemView);
    tvAiContent = itemView.findViewById(R.id.tv_ai_content);
    tvAiTimestamp = itemView.findViewById(R.id.tv_ai_timestamp);
    tvAiMode = itemView.findViewById(R.id.tv_ai_mode);
    progressThinking = itemView.findViewById(R.id.progress_thinking);
    tvSourcesHeader = itemView.findViewById(R.id.tv_sources_header);
    layoutSourcesContainer = itemView.findViewById(R.id.layout_sources_container);
  }

  public void bind(@NonNull ChatMessage message, @Nullable NewsItemClickListener sourceClickListener) {
    // Thinking state
    if (message.isThinking()) {
      progressThinking.setVisibility(View.VISIBLE);
      tvAiContent.setText(message.getContent() != null && !message.getContent().isEmpty()
          ? message.getContent()
          : "Analyzing intelligence streams...");
    } else {
      progressThinking.setVisibility(View.GONE);
      tvAiContent.setText(message.getContent());
    }

    // Mode tag
    String mode = message.getModeUsed();
    if (mode != null && !mode.trim().isEmpty()) {
      tvAiMode.setText("Mode: " + mode);
      tvAiMode.setVisibility(View.VISIBLE);
    } else {
      tvAiMode.setVisibility(View.GONE);
    }

    // Timestamp
    String ts = message.getTimestamp();
    if (ts != null && !ts.trim().isEmpty()) {
      tvAiTimestamp.setText(ts);
      tvAiTimestamp.setVisibility(View.VISIBLE);
    } else {
      tvAiTimestamp.setVisibility(View.GONE);
    }

    // Referenced Sources List
    List<NewsArticle> sources = message.getSources();
    layoutSourcesContainer.removeAllViews();

    if (sources != null && !sources.isEmpty()) {
      tvSourcesHeader.setVisibility(View.VISIBLE);
      layoutSourcesContainer.setVisibility(View.VISIBLE);

      LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
      for (NewsArticle article : sources) {
        View sourceView = inflater.inflate(R.layout.item_chat_source, layoutSourcesContainer, false);
        TextView tvSourceName = sourceView.findViewById(R.id.tv_source_name);
        TextView tvSourceTitle = sourceView.findViewById(R.id.tv_source_title);

        tvSourceName.setText(article.getSourceName());
        tvSourceTitle.setText(article.getTitle());

        sourceView.setOnClickListener(v -> {
          if (sourceClickListener != null) {
            sourceClickListener.onNewsItemClick(article);
          }
        });

        layoutSourcesContainer.addView(sourceView);
      }
    } else {
      tvSourcesHeader.setVisibility(View.GONE);
      layoutSourcesContainer.setVisibility(View.GONE);
    }
  }
}
