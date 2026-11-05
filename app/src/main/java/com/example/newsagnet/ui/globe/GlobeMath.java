package com.example.newsagnet.ui.globe;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import com.example.newsagnet.model.NewsArticle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 3D spherical projection geometry, cluster aggregation, and color palette utilities.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Utility Class) and Ch6A (Graphics & Color Systems).
 */
public final class GlobeMath {

  private GlobeMath() {}

  /**
   * Projects a geographic spherical coordinate (latitude, longitude) into 2D screen coordinates
   * with depth testing (Z-buffer) after applying Pitch and Yaw 3D matrix rotations.
   *
   * @param latRad Latitude in radians.
   * @param lonRad Longitude in radians.
   * @param pitchRad Pitch rotation (X-axis) in radians.
   * @param yawRad Yaw rotation (Y-axis) in radians.
   * @param radius Sphere radius in pixels.
   * @param centerX Horizontal center of globe.
   * @param centerY Vertical center of globe.
   * @return float array: [0] = screenX, [1] = screenY, [2] = depthZ (positive for visible hemisphere).
   */
  @NonNull
  public static float[] project3DPoint(
      float latRad,
      float lonRad,
      float pitchRad,
      float yawRad,
      float radius,
      float centerX,
      float centerY) {

    // Initial 3D Cartesian coordinates on unit sphere
    float x0 = (float) (Math.cos(latRad) * Math.sin(lonRad));
    float y0 = (float) (-Math.sin(latRad));
    float z0 = (float) (Math.cos(latRad) * Math.cos(lonRad));

    // Rotate Yaw (around Y-axis)
    float cosYaw = (float) Math.cos(yawRad);
    float sinYaw = (float) Math.sin(yawRad);
    float x1 = x0 * cosYaw + z0 * sinYaw;
    float z1 = -x0 * sinYaw + z0 * cosYaw;
    float y1 = y0;

    // Rotate Pitch (around X-axis)
    float cosPitch = (float) Math.cos(pitchRad);
    float sinPitch = (float) Math.sin(pitchRad);
    float y2 = y1 * cosPitch - z1 * sinPitch;
    float z2 = y1 * sinPitch + z1 * cosPitch;
    float x2 = x1;

    float screenX = centerX + x2 * radius;
    float screenY = centerY + y2 * radius;

    return new float[] {screenX, screenY, z2};
  }

  /**
   * Projects news articles to screen space and aggregates nearby points into spatial clusters.
   *
   * @param articles List of geolocated news articles.
   * @param pitchRad Pitch angle in radians.
   * @param yawRad Yaw angle in radians.
   * @param radius Globe radius in pixels.
   * @param centerX Center X.
   * @param centerY Center Y.
   * @param clusterThresholdPx Distance threshold in pixels to merge into clusters.
   * @return List of {@link HotspotCluster} on the visible front hemisphere.
   */
  @NonNull
  public static List<HotspotCluster> clusterVisibleMarkers(
      @NonNull List<NewsArticle> articles,
      float pitchRad,
      float yawRad,
      float radius,
      float centerX,
      float centerY,
      float clusterThresholdPx) {

    if (articles.isEmpty()) {
      return Collections.emptyList();
    }

    List<ProjectedPoint> visiblePoints = new ArrayList<>();
    for (NewsArticle article : articles) {
      Double lat = article.getLatitude();
      Double lon = article.getLongitude();
      if (lat != null && lon != null) {
        float latRad = (float) Math.toRadians(lat);
        float lonRad = (float) Math.toRadians(lon);
        float[] proj = project3DPoint(latRad, lonRad, pitchRad, yawRad, radius, centerX, centerY);

        // Positive Z indicates the front visible hemisphere
        if (proj[2] > 0.04f) {
          visiblePoints.add(new ProjectedPoint(proj[0], proj[1], proj[2], article));
        }
      }
    }

    if (visiblePoints.isEmpty()) {
      return Collections.emptyList();
    }

    List<HotspotCluster> clusters = new ArrayList<>();
    boolean[] visited = new boolean[visiblePoints.size()];

    for (int i = 0; i < visiblePoints.size(); i++) {
      if (visited[i]) {
        continue;
      }

      List<NewsArticle> group = new ArrayList<>();
      ProjectedPoint root = visiblePoints.get(i);
      group.add(root.article);
      visited[i] = true;

      float sumX = root.x;
      float sumY = root.y;
      float avgZ = root.z;

      for (int j = i + 1; j < visiblePoints.size(); j++) {
        if (visited[j]) {
          continue;
        }

        ProjectedPoint other = visiblePoints.get(j);
        float dx = root.x - other.x;
        float dy = root.y - other.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < clusterThresholdPx) {
          group.add(other.article);
          sumX += other.x;
          sumY += other.y;
          avgZ = (avgZ + other.z) / 2f;
          visited[j] = true;
        }
      }

      float centerScreenX = sumX / group.size();
      float centerScreenY = sumY / group.size();
      String primaryCat = group.get(0).getCategory();

      clusters.add(
          new HotspotCluster(group, centerScreenX, centerScreenY, avgZ, primaryCat));
    }

    return clusters;
  }

  /**
   * Returns category theme color corresponding to military intelligence visualization standards.
   */
  @ColorInt
  public static int getCategoryColor(@NonNull String category) {
    switch (category.toLowerCase()) {
      case "politics":
        return 0xFFF97316; // Orange
      case "economy":
      case "business":
        return 0xFF10B981; // Green
      case "technology":
        return 0xFF06B6D4; // Cyan
      case "military":
        return 0xFFEF4444; // Red
      case "disaster":
      case "health":
        return 0xFFF59E0B; // Amber
      case "world":
      case "science":
        return 0xFF38BDF8; // Light Sky Blue
      default:
        return 0xFF94A3B8; // Slate Gray
    }
  }

  private static class ProjectedPoint {
    final float x;
    final float y;
    final float z;
    final NewsArticle article;

    ProjectedPoint(float x, float y, float z, NewsArticle article) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.article = article;
    }
  }
}
