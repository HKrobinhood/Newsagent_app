package com.example.newsagnet

import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.model.NewsCategory
import com.example.newsagnet.repository.NewsPageResult
import com.example.newsagnet.repository.NewsRepository
import com.example.newsagnet.viewmodel.GlobeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GlobeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeNewsRepository(
        var shouldFail: Boolean = false,
        var lastCategoryRequested: String? = null
    ) : NewsRepository {

        val mockGeoArticles = listOf(
            NewsArticle(
                "geo-1",
                "Geneva Climate Accord",
                "bbc",
                "BBC News",
                "2026-11-03T12:00:00Z",
                "https://bbc.com/climate",
                "science",
                "Geneva, Switzerland",
                46.2044,
                6.1432
            ),
            NewsArticle(
                "geo-2",
                "Tokyo Quantum Lab",
                "cnn",
                "CNN Tech",
                "2026-11-03T11:00:00Z",
                "https://cnn.com/quantum",
                "technology",
                "Tokyo, Japan",
                35.6762,
                139.6503
            )
        )

        override suspend fun getNews(
            page: Int,
            limit: Int,
            category: String?,
            source: String?,
            keyword: String?
        ): Result<NewsPageResult> {
            return Result.success(NewsPageResult(emptyList(), 0, 1, false))
        }

        override suspend fun getNewsDetail(id: String): Result<NewsArticle> {
            return Result.success(mockGeoArticles.first())
        }

        override suspend fun getGeoNews(category: String?): Result<List<NewsArticle>> {
            lastCategoryRequested = category
            if (shouldFail) {
                return Result.failure(IOException("Server connection error"))
            }
            val filtered = if (category.isNullOrBlank()) {
                mockGeoArticles
            } else {
                mockGeoArticles.filter { it.category.equals(category, ignoreCase = true) }
            }
            return Result.success(filtered)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsGeoNewsSuccessfully() = runTest {
        val fakeRepo = FakeNewsRepository()
        val viewModel = GlobeViewModel(repository = fakeRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.articles.size)
        assertEquals("Geneva, Switzerland", state.articles[0].locationName)
    }

    @Test
    fun onCategorySelected_filtersArticlesAndReloads() = runTest {
        val fakeRepo = FakeNewsRepository()
        val viewModel = GlobeViewModel(repository = fakeRepo)
        advanceUntilIdle()

        viewModel.onCategorySelected(NewsCategory.TECHNOLOGY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("technology", fakeRepo.lastCategoryRequested)
        assertEquals(1, state.articles.size)
        assertEquals("Tokyo Quantum Lab", state.articles.first().title)
    }

    @Test
    fun onGlobeRotate_updatesPitchAndYawWithinLimits() = runTest {
        val fakeRepo = FakeNewsRepository()
        val viewModel = GlobeViewModel(repository = fakeRepo)

        // Pitch rotation
        viewModel.onGlobeRotate(deltaPitch = 20f, deltaYaw = 45f)
        var state = viewModel.uiState.value
        assertEquals(35f, state.rotationX, 0.01f)
        assertEquals(45f, state.rotationY, 0.01f)

        // Clamp extreme pitch (+100 should clamp to +75)
        viewModel.onGlobeRotate(deltaPitch = 100f, deltaYaw = 0f)
        state = viewModel.uiState.value
        assertEquals(75f, state.rotationX, 0.01f)
    }

    @Test
    fun onGlobeZoom_clampsBetweenLimits() = runTest {
        val fakeRepo = FakeNewsRepository()
        val viewModel = GlobeViewModel(repository = fakeRepo)

        viewModel.onGlobeZoom(0.5f)
        assertEquals(1.5f, viewModel.uiState.value.zoom, 0.01f)

        viewModel.onGlobeZoom(3.0f) // Extreme zoom in
        assertEquals(2.5f, viewModel.uiState.value.zoom, 0.01f) // Clamped to 2.5

        viewModel.onGlobeZoom(-5.0f) // Extreme zoom out
        assertEquals(0.6f, viewModel.uiState.value.zoom, 0.01f) // Clamped to 0.6
    }

    @Test
    fun selectAndClearArticle_togglesSelectionState() = runTest {
        val fakeRepo = FakeNewsRepository()
        val viewModel = GlobeViewModel(repository = fakeRepo)
        advanceUntilIdle()

        val targetArticle = fakeRepo.mockGeoArticles.first()
        viewModel.selectArticle(targetArticle)

        assertEquals("geo-1", viewModel.uiState.value.selectedArticle?.id)

        viewModel.clearSelectedArticle()
        assertNull(viewModel.uiState.value.selectedArticle)
    }

    @Test
    fun networkFailure_updatesErrorMessage() = runTest {
        val fakeRepo = FakeNewsRepository(shouldFail = true)
        val viewModel = GlobeViewModel(repository = fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.articles.isEmpty())
    }
}
