package com.example.newsagnet.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.AuthPreferences;
import com.example.newsagnet.model.AuthSession;
import com.example.newsagnet.model.UserProfile;
import com.example.newsagnet.network.ApiClient;
import com.example.newsagnet.network.AuthApiService;
import com.example.newsagnet.network.dto.AuthRequestDto;
import com.example.newsagnet.network.dto.AuthResponseDto;
import com.example.newsagnet.network.dto.LogoutResponseDto;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Concrete implementation of {@link AuthRepository} coordinating network authentication and
 * session persistence via {@link AppExecutors}.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Class & Interface Implementation) and Ch7 (ExecutorService &
 * Handler).
 */
public class AuthRepositoryImpl implements AuthRepository {

  private static volatile AuthRepository instance;

  private final AuthApiService apiService;
  private final AuthPreferences authPreferences;
  private final AppExecutors appExecutors;
  private final Set<AuthListener> listeners = new CopyOnWriteArraySet<>();

  public AuthRepositoryImpl(@NonNull Context context) {
    this(
        ApiClient.INSTANCE.getAuthApiService(),
        new AuthPreferences(context),
        AppExecutors.getInstance());
  }

  public AuthRepositoryImpl(
      @NonNull AuthApiService apiService,
      @NonNull AuthPreferences authPreferences,
      @NonNull AppExecutors appExecutors) {
    this.apiService = Objects.requireNonNull(apiService);
    this.authPreferences = Objects.requireNonNull(authPreferences);
    this.appExecutors = Objects.requireNonNull(appExecutors);
  }

  @NonNull
  public static AuthRepository getInstance(@NonNull Context context) {
    if (instance == null) {
      synchronized (AuthRepositoryImpl.class) {
        if (instance == null) {
          instance = new AuthRepositoryImpl(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  public static AuthRepository replaceForTesting(@Nullable AuthRepository replacement) {
    synchronized (AuthRepositoryImpl.class) {
      AuthRepository previous = instance;
      instance = replacement;
      return previous;
    }
  }

  @NonNull
  @Override
  public UserProfile getCurrentUser() {
    return authPreferences.getUserProfile();
  }

  @Override
  public boolean isLoggedIn() {
    return authPreferences.isLoggedIn();
  }

  @Override
  public void loginWithGoogle(
      @NonNull String idToken, @NonNull ResultCallback<UserProfile> callback) {
    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                AuthRequestDto requestDto = new AuthRequestDto(idToken);
                Call<AuthResponseDto> call = apiService.loginWithGoogleCall(requestDto);
                Response<AuthResponseDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                  AuthSession session = response.body().toDomain();
                  authPreferences.saveSession(session);
                  UserProfile profile = session.getUserProfile();

                  appExecutors
                      .mainThread()
                      .execute(
                          () -> {
                            notifyListeners(profile);
                            callback.onSuccess(profile);
                          });
                } else {
                  String errMsg =
                      "Authentication failed: HTTP "
                          + response.code()
                          + " "
                          + response.message();
                  appExecutors
                      .mainThread()
                      .execute(() -> callback.onError(new IOException(errMsg)));
                }
              } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
              }
            });
  }

  @Override
  public void logout(@NonNull ResultCallback<Void> callback) {
    AuthSession session = authPreferences.getSession();
    String authHeader = session != null ? session.getAuthorizationHeader() : null;

    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                if (authHeader != null) {
                  Call<LogoutResponseDto> call = apiService.logoutCall(authHeader);
                  call.execute();
                }
              } catch (Exception ignored) {
                // Best effort remote revocation
              } finally {
                authPreferences.clearSession();
                UserProfile guest = UserProfile.createGuest();
                appExecutors
                    .mainThread()
                    .execute(
                        () -> {
                          notifyListeners(guest);
                          callback.onSuccess(null);
                        });
              }
            });
  }

  @Override
  public void addAuthListener(@NonNull AuthListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeAuthListener(@NonNull AuthListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners(@NonNull UserProfile profile) {
    for (AuthListener listener : listeners) {
      listener.onAuthChanged(profile);
    }
  }
}
