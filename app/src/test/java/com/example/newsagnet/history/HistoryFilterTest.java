package com.example.newsagnet.history;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class HistoryFilterTest {
  private static long at(TimeZone zone, int year, int month, int day, int hour) {
    Calendar date = Calendar.getInstance(zone);
    date.clear();
    date.set(year, month - 1, day, hour, 0, 0);
    return date.getTimeInMillis();
  }

  @Test public void searchTreatsSqlWildcardsAsLiteralInput() {
    HistoryFilter filter = new HistoryFilter("  50%_off!  ", "  BBC News  ", null, null);
    assertEquals("%50!%!_off!!%", filter.likePattern());
    assertEquals("BBC News", filter.source);
    assertNull(new HistoryFilter(null, " ", null, null).source);
  }

  @Test public void lastSevenDaysUsesLocalCalendarBoundaries() {
    TimeZone hongKong = TimeZone.getTimeZone("Asia/Hong_Kong");
    HistoryFilter filter = HistoryFilter.lastSevenDays("", null,
        at(hongKong, 2026, 11, 9, 15), hongKong);
    assertEquals(Long.valueOf(at(hongKong, 2026, 11, 3, 0)), filter.from);
    assertEquals(Long.valueOf(at(hongKong, 2026, 11, 10, 0)), filter.until);
  }

  @Test public void customDayCanBeTwentyFiveHoursDuringDaylightSavingTransition() {
    TimeZone newYork = TimeZone.getTimeZone("America/New_York");
    long noon = at(newYork, 2026, 11, 1, 12);
    assertEquals(25L * 60 * 60 * 1000,
        HistoryFilter.followingDay(noon, newYork) - HistoryFilter.dayStart(noon, newYork));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsEmptyDateRange() { new HistoryFilter("", null, 100L, 100L); }
}
