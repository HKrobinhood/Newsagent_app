package com.example.newsagnet.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

/** Synchronous SQLite operations. Production callers use HistoryRepository's disk executor. */
final class HistoryStore implements AutoCloseable {
  private final HistoryDatabase helper;

  HistoryStore(Context context, String databaseName) {
    helper = new HistoryDatabase(context, databaseName);
  }

  void record(HistoryEntry entry) {
    SQLiteDatabase db = helper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("url", entry.getUrl());
    values.put("title", entry.getTitle());
    values.put("source_name", entry.getSourceName());
    values.put("published_at", entry.getPublishedAt());
    values.put("opened_at", entry.getOpenedAt());
    db.beginTransaction();
    try {
      // UPDATE, rather than REPLACE, deliberately preserves the user's existing note.
      if (db.update("history", values, "url = ?", new String[] {entry.getUrl()}) == 0) {
        values.put("note", "");
        db.insertOrThrow("history", null, values);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  List<HistoryEntry> find(HistoryFilter filter) {
    List<String> clauses = new ArrayList<>();
    List<String> args = new ArrayList<>();
    if (!filter.query.isEmpty()) {
      clauses.add("(title LIKE ? ESCAPE '!' OR note LIKE ? ESCAPE '!')");
      args.add(filter.likePattern());
      args.add(filter.likePattern());
    }
    if (filter.source != null) {
      clauses.add("source_name = ? COLLATE NOCASE");
      args.add(filter.source);
    }
    if (filter.from != null) {
      clauses.add("opened_at >= ?");
      args.add(Long.toString(filter.from));
    }
    if (filter.until != null) {
      clauses.add("opened_at < ?");
      args.add(Long.toString(filter.until));
    }
    List<HistoryEntry> entries = new ArrayList<>();
    try (Cursor cursor = helper.getReadableDatabase().query("history", null,
        clauses.isEmpty() ? null : TextUtils.join(" AND ", clauses), args.toArray(new String[0]),
        null, null, "opened_at DESC, url ASC")) {
      while (cursor.moveToNext()) {
        entries.add(new HistoryEntry(string(cursor, "url"), string(cursor, "title"),
            string(cursor, "source_name"), string(cursor, "published_at"),
            cursor.getLong(cursor.getColumnIndexOrThrow("opened_at")), string(cursor, "note")));
      }
    }
    return entries;
  }

  List<String> sources() {
    List<String> sources = new ArrayList<>();
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
        "SELECT DISTINCT source_name FROM history ORDER BY source_name COLLATE NOCASE", null)) {
      while (cursor.moveToNext()) sources.add(cursor.getString(0));
    }
    return sources;
  }

  int count() {
    try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM history", null)) {
      cursor.moveToFirst();
      return cursor.getInt(0);
    }
  }

  boolean saveNote(String url, String note) {
    ContentValues values = new ContentValues();
    values.put("note", note);
    return helper.getWritableDatabase().update("history", values, "url = ?",
        new String[] {url}) != 0;
  }

  void delete(String url) {
    helper.getWritableDatabase().delete("history", "url = ?", new String[] {url});
  }

  void clear() { helper.getWritableDatabase().delete("history", null, null); }

  private static String string(Cursor cursor, String column) {
    return cursor.getString(cursor.getColumnIndexOrThrow(column));
  }

  @Override public void close() { helper.close(); }
}
