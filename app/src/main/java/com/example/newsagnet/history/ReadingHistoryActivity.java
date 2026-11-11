package com.example.newsagnet.history;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.newsagnet.R;
import com.example.newsagnet.databinding.ActivityReadingHistoryBinding;
import com.example.newsagnet.databinding.DialogHistoryNoteBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/** An additive screen: search, annotate and manage links opened from the existing app. */
public class ReadingHistoryActivity extends AppCompatActivity implements HistoryAdapter.Actions {
  private ActivityReadingHistoryBinding binding;
  private HistoryRepository repository;
  private HistoryAdapter adapter;
  private LinearLayoutManager layoutManager;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable searchTask = this::load;
  private List<String> sourceChoices = new ArrayList<>();
  private String source;
  private int dateMode; // 0: all time; 1: last seven local calendar days; 2: custom inclusive dates
  private long rangeStart;
  private long rangeEnd;
  private int loadGeneration;
  private Parcelable restoredScroll;
  private String editingUrl;
  private String editingTitle;
  private DialogHistoryNoteBinding noteBinding;
  private AlertDialog noteDialog;

  @Override protected void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    binding = ActivityReadingHistoryBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    WindowCompat.getInsetsController(getWindow(), binding.getRoot()).setAppearanceLightNavigationBars(false);
    ViewCompat.setOnApplyWindowInsetsListener(binding.historyRoot, (view, insets) -> {
      Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
          | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return insets;
    });
    ViewCompat.requestApplyInsets(binding.historyRoot);
    repository = HistoryRepository.getInstance(this);
    adapter = new HistoryAdapter(this);
    layoutManager = new LinearLayoutManager(this);
    binding.historyList.setLayoutManager(layoutManager);
    binding.historyList.setAdapter(adapter);

    if (state != null) {
      source = state.getString("history.source");
      dateMode = state.getInt("history.dateMode");
      rangeStart = state.getLong("history.rangeStart");
      rangeEnd = state.getLong("history.rangeEnd");
      binding.historySearch.setText(state.getString("history.query", ""));
      restoredScroll = state.getParcelable("history.scroll");
    }
    binding.historyBack.setOnClickListener(v -> finish());
    binding.historyClear.setEnabled(false);
    binding.historyClear.setOnClickListener(v -> confirmClear());
    binding.historyRetry.setOnClickListener(v -> load());
    binding.historyReset.setOnClickListener(v -> resetFilters());
    binding.historyRange.setOnClickListener(v -> chooseDates());
    updateSources(new ArrayList<>());
    binding.historySource.setOnItemSelectedListener(selection(position -> {
      String chosen = position == 0 ? null : sourceChoices.get(position);
      if (!Objects.equals(source, chosen)) { source = chosen; load(); }
    }));
    binding.historyDates.setAdapter(spinnerAdapter(Arrays.asList(getString(R.string.history_all_time),
        getString(R.string.history_last_week), getString(R.string.history_custom_dates))));
    binding.historyDates.setSelection(dateMode);
    binding.historyDates.setOnItemSelectedListener(selection(position -> {
      if (position == dateMode) return;
      if (position == 2) chooseDates();
      else { dateMode = position; load(); }
    }));
    binding.historySearch.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        handler.removeCallbacks(searchTask);
        handler.postDelayed(searchTask, 250);
      }
      @Override public void afterTextChanged(Editable s) {}
    });
    binding.historySearch.setOnEditorActionListener((view, action, event) -> {
      if (action != EditorInfo.IME_ACTION_SEARCH) return false;
      handler.removeCallbacks(searchTask);
      load();
      return true;
    });
    updateRangeLabel();
    if (state != null && state.getString("history.editUrl") != null) {
      showNoteEditor(state.getString("history.editUrl"), state.getString("history.editTitle", ""),
          state.getString("history.editDraft", ""));
    }
  }

  @Override protected void onResume() { super.onResume(); load(); }

  private boolean active() { return binding != null && !isFinishing() && !isDestroyed(); }

  private HistoryFilter filter() {
    String query = binding.historySearch.getText() == null ? "" : binding.historySearch.getText().toString();
    if (dateMode == 1) {
      return HistoryFilter.lastSevenDays(query, source, System.currentTimeMillis(), TimeZone.getDefault());
    }
    if (dateMode == 2) {
      return new HistoryFilter(query, source, rangeStart,
          HistoryFilter.followingDay(rangeEnd, TimeZone.getDefault()));
    }
    return new HistoryFilter(query, source, null, null);
  }

  private void load() {
    if (!active()) return;
    int generation = ++loadGeneration;
    binding.historyLoading.setVisibility(View.VISIBLE);
    HistoryFilter filter = filter();
    binding.historyReset.setVisibility(!filter.query.isEmpty() || source != null || dateMode != 0
        ? View.VISIBLE : View.GONE);
    updateRangeLabel();
    repository.load(filter, new HistoryRepository.Callback<HistoryRepository.Page>() {
      @Override public void onSuccess(HistoryRepository.Page page) {
        if (!active() || generation != loadGeneration) return;
        binding.historyLoading.setVisibility(View.GONE);
        binding.historyClear.setEnabled(page.total > 0);
        binding.historyClear.setAlpha(page.total > 0 ? 1f : 0.35f);
        binding.historyCount.setText(getResources().getQuantityString(
            R.plurals.history_results, page.entries.size(), page.entries.size()));
        updateSources(page.sources);
        adapter.submitList(page.entries, () -> {
          if (active() && restoredScroll != null && !page.entries.isEmpty()) {
            layoutManager.onRestoreInstanceState(restoredScroll);
            restoredScroll = null;
          }
        });
        binding.historyList.setVisibility(page.entries.isEmpty() ? View.GONE : View.VISIBLE);
        binding.historyState.setVisibility(page.entries.isEmpty() ? View.VISIBLE : View.GONE);
        binding.historyRetry.setVisibility(View.GONE);
        binding.historyStateTitle.setText(page.total == 0
            ? R.string.history_empty_title : R.string.history_no_matches);
        binding.historyStateBody.setText(page.total == 0
            ? R.string.history_empty_body : R.string.history_no_matches_body);
      }
      @Override public void onError() {
        if (!active() || generation != loadGeneration) return;
        binding.historyLoading.setVisibility(View.GONE);
        binding.historyList.setVisibility(View.GONE);
        binding.historyState.setVisibility(View.VISIBLE);
        binding.historyRetry.setVisibility(View.VISIBLE);
        binding.historyClear.setEnabled(false);
        binding.historyCount.setText("");
        binding.historyStateTitle.setText(R.string.history_error_title);
        binding.historyStateBody.setText(R.string.history_error_body);
      }
    });
  }

  private ArrayAdapter<String> spinnerAdapter(List<String> choices) {
    ArrayAdapter<String> result = new ArrayAdapter<>(this, R.layout.history_spinner_item, choices);
    result.setDropDownViewResource(R.layout.history_spinner_item);
    return result;
  }

  private void updateSources(List<String> available) {
    List<String> next = new ArrayList<>();
    next.add(getString(R.string.history_all_sources));
    next.addAll(available);
    if (source != null && !next.contains(source)) next.add(source);
    if (sourceChoices.equals(next)) return;
    sourceChoices = next;
    binding.historySource.setAdapter(spinnerAdapter(next));
    binding.historySource.setSelection(source == null ? 0 : next.indexOf(source));
  }

  private interface Selection { void select(int position); }
  private AdapterView.OnItemSelectedListener selection(Selection selected) {
    return new AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selected.select(position);
      }
      @Override public void onNothingSelected(AdapterView<?> parent) {}
    };
  }

  private void resetFilters() {
    handler.removeCallbacks(searchTask);
    source = null;
    dateMode = 0;
    binding.historySearch.setText("");
    binding.historySource.setSelection(0);
    binding.historyDates.setSelection(0);
    restoredScroll = null;
    load();
  }

  private void updateRangeLabel() {
    binding.historyRange.setVisibility(dateMode == 2 ? View.VISIBLE : View.GONE);
    if (dateMode == 2) {
      DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
      binding.historyRange.setText(getString(R.string.history_date_change,
          format.format(new Date(rangeStart)), format.format(new Date(rangeEnd))));
    }
  }

  private void chooseDates() {
    Calendar initial = Calendar.getInstance();
    if (dateMode == 2) initial.setTimeInMillis(rangeStart);
    DatePickerDialog startDialog = new DatePickerDialog(this, (picker, year, month, day) -> {
      Calendar start = Calendar.getInstance();
      start.set(year, month, day);
      long chosenStart = HistoryFilter.dayStart(start.getTimeInMillis(), TimeZone.getDefault());
      Calendar initialEnd = Calendar.getInstance();
      initialEnd.setTimeInMillis(Math.max(chosenStart, dateMode == 2 ? rangeEnd : System.currentTimeMillis()));
      DatePickerDialog endDialog = new DatePickerDialog(this, (endPicker, endYear, endMonth, endDay) -> {
        Calendar end = Calendar.getInstance();
        end.set(endYear, endMonth, endDay);
        rangeStart = chosenStart;
        rangeEnd = Math.max(rangeStart, HistoryFilter.dayStart(end.getTimeInMillis(), TimeZone.getDefault()));
        dateMode = 2;
        binding.historyDates.setSelection(2);
        load();
      }, initialEnd.get(Calendar.YEAR), initialEnd.get(Calendar.MONTH), initialEnd.get(Calendar.DAY_OF_MONTH));
      endDialog.setTitle(R.string.history_date_end);
      endDialog.getDatePicker().setMinDate(chosenStart);
      endDialog.setOnCancelListener(dialog -> binding.historyDates.setSelection(dateMode));
      endDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(R.string.history_cancel),
          (dialog, which) -> binding.historyDates.setSelection(dateMode));
      endDialog.show();
    }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
    startDialog.setTitle(R.string.history_date_start);
    startDialog.setOnCancelListener(dialog -> binding.historyDates.setSelection(dateMode));
    startDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(R.string.history_cancel),
        (dialog, which) -> binding.historyDates.setSelection(dateMode));
    startDialog.show();
  }

  @Override public void open(HistoryEntry entry) {
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getUrl())));
      HistoryRepository.recordOpened(this, entry.toArticle());
    } catch (RuntimeException e) {
      Toast.makeText(this, R.string.history_open_failed, Toast.LENGTH_SHORT).show();
    }
  }

  @Override public void edit(HistoryEntry entry) {
    showNoteEditor(entry.getUrl(), entry.getTitle(), entry.getNote());
  }

  private void showNoteEditor(String url, String title, String note) {
    editingUrl = url;
    editingTitle = title;
    noteBinding = DialogHistoryNoteBinding.inflate(getLayoutInflater());
    noteBinding.historyNoteHeadline.setText(title);
    noteBinding.historyNoteInput.setText(note);
    noteDialog = new MaterialAlertDialogBuilder(this).setTitle(R.string.history_note)
        .setView(noteBinding.getRoot()).setNegativeButton(R.string.history_cancel, null)
        .setPositiveButton(R.string.history_save, null).create();
    AlertDialog dialog = noteDialog;
    DialogHistoryNoteBinding editor = noteBinding;
    dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      String draft = editor.historyNoteInput.getText() == null ? "" : editor.historyNoteInput.getText().toString().trim();
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
      repository.saveNote(url, draft, new HistoryRepository.Callback<Boolean>() {
        @Override public void onSuccess(Boolean found) {
          if (!active()) return;
          if (!found) Toast.makeText(ReadingHistoryActivity.this,
              R.string.history_item_gone, Toast.LENGTH_SHORT).show();
          dialog.dismiss();
          load();
        }
        @Override public void onError() {
          if (!active()) return;
          dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
          Toast.makeText(ReadingHistoryActivity.this, R.string.history_action_failed, Toast.LENGTH_SHORT).show();
        }
      });
    }));
    dialog.setOnDismissListener(ignored -> {
      editingUrl = null;
      editingTitle = null;
      noteBinding = null;
      noteDialog = null;
    });
    dialog.show();
  }

  @Override public void delete(HistoryEntry entry) {
    new MaterialAlertDialogBuilder(this).setTitle(R.string.history_delete_title)
        .setMessage(R.string.history_delete_body).setNegativeButton(R.string.history_cancel, null)
        .setPositiveButton(R.string.history_delete,
            (dialog, which) -> repository.delete(entry.getUrl(), mutationCallback())).show();
  }

  private void confirmClear() {
    new MaterialAlertDialogBuilder(this).setTitle(R.string.history_clear_title)
        .setMessage(R.string.history_clear_body).setNegativeButton(R.string.history_cancel, null)
        .setPositiveButton(R.string.history_clear,
            (dialog, which) -> repository.clear(mutationCallback())).show();
  }

  private HistoryRepository.Callback<Boolean> mutationCallback() {
    return new HistoryRepository.Callback<Boolean>() {
      @Override public void onSuccess(Boolean ignored) { if (active()) load(); }
      @Override public void onError() {
        if (active()) Toast.makeText(ReadingHistoryActivity.this,
            R.string.history_action_failed, Toast.LENGTH_SHORT).show();
      }
    };
  }

  @Override protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("history.query", binding.historySearch.getText() == null
        ? "" : binding.historySearch.getText().toString());
    state.putString("history.source", source);
    state.putInt("history.dateMode", dateMode);
    state.putLong("history.rangeStart", rangeStart);
    state.putLong("history.rangeEnd", rangeEnd);
    state.putParcelable("history.scroll", layoutManager.onSaveInstanceState());
    if (editingUrl != null && noteBinding != null) {
      state.putString("history.editUrl", editingUrl);
      state.putString("history.editTitle", editingTitle);
      state.putString("history.editDraft", noteBinding.historyNoteInput.getText() == null
          ? "" : noteBinding.historyNoteInput.getText().toString());
    }
  }

  @Override protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (noteDialog != null) noteDialog.dismiss();
    binding = null;
    super.onDestroy();
  }
}
