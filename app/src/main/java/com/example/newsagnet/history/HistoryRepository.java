package com.example.newsagnet.history;

import android.content.Context;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.model.NewsArticle;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/** COMP7506 Ch7: serialize database work off the UI thread and deliver results on it. */
public final class HistoryRepository {
  private static final String TAG = "ReadingHistory";
  private static volatile HistoryRepository instance;
  private final HistoryStore store;
  private final Executor disk;
  private final Executor main;

  public interface Callback<T> {
    void onSuccess(T value);
    void onError();
  }

  public static final class Page {
    public final List<HistoryEntry> entries;
    public final List<String> sources;
    public final int total;

    Page(List<HistoryEntry> entries, List<String> sources, int total) {
      this.entries = entries;
      this.sources = sources;
      this.total = total;
    }
  }

  HistoryRepository(HistoryStore store, Executor disk, Executor main) {
    this.store = store;
    this.disk = disk;
    this.main = main;
  }

  public static HistoryRepository getInstance(Context context) {
    if (instance == null) {
      synchronized (HistoryRepository.class) {
        if (instance == null) {
          AppExecutors executors = AppExecutors.getInstance();
          instance = new HistoryRepository(new HistoryStore(context, HistoryDatabase.NAME),
              executors.diskIO(), executors.mainThread());
        }
      }
    }
    return instance;
  }

  /** Best-effort integration: storage or executor failures must not interrupt opening a link. */
  public static void recordOpened(Context context, NewsArticle article) {
    try {
      HistoryEntry snapshot = HistoryEntry.fromArticle(article, System.currentTimeMillis());
      HistoryRepository repository = getInstance(context);
      repository.disk.execute(() -> {
        try {
          repository.store.record(snapshot);
        } catch (RuntimeException e) {
          Log.w(TAG, "Could not save reading history", e);
        }
      });
    } catch (RuntimeException e) {
      Log.w(TAG, "Could not schedule reading history", e);
    }
  }

  public void load(HistoryFilter filter, Callback<Page> callback) {
    perform(() -> new Page(store.find(filter), store.sources(), store.count()), callback);
  }

  public void saveNote(String url, String note, Callback<Boolean> callback) {
    perform(() -> store.saveNote(url, note), callback);
  }

  public void delete(String url, Callback<Boolean> callback) {
    perform(() -> { store.delete(url); return true; }, callback);
  }

  public void clear(Callback<Boolean> callback) {
    perform(() -> { store.clear(); return true; }, callback);
  }

  private <T> void perform(Callable<T> operation, Callback<T> callback) {
    try {
      disk.execute(() -> {
        try {
          T result = operation.call();
          main.execute(() -> callback.onSuccess(result));
        } catch (Exception e) {
          Log.w(TAG, "Reading history operation failed", e);
          main.execute(callback::onError);
        }
      });
    } catch (RuntimeException e) {
      Log.w(TAG, "Reading history executor unavailable", e);
      main.execute(callback::onError);
    }
  }

  @VisibleForTesting
  static synchronized HistoryRepository replaceForTesting(HistoryRepository replacement) {
    HistoryRepository previous = instance;
    instance = replacement;
    return previous;
  }
}
