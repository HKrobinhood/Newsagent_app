package com.example.newsagnet.data.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsCategory;
import com.example.newsagnet.model.NewsSourceItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for NewsPreferences verifying key-value persistence.
 *
 * <p>Conforms to COMP7506 Ch6 (SharedPreferences testing & Dependency Injection).
 */
public class NewsPreferencesTest {

  private NewsPreferences preferences;
  private FakeSharedPreferences fakePrefs;

  @Before
  public void setUp() {
    fakePrefs = new FakeSharedPreferences();
    preferences = new NewsPreferences(fakePrefs);
  }

  @Test
  public void testDefaultCategory_defaultsToAll() {
    assertEquals(NewsCategory.ALL, preferences.getDefaultCategory());
  }

  @Test
  public void testDefaultCategory_saveAndRetrieve() {
    preferences.setDefaultCategory(NewsCategory.TECHNOLOGY);
    assertEquals(NewsCategory.TECHNOLOGY, preferences.getDefaultCategory());

    preferences.setDefaultCategory(NewsCategory.BUSINESS);
    assertEquals(NewsCategory.BUSINESS, preferences.getDefaultCategory());
  }

  @Test
  public void testLastRefreshTime_saveAndRetrieve() {
    assertEquals(0L, preferences.getLastRefreshTime());

    long now = 1730700000000L;
    preferences.setLastRefreshTime(now);
    assertEquals(now, preferences.getLastRefreshTime());
  }

  @Test
  public void testDarkMode_saveAndRetrieve() {
    assertTrue("Dark mode should be enabled by default", preferences.isDarkModeEnabled());

    preferences.setDarkModeEnabled(false);
    assertFalse(preferences.isDarkModeEnabled());

    preferences.setDarkModeEnabled(true);
    assertTrue(preferences.isDarkModeEnabled());
  }

  @Test
  public void testPageSize_saveAndRetrieve() {
    assertEquals(20, preferences.getPageSize());

    preferences.setPageSize(50);
    assertEquals(50, preferences.getPageSize());
  }

  @Test
  public void testClear_removesAllValues() {
    preferences.setDefaultCategory(NewsCategory.SCIENCE);
    preferences.setLastRefreshTime(123456L);
    preferences.setPageSize(30);

    preferences.clear();

    assertEquals(NewsCategory.ALL, preferences.getDefaultCategory());
    assertEquals(0L, preferences.getLastRefreshTime());
    assertEquals(20, preferences.getPageSize());
  }

  @Test
  public void testUserPreferencesRepository_defaultEnabledSourcesAndFontScale() {
    LocalPreferencesRepository repo = new LocalPreferencesRepository(preferences);
    UserPreferences userPrefs = repo.getPreferences();

    assertEquals(
        new LinkedHashSet<>(Arrays.asList("bbc", "cnn", "aljazeera", "fox")),
        userPrefs.getEnabledSourceIds());
    assertEquals(1.00f, userPrefs.getFontScale(), 0.001f);
    assertEquals(FontScaleOption.MEDIUM, userPrefs.getFontScaleOption());
    assertEquals(1, userPrefs.getSettingsVersion());
  }

  @Test
  public void testUserPreferencesRepository_toggleSourceAndFilterArticles() {
    LocalPreferencesRepository repo = new LocalPreferencesRepository(preferences);
    AtomicReference<UserPreferences> notified = new AtomicReference<>();
    repo.addOnPreferencesChangedListener(notified::set);

    repo.setSourceEnabled("bbc", false);

    assertFalse(repo.getEnabledSourceIds().contains("bbc"));
    assertTrue(repo.getEnabledSourceIds().contains("cnn"));
    assertTrue(notified.get() != null && !notified.get().isSourceEnabled("bbc"));

    NewsArticle bbcArticle =
        new NewsArticle(
            "1",
            "BBC Headline",
            "bbc",
            "BBC News",
            "2026-11-12T00:00:00Z",
            "https://bbc.com/1",
            "WORLD");
    NewsArticle cnnArticle =
        new NewsArticle(
            "2",
            "CNN Headline",
            "cnn",
            "CNN",
            "2026-11-12T00:00:00Z",
            "https://cnn.com/2",
            "WORLD");

    List<NewsArticle> filtered =
        repo.getPreferences().filterArticles(Arrays.asList(bbcArticle, cnnArticle));
    assertEquals(1, filtered.size());
    assertEquals("CNN", filtered.get(0).getSourceName());
  }

  @Test
  public void testUserPreferencesRepository_disableAllAndResetToDefault() {
    LocalPreferencesRepository repo = new LocalPreferencesRepository(preferences);

    repo.setEnabledSourceIds(Collections.emptySet());
    assertTrue(repo.getEnabledSourceIds().isEmpty());
    assertTrue(repo.getPreferences().areAllSourcesDisabled());

    repo.resetSourcesToDefault(NewsSourceItem.getDefaultSourceIds());
    assertEquals(4, repo.getEnabledSourceIds().size());
    assertFalse(repo.getPreferences().areAllSourcesDisabled());
  }

  @Test
  public void testUserPreferencesRepository_reconcileNewSourcesPreservesExistingSelection() {
    LocalPreferencesRepository repo = new LocalPreferencesRepository(preferences);
    // User disables BBC
    repo.setSourceEnabled("bbc", false);

    // Backend introduces a new source "reuters" while keeping known sources
    Set<String> backendSources =
        new LinkedHashSet<>(Arrays.asList("bbc", "cnn", "aljazeera", "fox", "reuters"));
    repo.reconcileAvailableSources(backendSources);

    Set<String> enabledAfter = repo.getEnabledSourceIds();
    assertFalse("BBC should remain disabled", enabledAfter.contains("bbc"));
    assertTrue("CNN should remain enabled", enabledAfter.contains("cnn"));
    assertTrue(
        "Newly discovered source should be enabled by default", enabledAfter.contains("reuters"));
  }

  @Test
  public void testUserPreferencesRepository_fontScaleSaveAndRetrieve() {
    LocalPreferencesRepository repo = new LocalPreferencesRepository(preferences);

    repo.setFontScale(FontScaleOption.SMALL.getScale());
    assertEquals(0.90f, repo.getFontScale(), 0.001f);

    repo.setFontScale(FontScaleOption.LARGE.getScale());
    assertEquals(1.15f, repo.getFontScale(), 0.001f);

    repo.setFontScale(FontScaleOption.EXTRA_LARGE.getScale());
    assertEquals(1.30f, repo.getFontScale(), 0.001f);
    assertTrue(repo.getPreferences().getLastUpdated() > 0L);
  }

  /** In-memory test double for SharedPreferences without requiring Android framework mocks. */
  private static class FakeSharedPreferences implements SharedPreferences {
    private final Map<String, Object> storage = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
      return new HashMap<>(storage);
    }

    @Override
    public String getString(String key, String defValue) {
      Object val = storage.get(key);
      return val instanceof String ? (String) val : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
      return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
      Object val = storage.get(key);
      return val instanceof Integer ? (Integer) val : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
      Object val = storage.get(key);
      return val instanceof Long ? (Long) val : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
      Object val = storage.get(key);
      return val instanceof Float ? (Float) val : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      Object val = storage.get(key);
      return val instanceof Boolean ? (Boolean) val : defValue;
    }

    @Override
    public boolean contains(String key) {
      return storage.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new FakeEditor(storage);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}

    private static class FakeEditor implements Editor {
      private final Map<String, Object> target;
      private final Map<String, Object> pending = new HashMap<>();
      private boolean clearPending = false;

      FakeEditor(Map<String, Object> target) {
        this.target = target;
      }

      @Override
      public Editor putString(String key, String value) {
        pending.put(key, value);
        return this;
      }

      @Override
      public Editor putStringSet(String key, Set<String> values) {
        return this;
      }

      @Override
      public Editor putInt(String key, int value) {
        pending.put(key, value);
        return this;
      }

      @Override
      public Editor putLong(String key, long value) {
        pending.put(key, value);
        return this;
      }

      @Override
      public Editor putFloat(String key, float value) {
        pending.put(key, value);
        return this;
      }

      @Override
      public Editor putBoolean(String key, boolean value) {
        pending.put(key, value);
        return this;
      }

      @Override
      public Editor remove(String key) {
        pending.remove(key);
        target.remove(key);
        return this;
      }

      @Override
      public Editor clear() {
        clearPending = true;
        return this;
      }

      @Override
      public boolean commit() {
        apply();
        return true;
      }

      @Override
      public void apply() {
        if (clearPending) {
          target.clear();
        }
        target.putAll(pending);
      }
    }
  }
}
