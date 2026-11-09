# Reading history

Reading history helps users find news links they previously opened and keep personal notes.
Open it using the history icon beside Refresh on the **News** screen.

## Behavior

- Links opened from News, Globe's **Read full article**, and AI Intel's source cards are recorded.
- Selecting a globe marker alone is not recorded. A record means a browser launch was requested
  successfully; it does not establish that the article loaded or was completely read.
- Reopening the same original URL updates its metadata and opening time without losing its note.
  Different article IDs, including generated AI citation IDs, do not produce duplicate rows.
- Search matches literal text in titles or notes. Media source and opening-date filters can be combined.
- **Last 7 days** includes today and the previous six local calendar days. Custom ranges include
  both selected dates. Dates refer to when a link was opened, not when it was published.
- Notes and filters survive activity recreation. The note editor also restores an unsaved draft.
- Users can delete individual records or clear all history, including records hidden by filters.
  Destructive operations ask for confirmation.
- A storage failure does not prevent the existing browser-opening flow.

## Storage and scope

The feature uses its own Android SQLite database, `reading_history.db`, in the app's private data.
It stores titles, source names, publication strings, URLs, last-opened timestamps and notes.
It does not download article bodies, add backend endpoints, or sync history through the project server.
Android system backup remains governed by the application's existing backup settings.
Uninstalling the app normally removes its local data; closing or restarting the app does not.

No new third-party dependency, permission or global tool configuration is required.
The implementation uses the existing XML/Java UI and existing `AppExecutors`.

## Integration boundaries

All new implementation files are under `com.example.newsagnet.history`, with separately named resources.
The only existing production files changed are:

1. `NewsFragment.java`: add the History entry and an asynchronous recording call after `startActivity`.
2. `GlobeFragment.java`: add a recording call after opening a selected article's original URL.
3. `ChatFragment.java`: add a recording call after opening a news source URL.
4. `fragment_news.xml`: add the entry icon and constrain header text to avoid overlap.
5. `AndroidManifest.xml`: register the new, non-exported activity with its own scoped theme.

The backend, original news adapter/model, bottom navigation, globe renderer and AI logic are unchanged.
Existing local AGP/Kotlin compatibility edits are separate from this feature's Git commit.

## Course concepts

- Chapter 3: encapsulation and immutable data snapshots (`HistoryEntry`, `HistoryFilter`).
- Chapter 5: XML layouts, RecyclerView/Adapter and separation of UI from data access.
- Chapter 6: Activity lifecycle, Intent, SQLiteOpenHelper, Cursor and parameterized CRUD operations.
- Chapter 7: background disk work and main-thread callbacks through Executor/Handler.

`HistoryDatabase` owns schema creation; `HistoryStore` contains synchronous SQL operations;
`HistoryRepository` schedules work and handles errors; `ReadingHistoryActivity` renders and filters results.
Latest-request checks prevent an older query from replacing newer search results.

## Validation

From the project root, with a compatible JDK and Android SDK available:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

Instrumented tests use uniquely named test databases. They never clear the production history database.
Browser intents are intercepted in integration tests, so test fixtures do not navigate to external sites.
The command above keeps the app installed when tests finish; without this option, AGP's device-test
runner may uninstall the app and remove its local data. Use a development emulator for testing.

Manual demonstration:

1. Open a news article, return to the app, then open Reading history.
2. Add a note, reopen the article, and verify one record remains with the note preserved.
3. Search a word in the note and combine it with a source/date filter.
4. Open an original article from Globe and an AI citation; verify they appear in history.
5. Restart the app and confirm the saved record and note are still present.
6. Cancel a Clear all confirmation, then test deletion using an expendable record.

Historical metadata and notes can be viewed without the backend; fetching new news and opening
publisher websites still need their respective network connections.
