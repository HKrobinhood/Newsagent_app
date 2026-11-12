package com.example.newsagnet.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.prefs.NewsPreferences;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.data.prefs.UserPreferencesRepository;
import com.example.newsagnet.databinding.FragmentNewsBinding;
import com.example.newsagnet.history.HistoryRepository;
import com.example.newsagnet.history.ReadingHistoryActivity;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsCategory;
import com.example.newsagnet.repository.JavaNewsRepository;
import com.example.newsagnet.repository.JavaNewsRepositoryImpl;
import com.example.newsagnet.repository.NewsCallback;
import com.example.newsagnet.repository.NewsPageResult;
import com.example.newsagnet.ui.adapter.NewsAdapter;
import com.example.newsagnet.ui.adapter.NewsItemClickListener;
import com.example.newsagnet.ui.theme.FontScaleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * NewsFragment hosts the global news feed, category filter chips, media source filtering, and
 * pagination.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP & Interfaces), Ch4 (Lifecycle), Ch5 (XML Layouts,
 * RecyclerView, Adapter, ViewHolder), Ch6 (Intent & SharedPreferences), and Ch7 (Thread,
 * Runnable, ExecutorService, Handler, UI Thread).
 */
public class NewsFragment extends Fragment
    implements NewsItemClickListener, UserPreferencesRepository.OnPreferencesChangedListener {

  private static final String TAG = "NewsFragment";
  private static final int PAGE_SIZE = 20;

  private FragmentNewsBinding binding;
  private JavaNewsRepository newsRepository;
  private AppExecutors appExecutors;
  private NewsPreferences newsPreferences;
  private UserPreferencesRepository preferencesRepository;
  private NewsAdapter newsAdapter;
  private LinearLayoutManager layoutManager;

  private final List<NewsArticle> articleDataset = new ArrayList<>();
  private NewsCategory currentCategory = NewsCategory.ALL;
  private String lastLoadedSourceIdsParam = null;
  private int currentPage = 1;
  private boolean hasMore = false;
  private boolean isLoading = false;
  private boolean isRefreshing = false;
  private boolean isLoadingMore = false;

  public static NewsFragment newInstance() {
    return new NewsFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    Log.d(TAG, "onAttach: Fragment attached to Activity");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate: Initializing concurrency executors and repository");
    appExecutors = AppExecutors.getInstance();
    newsRepository = new JavaNewsRepositoryImpl();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: Inflating fragment_news layout with ViewBinding");
    binding = FragmentNewsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated: Setting up RecyclerView, Concurrency, and Preferences");

    newsPreferences = new NewsPreferences(requireContext());
    preferencesRepository = LocalPreferencesRepository.getInstance(requireContext());
    preferencesRepository.addOnPreferencesChangedListener(this);

    setupRecyclerView();
    setupCategoryChips();
    setupSwipeRefresh();
    setupHeaderActions();
    setupScrollPagination();

    java.text.SimpleDateFormat dateFormat =
        new java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.ENGLISH);
    binding.tvHeaderDate.setText(dateFormat.format(new java.util.Date()));

    applyUserFontScale(preferencesRepository.getFontScale());

    // Restore persistent user category preference (COMP7506 Ch6 SharedPreferences)
    NewsCategory savedCategory = newsPreferences.getDefaultCategory();
    if (savedCategory != NewsCategory.ALL) {
      Log.d(TAG, "Restoring saved category preference: " + savedCategory.getDisplayName());
      currentCategory = savedCategory;
      selectCategoryChipInView(savedCategory);
    }

    // Trigger initial background load via ExecutorService (COMP7506 Ch7)
    loadNews(false);
  }

  private void applyUserFontScale(float fontScale) {
    if (binding != null) {
      FontScaleManager.applyFontScale(binding.getRoot(), fontScale);
    }
    if (newsAdapter != null) {
      newsAdapter.setFontScale(fontScale);
    }
  }

  @Override
  public void onPreferencesChanged(@NonNull UserPreferences updatedPreferences) {
    if (binding == null) {
      return;
    }
    applyUserFontScale(updatedPreferences.getFontScale());

    String nextSourceParam = updatedPreferences.toSourceIdsQueryParam();
    if (!Objects.equals(lastLoadedSourceIdsParam, nextSourceParam)) {
      Log.d(TAG, "onPreferencesChanged: enabled sources changed -> reloading news feed");
      isLoading = false;
      isRefreshing = false;
      isLoadingMore = false;
      articleDataset.clear();
      newsAdapter.submitList(new ArrayList<>());
      loadNews(false);
    }
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden && preferencesRepository != null) {
      onPreferencesChanged(preferencesRepository.getPreferences());
    }
  }

  private void setupRecyclerView() {
    androidx.recyclerview.widget.GridLayoutManager gridLayoutManager =
        new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2);
    newsAdapter = new NewsAdapter(this, true);
    gridLayoutManager.setSpanSizeLookup(
        new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
          @Override
          public int getSpanSize(int position) {
            return newsAdapter.getSpanSize(position);
          }
        });

    layoutManager = gridLayoutManager;
    binding.recyclerNews.setLayoutManager(layoutManager);
    binding.recyclerNews.setAdapter(newsAdapter);
    binding.recyclerNews.setHasFixedSize(true);
  }

  private void setupCategoryChips() {
    binding.layoutCategories.chipGroupCategories.setOnCheckedStateChangeListener(
        (group, checkedIds) -> {
          if (checkedIds.isEmpty()) {
            return;
          }
          int selectedId = checkedIds.get(0);
          NewsCategory targetCategory;

          if (selectedId == R.id.chip_category_world) {
            targetCategory = NewsCategory.WORLD;
          } else if (selectedId == R.id.chip_category_tech) {
            targetCategory = NewsCategory.TECHNOLOGY;
          } else if (selectedId == R.id.chip_category_business) {
            targetCategory = NewsCategory.BUSINESS;
          } else if (selectedId == R.id.chip_category_science) {
            targetCategory = NewsCategory.SCIENCE;
          } else if (selectedId == R.id.chip_category_health) {
            targetCategory = NewsCategory.HEALTH;
          } else {
            targetCategory = NewsCategory.ALL;
          }

          if (currentCategory == targetCategory) {
            return;
          }

          Log.d(TAG, "Category changed to: " + targetCategory.getDisplayName());
          currentCategory = targetCategory;
          newsPreferences.setDefaultCategory(targetCategory);

          articleDataset.clear();
          newsAdapter.submitList(new ArrayList<>());
          loadNews(false);
        });
  }

  private void selectCategoryChipInView(@NonNull NewsCategory category) {
    int targetChipId;
    if (category == NewsCategory.WORLD) {
      targetChipId = R.id.chip_category_world;
    } else if (category == NewsCategory.TECHNOLOGY) {
      targetChipId = R.id.chip_category_tech;
    } else if (category == NewsCategory.BUSINESS) {
      targetChipId = R.id.chip_category_business;
    } else if (category == NewsCategory.SCIENCE) {
      targetChipId = R.id.chip_category_science;
    } else if (category == NewsCategory.HEALTH) {
      targetChipId = R.id.chip_category_health;
    } else {
      targetChipId = R.id.chip_category_all;
    }
    binding.layoutCategories.chipGroupCategories.check(targetChipId);
  }

  private void setupSwipeRefresh() {
    binding.swipeRefresh.setColorSchemeResources(R.color.primary_blue, R.color.primary_cyan);
    binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_dark);
    binding.swipeRefresh.setOnRefreshListener(
        () -> {
          Log.d(TAG, "SwipeRefresh triggered: reloading news via background executor");
          loadNews(true);
        });
  }

  private void setupHeaderActions() {
    binding.btnRefresh.setOnClickListener(
        v -> {
          Log.d(TAG, "Header refresh clicked");
          loadNews(true);
        });

    binding.btnAccount.setOnClickListener(
        v -> {
          Log.d(TAG, "Header account avatar clicked: opening Account Bottom Sheet");
          if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openAccountBottomSheet();
          }
        });

    binding.layoutStateViews.btnRetry.setOnClickListener(
        v -> {
          Log.d(TAG, "Retry button clicked");
          loadNews(false);
        });
  }

  private void setupScrollPagination() {
    binding.recyclerNews.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (dy <= 0) {
              return;
            }

            int totalItemCount = layoutManager.getItemCount();
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();

            if (totalItemCount > 0 && lastVisiblePosition >= totalItemCount - 3) {
              if (hasMore && !isLoadingMore && !isLoading && !isRefreshing) {
                Log.d(TAG, "Pagination threshold reached: requesting page " + (currentPage + 1));
                loadNextPage();
              }
            }
          }
        });
  }

  /**
   * Loads initial or refreshed news dataset using {@link JavaNewsRepository} and {@link
   * AppExecutors}.
   *
   * <p>COMP7506 Ch7: Network call executes on {@code ExecutorService} background thread; result
   * delivers to UI thread via {@code Handler}.
   *
   * @param isRefresh True if initiated via pull-to-refresh.
   */
  public void loadNews(boolean isRefresh) {
    if (isLoading || isRefreshing) {
      return;
    }

    UserPreferences prefs =
        preferencesRepository != null
            ? preferencesRepository.getPreferences()
            : newsPreferences.getUserPreferences();
    String sourceIdsParam = prefs.toSourceIdsQueryParam();
    lastLoadedSourceIdsParam = sourceIdsParam;

    if (prefs.areAllSourcesDisabled()) {
      isLoading = false;
      isRefreshing = false;
      hasMore = false;
      articleDataset.clear();
      if (binding != null) {
        binding.swipeRefresh.setRefreshing(false);
        renderData(0, true);
      }
      return;
    }

    if (isRefresh) {
      isRefreshing = true;
      binding.swipeRefresh.setRefreshing(true);
    } else if (articleDataset.isEmpty()) {
      isLoading = true;
      showLoadingState();
    }

    String categoryParam = currentCategory == NewsCategory.ALL ? null : currentCategory.getKey();

    newsRepository.getNews(
        1,
        PAGE_SIZE,
        categoryParam,
        null,
        sourceIdsParam,
        null,
        new NewsCallback<NewsPageResult>() {
          @Override
          public void onSuccess(@NonNull NewsPageResult result) {
            Log.d(
                TAG,
                "loadNews onSuccess: fetched "
                    + result.getArticles().size()
                    + " items, thread="
                    + Thread.currentThread().getName());

            isLoading = false;
            isRefreshing = false;
            currentPage = 1;
            hasMore = result.isHasMore();

            List<NewsArticle> editorialFeed =
                com.example.newsagnet.data.mock.MockEditorialNewsProvider.provideEditorialFeed(
                    result.getArticles(), prefs, currentCategory);
            articleDataset.clear();
            articleDataset.addAll(editorialFeed);

            if (binding != null) {
              binding.swipeRefresh.setRefreshing(false);
              renderData(editorialFeed.size(), false);
            }
          }

          @Override
          public void onError(@NonNull Throwable throwable) {
            Log.e(TAG, "loadNews onError: " + throwable.getMessage(), throwable);

            isLoading = false;
            isRefreshing = false;

            // In N1 prototype, provide editorial mock feed if server is not reachable
            List<NewsArticle> fallbackFeed =
                com.example.newsagnet.data.mock.MockEditorialNewsProvider.provideEditorialFeed(
                    new ArrayList<>(), prefs, currentCategory);

            if (!fallbackFeed.isEmpty()) {
              articleDataset.clear();
              articleDataset.addAll(fallbackFeed);
              if (binding != null) {
                binding.swipeRefresh.setRefreshing(false);
                renderData(fallbackFeed.size(), false);
              }
            } else if (binding != null) {
              binding.swipeRefresh.setRefreshing(false);
              if (articleDataset.isEmpty()) {
                showErrorState(throwable.getMessage());
              }
            }
          }
        });
  }

  /** Fetches subsequent page of news articles via background thread pool. */
  private void loadNextPage() {
    if (isLoadingMore || !hasMore || isLoading || isRefreshing) {
      return;
    }

    UserPreferences prefs =
        preferencesRepository != null
            ? preferencesRepository.getPreferences()
            : newsPreferences.getUserPreferences();
    if (prefs.areAllSourcesDisabled()) {
      hasMore = false;
      return;
    }

    isLoadingMore = true;
    binding.progressLoadingMore.setVisibility(View.VISIBLE);

    int nextPage = currentPage + 1;
    String categoryParam = currentCategory == NewsCategory.ALL ? null : currentCategory.getKey();
    String sourceIdsParam = prefs.toSourceIdsQueryParam();

    newsRepository.getNews(
        nextPage,
        PAGE_SIZE,
        categoryParam,
        null,
        sourceIdsParam,
        null,
        new NewsCallback<NewsPageResult>() {
          @Override
          public void onSuccess(@NonNull NewsPageResult result) {
            Log.d(
                TAG,
                "loadNextPage onSuccess: added "
                    + result.getArticles().size()
                    + " items, thread="
                    + Thread.currentThread().getName());

            isLoadingMore = false;
            currentPage = nextPage;
            hasMore = result.isHasMore();

            articleDataset.addAll(prefs.filterArticles(result.getArticles()));

            if (binding != null) {
              binding.progressLoadingMore.setVisibility(View.GONE);
              newsAdapter.submitList(new ArrayList<>(articleDataset));
              updateHeaderSubtitle(result.getTotal());
            }
          }

          @Override
          public void onError(@NonNull Throwable throwable) {
            Log.e(TAG, "loadNextPage onError: " + throwable.getMessage(), throwable);

            isLoadingMore = false;
            if (binding != null) {
              binding.progressLoadingMore.setVisibility(View.GONE);
            }
          }
        });
  }

  private void showLoadingState() {
    binding.layoutStateViews.layoutLoading.setVisibility(View.VISIBLE);
    binding.layoutStateViews.layoutError.setVisibility(View.GONE);
    binding.layoutStateViews.tvEmpty.setVisibility(View.GONE);
    binding.recyclerNews.setVisibility(View.GONE);
  }

  private void showErrorState(String errorMessage) {
    binding.layoutStateViews.layoutLoading.setVisibility(View.GONE);
    binding.layoutStateViews.layoutError.setVisibility(View.VISIBLE);
    binding.layoutStateViews.tvErrorMessage.setText(
        errorMessage != null ? errorMessage : getString(R.string.news_error_default));
    binding.layoutStateViews.tvEmpty.setVisibility(View.GONE);
    binding.recyclerNews.setVisibility(View.GONE);
  }

  private void renderData(int totalCount, boolean allSourcesDisabled) {
    binding.layoutStateViews.layoutLoading.setVisibility(View.GONE);
    binding.layoutStateViews.layoutError.setVisibility(View.GONE);

    if (articleDataset.isEmpty()) {
      binding.layoutStateViews.tvEmpty.setText(
          allSourcesDisabled ? R.string.settings_empty_all_sources_disabled : R.string.news_empty);
      binding.layoutStateViews.tvEmpty.setVisibility(View.VISIBLE);
      binding.recyclerNews.setVisibility(View.GONE);
      newsAdapter.submitList(new ArrayList<>());
    } else {
      binding.layoutStateViews.tvEmpty.setVisibility(View.GONE);
      binding.recyclerNews.setVisibility(View.VISIBLE);
      newsAdapter.submitList(new ArrayList<>(articleDataset));
      newsPreferences.setLastRefreshTime(System.currentTimeMillis());
    }

    updateHeaderSubtitle(totalCount);
  }

  private void updateHeaderSubtitle(int totalCount) {
    if (totalCount > 0) {
      binding.tvHeaderSubtitle.setText(getString(R.string.news_subtitle_format, totalCount));
      binding.tvHeaderSubtitle.setVisibility(View.VISIBLE);
    } else {
      binding.tvHeaderSubtitle.setVisibility(View.GONE);
    }
  }

  @Override
  public void onNewsItemClick(@NonNull NewsArticle article) {
    String url = article.getOriginalUrl();
    Log.d(TAG, "onNewsItemClick: Opening URL via Intent: " + url);
    if (url != null && !url.trim().isEmpty()) {
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        HistoryRepository.recordOpened(requireContext(), article);
      } catch (Exception e) {
        Log.e(TAG, "Failed to launch intent for URL: " + url, e);
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "onStart: NewsFragment becoming visible");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume: NewsFragment active and interactive");
    if (preferencesRepository != null) {
      onPreferencesChanged(preferencesRepository.getPreferences());
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause: NewsFragment paused");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(TAG, "onStop: NewsFragment no longer visible");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView: Clearing ViewBinding reference to prevent memory leaks");
    if (preferencesRepository != null) {
      preferencesRepository.removeOnPreferencesChangedListener(this);
    }
    binding = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy: Fragment destroyed");
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "onDetach: Fragment detached from Activity");
  }
}
