package com.example.newsagnet.concurrency;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global executor pools for asynchronous background processing and UI thread synchronization.
 *
 * <p>Conforms to COMP7506 Ch7 (Thread, Runnable, ExecutorService, Handler, UI Thread).
 */
public class AppExecutors {

  private static final Object LOCK = new Object();
  private static AppExecutors sInstance;

  private final ExecutorService diskIO;
  private final ExecutorService networkIO;
  private final Executor mainThread;

  public AppExecutors(
      @NonNull ExecutorService diskIO,
      @NonNull ExecutorService networkIO,
      @NonNull Executor mainThread) {
    this.diskIO = diskIO;
    this.networkIO = networkIO;
    this.mainThread = mainThread;
  }

  public AppExecutors() {
    this(
        Executors.newSingleThreadExecutor(),
        Executors.newFixedThreadPool(4),
        new MainThreadExecutor());
  }

  public static AppExecutors getInstance() {
    if (sInstance == null) {
      synchronized (LOCK) {
        if (sInstance == null) {
          sInstance = new AppExecutors();
        }
      }
    }
    return sInstance;
  }

  @VisibleForTesting
  public static void setInstance(@NonNull AppExecutors appExecutors) {
    synchronized (LOCK) {
      sInstance = appExecutors;
    }
  }

  public ExecutorService diskIO() {
    return diskIO;
  }

  public ExecutorService networkIO() {
    return networkIO;
  }

  public Executor mainThread() {
    return mainThread;
  }

  public void executeOnBackground(@NonNull Runnable runnable) {
    networkIO.execute(runnable);
  }

  public void postToMainThread(@NonNull Runnable runnable) {
    mainThread.execute(runnable);
  }

  private static class MainThreadExecutor implements Executor {
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable command) {
      mainThreadHandler.post(command);
    }
  }
}
