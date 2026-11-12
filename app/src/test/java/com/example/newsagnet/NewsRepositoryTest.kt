package com.example.newsagnet

import com.example.newsagnet.network.NewsApiService
import com.example.newsagnet.network.dto.GeoFeatureCollectionDto
import com.example.newsagnet.network.dto.GeoFeatureDto
import com.example.newsagnet.network.dto.GeoGeometryDto
import com.example.newsagnet.network.dto.GeoPropertiesDto
import com.example.newsagnet.network.dto.NewsArticleDto
import com.example.newsagnet.network.dto.NewsListDto
import com.example.newsagnet.network.dto.NewsSourceDto
import com.example.newsagnet.repository.NewsRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Fake implementation of NewsApiService for repository tests.
 */
class FakeNewsApiService : NewsApiService {
    var shouldThrow = false
    var lastCategory: String? = null
    var lastPage: Int = 1

    override suspend fun getNews(
        page: Int,
        limit: Int,
        category: String?,
        source: String?,
        keyword: String?
    ): NewsListDto {
        lastCategory = category
        lastPage = page
        if (shouldThrow) throw IOException("Simulated 503 Service Unavailable")

        return NewsListDto(
            items = listOf(
                NewsArticleDto(
                    id = "art-101",
                    title = "Global Tech Innovation Index Published",
                    sourceId = "bbc",
                    sourceName = "BBC Tech",
                    publishedAt = "2026-11-03T18:00:00Z",
                    originalUrl = "https://bbc.com/news/tech-101",
                    category = "technology",
                    locationName = "London, UK",
                    latitude = 51.5074,
                    longitude = -0.1278
                )
            ),
            total = 1,
            page = page,
            limit = limit,
            hasMore = false
        )
    }

    override fun getNewsCall(
        page: Int,
        limit: Int,
        category: String?,
        source: String?,
        keyword: String?
    ): retrofit2.Call<NewsListDto> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun getGeoNewsCall(
        category: String?,
        limit: Int
    ): retrofit2.Call<GeoFeatureCollectionDto> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun getNewsFilteredCall(
        page: Int,
        limit: Int,
        category: String?,
        source: String?,
        sourceIds: String?,
        keyword: String?
    ): retrofit2.Call<NewsListDto> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun getGeoNewsFilteredCall(
        category: String?,
        sourceIds: String?,
        limit: Int
    ): retrofit2.Call<GeoFeatureCollectionDto> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun getNewsSourcesCall(): retrofit2.Call<List<NewsSourceDto>> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun getNewsDetailCall(id: String): retrofit2.Call<NewsArticleDto> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override suspend fun getNewsDetail(id: String): NewsArticleDto {
        if (shouldThrow) throw IOException("Network error")
        return NewsArticleDto(
            id = id,
            title = "Single Article Detail",
            sourceId = "cnn",
            sourceName = "CNN News",
            publishedAt = "2026-11-03T18:30:00Z",
            originalUrl = "https://cnn.com/news/$id",
            category = "world"
        )
    }

    override suspend fun getGeoNews(category: String?, limit: Int): GeoFeatureCollectionDto {
        if (shouldThrow) throw IOException("Geo service error")
        return GeoFeatureCollectionDto(
            features = listOf(
                GeoFeatureDto(
                    geometry = GeoGeometryDto(coordinates = listOf(139.6503, 35.6762)),
                    properties = GeoPropertiesDto(
                        id = "geo-101",
                        title = "Tokyo High Tech Summit",
                        sourceId = "cnn",
                        sourceName = "CNN Tech",
                        publishedAt = "2026-11-03T12:00:00Z",
                        originalUrl = "https://cnn.com/tokyo",
                        category = "technology",
                        locationName = "Tokyo, Japan"
                    )
                )
            )
        )
    }
}

class NewsRepositoryTest {

    @Test
    fun getNews_success_mapsDtoToDomain() = runTest {
        val fakeService = FakeNewsApiService()
        val repository = NewsRepositoryImpl(apiService = fakeService)

        val result = repository.getNews(page = 1, limit = 10, category = "all")
        assertTrue(result.isSuccess)

        // Verify "all" is mapped to null category for backend API
        assertNull(fakeService.lastCategory)

        val pageResult = result.getOrThrow()
        assertEquals(1, pageResult.articles.size)
        val article = pageResult.articles.first()
        assertEquals("art-101", article.id)
        assertEquals("Global Tech Innovation Index Published", article.title)
        assertEquals("BBC Tech", article.sourceName)
        assertEquals(51.5074, article.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun getNews_networkError_returnsFailureResult() = runTest {
        val fakeService = FakeNewsApiService().apply { shouldThrow = true }
        val repository = NewsRepositoryImpl(apiService = fakeService)

        val result = repository.getNews(page = 1, limit = 10)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun getNewsDetail_success_returnsDomainArticle() = runTest {
        val fakeService = FakeNewsApiService()
        val repository = NewsRepositoryImpl(apiService = fakeService)

        val result = repository.getNewsDetail("test-id-999")
        assertTrue(result.isSuccess)
        assertEquals("test-id-999", result.getOrThrow().id)
        assertEquals("world", result.getOrThrow().category)
    }

    @Test
    fun getGeoNews_success_mapsGeoJsonFeatures() = runTest {
        val fakeService = FakeNewsApiService()
        val repository = NewsRepositoryImpl(apiService = fakeService)

        val result = repository.getGeoNews("technology")
        assertTrue(result.isSuccess)

        val articles = result.getOrThrow()
        assertEquals(1, articles.size)
        val article = articles.first()
        assertEquals("geo-101", article.id)
        assertEquals(35.6762, article.latitude ?: 0.0, 0.0001)
        assertEquals(139.6503, article.longitude ?: 0.0, 0.0001)
    }
}
