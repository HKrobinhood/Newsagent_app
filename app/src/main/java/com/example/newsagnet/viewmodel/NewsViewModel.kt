package com.example.newsagnet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.model.NewsCategory
import com.example.newsagnet.repository.NewsRepository
import com.example.newsagnet.repository.NewsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the News list screen.
 */
data class NewsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val articles: List<NewsArticle> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing real backend news feed, pagination, and category filtering.
 */
class NewsViewModel(
    private val repository: NewsRepository = NewsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState(isLoading = true))
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    companion object {
        const val PAGE_SIZE = 20
    }

    init {
        loadNews()
    }

    fun loadNews(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else if (_uiState.value.articles.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val categoryParam = if (_uiState.value.selectedCategory == NewsCategory.ALL) {
                null
            } else {
                _uiState.value.selectedCategory.key
            }

            val result = repository.getNews(
                page = 1,
                limit = PAGE_SIZE,
                category = categoryParam
            )

            result.fold(
                onSuccess = { pageResult ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            articles = pageResult.articles,
                            total = pageResult.total,
                            page = pageResult.page,
                            hasMore = pageResult.hasMore,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = throwable.localizedMessage ?: "Unable to connect to GlobalNewsAI service."
                        )
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.isLoading || currentState.isRefreshing) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val nextPage = currentState.page + 1
            val categoryParam = if (currentState.selectedCategory == NewsCategory.ALL) {
                null
            } else {
                currentState.selectedCategory.key
            }

            val result = repository.getNews(
                page = nextPage,
                limit = PAGE_SIZE,
                category = categoryParam
            )

            result.fold(
                onSuccess = { pageResult ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            articles = it.articles + pageResult.articles,
                            total = pageResult.total,
                            page = pageResult.page,
                            hasMore = pageResult.hasMore
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun selectCategory(category: NewsCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update {
            it.copy(
                selectedCategory = category,
                articles = emptyList(),
                page = 1,
                hasMore = false,
                errorMessage = null
            )
        }
        loadNews()
    }

    fun retry() {
        loadNews(isRefresh = false)
    }
}
