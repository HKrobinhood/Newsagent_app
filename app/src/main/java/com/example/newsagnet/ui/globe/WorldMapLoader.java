package com.example.newsagnet.ui.globe;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.concurrency.AppExecutors;
import com.example.newsagnet.concurrency.ResultCallback;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe loader for Natural Earth 110m vector geography assets.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Utility & Singleton pattern) and Ch7 (ExecutorService Concurrency).
 */
public final class WorldMapLoader {

  private static volatile WorldMapVector cachedMap;

  private WorldMapLoader() {}

  /**
   * Returns in-memory cached map if already parsed, or null.
   */
  @Nullable
  public static WorldMapVector getCachedMap() {
    return cachedMap;
  }

  /**
   * Asynchronously loads and parses the world map vector from application assets.
   *
   * @param context Application context.
   * @param callback Callback delivering the parsed {@link WorldMapVector} on the UI thread.
   */
  public static void loadWorldMapAsync(
      @NonNull Context context, @NonNull ResultCallback<WorldMapVector> callback) {
    if (cachedMap != null) {
      callback.onSuccess(cachedMap);
      return;
    }

    Context appContext = context.getApplicationContext();
    AppExecutors.getInstance()
        .diskIO()
        .execute(
            () -> {
              try {
                WorldMapVector vector = loadWorldMapSync(appContext);
                cachedMap = vector;
                AppExecutors.getInstance()
                    .mainThread()
                    .execute(() -> callback.onSuccess(vector));
              } catch (Exception e) {
                AppExecutors.getInstance()
                    .mainThread()
                    .execute(() -> callback.onError(e));
              }
            });
  }

  /**
   * Synchronously parses the vector json asset on the current thread.
   */
  @NonNull
  public static WorldMapVector loadWorldMapSync(@NonNull Context context) {
    if (cachedMap != null) {
      return cachedMap;
    }

    try (InputStream is = context.getAssets().open("world_map_vector.json");
        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

      Gson gson = new Gson();
      RawWorldMapDto raw = gson.fromJson(reader, RawWorldMapDto.class);

      List<float[]> parsedLand = new ArrayList<>();
      if (raw.land != null) {
        for (List<List<Float>> ring : raw.land) {
          float[] flat = new float[ring.size() * 2];
          int idx = 0;
          for (List<Float> pt : ring) {
            flat[idx++] = pt.get(0);
            flat[idx++] = pt.get(1);
          }
          parsedLand.add(flat);
        }
      }

      List<float[]> parsedBorders = new ArrayList<>();
      if (raw.borders != null) {
        for (List<List<Float>> line : raw.borders) {
          float[] flat = new float[line.size() * 2];
          int idx = 0;
          for (List<Float> pt : line) {
            flat[idx++] = pt.get(0);
            flat[idx++] = pt.get(1);
          }
          parsedBorders.add(flat);
        }
      }

      WorldMapVector vector = new WorldMapVector(parsedLand, parsedBorders);
      cachedMap = vector;
      return vector;
    } catch (Exception e) {
      return new WorldMapVector();
    }
  }

  private static class RawWorldMapDto {
    List<List<List<Float>>> land;
    List<List<List<Float>>> borders;
  }
}
