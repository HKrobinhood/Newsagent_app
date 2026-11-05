package com.example.newsagnet.ui.adapter;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.R;
import com.example.newsagnet.model.ChatMessage;

/**
 * ViewHolder for user inquiry message bubbles.
 *
 * <p>Conforms to COMP7506 Ch5 (RecyclerView Multi-Type ViewHolder Pattern).
 */
public class UserMessageViewHolder extends RecyclerView.ViewHolder {

  private final TextView tvUserContent;
  private final TextView tvUserTimestamp;

  public UserMessageViewHolder(@NonNull View itemView) {
    super(itemView);
    tvUserContent = itemView.findViewById(R.id.tv_user_content);
    tvUserTimestamp = itemView.findViewById(R.id.tv_user_timestamp);
  }

  public void bind(@NonNull ChatMessage message) {
    tvUserContent.setText(message.getContent());

    String ts = message.getTimestamp();
    if (ts != null && !ts.trim().isEmpty()) {
      tvUserTimestamp.setText(ts);
      tvUserTimestamp.setVisibility(View.VISIBLE);
    } else {
      tvUserTimestamp.setVisibility(View.GONE);
    }
  }
}
