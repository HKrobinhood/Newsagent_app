package com.example.newsagnet.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.data.security.AiProviderOption;
import com.example.newsagnet.data.security.SecureApiKeyStore;
import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.KeyTestResult;
import com.example.newsagnet.model.MessageRole;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.network.ApiClient;
import com.example.newsagnet.network.ChatApiService;
import com.example.newsagnet.network.dto.ChatRequestDto;
import com.example.newsagnet.network.dto.ChatResponseDto;
import com.example.newsagnet.network.dto.KeyTestRequestDto;
import com.example.newsagnet.network.dto.KeyTestResponseDto;
import com.example.newsagnet.network.dto.NewsSourceRefDto;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Concrete Java implementation of {@link JavaChatRepository} executing on background thread pool
 * and delivering AI responses to the UI thread via {@link AppExecutors}.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Class & Interface Implementation) and Ch7 (ExecutorService &
 * Handler).
 */
public class JavaChatRepositoryImpl implements JavaChatRepository {

  private final ChatApiService apiService;
  private final AppExecutors appExecutors;
  @Nullable private final SecureApiKeyStore secureApiKeyStore;
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

  public JavaChatRepositoryImpl() {
    this(ApiClient.INSTANCE.getChatApiService(), AppExecutors.getInstance(), null);
  }

  public JavaChatRepositoryImpl(@NonNull Context context) {
    this(
        ApiClient.INSTANCE.getChatApiService(),
        AppExecutors.getInstance(),
        SecureApiKeyStore.getInstance(context));
  }

  public JavaChatRepositoryImpl(
      @NonNull ChatApiService apiService, @NonNull AppExecutors appExecutors) {
    this(apiService, appExecutors, null);
  }

  public JavaChatRepositoryImpl(
      @NonNull ChatApiService apiService,
      @NonNull AppExecutors appExecutors,
      @Nullable SecureApiKeyStore secureApiKeyStore) {
    this.apiService = apiService;
    this.appExecutors = appExecutors;
    this.secureApiKeyStore = secureApiKeyStore;
  }

  @Override
  public void sendMessage(
      @NonNull String message,
      @Nullable String sessionId,
      @NonNull String mode,
      @NonNull ResultCallback<AgentChatResult> callback) {
    sendMessage(message, sessionId, mode, null, callback);
  }

  @Override
  public void sendMessage(
      @NonNull String message,
      @Nullable String sessionId,
      @NonNull String mode,
      @Nullable Collection<String> allowedSourceIds,
      @NonNull ResultCallback<AgentChatResult> callback) {

    List<String> sourceIdList =
        allowedSourceIds == null ? null : new ArrayList<>(allowedSourceIds);

    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                ChatRequestDto requestDto =
                    new ChatRequestDto(message, sessionId, mode, sourceIdList);

                String providerHeader = null;
                String apiKeyHeader = null;
                if (secureApiKeyStore != null) {
                  AiProviderOption selected = secureApiKeyStore.getSelectedProvider();
                  String activeKey = secureApiKeyStore.getActiveApiKeyForRequest();
                  if (selected != AiProviderOption.SERVER_DEFAULT
                      && activeKey != null
                      && !activeKey.trim().isEmpty()) {
                    providerHeader = selected.getId();
                    apiKeyHeader = activeKey.trim();
                  }
                }

                Call<ChatResponseDto> call =
                    (providerHeader != null && apiKeyHeader != null)
                        ? apiService.sendMessageWithKeyCall(
                            providerHeader, apiKeyHeader, requestDto)
                        : apiService.sendMessageCall(requestDto);
                Response<ChatResponseDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                  ChatResponseDto dto = response.body();

                  List<NewsArticle> sources = new ArrayList<>();
                  if (dto.getSources() != null) {
                    for (NewsSourceRefDto ref : dto.getSources()) {
                      sources.add(ref.toDomain());
                    }
                  }

                  if (sourceIdList != null) {
                    UserPreferences filterPrefs =
                        new UserPreferences(
                            new LinkedHashSet<>(sourceIdList),
                            UserPreferences.DEFAULT_FONT_SCALE,
                            UserPreferences.DEFAULT_SETTINGS_VERSION,
                            0L);
                    sources = filterPrefs.filterArticles(sources);
                  }

                  ChatMessage chatMessage =
                      new ChatMessage.Builder()
                          .setRole(MessageRole.ASSISTANT)
                          .setContent(dto.getAnswer())
                          .setTimestamp(timeFormat.format(new Date()))
                          .setSources(sources)
                          .setModeUsed(dto.getModeUsed())
                          .build();

                  AgentChatResult result =
                      new AgentChatResult(dto.getSessionId(), chatMessage, dto.getConfidence());

                  appExecutors.mainThread().execute(() -> callback.onSuccess(result));
                } else {
                  String errorMsg = "HTTP " + response.code() + ": " + response.message();
                  appExecutors
                      .mainThread()
                      .execute(() -> callback.onError(new IOException(errorMsg)));
                }
              } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
              }
            });
  }

  @Override
  public void testApiKeyConnection(
      @NonNull AiProviderOption provider,
      @Nullable String apiKey,
      @NonNull ResultCallback<KeyTestResult> callback) {
    appExecutors
        .networkIO()
        .execute(
            () -> {
              try {
                String cleanKey =
                    (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey.trim() : null;
                KeyTestRequestDto requestDto = new KeyTestRequestDto(provider.getId());
                Call<KeyTestResponseDto> call =
                    apiService.testConnectionCall(cleanKey, requestDto);
                Response<KeyTestResponseDto> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                  KeyTestResult result = response.body().toDomain();
                  appExecutors.mainThread().execute(() -> callback.onSuccess(result));
                } else if (response.code() == 429) {
                  KeyTestResult rateLimited =
                      new KeyTestResult(
                          KeyTestResult.STATUS_PROVIDER_ERROR,
                          provider.getId(),
                          "Too many failed connection attempts. Please wait a minute and retry.");
                  appExecutors.mainThread().execute(() -> callback.onSuccess(rateLimited));
                } else {
                  KeyTestResult errorResult =
                      new KeyTestResult(
                          KeyTestResult.STATUS_PROVIDER_ERROR,
                          provider.getId(),
                          "Backend verification returned HTTP " + response.code() + ".");
                  appExecutors.mainThread().execute(() -> callback.onSuccess(errorResult));
                }
              } catch (SocketTimeoutException timeoutEx) {
                KeyTestResult timeoutResult =
                    new KeyTestResult(
                        KeyTestResult.STATUS_TIMEOUT,
                        provider.getId(),
                        "Connection timed out while verifying API key.");
                appExecutors.mainThread().execute(() -> callback.onSuccess(timeoutResult));
              } catch (Exception ex) {
                KeyTestResult offlineResult =
                    new KeyTestResult(
                        KeyTestResult.STATUS_PROVIDER_ERROR,
                        provider.getId(),
                        "Network unavailable or backend unreachable.");
                appExecutors.mainThread().execute(() -> callback.onSuccess(offlineResult));
              }
            });
  }
}
