package com.example.newsagnet.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.security.AiProviderOption;
import com.example.newsagnet.model.KeyTestResult;
import com.example.newsagnet.model.MessageRole;
import com.example.newsagnet.network.ChatApiService;
import com.example.newsagnet.network.dto.ChatRequestDto;
import com.example.newsagnet.network.dto.ChatResponseDto;
import com.example.newsagnet.network.dto.KeyTestRequestDto;
import com.example.newsagnet.network.dto.KeyTestResponseDto;
import com.example.newsagnet.network.dto.NewsSourceRefDto;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
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
 * Unit tests for JavaChatRepositoryImpl verifying background network execution and UI thread delivery.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Interface testing) and Ch7 (ExecutorService concurrency).
 */
public class JavaChatRepositoryTest {

  private ExecutorService testNetworkIO;
  private AppExecutors testExecutors;

  @Before
  public void setUp() {
    testNetworkIO = Executors.newFixedThreadPool(2);
    Executor directExecutor = Runnable::run;
    testExecutors = new AppExecutors(testNetworkIO, testNetworkIO, directExecutor);
  }

  @After
  public void tearDown() {
    testNetworkIO.shutdown();
  }

  @Test
  public void testSendMessage_successDeliversResult() throws InterruptedException {
    ChatResponseDto fakeResponse =
        new ChatResponseDto(
            "sess-123",
            "Renewable energy investments surged 20% globally.",
            "rag_search",
            Collections.singletonList(
                new NewsSourceRefDto(
                    "Global Clean Energy Surge",
                    "Reuters",
                    "https://reuters.com/clean-energy",
                    "2026-11-04T08:00:00Z",
                    "London")),
            0.95);

    ChatApiService fakeService =
        new ChatApiService() {
          @Override
          public Object sendMessage(
              ChatRequestDto request, kotlin.coroutines.Continuation<? super ChatResponseDto> continuation) {
            return null;
          }

          @NonNull
          @Override
          public Call<ChatResponseDto> sendMessageCall(@NonNull ChatRequestDto request) {
            return new FakeCall<>(Response.success(fakeResponse));
          }
        };

    JavaChatRepository repository = new JavaChatRepositoryImpl(fakeService, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<AgentChatResult> resultRef = new AtomicReference<>();
    AtomicBoolean errorOccurred = new AtomicBoolean(false);

    repository.sendMessage(
        "Tell me about renewable energy",
        null,
        "auto",
        new ResultCallback<AgentChatResult>() {
          @Override
          public void onSuccess(AgentChatResult data) {
            resultRef.set(data);
            latch.countDown();
          }

          @Override
          public void onError(Exception exception) {
            errorOccurred.set(true);
            latch.countDown();
          }
        });

    boolean completed = latch.await(2, TimeUnit.SECONDS);
    assertTrue("Repository call should complete within timeout", completed);
    assertTrue("Should succeed without error", !errorOccurred.get());

    AgentChatResult result = resultRef.get();
    assertNotNull(result);
    assertEquals("sess-123", result.getSessionId());
    assertEquals(0.95, result.getConfidence(), 0.001);
    assertNotNull(result.getMessage());
    assertEquals(MessageRole.ASSISTANT, result.getMessage().getRole());
    assertEquals("Renewable energy investments surged 20% globally.", result.getMessage().getContent());
    assertEquals("rag_search", result.getMessage().getModeUsed());
    assertNotNull(result.getMessage().getSources());
    assertEquals(1, result.getMessage().getSources().size());
    assertEquals("Global Clean Energy Surge", result.getMessage().getSources().get(0).getTitle());
  }

  @Test
  public void testSendMessage_networkErrorDeliversErrorCallback() throws InterruptedException {
    ChatApiService fakeService =
        new ChatApiService() {
          @Override
          public Object sendMessage(
              ChatRequestDto request, kotlin.coroutines.Continuation<? super ChatResponseDto> continuation) {
            return null;
          }

          @NonNull
          @Override
          public Call<ChatResponseDto> sendMessageCall(@NonNull ChatRequestDto request) {
            return new FailingCall<>(new IOException("Connection timed out"));
          }
        };

    JavaChatRepository repository = new JavaChatRepositoryImpl(fakeService, testExecutors);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Exception> errorRef = new AtomicReference<>();

    repository.sendMessage(
        "Failing inquiry",
        null,
        "auto",
        new ResultCallback<AgentChatResult>() {
          @Override
          public void onSuccess(AgentChatResult data) {
            latch.countDown();
          }

          @Override
          public void onError(Exception exception) {
            errorRef.set(exception);
            latch.countDown();
          }
        });

    boolean completed = latch.await(2, TimeUnit.SECONDS);
    assertTrue("Repository call should complete within timeout", completed);
    assertNotNull(errorRef.get());
    assertTrue(errorRef.get() instanceof IOException);
    assertEquals("Connection timed out", errorRef.get().getMessage());
  }

  @Test
  public void testApiKeyConnection_mapsSuccessTimeoutAndOfflineSafely()
      throws InterruptedException {
    ChatApiService fakeService =
        new ChatApiService() {
          @Override
          public Object sendMessage(
              ChatRequestDto request,
              kotlin.coroutines.Continuation<? super ChatResponseDto> continuation) {
            return null;
          }

          @NonNull
          @Override
          public Call<ChatResponseDto> sendMessageCall(@NonNull ChatRequestDto request) {
            throw new UnsupportedOperationException();
          }

          @NonNull
          @Override
          public Call<KeyTestResponseDto> testConnectionCall(
              @Nullable String llmApiKey, @NonNull KeyTestRequestDto request) {
            if ("timeout-key".equals(llmApiKey)) {
              return new FailingCall<>(new SocketTimeoutException("Read timed out"));
            }
            if ("offline-key".equals(llmApiKey)) {
              return new FailingCall<>(new IOException("Network unreachable"));
            }
            return new FakeCall<>(
                Response.success(
                    new KeyTestResponseDto(
                        KeyTestResult.STATUS_SUCCESS,
                        request.getProvider(),
                        "Connected to GEMINI successfully.")));
          }
        };

    JavaChatRepository repository = new JavaChatRepositoryImpl(fakeService, testExecutors);

    // 1. Success case
    CountDownLatch latch1 = new CountDownLatch(1);
    AtomicReference<KeyTestResult> res1 = new AtomicReference<>();
    repository.testApiKeyConnection(
        AiProviderOption.GEMINI,
        "valid-key-12345678",
        new ResultCallback<KeyTestResult>() {
          @Override
          public void onSuccess(KeyTestResult data) {
            res1.set(data);
            latch1.countDown();
          }

          @Override
          public void onError(Exception e) {
            latch1.countDown();
          }
        });
    assertTrue(latch1.await(2, TimeUnit.SECONDS));
    assertEquals(KeyTestResult.STATUS_SUCCESS, res1.get().getStatus());

    // 2. Timeout case
    CountDownLatch latch2 = new CountDownLatch(1);
    AtomicReference<KeyTestResult> res2 = new AtomicReference<>();
    repository.testApiKeyConnection(
        AiProviderOption.GEMINI,
        "timeout-key",
        new ResultCallback<KeyTestResult>() {
          @Override
          public void onSuccess(KeyTestResult data) {
            res2.set(data);
            latch2.countDown();
          }

          @Override
          public void onError(Exception e) {
            latch2.countDown();
          }
        });
    assertTrue(latch2.await(2, TimeUnit.SECONDS));
    assertEquals(KeyTestResult.STATUS_TIMEOUT, res2.get().getStatus());

    // 3. Offline network failure case
    CountDownLatch latch3 = new CountDownLatch(1);
    AtomicReference<KeyTestResult> res3 = new AtomicReference<>();
    repository.testApiKeyConnection(
        AiProviderOption.OPENAI,
        "offline-key",
        new ResultCallback<KeyTestResult>() {
          @Override
          public void onSuccess(KeyTestResult data) {
            res3.set(data);
            latch3.countDown();
          }

          @Override
          public void onError(Exception e) {
            latch3.countDown();
          }
        });
    assertTrue(latch3.await(2, TimeUnit.SECONDS));
    assertEquals(KeyTestResult.STATUS_PROVIDER_ERROR, res3.get().getStatus());
  }

  // --- Fake Call helpers for Retrofit Call testing ---

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
}
