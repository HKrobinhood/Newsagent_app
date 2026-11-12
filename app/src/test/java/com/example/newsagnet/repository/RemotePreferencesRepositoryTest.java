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
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.prefs.NewsPreferences;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.model.AuthSession;
import com.example.newsagnet.model.UserProfile;
import com.example.newsagnet.network.AuthApiService;
import com.example.newsagnet.network.dto.AuthRequestDto;
import com.example.newsagnet.network.dto.AuthResponseDto;
import com.example.newsagnet.network.dto.LogoutResponseDto;
import com.example.newsagnet.network.dto.PreferencesUpdateDto;
import com.example.newsagnet.network.dto.UserDto;
import com.example.newsagnet.network.dto.UserPreferencesDto;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * Unit tests for {@link RemotePreferencesRepositoryImpl} verifying cloud preference synchronization,
 * Last-Write-Wins reconciliation, and offline resilience.
 */
public class RemotePreferencesRepositoryTest {

  private ExecutorService testService;
  private AppExecutors testExecutors;
  private AuthPreferences authPreferences;
  private LocalPreferencesRepository localRepository;

  @Before
  public void setUp() {
    testService = Executors.newSingleThreadExecutor();
    Executor direct = Runnable::run;
    testExecutors = new AppExecutors(testService, testService, direct);
    authPreferences = new AuthPreferences(new FakeSharedPreferences());
    localRepository = new LocalPreferencesRepository(new NewsPreferences(new FakeSharedPreferences()));
  }

  @After
  public void tearDown() {
    if (testService != null) {
      testService.shutdown();
    }
  }

  @Test
  public void syncFromRemote_whenGuest_returnsLocalWithoutNetworkCall() throws Exception {
    AtomicBoolean apiCalled = new AtomicBoolean(false);
    AuthApiService fakeApi =
        new FakeAuthApiService() {
          @Override
          public Call<UserPreferencesDto> getPreferencesCall(String authHeader) {
            apiCalled.set(true);
            return new ImmediateCall<>(null);
          }
        };

    RemotePreferencesRepository repo =
        new RemotePreferencesRepositoryImpl(
            authPreferences, localRepository, fakeApi, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<UserPreferences> resultRef = new AtomicReference<>();
    repo.syncFromRemote(
        new ResultCallback<UserPreferences>() {
          @Override
          public void onSuccess(@NonNull UserPreferences preferences) {
            resultRef.set(preferences);
            latch.countDown();
          }

          @Override
          public void onError(@NonNull Exception exception) {
            latch.countDown();
          }
        });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertFalse(apiCalled.get());
    assertNotNull(resultRef.get());
    assertEquals(1.00f, resultRef.get().getFontScale(), 0.001f);
  }

  @Test
  public void syncFromRemote_whenLoggedIn_pullsRemoteAndAppliesToLocal() throws Exception {
    loginFakeUser("test-access-token");

    UserPreferencesDto remoteDto =
        new UserPreferencesDto(
            "user-123",
            Arrays.asList("bbc", "fox"),
            1.30f,
            1,
            "2026-11-12T05:00:00Z");

    AuthApiService fakeApi =
        new FakeAuthApiService() {
          @Override
          public Call<UserPreferencesDto> getPreferencesCall(String authHeader) {
            assertEquals("Bearer test-access-token", authHeader);
            return new ImmediateCall<>(remoteDto);
          }
        };

    RemotePreferencesRepository repo =
        new RemotePreferencesRepositoryImpl(
            authPreferences, localRepository, fakeApi, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<UserPreferences> resultRef = new AtomicReference<>();
    repo.syncFromRemote(
        new ResultCallback<UserPreferences>() {
          @Override
          public void onSuccess(@NonNull UserPreferences preferences) {
            resultRef.set(preferences);
            latch.countDown();
          }

          @Override
          public void onError(@NonNull Exception exception) {
            latch.countDown();
          }
        });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertNotNull(resultRef.get());
    assertEquals(1.30f, localRepository.getFontScale(), 0.001f);
    assertEquals(2, localRepository.getEnabledSourceIds().size());
    assertTrue(localRepository.getEnabledSourceIds().contains("bbc"));
    assertTrue(localRepository.getEnabledSourceIds().contains("fox"));
    assertFalse(localRepository.getEnabledSourceIds().contains("cnn"));
  }

  @Test
  public void syncToRemote_whenLoggedIn_pushesLocalPreferencesToBackend() throws Exception {
    loginFakeUser("token-for-push");

    AtomicReference<PreferencesUpdateDto> pushedPayload = new AtomicReference<>();
    AuthApiService fakeApi =
        new FakeAuthApiService() {
          @Override
          public Call<UserPreferencesDto> updatePreferencesCall(
              String authHeader, PreferencesUpdateDto body) {
            assertEquals("Bearer token-for-push", authHeader);
            pushedPayload.set(body);
            UserPreferencesDto resp =
                new UserPreferencesDto("user-123", body.getEnabledSourceIds(), body.getFontScale(), 1, "2026-11-12T06:00:00Z");
            return new ImmediateCall<>(resp);
          }
        };

    RemotePreferencesRepository repo =
        new RemotePreferencesRepositoryImpl(
            authPreferences, localRepository, fakeApi, testExecutors);

    UserPreferences customLocal =
        new UserPreferences(Collections.singleton("aljazeera"), 1.15f, 1, System.currentTimeMillis());

    CountDownLatch latch = new CountDownLatch(1);
    repo.syncToRemote(
        customLocal,
        new ResultCallback<UserPreferences>() {
          @Override
          public void onSuccess(@NonNull UserPreferences preferences) {
            latch.countDown();
          }

          @Override
          public void onError(@NonNull Exception exception) {
            latch.countDown();
          }
        });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertNotNull(pushedPayload.get());
    assertEquals(1.15f, (float) pushedPayload.get().getFontScale(), 0.001f);
    assertEquals(Collections.singletonList("aljazeera"), pushedPayload.get().getEnabledSourceIds());
  }

  @Test
  public void reconcileOnLogin_whenRemoteHasCustomSettings_overwritesLocal() throws Exception {
    loginFakeUser("token-reconcile-remote-wins");

    // Local is default
    assertEquals(1.00f, localRepository.getFontScale(), 0.001f);

    UserPreferencesDto remoteDto =
        new UserPreferencesDto(
            "user-123",
            Collections.singletonList("cnn"),
            0.90f,
            1,
            "2026-11-12T07:00:00Z");

    AuthApiService fakeApi =
        new FakeAuthApiService() {
          @Override
          public Call<UserPreferencesDto> getPreferencesCall(String authHeader) {
            return new ImmediateCall<>(remoteDto);
          }
        };

    RemotePreferencesRepository repo =
        new RemotePreferencesRepositoryImpl(
            authPreferences, localRepository, fakeApi, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    repo.reconcileOnLogin(
        new ResultCallback<UserPreferences>() {
          @Override
          public void onSuccess(@NonNull UserPreferences preferences) {
            latch.countDown();
          }

          @Override
          public void onError(@NonNull Exception exception) {
            latch.countDown();
          }
        });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertEquals(0.90f, localRepository.getFontScale(), 0.001f);
    assertEquals(Collections.singleton("cnn"), localRepository.getEnabledSourceIds());
  }

  @Test
  public void syncFromRemote_networkFailure_fallsBackGracefullyToLocal() throws Exception {
    loginFakeUser("token-failure");

    AuthApiService fakeApi =
        new FakeAuthApiService() {
          @Override
          public Call<UserPreferencesDto> getPreferencesCall(String authHeader) {
            return new FailingCall<>(new IOException("503 Service Unavailable"));
          }
        };

    RemotePreferencesRepository repo =
        new RemotePreferencesRepositoryImpl(
            authPreferences, localRepository, fakeApi, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean errorDelivered = new AtomicBoolean(false);
    repo.syncFromRemote(
        new ResultCallback<UserPreferences>() {
          @Override
          public void onSuccess(@NonNull UserPreferences preferences) {
            latch.countDown();
          }

          @Override
          public void onError(@NonNull Exception exception) {
            errorDelivered.set(true);
            latch.countDown();
          }
        });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertTrue(errorDelivered.get());
    // Local preferences remain intact
    assertEquals(1.00f, localRepository.getFontScale(), 0.001f);
  }

  private void loginFakeUser(String token) {
    UserProfile user = new UserProfile("user-123", "sync.user@example.com", "Sync User", "google");
    AuthSession session = new AuthSession(token, "Bearer", "2026-12-31T23:59:59Z", user);
    authPreferences.saveSession(session);
  }

  private static class FakeAuthApiService implements AuthApiService {
    @Override
    public Call<AuthResponseDto> loginWithGoogleCall(AuthRequestDto request) {
      return new ImmediateCall<>(null);
    }

    @Override
    public Call<LogoutResponseDto> logoutCall(String authHeader) {
      return new ImmediateCall<>(new LogoutResponseDto("success", "Logged out"));
    }

    @Override
    public Call<UserDto> getMeCall(String authHeader) {
      return new ImmediateCall<>(new UserDto("u1", "u@ex.com", "U", "google"));
    }

    @Override
    public Call<UserPreferencesDto> getPreferencesCall(String authHeader) {
      return new ImmediateCall<>(null);
    }

    @Override
    public Call<UserPreferencesDto> updatePreferencesCall(
        String authHeader, PreferencesUpdateDto body) {
      return new ImmediateCall<>(null);
    }
  }

  private static class ImmediateCall<T> implements Call<T> {
    private final T responseBody;

    ImmediateCall(T responseBody) {
      this.responseBody = responseBody;
    }

    @NonNull
    @Override
    public Response<T> execute() {
      return Response.success(responseBody);
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
      callback.onResponse(this, Response.success(responseBody));
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
      return new ImmediateCall<>(responseBody);
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
    private final Throwable exception;

    FailingCall(Throwable exception) {
      this.exception = exception;
    }

    @NonNull
    @Override
    public Response<T> execute() throws IOException {
      if (exception instanceof IOException) {
        throw (IOException) exception;
      }
      throw new IOException(exception);
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
      Object v = map.get(key);
      return v instanceof Integer ? (Integer) v : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
      Object v = map.get(key);
      return v instanceof Long ? (Long) v : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
      Object v = map.get(key);
      return v instanceof Float ? (Float) v : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      Object v = map.get(key);
      return v instanceof Boolean ? (Boolean) v : defValue;
    }

    @Override
    public boolean contains(String key) {
      return map.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new FakeEditor(map);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}
  }

  private static class FakeEditor implements SharedPreferences.Editor {
    private final Map<String, Object> store;
    private final Map<String, Object> pending = new HashMap<>();

    FakeEditor(Map<String, Object> store) {
      this.store = store;
    }

    @Override
    public SharedPreferences.Editor putString(String key, @Nullable String value) {
      pending.put(key, value);
      return this;
    }

    @Override
    public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
      pending.put(key, values);
      return this;
    }

    @Override
    public SharedPreferences.Editor putInt(String key, int value) {
      pending.put(key, value);
      return this;
    }

    @Override
    public SharedPreferences.Editor putLong(String key, long value) {
      pending.put(key, value);
      return this;
    }

    @Override
    public SharedPreferences.Editor putFloat(String key, float value) {
      pending.put(key, value);
      return this;
    }

    @Override
    public SharedPreferences.Editor putBoolean(String key, boolean value) {
      pending.put(key, value);
      return this;
    }

    @Override
    public SharedPreferences.Editor remove(String key) {
      pending.put(key, this);
      return this;
    }

    @Override
    public SharedPreferences.Editor clear() {
      store.clear();
      pending.clear();
      return this;
    }

    @Override
    public boolean commit() {
      apply();
      return true;
    }

    @Override
    public void apply() {
      for (Map.Entry<String, Object> entry : pending.entrySet()) {
        if (entry.getValue() == this) {
          store.remove(entry.getKey());
        } else {
          store.put(entry.getKey(), entry.getValue());
        }
      }
      pending.clear();
    }
  }
}
