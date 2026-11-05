package com.example.newsagnet.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.R;
import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.MessageRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView Adapter managing multi-type chat messages (User Bubbles vs AI Assistant Bubbles).
 *
 * <p>Conforms to COMP7506 Ch5 (RecyclerView Multi-Type Adapter Pattern).
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  public static final int VIEW_TYPE_USER = 1;
  public static final int VIEW_TYPE_AI = 2;

  private final List<ChatMessage> messages = new ArrayList<>();
  private NewsItemClickListener sourceClickListener;

  public ChatAdapter() {}

  public ChatAdapter(@Nullable NewsItemClickListener sourceClickListener) {
    this.sourceClickListener = sourceClickListener;
  }

  public void setOnSourceClickListener(@Nullable NewsItemClickListener listener) {
    this.sourceClickListener = listener;
  }

  public void submitList(@NonNull List<ChatMessage> newMessages) {
    DiffUtil.DiffResult diffResult =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return messages.size();
              }

              @Override
              public int getNewListSize() {
                return newMessages.size();
              }

              @Override
              public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                ChatMessage oldItem = messages.get(oldItemPosition);
                ChatMessage newItem = newMessages.get(newItemPosition);
                return Objects.equals(oldItem.getId(), newItem.getId());
              }

              @Override
              public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ChatMessage oldItem = messages.get(oldItemPosition);
                ChatMessage newItem = newMessages.get(newItemPosition);
                return Objects.equals(oldItem.getContent(), newItem.getContent())
                    && oldItem.getRole() == newItem.getRole()
                    && oldItem.isThinking() == newItem.isThinking()
                    && Objects.equals(oldItem.getSources(), newItem.getSources());
              }
            });

    messages.clear();
    messages.addAll(newMessages);
    try {
      diffResult.dispatchUpdatesTo(this);
    } catch (Exception ignored) {
      // Allows unit testing without full Android framework runtime
    }
  }

  @Override
  public int getItemViewType(int position) {
    ChatMessage message = messages.get(position);
    return message.getRole() == MessageRole.USER ? VIEW_TYPE_USER : VIEW_TYPE_AI;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == VIEW_TYPE_USER) {
      View view = inflater.inflate(R.layout.item_chat_user, parent, false);
      return new UserMessageViewHolder(view);
    } else {
      View view = inflater.inflate(R.layout.item_chat_ai, parent, false);
      return new AiMessageViewHolder(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ChatMessage message = messages.get(position);
    if (holder instanceof UserMessageViewHolder) {
      ((UserMessageViewHolder) holder).bind(message);
    } else if (holder instanceof AiMessageViewHolder) {
      ((AiMessageViewHolder) holder).bind(message, sourceClickListener);
    }
  }

  @Override
  public int getItemCount() {
    return messages.size();
  }

  @Nullable
  public ChatMessage getItem(int position) {
    if (position >= 0 && position < messages.size()) {
      return messages.get(position);
    }
    return null;
  }
}
