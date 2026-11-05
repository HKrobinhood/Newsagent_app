package com.example.newsagnet.ui.fragment;

import com.example.newsagnet.viewmodel.NewsUiState;

/**
 * Functional callback interface for receiving state updates from NewsViewModel in Java.
 *
 * <p>Conforms to COMP7506 Ch5 (MVC/MVVM Model-View synchronization).
 */
@FunctionalInterface
public interface NewsUiStateObserver {

  /**
   * Invoked when the NewsUiState changes.
   *
   * @param state The updated UI state snapshot.
   */
  void onStateChanged(NewsUiState state);
}
