package com.example.newsagnet.repository

import com.example.newsagnet.model.NewsArticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Interop bridge between suspend Kotlin Repository and Java callback-based contracts.
 */
object NewsRepositoryBridge {

    private val repo: NewsRepository = NewsRepositoryImpl()

    @JvmStatic
    fun fetchNews(
        page: Int,
        limit: Int,
        category: String?,
        source: String?,
        keyword: String?,
        callback: NewsCallback<NewsPageResult>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getNews(page, limit, category, source, keyword)
            result.fold(
                onSuccess = { res ->
                    callback.onSuccess(NewsPageResult(res.articles, res.total, res.page, res.hasMore))
                },
                onFailure = { err -> callback.onError(err) }
            )
        }
    }

    @JvmStatic
    fun fetchGeoNews(
        category: String?,
        callback: NewsCallback<List<NewsArticle>>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getGeoNews(category)
            result.fold(
                onSuccess = { res -> callback.onSuccess(res) },
                onFailure = { err -> callback.onError(err) }
            )
        }
    }

    @JvmStatic
    fun fetchNewsDetail(
        id: String,
        callback: NewsCallback<NewsArticle>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getNewsDetail(id)
            result.fold(
                onSuccess = { res -> callback.onSuccess(res) },
                onFailure = { err -> callback.onError(err) }
            )
        }
    }
}
