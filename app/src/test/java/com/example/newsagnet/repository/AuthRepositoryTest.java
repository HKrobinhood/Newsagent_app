package com.example.newsagnet.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.AuthPreferences;
import com.example.newsagnet.model.UserProfile;
import com.example.newsagnet.network.AuthApiService;
import com.example.newsagnet.network.dto.AuthRequestDto;
import com.example.newsagnet.network.dto.AuthResponseDto;
import com.example.newsagnet.network.dto.LogoutResponseDto;
import com.example.newsagnet.network.dto.PreferencesUpdateDto;
import com.example.newsagnet.network.dto.UserDto;
import com.example.newsagnet.network.dto.UserPreferencesDto;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Request;
import okio.Timeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Unit tests for {@link AuthRepositoryImpl} verifying login, session persistence, and logout flow.
 */
public class AuthRepositoryTest {

  private ExecutorService testService;
  private AppExecutors testExecutors;
  private AuthPreferences authPreferences;

  @Before
  public void setUp() {
    testService = Executors.newSingleThreadExecutor();
    Executor direct = Runnable::run;
    testExecutors = new AppExecutors(testService, testService, direct);
    authPreferences = new AuthPreferences(new FakeSharedPreferences());
  }

  @After
  public void tearDown() {
    if (testService != null) {
      testService.shutdown();
    }
  }

  @Test
  public void loginWithGoogle_successPersistsUserAndNotifiesListeners() throws Exception {
    UserDto userDto = new UserDto("u-999", "innovator@google.com", "Google Innovator", "google");
    AuthResponseDto authResponse =
        new AuthResponseDto("access-token-999", "bearer", "2026-12-01T00:00:00Z", userDto);

    AuthApiService fakeApi =
        new AuthApiService() {
          @NonNull
          @Override
          public Call<AuthResponseDto> loginWithGoogleCall(@NonNull AuthRequestDto request) {
            return new FakeCall<>(Response.success(authResponse));
          }

          @NonNull
          @Override
          public Call<LogoutResponseDto> logoutCall(@Nullable String authHeader) {
            return new FakeCall<>(Response.success(new LogoutResponseDto("success", "Logged out")));
          }

          @NonNull
          @Override
          public Call<UserDto> getMeCall(@NonNull String authHeader) {
            return new FakeCall<>(Response.success(userDto));
          }

          @NonNull
          @Override
          public Call<UserPreferencesDto> getPreferencesCall(@NonNull String authHeader) {
            throw new UnsupportedOperationException();
          }

          @NonNull
          @Override
          public Call<UserPreferencesDto> updatePreferencesCall(
              @NonNull String authHeader, @NonNull PreferencesUpdateDto body) {
            throw new UnsupportedOperationException();
          }
        };

    AuthRepository repo = new AuthRepositoryImpl(fakeApi, authPreferences, testExecutors);
    AtomicReference<UserProfile> listenerProfile = new AtomicReference<>();
    repo.addAuthListener(listenerProfile::set);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<UserProfile> callbackProfile = new AtomicReference<>();

    repo.loginWithGoogle(
        "valid-mock-token",
        new ResultCallback<UserProfile>() {
          @Override
          public void onSuccess(UserProfile data) {
            callbackProfile.set(data);
            latch.countDown();
          }

          @Override
          public void onError(Exception exception) {
            latch.countDown();
          }
        });

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertNotNull(callbackProfile.get());
    assertEquals("innovator@google.com", callbackProfile.get().getEmail());
    assertEquals("Google Innovator", callbackProfile.get().getFormattedDisplayName());
    assertTrue(repo.isLoggedIn());
    assertEquals(callbackProfile.get(), listenerProfile.get());

    // Verify logout
    CountDownLatch logoutLatch = new CountDownLatch(1);
    repo.logout(
        new ResultCallback<Void>() {
          @Override
          public void onSuccess(Void data) {
            logoutLatch.countDown();
          }

          @Override
          public void onError(Exception exception) {
            logoutLatch.countDown();
          }
        });

    assertTrue(logoutLatch.await(2, TimeUnit.SECONDS));
    assertFalse(repo.isLoggedIn());
    assertTrue(repo.getCurrentUser().isGuest());
    assertTrue(listenerProfile.get().isGuest());
  }

  @Test
  public void loginWithGoogle_networkFailureDeliversErrorCallback() throws Exception {
    AuthApiService failingApi =
        new AuthApiService() {
          @NonNull
          @Override
          public Call<AuthResponseDto> loginWithGoogleCall(@NonNull AuthRequestDto request) {
            return new FailingCall<>(new IOException("Connection timed out"));
          }

          @NonNull
          @Override
          public Call<LogoutResponseDto> logoutCall(@Nullable String authHeader) {
            throw new UnsupportedOperationException();
          }

          @NonNull
          @Override
          public Call<UserDto> getMeCall(@NonNull String authHeader) {
            throw new UnsupportedOperationException();
          }

          @NonNull
          @Override
          public Call<UserPreferencesDto> getPreferencesCall(@NonNull String authHeader) {
            throw new UnsupportedOperationException();
          }

          @NonNull
          @Override
          public Call<UserPreferencesDto> updatePreferencesCall(
              @NonNull String authHeader, @NonNull PreferencesUpdateDto body) {
            throw new UnsupportedOperationException();
          }
        };

    AuthRepository repo = new AuthRepositoryImpl(failingApi, authPreferences, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean errorReceived = new AtomicBoolean(false);

    repo.loginWithGoogle(
        "bad-token",
        new ResultCallback<UserProfile>() {
          @Override
          public void onSuccess(UserProfile data) {
            latch.countDown();
          }

          @Override
          public void onError(Exception exception) {
            errorReceived.set(true);
            latch.countDown();
          }
        });

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertTrue(errorReceived.get());
    assertFalse(repo.isLoggedIn());
  }

  // --- Helpers ---
  private static class FakeCall<T> implements Call<T> {
    private final Response<T> response;

    FakeCall(Response<T> response) {
      this.response = response;
    }

    @NonNull
    @Override
    public Response<T> execute() {
      return response;
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
      callback.onResponse(this, response);
    }

    @Override
    public boolean isExecuted() {
      return true;
    }

    @Override
    public void cancel() {}

    @Override
    public boolean isCanceled() {
      return false;
    }

    @NonNull
    @Override
    public Call<T> clone() {
      return new FakeCall<>(response);
    }

    @NonNull
    @Override
    public Request request() {
      return new Request.Builder().url("http://localhost/").build();
    }

    @NonNull
    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }
  }

  private static class FailingCall<T> implements Call<T> {
    private final IOException exception;

    FailingCall(IOException exception) {
      this.exception = exception;
    }

    @NonNull
    @Override
    public Response<T> execute() throws IOException {
      throw exception;
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
      callback.onFailure(this, exception);
    }

    @Override
    public boolean isExecuted() {
      return true;
    }

    @Override
    public void cancel() {}

    @Override
    public boolean isCanceled() {
      return false;
    }

    @NonNull
    @Override
    public Call<T> clone() {
      return new FailingCall<>(exception);
    }

    @NonNull
    @Override
    public Request request() {
      return new Request.Builder().url("http://localhost/").build();
    }

    @NonNull
    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }
  }

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
          map.remove(key);
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
