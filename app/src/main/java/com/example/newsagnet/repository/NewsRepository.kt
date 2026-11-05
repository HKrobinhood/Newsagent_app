package com.example.newsagnet.repository

import com.example.newsagnet.model.NewsArticle

/**
 * Contract for querying news articles from remote and local caches.
 */
interface NewsRepository {

    suspend fun getNews(
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        source: String? = null,
        keyword: String? = null
    ): Result<NewsPageResult>

    suspend fun getNewsDetail(id: String): Result<NewsArticle>

    suspend fun getGeoNews(category: String? = null): Result<List<NewsArticle>>
}
