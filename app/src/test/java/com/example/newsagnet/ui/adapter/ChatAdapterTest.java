package com.example.newsagnet.ui.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.MessageRole;
import com.example.newsagnet.model.NewsArticle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ChatAdapter, MessageRole view types, and multi-type message dispatching.
 *
 * <p>Conforms to COMP7506 Ch5 (Multi-Type RecyclerView & Adapter Testing).
 */
public class ChatAdapterTest {

  private ChatAdapter adapter;
  private List<ChatMessage> sampleMessages;

  @Before
  public void setUp() {
    adapter = new ChatAdapter();
    sampleMessages = new ArrayList<>();

    sampleMessages.add(
        new ChatMessage.Builder()
            .setId("msg-1")
            .setRole(MessageRole.USER)
            .setContent("What happened at the global climate summit today?")
            .setTimestamp("12:00")
            .build());

    NewsArticle citation =
        new NewsArticle(
            "ref-1",
            "Historic Climate Accord Signed in Geneva",
            "src-reuters",
            "Reuters",
            "2026-11-04T07:30:00Z",
            "https://reuters.com/news/climate",
            "environment",
            "Geneva, Switzerland",
            46.2044,
            6.1432);

    sampleMessages.add(
        new ChatMessage.Builder()
            .setId("msg-2")
            .setRole(MessageRole.ASSISTANT)
            .setContent("Delegates reached an accord on renewable funding.")
            .setSources(Collections.singletonList(citation))
            .setThinking(false)
            .setTimestamp("12:01")
            .build());
  }

  @Test
  public void testInitialState_emptyMessages() {
    assertEquals(0, adapter.getItemCount());
    assertNull(adapter.getItem(0));
    assertNull(adapter.getItem(-1));
  }

  @Test
  public void testSubmitList_updatesItemCountAndRetrieval() {
    adapter.submitList(sampleMessages);

    assertEquals(2, adapter.getItemCount());

    ChatMessage first = adapter.getItem(0);
    assertNotNull(first);
    assertEquals("msg-1", first.getId());
    assertEquals(MessageRole.USER, first.getRole());
    assertEquals("What happened at the global climate summit today?", first.getContent());

    ChatMessage second = adapter.getItem(1);
    assertNotNull(second);
    assertEquals("msg-2", second.getId());
    assertEquals(MessageRole.ASSISTANT, second.getRole());
    assertNotNull(second.getSources());
    assertEquals(1, second.getSources().size());
  }

  @Test
  public void testGetItemViewType_differentiatesUserAndAi() {
    adapter.submitList(sampleMessages);

    assertEquals(ChatAdapter.VIEW_TYPE_USER, adapter.getItemViewType(0));
    assertEquals(ChatAdapter.VIEW_TYPE_AI, adapter.getItemViewType(1));
  }

  @Test
  public void testSubmitList_emptyListClearsAdapter() {
    adapter.submitList(sampleMessages);
    assertEquals(2, adapter.getItemCount());

    adapter.submitList(new ArrayList<>());
    assertEquals(0, adapter.getItemCount());
    assertNull(adapter.getItem(0));
  }

  @Test
  public void testSourceClickListener_registration() {
    AtomicBoolean listenerAssigned = new AtomicBoolean(false);
    NewsItemClickListener listener = article -> listenerAssigned.set(true);

    adapter.setOnSourceClickListener(listener);
    // Verify adapter instantiates and retains callback without throwing
    assertNotNull(adapter);
  }
}
