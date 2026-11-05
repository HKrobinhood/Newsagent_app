package com.example.newsagnet.data.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import com.example.newsagnet.model.NewsCategory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
