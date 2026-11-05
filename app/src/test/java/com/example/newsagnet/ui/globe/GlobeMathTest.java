package com.example.newsagnet.ui.globe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.newsagnet.model.NewsArticle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests verifying 3D spherical projection math, back-face culling, spatial clustering, and category colors.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Unit Testing) and Ch6A (Graphics Math).
 */
public class GlobeMathTest {

  @Test
  public void testProject3DPoint_frontCenterOrigin() {
    float latRad = 0f;
    float lonRad = 0f;
    float pitchRad = 0f;
    float yawRad = 0f;
    float radius = 100f;
    float cx = 200f;
    float cy = 200f;

    float[] proj = GlobeMath.project3DPoint(latRad, lonRad, pitchRad, yawRad, radius, cx, cy);

    assertEquals(200f, proj[0], 0.01f); // screenX == centerX
    assertEquals(200f, proj[1], 0.01f); // screenY == centerY
    assertEquals(1.0f, proj[2], 0.01f);  // depthZ == 1.0 (pointing towards camera)
  }

  @Test
  public void testProject3DPoint_backHemisphereCulling() {
    float latRad = 0f;
    float lonRad = (float) Math.PI; // 180 degrees (antipode)
    float pitchRad = 0f;
    float yawRad = 0f;
    float radius = 100f;
    float cx = 200f;
    float cy = 200f;

    float[] proj = GlobeMath.project3DPoint(latRad, lonRad, pitchRad, yawRad, radius, cx, cy);

    // On back of globe, depthZ should be negative (-1.0)
    assertTrue("Back facing point should have negative depth", proj[2] < 0f);
  }

  @Test
  public void testProject3DPoint_yawRotationBringsAntipodeToFront() {
    float latRad = 0f;
    float lonRad = (float) Math.PI; // 180 degrees
    float pitchRad = 0f;
    float yawRad = (float) Math.PI;  // 180 degrees yaw rotation
    float radius = 100f;
    float cx = 200f;
    float cy = 200f;

    float[] proj = GlobeMath.project3DPoint(latRad, lonRad, pitchRad, yawRad, radius, cx, cy);

    // Rotating globe 180 degrees brings the antipode to front center
    assertEquals(200f, proj[0], 0.01f);
    assertEquals(200f, proj[1], 0.01f);
    assertTrue("Point should now face camera", proj[2] > 0.9f);
  }

  @Test
  public void testClusterVisibleMarkers_emptyListReturnsEmpty() {
    List<HotspotCluster> clusters =
        GlobeMath.clusterVisibleMarkers(Collections.emptyList(), 0f, 0f, 100f, 200f, 200f, 30f);
    assertNotNull(clusters);
    assertTrue(clusters.isEmpty());
  }

  @Test
  public void testClusterVisibleMarkers_nearbyPointsAggregatedIntoCluster() {
    List<NewsArticle> articles = new ArrayList<>();
    // Two articles in London (~51.5, -0.12)
    articles.add(
        new NewsArticle(
            "art-1",
            "London AI Summit",
            "src-bbc",
            "BBC",
            "2026-11-04T08:00:00Z",
            "https://bbc.com/1",
            "technology",
            "London, UK",
            51.5074,
            -0.1278));
    articles.add(
        new NewsArticle(
            "art-2",
            "London Tech Week",
            "src-reuters",
            "Reuters",
            "2026-11-04T09:00:00Z",
            "https://reuters.com/2",
            "technology",
            "London, UK",
            51.5080,
            -0.1280));

    // Pitch & Yaw facing Europe (lat ~51, lon ~0)
    float pitchRad = (float) Math.toRadians(-51.5);
    float yawRad = 0f;
    float radius = 200f;
    float cx = 300f;
    float cy = 300f;

    List<HotspotCluster> clusters =
        GlobeMath.clusterVisibleMarkers(articles, pitchRad, yawRad, radius, cx, cy, 50f);

    assertEquals(1, clusters.size());
    HotspotCluster cluster = clusters.get(0);
    assertTrue(cluster.isCluster());
    assertEquals(2, cluster.getCount());
    assertEquals("technology", cluster.getPrimaryCategory());
  }

  @Test
  public void testClusterVisibleMarkers_distantPointsKeptSeparate() {
    List<NewsArticle> articles = new ArrayList<>();
    // Article 1 in Western Europe (lat 50, lon 0)
    articles.add(
        new NewsArticle(
            "art-1",
            "European Accord",
            "src-bbc",
            "BBC",
            "2026-11-04T08:00:00Z",
            "https://bbc.com/1",
            "world",
            "London, UK",
            50.0,
            0.0));
    // Article 2 in Middle East (lat 25, lon 45)
    articles.add(
        new NewsArticle(
            "art-2",
            "Middle East Energy",
            "src-reuters",
            "Reuters",
            "2026-11-04T09:00:00Z",
            "https://reuters.com/2",
            "business",
            "Riyadh, SA",
            25.0,
            45.0));

    List<HotspotCluster> clusters =
        GlobeMath.clusterVisibleMarkers(articles, 0f, 0f, 200f, 300f, 300f, 30f);

    // Both should be visible and separate
    assertEquals(2, clusters.size());
    assertFalse(clusters.get(0).isCluster());
    assertFalse(clusters.get(1).isCluster());
  }

  @Test
  public void testGetCategoryColor_returnsDistinctPalette() {
    assertEquals(0xFFF97316, GlobeMath.getCategoryColor("politics"));
    assertEquals(0xFF10B981, GlobeMath.getCategoryColor("economy"));
    assertEquals(0xFF10B981, GlobeMath.getCategoryColor("business"));
    assertEquals(0xFF06B6D4, GlobeMath.getCategoryColor("technology"));
    assertEquals(0xFFEF4444, GlobeMath.getCategoryColor("military"));
    assertEquals(0xFFF59E0B, GlobeMath.getCategoryColor("disaster"));
    assertEquals(0xFF38BDF8, GlobeMath.getCategoryColor("world"));
    assertEquals(0xFF94A3B8, GlobeMath.getCategoryColor("unknown"));
  }

  @Test
  public void testWorldMapVector_dataEncapsulation() {
    WorldMapVector empty = new WorldMapVector();
    assertTrue(empty.isEmpty());
    assertEquals(0, empty.getLandRings().size());
    assertEquals(0, empty.getBorderLines().size());

    List<float[]> rings = Collections.singletonList(new float[] {10f, 20f, 30f, 40f});
    List<float[]> borders = Collections.singletonList(new float[] {15f, 25f});
    WorldMapVector vector = new WorldMapVector(rings, borders);

    assertFalse(vector.isEmpty());
    assertEquals(1, vector.getLandRings().size());
    assertEquals(1, vector.getBorderLines().size());
    assertEquals(4, vector.getLandRings().get(0).length);
  }
}
