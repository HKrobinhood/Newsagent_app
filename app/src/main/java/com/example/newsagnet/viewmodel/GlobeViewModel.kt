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
 * UI State for the 3D Globe Spatial News Screen.
 */
data class GlobeUiState(
    val isLoading: Boolean = false,
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val articles: List<NewsArticle> = emptyList(),
    val selectedArticle: NewsArticle? = null,
    val selectedClusterArticles: List<NewsArticle> = emptyList(),
    val rotationX: Float = 15f,
    val rotationY: Float = 0f,
    val zoom: Float = 1.0f,
    val errorMessage: String? = null
)

/**
 * ViewModel managing 3D globe camera orientation, spatial news hotspots, and category filters.
 */
class GlobeViewModel(
    private val repository: NewsRepository = NewsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobeUiState(isLoading = true))
    val uiState: StateFlow<GlobeUiState> = _uiState.asStateFlow()

    init {
        loadGeoNews()
    }

    fun loadGeoNews() {
        val categoryParam = if (_uiState.value.selectedCategory == NewsCategory.ALL) {
            null
        } else {
            _uiState.value.selectedCategory.name.lowercase()
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getGeoNews(categoryParam)
            result.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            articles = items,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load spatial news"
                        )
                    }
                }
            )
        }
    }

    fun onCategorySelected(category: NewsCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedArticle = null,
                selectedClusterArticles = emptyList()
            )
        }
        loadGeoNews()
    }

    fun onGlobeRotate(deltaPitch: Float, deltaYaw: Float) {
        _uiState.update {
            val newPitch = (it.rotationX + deltaPitch).coerceIn(-75f, 75f)
            val newYaw = (it.rotationY + deltaYaw) % 360f
            it.copy(rotationX = newPitch, rotationY = newYaw)
        }
    }

    fun onGlobeZoom(deltaZoom: Float) {
        _uiState.update {
            val newZoom = (it.zoom + deltaZoom).coerceIn(0.6f, 2.5f)
            it.copy(zoom = newZoom)
        }
    }

    fun selectArticle(article: NewsArticle?) {
        _uiState.update {
            it.copy(
                selectedArticle = article,
                selectedClusterArticles = if (article != null) listOf(article) else emptyList()
            )
        }
    }

    fun selectCluster(articles: List<NewsArticle>) {
        _uiState.update {
            it.copy(
                selectedClusterArticles = articles,
                selectedArticle = articles.firstOrNull()
            )
        }
    }

    fun clearSelectedArticle() {
        _uiState.update {
            it.copy(
                selectedArticle = null,
                selectedClusterArticles = emptyList()
            )
        }
    }
}
