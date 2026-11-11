package com.example.newsagnet.history;

import java.util.Calendar;
import java.util.TimeZone;

/** Search values and an inclusive-start, exclusive-end range of local opening dates. */
public final class HistoryFilter {
  public final String query;
  public final String source;
  public final Long from;
  public final Long until;

  public HistoryFilter(String query, String source, Long from, Long until) {
    this.query = query == null ? "" : query.trim();
    this.source = source == null || source.trim().isEmpty() ? null : source.trim();
    if (from != null && until != null && from >= until) {
      throw new IllegalArgumentException("Invalid opening-date range");
    }
    this.from = from;
    this.until = until;
  }

  /** SQL LIKE wildcards in user input are literal characters, not search operators. */
  public String likePattern() {
    return "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

  public static long dayStart(long time, TimeZone zone) {
    Calendar day = Calendar.getInstance(zone);
    day.setTimeInMillis(time);
    day.set(Calendar.HOUR_OF_DAY, 0);
    day.set(Calendar.MINUTE, 0);
    day.set(Calendar.SECOND, 0);
    day.set(Calendar.MILLISECOND, 0);
    return day.getTimeInMillis();
  }

  public static long followingDay(long time, TimeZone zone) {
    Calendar day = Calendar.getInstance(zone);
    day.setTimeInMillis(dayStart(time, zone));
    day.add(Calendar.DATE, 1);
    return day.getTimeInMillis();
  }

  /** Today and the previous six calendar days, including daylight-saving transitions. */
  public static HistoryFilter lastSevenDays(String query, String source, long now, TimeZone zone) {
    Calendar start = Calendar.getInstance(zone);
    start.setTimeInMillis(dayStart(now, zone));
    start.add(Calendar.DATE, -6);
    return new HistoryFilter(query, source, start.getTimeInMillis(), followingDay(now, zone));
  }
}
