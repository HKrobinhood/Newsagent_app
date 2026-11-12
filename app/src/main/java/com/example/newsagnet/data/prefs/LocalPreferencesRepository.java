package com.example.newsagnet.data.prefs;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Local SharedPreferences-backed implementation of {@link UserPreferencesRepository} extending
 * {@link NewsPreferences}.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Interface Implementation & Encapsulation) and Ch6
 * (SharedPreferences Persistence).
 */
public class LocalPreferencesRepository implements UserPreferencesRepository {

  private static volatile LocalPreferencesRepository instance;

  private final NewsPreferences newsPreferences;
  private final Set<OnPreferencesChangedListener> listeners = new CopyOnWriteArraySet<>();

  public LocalPreferencesRepository(@NonNull Context context) {
    this(new NewsPreferences(context));
  }

  public LocalPreferencesRepository(@NonNull NewsPreferences newsPreferences) {
    this.newsPreferences = Objects.requireNonNull(newsPreferences);
  }

  /**
   * Returns the process-wide singleton {@link LocalPreferencesRepository}.
   *
   * @param context Android context.
   * @return Shared {@link LocalPreferencesRepository} instance.
   */
  @NonNull
  public static LocalPreferencesRepository getInstance(@NonNull Context context) {
    if (instance == null) {
      synchronized (LocalPreferencesRepository.class) {
        if (instance == null) {
          instance = new LocalPreferencesRepository(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  /**
   * Replaces the singleton instance for deterministic testing.
   *
   * @param replacement Test repository instance.
   * @return Previous singleton instance.
   */
  public static LocalPreferencesRepository replaceForTesting(
      LocalPreferencesRepository replacement) {
    synchronized (LocalPreferencesRepository.class) {
      LocalPreferencesRepository previous = instance;
      instance = replacement;
      return previous;
    }
  }

  @NonNull
  @Override
  public UserPreferences getPreferences() {
    return newsPreferences.getUserPreferences();
  }

  @NonNull
  @Override
  public Set<String> getEnabledSourceIds() {
    return newsPreferences.getEnabledSourceIds();
  }

  @Override
  public void setEnabledSourceIds(@NonNull Collection<String> sourceIds) {
    newsPreferences.setEnabledSourceIds(sourceIds);
    notifyListeners();
  }

  @Override
  public void setSourceEnabled(@NonNull String sourceId, boolean enabled) {
    String normalizedId = sourceId.trim().toLowerCase();
    Set<String> current = new LinkedHashSet<>(newsPreferences.getEnabledSourceIds());
    boolean modified;
    if (enabled) {
      modified = current.add(normalizedId);
    } else {
      modified = current.remove(normalizedId);
    }
    if (modified || !current.equals(newsPreferences.getEnabledSourceIds())) {
      newsPreferences.setEnabledSourceIds(current);
      notifyListeners();
    }
  }

  @Override
  public void selectAllSources(@NonNull Collection<String> allSourceIds) {
    newsPreferences.setEnabledSourceIds(allSourceIds);
    notifyListeners();
  }

  @Override
  public void resetSourcesToDefault(@NonNull Collection<String> defaultSourceIds) {
    newsPreferences.resetSourcesToDefault(defaultSourceIds);
    notifyListeners();
  }

  @Override
  public void reconcileAvailableSources(@NonNull Collection<String> availableSourceIds) {
    Set<String> before = newsPreferences.getEnabledSourceIds();
    newsPreferences.reconcileAvailableSources(availableSourceIds);
    Set<String> after = newsPreferences.getEnabledSourceIds();
    if (!before.equals(after)) {
      notifyListeners();
    }
  }

  @Override
  public float getFontScale() {
    return newsPreferences.getFontScale();
  }

  @Override
  public void setFontScale(float fontScale) {
    float before = newsPreferences.getFontScale();
    newsPreferences.setFontScale(fontScale);
    float after = newsPreferences.getFontScale();
    if (Float.compare(before, after) != 0) {
      notifyListeners();
    }
  }

  @Override
  public int getSettingsVersion() {
    return newsPreferences.getSettingsVersion();
  }

  @Override
  public long getLastUpdated() {
    return newsPreferences.getLastUpdated();
  }

  @Override
  public void addOnPreferencesChangedListener(@NonNull OnPreferencesChangedListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeOnPreferencesChangedListener(@NonNull OnPreferencesChangedListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners() {
    UserPreferences snapshot = getPreferences();
    for (OnPreferencesChangedListener listener : listeners) {
      listener.onPreferencesChanged(snapshot);
    }
  }
}
