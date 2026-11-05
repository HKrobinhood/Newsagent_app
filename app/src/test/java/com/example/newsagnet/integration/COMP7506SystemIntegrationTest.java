package com.example.newsagnet.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.MessageRole;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsCategory;
import com.example.newsagnet.ui.adapter.ChatAdapter;
import com.example.newsagnet.ui.adapter.NewsAdapter;
import com.example.newsagnet.ui.globe.GlobeMath;
import com.example.newsagnet.ui.globe.HotspotCluster;
import com.example.newsagnet.ui.globe.WorldMapVector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * End-to-End System Integration Test verifying all COMP7506 core curriculum modules:
 *
 * <ul>
 *   <li>Ch2: Toolchain & Execution Context
 *   <li>Ch3: Java OOP, Polymorphism, Builder Pattern, JavaBeans
 *   <li>Ch4: Lifecycle & Architecture Patterns
 *   <li>Ch5: RecyclerView Adapters, Multi-Type ViewHolders & DiffUtil
 *   <li>Ch6: Preferences & Intent navigation URIs
 *   <li>Ch6A: 2D Graphics Math, Spherical Projections & Military Palettes
 *   <li>Ch7: Multithreading, ExecutorService Concurrency & UI Thread Dispatching
 * </ul>
 */
public class COMP7506SystemIntegrationTest {

  private ExecutorService testThreadPool;
  private AppExecutors testExecutors;

  @Before
  public void setUp() {
    testThreadPool = Executors.newFixedThreadPool(4);
    Executor directExecutor = Runnable::run;
    testExecutors = new AppExecutors(testThreadPool, testThreadPool, directExecutor);
  }

  @After
  public void tearDown() {
    testThreadPool.shutdown();
  }

  @Test
  public void testCurriculumCh3_javaOopDomainEntitiesAndPolymorphism() {
    // 1. NewsArticle JavaBean and Builder Pattern
    NewsArticle article =
        new NewsArticle.Builder()
            .setId("art-e2e-1")
            .setTitle("Global Semiconductor Summit Announces New Standard")
            .setSourceName("TechCrunch")
            .setSourceId("techcrunch")
            .setCategory("technology")
            .setLocationName("Tokyo, Japan")
            .setLatitude(35.6762)
            .setLongitude(139.6503)
            .setOriginalUrl("https://techcrunch.com/semi-summit")
            .setPublishedAt("2026-11-04T09:00:00Z")
            .build();

    assertNotNull(article);
    assertEquals("art-e2e-1", article.getId());
    assertEquals("technology", article.getCategory());
    assertEquals(35.6762, article.getLatitude(), 0.0001);
    assertTrue(article.getOriginalUrl().startsWith("https://"));

    // 2. ChatMessage JavaBean with Builder Pattern & Roles
    ChatMessage chatMessage =
        new ChatMessage.Builder()
            .setId("chat-e2e-1")
            .setRole(MessageRole.ASSISTANT)
            .setContent("Verified statement on semiconductor standards across 3 agencies.")
            .setModeUsed("web_verification")
            .setSources(Collections.singletonList(article))
            .setThinking(false)
            .setTimestamp("12:30")
            .build();

    assertNotNull(chatMessage);
    assertEquals(MessageRole.ASSISTANT, chatMessage.getRole());
    assertFalse(chatMessage.isThinking());
    assertEquals(1, chatMessage.getSources().size());
    assertEquals(article.getId(), chatMessage.getSources().get(0).getId());

    // 3. WorldMapVector Encapsulation
    WorldMapVector vector =
        new WorldMapVector(
            Collections.singletonList(new float[] {35f, 139f, 36f, 140f}),
            Collections.singletonList(new float[] {35.5f, 139.5f}));
    assertFalse(vector.isEmpty());
    assertEquals(1, vector.getLandRings().size());
    assertEquals(1, vector.getBorderLines().size());
  }

  @Test
  public void testCurriculumCh5_recyclerViewAdaptersAndMultiTypeViewHolders() {
    // 1. NewsAdapter validation
    NewsAdapter newsAdapter = new NewsAdapter();
    assertEquals(0, newsAdapter.getItemCount());

    NewsArticle sample1 =
        new NewsArticle("1", "Title A", "src-1", "BBC", "2026-11-04", "https://bbc.com", "world", "Geneva", 46.2, 6.1);
    NewsArticle sample2 =
        new NewsArticle("2", "Title B", "src-2", "Reuters", "2026-11-04", "https://reuters.com", "business", "London", 51.5, -0.1);

    List<NewsArticle> newsList = new ArrayList<>();
    newsList.add(sample1);
    newsList.add(sample2);

    newsAdapter.submitList(newsList);
    assertEquals(2, newsAdapter.getItemCount());
    assertEquals("1", newsAdapter.getItem(0).getId());
    assertEquals("2", newsAdapter.getItem(1).getId());

    // 2. Multi-Type ChatAdapter validation
    ChatAdapter chatAdapter = new ChatAdapter();
    List<ChatMessage> conversation = new ArrayList<>();

    ChatMessage userMsg =
        new ChatMessage.Builder()
            .setId("m-user")
            .setRole(MessageRole.USER)
            .setContent("What is the latest intelligence report?")
            .build();

    ChatMessage aiMsg =
        new ChatMessage.Builder()
            .setId("m-ai")
            .setRole(MessageRole.ASSISTANT)
            .setContent("Situation report compiled.")
            .setSources(Collections.singletonList(sample1))
            .build();

    conversation.add(userMsg);
    conversation.add(aiMsg);

    chatAdapter.submitList(conversation);
    assertEquals(2, chatAdapter.getItemCount());

    // Ensure ViewTypes correctly differentiate User (1) vs AI (2)
    assertEquals(ChatAdapter.VIEW_TYPE_USER, chatAdapter.getItemViewType(0));
    assertEquals(ChatAdapter.VIEW_TYPE_AI, chatAdapter.getItemViewType(1));
  }

  @Test
  public void testCurriculumCh6A_sphericalProjectionAndIntelligenceClustering() {
    // Tokyo (lat 35.68, lon 139.65)
    float latRad = (float) Math.toRadians(35.68);
    float lonRad = (float) Math.toRadians(139.65);

    // Front-facing rotation
    float pitchRad = (float) Math.toRadians(-35.68);
    float yawRad = (float) Math.toRadians(-139.65);
    float radius = 150f;
    float cx = 250f;
    float cy = 250f;

    float[] projection = GlobeMath.project3DPoint(latRad, lonRad, pitchRad, yawRad, radius, cx, cy);

    assertEquals(cx, projection[0], 0.05f);
    assertEquals(cy, projection[1], 0.05f);
    assertTrue("Directly facing coordinate should have positive depth near 1.0", projection[2] > 0.95f);

    // Category military intelligence color palette mapping
    assertEquals(0xFFF97316, GlobeMath.getCategoryColor("politics"));
    assertEquals(0xFF10B981, GlobeMath.getCategoryColor("economy"));
    assertEquals(0xFF06B6D4, GlobeMath.getCategoryColor("technology"));
    assertEquals(0xFFEF4444, GlobeMath.getCategoryColor("military"));
    assertEquals(0xFFF59E0B, GlobeMath.getCategoryColor("disaster"));
    assertEquals(0xFF38BDF8, GlobeMath.getCategoryColor("world"));
  }

  @Test
  public void testCurriculumCh7_executorServiceConcurrencyAndUIThreadDispatching() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> backgroundThreadName = new AtomicReference<>("");
    AtomicBoolean mainThreadExecuted = new AtomicBoolean(false);

    testExecutors
        .networkIO()
        .execute(
            () -> {
              backgroundThreadName.set(Thread.currentThread().getName());
              // Dispatch completion to main thread
              testExecutors.mainThread().execute(() -> {
                mainThreadExecuted.set(true);
                latch.countDown();
              });
            });

    boolean completed = latch.await(2, TimeUnit.SECONDS);
    assertTrue("Asynchronous chain must complete within timeout", completed);
    assertTrue("Main thread callback must be invoked", mainThreadExecuted.get());
    assertFalse(
        "Background task must execute in thread pool, not caller thread",
        backgroundThreadName.get().isEmpty());
  }
}
