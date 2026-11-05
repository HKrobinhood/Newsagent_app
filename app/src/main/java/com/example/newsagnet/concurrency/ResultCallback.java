package com.example.newsagnet.concurrency;

import androidx.annotation.NonNull;

/**
 * Standard callback interface for background tasks delivering results to the UI thread.
 *
 * <p>Conforms to COMP7506 Ch7 (Thread, Runnable, UI Thread Dispatching).
 *
 * @param <T> Result payload type.
 */
public interface ResultCallback<T> {

  /**
   * Invoked on the Main UI Thread when background execution succeeds.
   *
   * @param result Data payload.
   */
  void onSuccess(@NonNull T result);

  /**
   * Invoked on the Main UI Thread when an exception occurs during background execution.
   *
   * @param exception Exception thrown by the background task.
   */
  void onError(@NonNull Exception exception);
}
