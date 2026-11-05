package com.example.newsagnet.ui.fragment

import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newsagnet.ui.chat.AiChatScreen
import com.example.newsagnet.ui.globe.GlobeScreen
import com.example.newsagnet.ui.news.NewsScreen
import com.example.newsagnet.ui.theme.GlobalNewsTheme
import com.example.newsagnet.viewmodel.ChatViewModel
import com.example.newsagnet.viewmodel.NewsViewModel
import kotlinx.coroutines.launch

/**
 * Interoperability bridge hosting existing Compose screens inside Java Fragments during refactoring.
 */
object ComposeScreenBridge {

    @JvmStatic
    fun bindNewsScreen(composeView: ComposeView) {
        composeView.setContent {
            GlobalNewsTheme {
                val newsViewModel: NewsViewModel = viewModel()
                NewsScreen(viewModel = newsViewModel)
            }
        }
    }

    @JvmStatic
    fun bindGlobeScreen(composeView: ComposeView) {
        composeView.setContent {
            GlobalNewsTheme {
                GlobeScreen()
            }
        }
    }

    @JvmStatic
    fun bindChatScreen(composeView: ComposeView) {
        composeView.setContent {
            GlobalNewsTheme {
                val chatViewModel: ChatViewModel = viewModel()
                AiChatScreen(viewModel = chatViewModel)
            }
        }
    }

    @JvmStatic
    fun observeNewsUiState(
        lifecycleOwner: LifecycleOwner,
        viewModel: NewsViewModel,
        observer: NewsUiStateObserver
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    observer.onStateChanged(state)
                }
            }
        }
    }
}
