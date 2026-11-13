package com.example.newsagnet.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.databinding.ActivityArticleWebBinding;
import com.example.newsagnet.history.HistoryRepository;
import com.example.newsagnet.model.NewsArticle;

/**
 * In-app article reader activity supporting Google News-style inline browsing.
 * Opens news articles directly inside the application with seamless back navigation.
 */
public class ArticleWebActivity extends AppCompatActivity {

  private static final String TAG = "ArticleWebActivity";

  public static final String EXTRA_URL = "extra_article_url";
  public static final String EXTRA_TITLE = "extra_article_title";
  public static final String EXTRA_SOURCE = "extra_article_source";

  private ActivityArticleWebBinding binding;
  private String articleUrl;
  private String articleTitle;
  private String articleSource;

  /**
   * Helper to open an article in the in-app reader and record it in reading history.
   *
   * @param context active context
   * @param article the news article to display
   */
  public static void open(@NonNull Context context, @NonNull NewsArticle article) {
    open(context, article.getOriginalUrl(), article.getTitle(), article.getSourceName());
    HistoryRepository.recordOpened(context, article);
  }

  /**
   * Helper to open a URL in the in-app reader.
   *
   * @param context active context
   * @param rawUrl article target URL
   * @param title article headline
   * @param source media publication name
   */
  public static void open(
      @NonNull Context context,
      @Nullable String rawUrl,
      @Nullable String title,
      @Nullable String source) {
    if (rawUrl == null || rawUrl.trim().isEmpty()) {
      return;
    }
    String cleanUrl = normalizeUrl(rawUrl);
    Intent intent = new Intent(context, ArticleWebActivity.class);
    intent.putExtra(EXTRA_URL, cleanUrl);
    intent.putExtra(EXTRA_TITLE, title);
    intent.putExtra(EXTRA_SOURCE, source);
    if (!(context instanceof Activity)) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(intent);
  }

  /**
   * Normalizes URLs by trimming whitespace and ensuring http/https protocol prefix.
   */
  @NonNull
  public static String normalizeUrl(@NonNull String url) {
    String clean = url.trim();
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
      clean = "https://" + clean;
    }
    return clean;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityArticleWebBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Intent intent = getIntent();
    articleUrl = intent != null ? intent.getStringExtra(EXTRA_URL) : null;
    articleTitle = intent != null ? intent.getStringExtra(EXTRA_TITLE) : null;
    articleSource = intent != null ? intent.getStringExtra(EXTRA_SOURCE) : null;

    if (articleUrl == null || articleUrl.trim().isEmpty()) {
      Toast.makeText(this, R.string.news_link_open_failed, Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    setupToolbar();
    setupWebView();
    setupBackNavigation();

    loadArticle();
  }

  private void setupToolbar() {
    binding.btnArticleBack.setOnClickListener(v -> finish());

    String host = extractHost(articleUrl);
    String headerText;
    if (!TextUtils.isEmpty(articleSource) && !TextUtils.isEmpty(host)) {
      headerText = articleSource + " • " + host;
    } else if (!TextUtils.isEmpty(articleSource)) {
      headerText = articleSource;
    } else if (!TextUtils.isEmpty(host)) {
      headerText = host;
    } else {
      headerText = getString(R.string.app_name);
    }
    binding.tvArticleSourceDomain.setText(headerText);

    if (!TextUtils.isEmpty(articleTitle)) {
      binding.tvArticleTitleSub.setText(articleTitle);
      binding.tvArticleTitleSub.setVisibility(View.VISIBLE);
    } else {
      binding.tvArticleTitleSub.setVisibility(View.GONE);
    }

    binding.btnArticleShare.setOnClickListener(v -> shareArticle());
    binding.btnArticleOpenBrowser.setOnClickListener(v -> openInExternalBrowser());
  }

  private void setupWebView() {
    WebSettings settings = binding.webViewArticle.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setUseWideViewPort(true);
    settings.setLoadWithOverviewMode(true);
    settings.setSupportZoom(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

    binding.webViewArticle.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(
              WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri != null ? uri.getScheme() : null;
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
              return false; // Keep browsing inside this in-app WebView
            }
            try {
              Intent externalIntent = new Intent(Intent.ACTION_VIEW, uri);
              startActivity(externalIntent);
              return true;
            } catch (Exception e) {
              Log.w(TAG, "Cannot handle external scheme: " + uri, e);
              return true;
            }
          }

          @Override
          public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            binding.progressArticleLoading.setVisibility(View.VISIBLE);
            binding.layoutWebError.setVisibility(View.GONE);
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            binding.progressArticleLoading.setVisibility(View.GONE);
          }

          @Override
          public void onReceivedError(
              WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request != null && request.isForMainFrame()) {
              binding.progressArticleLoading.setVisibility(View.GONE);
              Log.e(TAG, "Main frame error while loading article: " + error);
            }
          }
        });

    binding.webViewArticle.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100) {
              binding.progressArticleLoading.setVisibility(View.VISIBLE);
              binding.progressArticleLoading.setProgressCompat(newProgress, true);
            } else {
              binding.progressArticleLoading.setVisibility(View.GONE);
            }
          }

          @Override
          public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (TextUtils.isEmpty(articleTitle) && !TextUtils.isEmpty(title)) {
              binding.tvArticleTitleSub.setText(title);
              binding.tvArticleTitleSub.setVisibility(View.VISIBLE);
            }
          }
        });

    binding.btnWebRetry.setOnClickListener(
        v -> {
          binding.layoutWebError.setVisibility(View.GONE);
          binding.webViewArticle.reload();
        });
  }

  private void setupBackNavigation() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (binding.webViewArticle.canGoBack()) {
                  binding.webViewArticle.goBack();
                } else {
                  finish();
                }
              }
            });
  }

  private void loadArticle() {
    if (articleUrl != null) {
      binding.webViewArticle.loadUrl(articleUrl);
    }
  }

  private void shareArticle() {
    if (articleUrl == null) {
      return;
    }
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    String shareText =
        !TextUtils.isEmpty(articleTitle)
            ? articleTitle + "\n" + articleUrl
            : articleUrl;
    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
    startActivity(Intent.createChooser(shareIntent, getString(R.string.article_web_share_title)));
  }

  private void openInExternalBrowser() {
    if (articleUrl == null) {
      return;
    }
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
      startActivity(intent);
    } catch (Exception e) {
      Log.e(TAG, "Failed to launch external browser for URL: " + articleUrl, e);
      Toast.makeText(this, R.string.news_link_open_failed, Toast.LENGTH_SHORT).show();
    }
  }

  @Nullable
  private static String extractHost(@Nullable String url) {
    if (url == null) {
      return null;
    }
    try {
      Uri uri = Uri.parse(url);
      String host = uri.getHost();
      if (host != null && host.startsWith("www.")) {
        host = host.substring(4);
      }
      return host;
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    binding.webViewArticle.onResume();
  }

  @Override
  protected void onPause() {
    binding.webViewArticle.onPause();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    binding.webViewArticle.stopLoading();
    binding.webViewArticle.clearHistory();
    binding.webViewArticle.destroy();
    super.onDestroy();
  }
}
