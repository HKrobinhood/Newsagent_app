package com.example.newsagnet.history;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Uses an isolated database; never deletes or seeds the user's actual history. */
@RunWith(AndroidJUnit4.class)
public class HistoryStoreTest {
  private Context context;
  private String databaseName;
  private HistoryStore store;
  private final HistoryFilter all = new HistoryFilter("", null, null, null);

  @Before public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    databaseName = "history-test-" + UUID.randomUUID() + ".db";
    store = new HistoryStore(context, databaseName);
  }

  @After public void tearDown() { store.close(); context.deleteDatabase(databaseName); }

  private HistoryEntry entry(String url, String title, String source, long time) {
    return new HistoryEntry(url, title, source, "2026-11-01", time, "");
  }

  @Test public void reopeningSameUrlUpdatesTimeWithoutLosingNote() {
    store.record(entry("https://example.org/a", "First headline", "BBC News", 100));
    assertTrue(store.saveNote("https://example.org/a", "My research note"));
    store.record(entry("https://example.org/a", "Updated headline", "BBC News", 200));
    List<HistoryEntry> result = store.find(all);
    assertEquals(1, result.size());
    assertEquals(200, result.get(0).getOpenedAt());
    assertEquals("Updated headline", result.get(0).getTitle());
    assertEquals("My research note", result.get(0).getNote());
  }

  @Test public void noteSourceAndOpeningDateFiltersAreCombined() {
    store.record(entry("a", "Chip report", "BBC News", 100));
    store.record(entry("b", "Other report", "CNN News", 200));
    store.record(entry("c", "Older report", "BBC News", 50));
    store.saveNote("a", "Use in the campus project");
    store.saveNote("b", "Campus project");
    store.saveNote("c", "Campus project");
    List<HistoryEntry> result = store.find(new HistoryFilter("CAMPUS", "bbc news", 100L, 201L));
    assertEquals(1, result.size());
    assertEquals("a", result.get(0).getUrl());
    assertEquals(3, store.count());
  }

  @Test public void percentUnderscoreAndSqlTextAreLiteralSearches() {
    store.record(entry("a", "50%_off!", "BBC", 1));
    store.record(entry("b", "50 percent off", "CNN", 2));
    assertEquals(1, store.find(new HistoryFilter("%_", null, null, null)).size());
    assertTrue(store.find(new HistoryFilter("' OR 1=1 --", null, null, null)).isEmpty());
  }

  @Test public void dateBoundsAreInclusiveStartAndExclusiveEndWithNewestFirst() {
    store.record(entry("before", "Before", "BBC", 99));
    store.record(entry("first", "First", "BBC", 100));
    store.record(entry("last", "Last", "BBC", 199));
    store.record(entry("after", "After", "BBC", 200));
    List<HistoryEntry> result = store.find(new HistoryFilter("", null, 100L, 200L));
    assertEquals(2, result.size());
    assertEquals("last", result.get(0).getUrl());
    assertEquals("first", result.get(1).getUrl());
  }

  @Test public void historyAndNotesPersistAfterDatabaseReopens() {
    store.record(entry("a", "Saved title", "BBC", 123));
    store.saveNote("a", "Saved note");
    store.close();
    store = new HistoryStore(context, databaseName);
    assertEquals("Saved note", store.find(all).get(0).getNote());
  }

  @Test public void deletionAndClearDoNotRecreateStaleNotes() {
    store.record(entry("a", "First", "BBC", 1));
    store.record(entry("b", "Second", "CNN", 2));
    store.delete("a");
    assertFalse(store.saveNote("a", "Should not resurrect a deleted item"));
    assertEquals(1, store.count());
    store.clear();
    assertEquals(0, store.count());
    assertTrue(store.sources().isEmpty());
  }

  @Test public void sourceChoicesUseAllHistoryEvenWhenResultsAreFiltered() {
    store.record(entry("a", "First", "BBC News", 1));
    store.record(entry("b", "Second", "CNN News", 2));
    assertEquals(1, store.find(new HistoryFilter("First", null, null, null)).size());
    assertEquals(2, store.sources().size());
  }
}
