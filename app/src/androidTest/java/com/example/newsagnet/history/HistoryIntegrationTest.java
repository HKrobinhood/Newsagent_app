package com.example.newsagnet.history;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.ui.fragment.ChatFragment;
import com.example.newsagnet.ui.fragment.GlobeFragment;
import com.example.newsagnet.ui.fragment.NewsFragment;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the three existing ACTION_VIEW flows using intercepted browser intents. */
@RunWith(AndroidJUnit4.class)
public class HistoryIntegrationTest {
  private Context context;
  private String databaseName;
  private HistoryStore store;
  private HistoryRepository previous;
  private Instrumentation instrumentation;
  private Instrumentation.ActivityMonitor browser;
  private ActivityScenario<MainActivity> scenario;

  @Before public void setUp() {
    instrumentation = InstrumentationRegistry.getInstrumentation();
    context = instrumentation.getTargetContext();
    databaseName = "history-integration-test-" + UUID.randomUUID() + ".db";
    store = new HistoryStore(context, databaseName);
    AppExecutors executors = AppExecutors.getInstance();
    previous = HistoryRepository.replaceForTesting(new HistoryRepository(store,
        executors.diskIO(), executors.mainThread()));
    IntentFilter webLinks = new IntentFilter(Intent.ACTION_VIEW);
    webLinks.addDataScheme("https");
    webLinks.addDataScheme("http");
    browser = instrumentation.addMonitor(webLinks,
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()), true);
    scenario = ActivityScenario.launch(MainActivity.class);
  }

  @After public void tearDown() throws Exception {
    if (scenario != null) scenario.close();
    AppExecutors.getInstance().diskIO().submit(() -> {}).get(5, TimeUnit.SECONDS);
    instrumentation.removeMonitor(browser);
    HistoryRepository.replaceForTesting(previous);
    store.close();
    context.deleteDatabase(databaseName);
  }

  private NewsArticle article(String id) {
    return new NewsArticle(id, "Integration story", "bbc", "BBC News", "2026-11-01",
        "https://example.org/integration", "world", "London", 51.5, -0.1);
  }

  private void flush() throws Exception {
    AppExecutors.getInstance().diskIO().submit(() -> {}).get(5, TimeUnit.SECONDS);
  }

  @Test public void existingNewsGlobeAndCitationLinksStillOpenAndShareOneHistoryEntry() throws Exception {
    scenario.onActivity(activity -> {
      activity.getSupportFragmentManager().executePendingTransactions();
      NewsFragment news = (NewsFragment) activity.getSupportFragmentManager().findFragmentByTag("tag_news");
      news.onNewsItemClick(article("news-id"));
    });
    flush();
    assertEquals(1, store.count());
    store.saveNote("https://example.org/integration", "Keep across all entry points");
    scenario.onActivity(activity -> {
      GlobeFragment globe = (GlobeFragment) activity.getSupportFragmentManager().findFragmentByTag("tag_globe");
      globe.onSingleHotspotClick(article("globe-id"));
    });
    flush();
    assertEquals(1, browser.getHits()); // Selecting a marker is not opening a news link.
    scenario.onActivity(activity -> {
      GlobeFragment globe = (GlobeFragment) activity.getSupportFragmentManager().findFragmentByTag("tag_globe");
      globe.requireView().findViewById(R.id.btn_card_read_full).performClick();
      ChatFragment chat = (ChatFragment) activity.getSupportFragmentManager().findFragmentByTag("tag_chat");
      chat.onNewsItemClick(article("different-citation-id"));
    });
    flush();
    assertEquals(3, browser.getHits());
    assertEquals(1, store.count());
    assertEquals("Keep across all entry points",
        store.find(new HistoryFilter("", null, null, null)).get(0).getNote());
  }

  @Test public void historySchedulingFailureDoesNotBlockOriginalBrowserIntent() throws Exception {
    HistoryRepository.replaceForTesting(new HistoryRepository(store,
        command -> { throw new RejectedExecutionException("Test disk unavailable"); }, Runnable::run));
    scenario.onActivity(activity -> {
      activity.getSupportFragmentManager().executePendingTransactions();
      NewsFragment news = (NewsFragment) activity.getSupportFragmentManager().findFragmentByTag("tag_news");
      news.onNewsItemClick(article("news-id"));
    });
    assertEquals(1, browser.getHits());
    assertEquals(0, store.count());
  }
}
