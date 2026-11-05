package com.example.newsagnet.repository

import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.network.ApiClient
import com.example.newsagnet.network.NewsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of NewsRepository communicating with the FastAPI backend.
 */
class NewsRepositoryImpl(
    private val apiService: NewsApiService = ApiClient.newsApiService
) : NewsRepository {

    override suspend fun getNews(
        page: Int,
        limit: Int,
        category: String?,
        source: String?,
        keyword: String?
    ): Result<NewsPageResult> = withContext(Dispatchers.IO) {
        try {
            val apiCategory = if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) {
                null
            } else {
                category
            }

            val response = apiService.getNews(
                page = page,
                limit = limit,
                category = apiCategory,
                source = source,
                keyword = keyword
            )

            val articles = response.items.map { it.toDomainModel() }
            Result.success(
                NewsPageResult(
                    articles,
                    response.total,
                    response.page,
                    response.hasMore
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNewsDetail(id: String): Result<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val dto = apiService.getNewsDetail(id)
            Result.success(dto.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGeoNews(category: String?): Result<List<NewsArticle>> =
        withContext(Dispatchers.IO) {
            try {
                val apiCategory = if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) {
                    null
                } else {
                    category
                }
                val response = apiService.getGeoNews(category = apiCategory)
                val articles = response.features.map { it.toDomain() }
                Result.success(articles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
