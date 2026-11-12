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
import com.example.newsagnet.R;
import com.example.newsagnet.concurrency.ResultCallback;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GlobeFragment hosts the 3D spherical situational intelligence visualization.
 *
 * <p>Conforms to COMP7506 Ch4 (Mobile Lifecycle), Ch5 (XML Layouts, RecyclerView, Adapter,
 * ViewHolder), Ch6 (Component Lifecycle, Intent), and Ch7 (ExecutorService & Concurrency).
 */
public class GlobeFragment extends Fragment
    implements OnHotspotClickListener, NewsItemClickListener {

  private static final String TAG = "GlobeFragment";

  private FragmentGlobeBinding binding;
  private JavaNewsRepository newsRepository;
  private NewsAdapter geoNewsAdapter;

  private final List<NewsArticle> geoArticles = new ArrayList<>();
  private String selectedCategory = null;
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

    setupRecyclerView();
    setupGlobeView();
    setupCategoryChips();
    setupActions();

    loadWorldMapGeography();
    loadGeolocatedNews();
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
          if (getActivity() instanceof com.example.newsagnet.MainActivity) {
            ((com.example.newsagnet.MainActivity) getActivity()).openSettings();
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
    isLoading = true;
    showLoadingState();

    newsRepository.getGeoNews(
        selectedCategory,
        new NewsCallback<List<NewsArticle>>() {
          @Override
          public void onSuccess(@NonNull List<NewsArticle> articles) {
            isLoading = false;
            geoArticles.clear();
            geoArticles.addAll(articles);

            if (binding != null) {
              renderArticles(articles);
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

  private void renderArticles(List<NewsArticle> articles) {
    binding.progressGlobeLoading.setVisibility(View.GONE);
    binding.layoutGlobeStateViews.layoutLoading.setVisibility(View.GONE);
    binding.layoutGlobeStateViews.layoutError.setVisibility(View.GONE);

    if (articles.isEmpty()) {
      binding.layoutGlobeStateViews.tvEmpty.setVisibility(View.VISIBLE);
      binding.recyclerGeoNews.setVisibility(View.GONE);
      binding.tvSituationFeedTitle.setText(
          getString(R.string.globe_feed_title, 0));
    } else {
      binding.layoutGlobeStateViews.tvEmpty.setVisibility(View.GONE);
      binding.recyclerGeoNews.setVisibility(View.VISIBLE);
      binding.tvSituationFeedTitle.setText(
          getString(R.string.globe_feed_title, articles.size()));

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

  /**
   * Invoked when user taps a single hotspot on the 3D Globe.
   */
  @Override
  public void onSingleHotspotClick(@NonNull NewsArticle article) {
    Log.d(TAG, "onSingleHotspotClick: " + article.getTitle());
    showHotspotDetail(article);
  }

  /**
   * Invoked when user taps a clustered group of markers on the 3D Globe.
   */
  @Override
  public void onClusterHotspotClick(@NonNull List<NewsArticle> clusterArticles) {
    if (clusterArticles.isEmpty()) {
      return;
    }
    Log.d(TAG, "onClusterHotspotClick: cluster of " + clusterArticles.size() + " articles");
    // Show the primary article and rotate to its position
    showHotspotDetail(clusterArticles.get(0));
  }

  /**
   * Invoked when user clicks an article card in the Geolocated Situation Feed RecyclerView.
   */
  @Override
  public void onNewsItemClick(@NonNull NewsArticle article) {
    Log.d(TAG, "onNewsItemClick from feed: " + article.getTitle());

    // Rotate 3D globe to this article's geographic coordinates (COMP7506 Ch6 & Ch6A)
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

    // Format location name with coordinates
    String locText = article.getLocationName() != null ? article.getLocationName() : "Unknown Location";
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

    // Category tag
    String cat = article.getCategory();
    binding.tvCardCategory.setText(cat != null ? cat.toUpperCase(Locale.US) : "GENERAL");
    binding.tvCardCategory.setTextColor(GlobeMath.getCategoryColor(cat != null ? cat : "general"));

    // Title
    binding.tvCardTitle.setText(article.getTitle());

    // Source & Date
    String source = article.getSourceName() != null ? article.getSourceName() : "Intelligence Feed";
    String published = article.getPublishedAt() != null ? article.getPublishedAt() : "";
    binding.tvCardMeta.setText(source + " • " + published);

    // Show card
    binding.cardSelectedHotspot.setVisibility(View.VISIBLE);
  }
}
