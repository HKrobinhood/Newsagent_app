package com.example.newsagnet.history;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** COMP7506 Ch6: a separate Android database, independent of backend news storage. */
final class HistoryDatabase extends SQLiteOpenHelper {
  static final String NAME = "reading_history.db";

  HistoryDatabase(Context context, String name) {
    super(context.getApplicationContext(), name, null, 1);
  }

  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE history (url TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, "
        + "source_name TEXT NOT NULL, published_at TEXT NOT NULL, "
        + "opened_at INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')");
    db.execSQL("CREATE INDEX history_opened_at ON history(opened_at DESC)");
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Version 1 has no migrations. Future versions must preserve existing notes.
  }
}
