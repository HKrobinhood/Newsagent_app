package com.example.newsagnet.data.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import com.example.newsagnet.model.AuthSession;
import com.example.newsagnet.model.UserProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AuthPreferences} verifying session storage, profile loading, and logout.
 */
public class AuthPreferencesTest {

  private FakeSharedPreferences fakePrefs;
  private AuthPreferences authPreferences;

  @Before
  public void setUp() {
    fakePrefs = new FakeSharedPreferences();
    authPreferences = new AuthPreferences(fakePrefs);
  }

  @Test
  public void defaultState_isGuestAndNotLoggedIn() {
    assertFalse(authPreferences.isLoggedIn());
    assertNull(authPreferences.getSession());

    UserProfile user = authPreferences.getUserProfile();
    assertNotNull(user);
    assertTrue(user.isGuest());
    assertEquals("guest", user.getId());
    assertEquals("Guest User", user.getFormattedDisplayName());
  }

  @Test
  public void saveSession_persistsProfileAndToken() {
    UserProfile user =
        new UserProfile("u-123", "alice@example.com", "Alice Smith", UserProfile.PROVIDER_GOOGLE);
    AuthSession session = new AuthSession("tok-abc", "bearer", "2026-12-01T00:00:00Z", user);

    authPreferences.saveSession(session);

    assertTrue(authPreferences.isLoggedIn());
    AuthSession retrieved = authPreferences.getSession();
    assertNotNull(retrieved);
    assertEquals("tok-abc", retrieved.getAccessToken());
    assertEquals("Bearer tok-abc", retrieved.getAuthorizationHeader());

    UserProfile profile = authPreferences.getUserProfile();
    assertFalse(profile.isGuest());
    assertEquals("u-123", profile.getId());
    assertEquals("alice@example.com", profile.getEmail());
    assertEquals("Alice Smith", profile.getFormattedDisplayName());
  }

  @Test
  public void clearSession_resetsBackToGuest() {
    UserProfile user = new UserProfile("u-456", "bob@example.com", null, "google");
    AuthSession session = new AuthSession("tok-xyz", "bearer", "2026-12-01T00:00:00Z", user);
    authPreferences.saveSession(session);
    assertTrue(authPreferences.isLoggedIn());

    authPreferences.clearSession();

    assertFalse(authPreferences.isLoggedIn());
    assertNull(authPreferences.getSession());
    assertTrue(authPreferences.getUserProfile().isGuest());
  }

  /** In-memory fake SharedPreferences. */
  private static class FakeSharedPreferences implements SharedPreferences {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
      return new HashMap<>(map);
    }

    @Override
    public String getString(String key, String defValue) {
      Object v = map.get(key);
      return v instanceof String ? (String) v : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
      return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
      return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
      return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
      return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      return defValue;
    }

    @Override
    public boolean contains(String key) {
      return map.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new Editor() {
        private final Map<String, Object> pending = new HashMap<>();
        private final Set<String> removes = new java.util.HashSet<>();

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
          return this;
        }

        @Override
        public Editor putLong(String key, long value) {
          return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
          return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
          return this;
        }

        @Override
        public Editor remove(String key) {
          removes.add(key);
          pending.remove(key);
          return this;
        }

        @Override
        public Editor clear() {
          map.clear();
          return this;
        }

        @Override
        public boolean commit() {
          apply();
          return true;
        }

        @Override
        public void apply() {
          for (String k : removes) {
            map.remove(k);
          }
          map.putAll(pending);
        }
      };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}
  }
}
