package com.example.newsagnet.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.R;
import com.example.newsagnet.databinding.ItemReadingHistoryBinding;
import java.text.DateFormat;
import java.util.Date;

final class HistoryAdapter extends ListAdapter<HistoryEntry, HistoryAdapter.Holder> {
  interface Actions {
    void open(HistoryEntry entry);
    void edit(HistoryEntry entry);
    void delete(HistoryEntry entry);
  }

  private final Actions actions;

  HistoryAdapter(Actions actions) {
    super(new DiffUtil.ItemCallback<HistoryEntry>() {
      @Override public boolean areItemsTheSame(@NonNull HistoryEntry a, @NonNull HistoryEntry b) {
        return a.getUrl().equals(b.getUrl());
      }
      @Override public boolean areContentsTheSame(@NonNull HistoryEntry a, @NonNull HistoryEntry b) {
        return a.equals(b);
      }
    });
    this.actions = actions;
    setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
  }

  @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(ItemReadingHistoryBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
    HistoryEntry entry = getItem(position);
    ItemReadingHistoryBinding b = holder.binding;
    b.historyItemSource.setText(entry.getSourceName());
    b.historyItemTitle.setText(entry.getTitle());
    b.historyItemOpened.setText(b.getRoot().getContext().getString(R.string.history_opened,
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(new Date(entry.getOpenedAt()))));
    String published = entry.getPublishedAt();
    if (published.matches("\\d{4}-\\d{2}-\\d{2}.*")) published = published.substring(0, 10);
    b.historyItemPublished.setText(published.isEmpty()
        ? b.getRoot().getContext().getString(R.string.history_published_unknown)
        : b.getRoot().getContext().getString(R.string.history_published, published));
    b.historyItemNote.setVisibility(entry.getNote().isEmpty() ? View.GONE : View.VISIBLE);
    b.historyItemNote.setText(b.getRoot().getContext().getString(
        R.string.history_note_preview, entry.getNote()));
    b.historyItemOpen.setOnClickListener(v -> actions.open(entry));
    b.historyItemEdit.setOnClickListener(v -> actions.edit(entry));
    b.historyItemDelete.setOnClickListener(v -> actions.delete(entry));
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final ItemReadingHistoryBinding binding;
    Holder(ItemReadingHistoryBinding binding) { super(binding.getRoot()); this.binding = binding; }
  }
}
