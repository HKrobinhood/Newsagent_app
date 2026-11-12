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
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.data.prefs.UserPreferencesRepository;
import com.example.newsagnet.databinding.FragmentGlobeBinding;
import com.example.newsagnet.history.HistoryRepository;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.repository.JavaNewsRepository;
import com.example.newsagnet.repository.JavaNewsRepositoryImpl;
import com.example.newsagnet.repository.NewsCallback;
import com.example.newsagnet.ui.adapter.NewsAdapter;
import com.example.newsagnet.ui.adapter.NewsItemClickListener;
import com.example.newsagnet.ui.globe.GlobeMath;
import com.example.newsagnet.ui.globe.OnHotspotClickListener;
import com.example.newsagnet.ui.globe.WorldMapLoader;
import com.example.newsagnet.ui.globe.WorldMapVector;
import com.example.newsagnet.ui.theme.FontScaleManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * GlobeFragment hosts the 3D spherical situational intelligence visualization with media source
 * filtering and dynamic font scaling.
 *
 * <p>Conforms to COMP7506 Ch4 (Mobile Lifecycle), Ch5 (XML Layouts, RecyclerView, Adapter,
 * ViewHolder), Ch6 (Component Lifecycle, Intent), and Ch7 (ExecutorService & Concurrency).
 */
public class GlobeFragment extends Fragment
    implements OnHotspotClickListener,
        NewsItemClickListener,
        UserPreferencesRepository.OnPreferencesChangedListener {

  private static final String TAG = "GlobeFragment";

  private FragmentGlobeBinding binding;
  private JavaNewsRepository newsRepository;
  private UserPreferencesRepository preferencesRepository;
  private NewsAdapter geoNewsAdapter;

  private final List<NewsArticle> geoArticles = new ArrayList<>();
  private String selectedCategory = null;
  private String lastLoadedSourceIdsParam = null;
  private NewsArticle selectedArticle = null;
  private boolean isLoading = false;

  public static GlobeFragment newInstance() {
    return new GlobeFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    Log.d(TAG, "onAttach: Fragment attached to context: " + context.getClass().getSimpleName());
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate: Initializing JavaNewsRepository and dependencies");
    newsRepository = new JavaNewsRepositoryImpl();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: Inflating fragment_globe with ViewBinding");
    binding = FragmentGlobeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated: Initializing 3D GlobeView, Category chips, and RecyclerView");

    preferencesRepository = LocalPreferencesRepository.getInstance(requireContext());
    preferencesRepository.addOnPreferencesChangedListener(this);

    setupRecyclerView();
    setupGlobeView();
    setupCategoryChips();
    setupActions();

    applyUserFontScale(preferencesRepository.getFontScale());

    loadWorldMapGeography();
    loadGeolocatedNews();
  }

  private void applyUserFontScale(float fontScale) {
    if (binding != null) {
      FontScaleManager.applyFontScale(binding.getRoot(), fontScale);
    }
    if (geoNewsAdapter != null) {
      geoNewsAdapter.setFontScale(fontScale);
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
      Log.d(TAG, "onPreferencesChanged: enabled sources changed -> reloading 3D globe hotspots");
      isLoading = false;
      if (selectedArticle != null
          && updatedPreferences.filterArticles(Collections.singletonList(selectedArticle)).isEmpty()) {
        binding.cardSelectedHotspot.setVisibility(View.GONE);
        selectedArticle = null;
        binding.viewGlobe3d.setSelectedArticle(null);
      }
      loadGeolocatedNews();
    }
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden && preferencesRepository != null) {
      onPreferencesChanged(preferencesRepository.getPreferences());
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "onStart: GlobeFragment visible to user");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume: GlobeFragment in foreground, active interaction enabled");
    if (preferencesRepository != null) {
      onPreferencesChanged(preferencesRepository.getPreferences());
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause: GlobeFragment pausing");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(TAG, "onStop: GlobeFragment no longer visible");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView: Cleaning up view bindings");
    if (preferencesRepository != null) {
      preferencesRepository.removeOnPreferencesChangedListener(this);
    }
    binding = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy: GlobeFragment instance destroyed");
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "onDetach: GlobeFragment detached from host Activity");
  }

  private void setupRecyclerView() {
    geoNewsAdapter = new NewsAdapter(this);
    binding.recyclerGeoNews.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerGeoNews.setAdapter(geoNewsAdapter);
  }

  private void setupGlobeView() {
    binding.viewGlobe3d.setOnHotspotClickListener(this);
  }

  private void setupCategoryChips() {
    binding.layoutGlobeCategories.chipGroupCategories.setOnCheckedStateChangeListener(
        (group, checkedIds) -> {
          if (checkedIds.isEmpty()) {
            return;
          }
          int checkedId = checkedIds.get(0);
          String category = null;

          if (checkedId == R.id.chip_category_all) {
            category = null;
          } else if (checkedId == R.id.chip_category_world) {
            category = "world";
          } else if (checkedId == R.id.chip_category_tech) {
            category = "technology";
          } else if (checkedId == R.id.chip_category_business) {
            category = "business";
          } else if (checkedId == R.id.chip_category_science) {
            category = "science";
          } else if (checkedId == R.id.chip_category_health) {
            category = "health";
          }

          selectedCategory = category;
          Log.d(TAG, "Category changed to: " + category);
          loadGeolocatedNews();
        });
  }

  private void setupActions() {
    binding.btnGlobeRefresh.setOnClickListener(v -> loadGeolocatedNews());

    binding.btnGlobeSettings.setOnClickListener(
        v -> {
          Log.d(TAG, "Globe header settings clicked");
          if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openSettings();
          }
        });

    binding.layoutGlobeStateViews.btnRetry.setOnClickListener(v -> loadGeolocatedNews());

    binding.btnCardClose.setOnClickListener(
        v -> {
          binding.cardSelectedHotspot.setVisibility(View.GONE);
          selectedArticle = null;
          binding.viewGlobe3d.setSelectedArticle(null);
        });

    binding.btnCardReadFull.setOnClickListener(
        v -> {
          if (selectedArticle != null && selectedArticle.getOriginalUrl() != null) {
            Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(selectedArticle.getOriginalUrl()));
            startActivity(browserIntent);
            HistoryRepository.recordOpened(requireContext(), selectedArticle);
          }
        });
  }

  private void loadWorldMapGeography() {
    WorldMapLoader.loadWorldMapAsync(
        requireContext(),
        new ResultCallback<WorldMapVector>() {
          @Override
          public void onSuccess(@NonNull WorldMapVector result) {
            if (binding != null) {
              Log.d(
                  TAG,
                  "WorldMapVector loaded successfully: "
                      + result.getLandRings().size()
                      + " land rings");
              binding.viewGlobe3d.setWorldMap(result);
            }
          }

          @Override
          public void onError(@NonNull Exception exception) {
            Log.e(TAG, "Failed to load WorldMapVector", exception);
          }
        });
  }

  private void loadGeolocatedNews() {
    if (isLoading) {
      return;
    }

    UserPreferences prefs =
        preferencesRepository != null
            ? preferencesRepository.getPreferences()
            : LocalPreferencesRepository.getInstance(requireContext()).getPreferences();
    String sourceIdsParam = prefs.toSourceIdsQueryParam();
    lastLoadedSourceIdsParam = sourceIdsParam;

    if (prefs.areAllSourcesDisabled()) {
      isLoading = false;
      geoArticles.clear();
      if (binding != null) {
        renderArticles(Collections.emptyList(), true);
      }
      return;
    }

    isLoading = true;
    showLoadingState();

    newsRepository.getGeoNews(
        selectedCategory,
        sourceIdsParam,
        new NewsCallback<List<NewsArticle>>() {
          @Override
          public void onSuccess(@NonNull List<NewsArticle> articles) {
            isLoading = false;
            List<NewsArticle> filtered = prefs.filterArticles(articles);
            geoArticles.clear();
            geoArticles.addAll(filtered);

            if (binding != null) {
              renderArticles(filtered, false);
            }
          }

          @Override
          public void onError(@NonNull Throwable error) {
            isLoading = false;
            Log.e(TAG, "getGeoNews failed", error);
            if (binding != null) {
              showErrorState(error.getMessage());
            }
          }
        });
  }

  private void renderArticles(List<NewsArticle> articles, boolean allSourcesDisabled) {
    binding.progressGlobeLoading.setVisibility(View.GONE);
    binding.layoutGlobeStateViews.layoutLoading.setVisibility(View.GONE);
    binding.layoutGlobeStateViews.layoutError.setVisibility(View.GONE);

    if (articles.isEmpty()) {
      binding.layoutGlobeStateViews.tvEmpty.setText(
          allSourcesDisabled ? R.string.settings_empty_all_sources_disabled : R.string.news_empty);
      binding.layoutGlobeStateViews.tvEmpty.setVisibility(View.VISIBLE);
      binding.recyclerGeoNews.setVisibility(View.GONE);
      binding.tvSituationFeedTitle.setText(getString(R.string.globe_feed_title, 0));
      geoNewsAdapter.submitList(new ArrayList<>());
      binding.viewGlobe3d.setArticles(Collections.emptyList());
    } else {
      binding.layoutGlobeStateViews.tvEmpty.setVisibility(View.GONE);
      binding.recyclerGeoNews.setVisibility(View.VISIBLE);
      binding.tvSituationFeedTitle.setText(getString(R.string.globe_feed_title, articles.size()));

      geoNewsAdapter.submitList(new ArrayList<>(articles));
      binding.viewGlobe3d.setArticles(articles);
    }
  }

  private void showLoadingState() {
    if (binding == null) {
      return;
    }
    binding.progressGlobeLoading.setVisibility(View.VISIBLE);
    if (geoArticles.isEmpty()) {
      binding.layoutGlobeStateViews.layoutLoading.setVisibility(View.VISIBLE);
      binding.layoutGlobeStateViews.layoutError.setVisibility(View.GONE);
      binding.layoutGlobeStateViews.tvEmpty.setVisibility(View.GONE);
    }
  }

  private void showErrorState(String message) {
    if (binding == null) {
      return;
    }
    binding.progressGlobeLoading.setVisibility(View.GONE);
    binding.layoutGlobeStateViews.layoutLoading.setVisibility(View.GONE);

    if (geoArticles.isEmpty()) {
      binding.layoutGlobeStateViews.layoutError.setVisibility(View.VISIBLE);
      binding.layoutGlobeStateViews.tvErrorMessage.setText(
          message != null ? message : getString(R.string.error_network_general));
    }
  }

  /** Invoked when user taps a single hotspot on the 3D Globe. */
  @Override
  public void onSingleHotspotClick(@NonNull NewsArticle article) {
    Log.d(TAG, "onSingleHotspotClick: " + article.getTitle());
    showHotspotDetail(article);
  }

  /** Invoked when user taps a clustered group of markers on the 3D Globe. */
  @Override
  public void onClusterHotspotClick(@NonNull List<NewsArticle> clusterArticles) {
    if (clusterArticles.isEmpty()) {
      return;
    }
    Log.d(TAG, "onClusterHotspotClick: cluster of " + clusterArticles.size() + " articles");
    showHotspotDetail(clusterArticles.get(0));
  }

  /** Invoked when user clicks an article card in the Geolocated Situation Feed RecyclerView. */
  @Override
  public void onNewsItemClick(@NonNull NewsArticle article) {
    Log.d(TAG, "onNewsItemClick from feed: " + article.getTitle());

    if (article.getLatitude() != null && article.getLongitude() != null) {
      binding.viewGlobe3d.rotateToLocation(article.getLatitude(), article.getLongitude());
    }

    showHotspotDetail(article);
  }

  private void showHotspotDetail(@NonNull NewsArticle article) {
    this.selectedArticle = article;
    if (binding == null) {
      return;
    }

    binding.viewGlobe3d.setSelectedArticle(article);

    String locText =
        article.getLocationName() != null ? article.getLocationName() : "Unknown Location";
    if (article.getLatitude() != null && article.getLongitude() != null) {
      locText =
          String.format(
              Locale.US,
              "%s (%.1f°, %.1f°)",
              locText,
              article.getLatitude(),
              article.getLongitude());
    }
    binding.tvCardLocation.setText(locText);

    String cat = article.getCategory();
    binding.tvCardCategory.setText(cat != null ? cat.toUpperCase(Locale.US) : "GENERAL");
    binding.tvCardCategory.setTextColor(GlobeMath.getCategoryColor(cat != null ? cat : "general"));

    binding.tvCardTitle.setText(article.getTitle());

    String source =
        article.getSourceName() != null ? article.getSourceName() : "Intelligence Feed";
    String published = article.getPublishedAt() != null ? article.getPublishedAt() : "";
    binding.tvCardMeta.setText(source + " • " + published);

    binding.cardSelectedHotspot.setVisibility(View.VISIBLE);
    if (preferencesRepository != null) {
      FontScaleManager.applyFontScale(
          binding.cardSelectedHotspot, preferencesRepository.getFontScale());
    }
  }
}
