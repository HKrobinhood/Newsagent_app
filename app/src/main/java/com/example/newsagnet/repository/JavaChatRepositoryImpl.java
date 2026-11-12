package com.example.newsagnet.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.MessageRole;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.network.ApiClient;
import com.example.newsagnet.network.ChatApiService;
import com.example.newsagnet.network.dto.ChatRequestDto;
import com.example.newsagnet.network.dto.ChatResponseDto;
import com.example.newsagnet.network.dto.NewsSourceRefDto;
import java.io.IOException;
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
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

  public JavaChatRepositoryImpl() {
    this(ApiClient.INSTANCE.getChatApiService(), AppExecutors.getInstance());
  }

  public JavaChatRepositoryImpl(
      @NonNull ChatApiService apiService, @NonNull AppExecutors appExecutors) {
    this.apiService = apiService;
    this.appExecutors = appExecutors;
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
                Call<ChatResponseDto> call = apiService.sendMessageCall(requestDto);
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
}
