package com.example.newsagnet.history;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.DatePicker;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.newsagnet.R;
import com.example.newsagnet.concurrency.AppExecutors;
import java.util.UUID;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HistoryScreenTest {
  private Context context;
  private String databaseName;
  private HistoryStore store;
  private HistoryRepository previous;
  private ActivityScenario<ReadingHistoryActivity> scenario;

  @Before public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    databaseName = "history-screen-test-" + UUID.randomUUID() + ".db";
    store = new HistoryStore(context, databaseName);
    long now = System.currentTimeMillis();
    store.record(new HistoryEntry("https://example.org/a", "Chip research brief", "BBC News", "2026-11-01", now, ""));
    store.saveNote("https://example.org/a", "Campus project");
    store.record(new HistoryEntry("https://example.org/b", "Global policy briefing", "CNN News", "2026-11-01", now, ""));
    store.record(new HistoryEntry("https://example.org/c", "Older science story", "BBC News", "2026-10-01", now - 10L * 86400000, ""));
    AppExecutors executors = AppExecutors.getInstance();
    previous = HistoryRepository.replaceForTesting(new HistoryRepository(store,
        executors.diskIO(), executors.mainThread()));
    scenario = ActivityScenario.launch(ReadingHistoryActivity.class);
    awaitCount(3);
  }

  @After public void tearDown() throws Exception {
    if (scenario != null) scenario.close();
    AppExecutors.getInstance().diskIO().submit(() -> {}).get(5, TimeUnit.SECONDS);
    HistoryRepository.replaceForTesting(previous);
    store.close();
    context.deleteDatabase(databaseName);
  }

  @Test public void searchNotesCombineWithSourceAndDateFilters() {
    onView(withId(R.id.history_search)).perform(replaceText("campus"), closeSoftKeyboard());
    awaitCount(1);
    onView(allOf(withId(R.id.history_item_title), withText("Chip research brief")))
        .check(matches(isDisplayed()));
    onView(withId(R.id.history_source)).perform(click());
    onData(allOf(is(instanceOf(String.class)), is("CNN News"))).perform(click());
    awaitCount(0);
    onView(withId(R.id.history_state_title)).check(matches(withText(R.string.history_no_matches)));
    onView(withId(R.id.history_reset)).perform(click());
    awaitCount(3);
    onView(withId(R.id.history_dates)).perform(click());
    onData(allOf(is(instanceOf(String.class)), is("Last 7 days"))).perform(click());
    awaitCount(2);
  }

  @Test public void filterAndUnsavedNoteSurviveActivityRecreation() {
    onView(withId(R.id.history_search)).perform(replaceText("Chip"), closeSoftKeyboard());
    awaitCount(1);
    onView(allOf(withId(R.id.history_item_edit), isDescendantOfA(allOf(
        withId(R.id.history_item_card), hasDescendant(withText("Chip research brief")))))).perform(click());
    onView(withId(R.id.history_note_input)).perform(replaceText("Draft survives rotation"), closeSoftKeyboard());
    scenario.recreate();
    onView(withId(R.id.history_note_input)).inRoot(isDialog())
        .check(matches(withText("Draft survives rotation")));
    onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
    onView(isRoot()).perform(waitFor(view -> {
      TextView note = view.findViewById(R.id.history_item_note);
      return note != null && note.getText().toString().contains("Draft survives rotation");
    }, "saved note to appear"));
    onView(withId(R.id.history_search)).check(matches(withText("Chip")));
    assertEquals("Draft survives rotation", store.find(new HistoryFilter("Chip", null, null, null)).get(0).getNote());
  }

  @Test public void deleteOneItemAndClearAllRequireConfirmation() {
    onView(withId(R.id.history_clear)).perform(click());
    onView(withId(android.R.id.button2)).perform(click());
    assertEquals(3, store.count());
    onView(withId(R.id.history_search)).perform(replaceText("Chip"), closeSoftKeyboard());
    awaitCount(1);
    onView(allOf(withId(R.id.history_item_delete), isDescendantOfA(allOf(
        withId(R.id.history_item_card), hasDescendant(withText("Chip research brief")))))).perform(click());
    onView(withId(android.R.id.button1)).perform(click());
    awaitCount(0);
    assertEquals(2, store.count());
    onView(withId(R.id.history_reset)).perform(click());
    awaitCount(2);
    onView(withId(R.id.history_clear)).perform(click());
    onView(withId(android.R.id.button1)).perform(click());
    awaitCount(0);
    onView(withId(R.id.history_state_title)).check(matches(withText(R.string.history_empty_title)));
  }

  @Test public void customDateRangeSurvivesRecreationAndCanBeCancelled() {
    Calendar today = Calendar.getInstance();
    onView(withId(R.id.history_dates)).perform(click());
    onData(allOf(is(instanceOf(String.class)), is("Custom dates…"))).perform(click());
    onView(isAssignableFrom(DatePicker.class)).perform(selectDate(today));
    onView(withId(android.R.id.button1)).perform(click());
    onView(isAssignableFrom(DatePicker.class)).perform(selectDate(today));
    onView(withId(android.R.id.button1)).perform(click());
    awaitCount(2);
    scenario.recreate();
    awaitCount(2);
    onView(withId(R.id.history_range)).check(matches(isDisplayed())).perform(click());
    onView(withId(android.R.id.button2)).perform(click());
    awaitCount(2);
    onView(withId(R.id.history_reset)).perform(click());
    awaitCount(3);
  }

  private ViewAction selectDate(Calendar date) {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() { return isAssignableFrom(DatePicker.class); }
      @Override public String getDescription() { return "Select a local calendar day"; }
      @Override public void perform(UiController controller, View view) {
        ((DatePicker) view).updateDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
            date.get(Calendar.DAY_OF_MONTH));
        controller.loopMainThreadUntilIdle();
      }
    };
  }

  private interface Condition { boolean matches(View root); }

  private ViewAction waitFor(Condition condition, String description) {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() { return isRoot(); }
      @Override public String getDescription() { return "Wait for " + description; }
      @Override public void perform(UiController controller, View root) {
        long deadline = android.os.SystemClock.uptimeMillis() + 6000;
        do {
          if (condition.matches(root)) return;
          controller.loopMainThreadForAtLeast(40);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Timed out waiting for " + description);
      }
    };
  }

  private void awaitCount(int count) {
    String expected = context.getResources().getQuantityString(R.plurals.history_results, count, count);
    onView(isRoot()).perform(waitFor(root -> {
      TextView label = root.findViewById(R.id.history_count);
      RecyclerView list = root.findViewById(R.id.history_list);
      return label != null && expected.contentEquals(label.getText())
          && list.getAdapter() != null && list.getAdapter().getItemCount() == count;
    }, expected));
  }
}
