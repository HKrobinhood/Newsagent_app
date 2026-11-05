package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.network.ApiClient;
import com.example.newsagnet.network.NewsApiService;
import com.example.newsagnet.network.dto.GeoFeatureCollectionDto;
import com.example.newsagnet.network.dto.GeoFeatureDto;
import com.example.newsagnet.network.dto.NewsArticleDto;
import com.example.newsagnet.network.dto.NewsListDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Pure Java implementation of {@link JavaNewsRepository} executing network operations on background
 * threads and delivering results to the UI thread via {@link AppExecutors}.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Class & Interface Implementation) and Ch7 (Thread, Runnable,
 * ExecutorService, Handler, UI Thread).
 */
public class JavaNewsRepositoryImpl implements JavaNewsRepository {

  private final NewsApiService apiService;
  private final AppExecutors appExecutors;

  public JavaNewsRepositoryImpl() {
    this(ApiClient.INSTANCE.getNewsApiService(), AppExecutors.getInstance());
  }

  public JavaNewsRepositoryImpl(
      @NonNull NewsApiService apiService, @NonNull AppExecutors appExecutors) {
    this.apiService = apiService;
    this.appExecutors = appExecutors;
  }

  @Override
  public void getNews(
      int page,
      int limit,
      @Nullable String category,
      @Nullable String source,
      @Nullable String keyword,
      @NonNull NewsCallback<NewsPageResult> callback) {

    String apiCategory =
        (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("all"))
            ? null
            : category.trim();

    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                Call<NewsListDto> call =
                    apiService.getNewsCall(page, limit, apiCategory, source, keyword);
                Response<NewsListDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                  NewsListDto dto = response.body();
                  List<NewsArticle> articles = new ArrayList<>();
                  for (NewsArticleDto item : dto.getItems()) {
                    articles.add(item.toDomainModel());
                  }
                  NewsPageResult pageResult =
                      new NewsPageResult(articles, dto.getTotal(), dto.getPage(), dto.getHasMore());

                  appExecutors.mainThread().execute(() -> callback.onSuccess(pageResult));
                } else {
                  String errorMsg =
                      "HTTP " + response.code() + ": " + response.message();
                  appExecutors
                      .mainThread()
                      .execute(() -> callback.onError(new IOException(errorMsg)));
                }
              } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
              }
            });
  }

  @Override
  public void getNewsDetail(@NonNull String id, @NonNull NewsCallback<NewsArticle> callback) {
    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                Call<NewsArticleDto> call = apiService.getNewsDetailCall(id);
                Response<NewsArticleDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                  NewsArticle article = response.body().toDomainModel();
                  appExecutors.mainThread().execute(() -> callback.onSuccess(article));
                } else {
                  String errorMsg =
                      "HTTP " + response.code() + ": " + response.message();
                  appExecutors
                      .mainThread()
                      .execute(() -> callback.onError(new IOException(errorMsg)));
                }
              } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
              }
            });
  }

  @Override
  public void getGeoNews(
      @Nullable String category, @NonNull NewsCallback<List<NewsArticle>> callback) {

    String apiCategory =
        (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("all"))
            ? null
            : category.trim();

    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                Call<GeoFeatureCollectionDto> call = apiService.getGeoNewsCall(apiCategory, 100);
                Response<GeoFeatureCollectionDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                  List<NewsArticle> articles = new ArrayList<>();
                  for (GeoFeatureDto feature : response.body().getFeatures()) {
                    articles.add(feature.toDomain());
                  }
                  appExecutors.mainThread().execute(() -> callback.onSuccess(articles));
                } else {
                  String errorMsg =
                      "HTTP " + response.code() + ": " + response.message();
                  appExecutors
                      .mainThread()
                      .execute(() -> callback.onError(new IOException(errorMsg)));
                }
              } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
              }
            });
  }
}
