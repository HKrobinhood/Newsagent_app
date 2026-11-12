package com.example.newsagnet.repository;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.AuthPreferences;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.data.prefs.UserPreferencesRepository;
import com.example.newsagnet.model.AuthSession;
import com.example.newsagnet.network.ApiClient;
import com.example.newsagnet.network.AuthApiService;
import com.example.newsagnet.network.dto.PreferencesUpdateDto;
import com.example.newsagnet.network.dto.UserPreferencesDto;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Concrete implementation of {@link RemotePreferencesRepository} coordinating cloud sync and
 * Last-Write-Wins conflict reconciliation.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Interface Implementation) and Ch7 (ExecutorService Concurrency).
 */
public class RemotePreferencesRepositoryImpl implements RemotePreferencesRepository {

  private static final String TAG = "RemotePreferencesRepo";
  private static volatile RemotePreferencesRepository instance;

  private final AuthPreferences authPreferences;
  private final UserPreferencesRepository localPreferencesRepository;
  private final AuthApiService authApiService;
  private final AppExecutors appExecutors;

  private volatile boolean isSyncing = false;

  public RemotePreferencesRepositoryImpl(
      @NonNull AuthPreferences authPreferences,
      @NonNull UserPreferencesRepository localPreferencesRepository,
      @NonNull AuthApiService authApiService,
      @NonNull AppExecutors appExecutors) {
    this.authPreferences = Objects.requireNonNull(authPreferences);
    this.localPreferencesRepository = Objects.requireNonNull(localPreferencesRepository);
    this.authApiService = Objects.requireNonNull(authApiService);
    this.appExecutors = Objects.requireNonNull(appExecutors);

    // Automatically push local changes to cloud if user is signed in
    this.localPreferencesRepository.addOnPreferencesChangedListener(
        updatedPreferences -> {
          if (!isSyncing && this.authPreferences.isLoggedIn()) {
            syncToRemote(updatedPreferences, null);
          }
        });
  }

  @NonNull
  public static RemotePreferencesRepository getInstance(@NonNull Context context) {
    if (instance == null) {
      synchronized (RemotePreferencesRepositoryImpl.class) {
        if (instance == null) {
          Context appContext = context.getApplicationContext();
          instance =
              new RemotePreferencesRepositoryImpl(
                  AuthPreferences.getInstance(appContext),
                  LocalPreferencesRepository.getInstance(appContext),
                  ApiClient.INSTANCE.getAuthApiService(),
                  AppExecutors.getInstance());
        }
      }
    }
    return instance;
  }

  @VisibleForTesting
  public static RemotePreferencesRepository replaceForTesting(
      @Nullable RemotePreferencesRepository replacement) {
    synchronized (RemotePreferencesRepositoryImpl.class) {
      RemotePreferencesRepository previous = instance;
      instance = replacement;
      return previous;
    }
  }

  @Override
  public void syncFromRemote(@NonNull ResultCallback<UserPreferences> callback) {
    if (!authPreferences.isLoggedIn()) {
      logD(TAG, "syncFromRemote: User is Guest; using local preferences without network call");
      deliverSuccess(localPreferencesRepository.getPreferences(), callback);
      return;
    }

    AuthSession session = authPreferences.getSession();
    if (session == null) {
      deliverSuccess(localPreferencesRepository.getPreferences(), callback);
      return;
    }

    String authHeader = session.getAuthorizationHeader();
    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                Call<UserPreferencesDto> call = authApiService.getPreferencesCall(authHeader);
                Response<UserPreferencesDto> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                  UserPreferencesDto dto = response.body();
                  long remoteTimestamp = parseIsoTimestamp(dto.getUpdatedAt());

                  isSyncing = true;
                  try {
                    localPreferencesRepository.applyRemotePreferences(
                        dto.getEnabledSourceIds(),
                        (float) dto.getFontScale(),
                        dto.getSchemaVersion(),
                        remoteTimestamp);
                  } finally {
                    isSyncing = false;
                  }

                  UserPreferences updated = localPreferencesRepository.getPreferences();
                  logD(TAG, "syncFromRemote: Successfully pulled remote preferences: " + updated);
                  deliverSuccess(updated, callback);
                } else {
                  String errMsg =
                      "Remote sync failed: HTTP " + response.code() + " " + response.message();
                  logW(TAG, errMsg);
                  deliverError(new IOException(errMsg), callback);
                }
              } catch (Exception e) {
                logW(TAG, "syncFromRemote network error; falling back to local preferences", e);
                deliverError(e, callback);
              }
            });
  }

  @Override
  public void syncToRemote(
      @NonNull UserPreferences localPreferences,
      @Nullable ResultCallback<UserPreferences> callback) {
    if (!authPreferences.isLoggedIn() || isSyncing) {
      if (callback != null) {
        deliverSuccess(localPreferences, callback);
      }
      return;
    }

    AuthSession session = authPreferences.getSession();
    if (session == null) {
      if (callback != null) {
        deliverSuccess(localPreferences, callback);
      }
      return;
    }

    String authHeader = session.getAuthorizationHeader();
    PreferencesUpdateDto updateDto =
        new PreferencesUpdateDto(
            new ArrayList<>(localPreferences.getEnabledSourceIds()),
            localPreferences.getFontScale(),
            localPreferences.getSettingsVersion());

    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                Call<UserPreferencesDto> call =
                    authApiService.updatePreferencesCall(authHeader, updateDto);
                Response<UserPreferencesDto> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                  logD(TAG, "syncToRemote: Successfully uploaded local preferences to cloud");
                  if (callback != null) {
                    deliverSuccess(localPreferences, callback);
                  }
                } else {
                  String errMsg =
                      "syncToRemote failed: HTTP " + response.code() + " " + response.message();
                  logW(TAG, errMsg);
                  if (callback != null) {
                    deliverError(new IOException(errMsg), callback);
                  }
                }
              } catch (Exception e) {
                logW(TAG, "syncToRemote network error; retained local preference", e);
                if (callback != null) {
                  deliverError(e, callback);
                }
              }
            });
  }

  @Override
  public void reconcileOnLogin(@NonNull ResultCallback<UserPreferences> callback) {
    if (!authPreferences.isLoggedIn()) {
      deliverSuccess(localPreferencesRepository.getPreferences(), callback);
      return;
    }

    AuthSession session = authPreferences.getSession();
    if (session == null) {
      deliverSuccess(localPreferencesRepository.getPreferences(), callback);
      return;
    }

    String authHeader = session.getAuthorizationHeader();
    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                Call<UserPreferencesDto> call = authApiService.getPreferencesCall(authHeader);
                Response<UserPreferencesDto> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                  UserPreferencesDto remoteDto = response.body();
                  long remoteTimestamp = parseIsoTimestamp(remoteDto.getUpdatedAt());
                  UserPreferences localSnapshot = localPreferencesRepository.getPreferences();

                  // Reconcile strategy:
                  // 1. If remote has customized preferences (or remote is newer), remote overwrites local
                  // 2. If remote is pristine default but local has user customizations, upload local to remote
                  boolean localIsCustomized =
                      localSnapshot.getFontScale() != 1.00f
                          || localSnapshot.getEnabledSourceIds().size() != 4;
                  boolean remoteIsDefault =
                      remoteDto.getFontScale() == 1.00
                          && remoteDto.getEnabledSourceIds().size() == 4;

                  if (localIsCustomized && remoteIsDefault) {
                    logD(TAG, "reconcileOnLogin: Uploading local custom preferences to new account");
                    PreferencesUpdateDto pushPayload =
                        new PreferencesUpdateDto(
                            new ArrayList<>(localSnapshot.getEnabledSourceIds()),
                            localSnapshot.getFontScale(),
                            localSnapshot.getSettingsVersion());
                    authApiService.updatePreferencesCall(authHeader, pushPayload).execute();
                    deliverSuccess(localSnapshot, callback);
                  } else {
                    logD(TAG, "reconcileOnLogin: Applying remote cloud preferences to local device");
                    isSyncing = true;
                    try {
                      localPreferencesRepository.applyRemotePreferences(
                          remoteDto.getEnabledSourceIds(),
                          (float) remoteDto.getFontScale(),
                          remoteDto.getSchemaVersion(),
                          remoteTimestamp);
                    } finally {
                      isSyncing = false;
                    }
                    deliverSuccess(localPreferencesRepository.getPreferences(), callback);
                  }
                } else {
                  logW(TAG, "reconcileOnLogin: Failed to fetch remote preferences; using local");
                  deliverSuccess(localPreferencesRepository.getPreferences(), callback);
                }
              } catch (Exception e) {
                logW(TAG, "reconcileOnLogin: Network error during login sync; using local", e);
                deliverSuccess(localPreferencesRepository.getPreferences(), callback);
              }
            });
  }

  private void deliverSuccess(
      @NonNull UserPreferences preferences, @NonNull ResultCallback<UserPreferences> callback) {
    appExecutors.mainThread().execute(() -> callback.onSuccess(preferences));
  }

  private void deliverError(@NonNull Exception error, @NonNull ResultCallback<UserPreferences> callback) {
    appExecutors.mainThread().execute(() -> callback.onError(error));
  }

  private static void logD(String tag, String message) {
    try {
      Log.d(tag, message);
    } catch (Throwable ignored) {
    }
  }

  private static void logW(String tag, String message) {
    try {
      Log.w(tag, message);
    } catch (Throwable ignored) {
    }
  }

  private static void logW(String tag, String message, Throwable t) {
    try {
      Log.w(tag, message, t);
    } catch (Throwable ignored) {
    }
  }

  private static long parseIsoTimestamp(@Nullable String isoString) {
    if (isoString == null || isoString.trim().isEmpty()) {
      return System.currentTimeMillis();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        return java.time.Instant.parse(isoString).toEpochMilli();
      } catch (Exception ignored) {
      }
    }
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date parsed = sdf.parse(isoString);
      return parsed != null ? parsed.getTime() : System.currentTimeMillis();
    } catch (Exception ignored) {
      return System.currentTimeMillis();
    }
  }
}
