package com.example.newsagnet

import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.model.NewsCategory
import com.example.newsagnet.repository.NewsPageResult
import com.example.newsagnet.repository.NewsRepository
import com.example.newsagnet.viewmodel.NewsViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Fake implementation of NewsRepository for unit testing NewsViewModel.
 */
class FakeNewsRepository : NewsRepository {
    var shouldFail: Boolean = false
    var failureException: Exception = IOException("Connection failed")
    var lastRequestedCategory: String? = null
    var lastRequestedPage: Int = 1

    private val sampleArticles = listOf(
        NewsArticle(
            "news-1",
            "Geneva Accord on Clean Energy Signed",
            "bbc",
            "BBC News",
            "2026-11-03T16:00:00Z",
            "https://bbc.com/news/1",
            "world",
            "Geneva, Switzerland",
            46.2044,
            6.1432
        ),
        NewsArticle(
            "news-2",
            "Quantum Hardware Scalability Breakthrough",
            "cnn",
            "CNN Tech",
            "2026-11-03T15:00:00Z",
            "https://cnn.com/tech/1",
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
        lastRequestedPage = page
        lastRequestedCategory = category

        if (shouldFail) {
            return Result.failure(failureException)
        }

        val filtered = if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) {
            sampleArticles
        } else {
            sampleArticles.filter { it.category.equals(category, ignoreCase = true) }
        }

        return Result.success(
            NewsPageResult(
                filtered,
                filtered.size,
                page,
                page < 2
            )
        )
    }

    override suspend fun getNewsDetail(id: String): Result<NewsArticle> {
        if (shouldFail) return Result.failure(failureException)
        val item = sampleArticles.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("Not found"))
        return Result.success(item)
    }

    override suspend fun getGeoNews(category: String?): Result<List<NewsArticle>> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(sampleArticles)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeNewsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNewsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_success_populatesArticles() = runTest {
        val viewModel = NewsViewModel(repository = fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals(2, state.articles.size)
        assertEquals(2, state.total)
        assertEquals(1, state.page)
    }

    @Test
    fun initialLoad_networkError_setsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureException = IOException("Failed to connect to backend")

        val viewModel = NewsViewModel(repository = fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.articles.isEmpty())
        assertTrue(state.errorMessage?.contains("Failed to connect") == true)
    }

    @Test
    fun refresh_reloadsFirstPageSuccessfully() = runTest {
        val viewModel = NewsViewModel(repository = fakeRepository)
        advanceUntilIdle()

        viewModel.loadNews(isRefresh = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(1, state.page)
        assertEquals(2, state.articles.size)
    }

    @Test
    fun selectCategory_filtersAndQueriesRepository() = runTest {
        val viewModel = NewsViewModel(repository = fakeRepository)
        advanceUntilIdle()

        viewModel.selectCategory(NewsCategory.TECHNOLOGY)
        advanceUntilIdle()

        assertEquals("technology", fakeRepository.lastRequestedCategory)
        val state = viewModel.uiState.value
        assertEquals(NewsCategory.TECHNOLOGY, state.selectedCategory)
        assertEquals(1, state.articles.size)
        assertEquals("technology", state.articles.first().category)
    }

    @Test
    fun loadNextPage_appendsMoreArticles() = runTest {
        val viewModel = NewsViewModel(repository = fakeRepository)
        advanceUntilIdle()

        // Page 1 loaded, hasMore should be true based on FakeNewsRepository
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        assertEquals(2, state.page)
        assertEquals(4, state.articles.size) // 2 items from page 1 + 2 items appended from page 2
    }

    @Test
    fun retry_afterFailure_recoversData() = runTest {
        fakeRepository.shouldFail = true
        val viewModel = NewsViewModel(repository = fakeRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.articles.isEmpty())

        // Network recovers
        fakeRepository.shouldFail = false
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(2, state.articles.size)
    }
}
