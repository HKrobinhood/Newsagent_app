package com.example.newsagnet.repository;

import androidx.annotation.NonNull;

/**
 * Asynchronous callback contract for repository data queries.
 *
 * <p>Conforms to COMP7506 Ch3 (Java Interfaces & Callback Pattern).
 *
 * @param <T> Response model type.
 */
public interface NewsCallback<T> {

  /**
   * Invoked upon successful data retrieval.
   *
   * @param result Retrieved data instance.
   */
  void onSuccess(@NonNull T result);

  /**
   * Invoked when an exception or network error occurs.
   *
   * @param throwable Root cause exception.
   */
  void onError(@NonNull Throwable throwable);
}
