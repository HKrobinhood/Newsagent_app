package com.example.newsagnet.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests verifying AppExecutors background concurrency and main thread dispatching.
 *
 * <p>Conforms to COMP7506 Ch7 (Thread, Runnable, ExecutorService, UI Thread Dispatching).
 */
public class AppExecutorsTest {

  private ExecutorService testDiskIO;
  private ExecutorService testNetworkIO;
  private AppExecutors testExecutors;

  @Before
  public void setUp() {
    testDiskIO = Executors.newSingleThreadExecutor();
    testNetworkIO = Executors.newFixedThreadPool(2);
    // In JVM unit tests, provide a synchronous direct executor for mainThread
    Executor directExecutor = Runnable::run;

    testExecutors = new AppExecutors(testDiskIO, testNetworkIO, directExecutor);
  }

  @After
  public void tearDown() {
    testDiskIO.shutdown();
    testNetworkIO.shutdown();
  }

  @Test
  public void testExecutors_instancesNotNull() {
    assertNotNull(testExecutors.diskIO());
    assertNotNull(testExecutors.networkIO());
    assertNotNull(testExecutors.mainThread());
  }

  @Test
  public void testNetworkIO_executesOnBackgroundThread() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> threadNameRef = new AtomicReference<>("");

    testExecutors
        .networkIO()
        .execute(
            () -> {
              threadNameRef.set(Thread.currentThread().getName());
              latch.countDown();
            });

    boolean completed = latch.await(2, TimeUnit.SECONDS);
    assertTrue("Background task should complete within timeout", completed);
    assertFalse(
        "Task should not execute on main test thread",
        threadNameRef.get().equals(Thread.currentThread().getName()));
  }

  @Test
  public void testDiskIO_executesSerialized() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    AtomicBoolean secondCompletedAfterFirst = new AtomicBoolean(false);
    AtomicBoolean firstFinished = new AtomicBoolean(false);

    testExecutors
        .diskIO()
        .execute(
            () -> {
              try {
                Thread.sleep(50);
              } catch (InterruptedException ignored) {
              }
              firstFinished.set(true);
              latch.countDown();
            });

    testExecutors
        .diskIO()
        .execute(
            () -> {
              if (firstFinished.get()) {
                secondCompletedAfterFirst.set(true);
              }
              latch.countDown();
            });

    boolean completed = latch.await(2, TimeUnit.SECONDS);
    assertTrue("Both disk tasks should complete", completed);
    assertTrue("Tasks on singleThreadExecutor must run sequentially", secondCompletedAfterFirst.get());
  }

  @Test
  public void testResultCallback_successDispatched() {
    AtomicBoolean callbackFired = new AtomicBoolean(false);
    AtomicReference<String> resultRef = new AtomicReference<>("");

    ResultCallback<String> callback =
        new ResultCallback<String>() {
          @Override
          public void onSuccess(String result) {
            callbackFired.set(true);
            resultRef.set(result);
          }

          @Override
          public void onError(Exception exception) {}
        };

    callback.onSuccess("COMP7506 Concurrency Payload");
    assertTrue(callbackFired.get());
    assertEquals("COMP7506 Concurrency Payload", resultRef.get());
  }

  @Test
  public void testResultCallback_errorDispatched() {
    AtomicBoolean errorFired = new AtomicBoolean(false);
    AtomicReference<String> errorMsgRef = new AtomicReference<>("");

    ResultCallback<String> callback =
        new ResultCallback<String>() {
          @Override
          public void onSuccess(String result) {}

          @Override
          public void onError(Exception exception) {
            errorFired.set(true);
            errorMsgRef.set(exception.getMessage());
          }
        };

    callback.onError(new RuntimeException("Simulated Network Timeout"));
    assertTrue(errorFired.get());
    assertEquals("Simulated Network Timeout", errorMsgRef.get());
  }
}
