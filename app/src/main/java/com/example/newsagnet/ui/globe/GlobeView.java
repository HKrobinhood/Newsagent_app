package com.example.newsagnet.ui.globe;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.model.NewsArticle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-performance native Android View rendering a 3D spherical dark tactical situation globe.
 *
 * <p>Conforms to COMP7506 Ch4 (Custom View Lifecycle), Ch6 (Touch Events, MotionEvent & Gestures),
 * Ch6A (Canvas 2D Graphics, Shaders & Paints), and Ch7 (ValueAnimator UI Concurrency).
 */
public class GlobeView extends View {

  private float pitch = -22f; // Degrees: -80 to +80
  private float yaw = 38f;    // Degrees: 0 to 360
  private float zoom = 1.0f;  // Scale factor: 0.7 to 2.4

  private final List<NewsArticle> articles = new ArrayList<>();
  private WorldMapVector worldMap;
  private NewsArticle selectedArticle;
  private final List<HotspotCluster> projectedClusters = new ArrayList<>();
  private OnHotspotClickListener hotspotClickListener;

  // Touch & Gesture detectors
  private ScaleGestureDetector scaleGestureDetector;
  private float touchDownX = 0f;
  private float touchDownY = 0f;
  private float lastTouchX = 0f;
  private float lastTouchY = 0f;
  private boolean isDragging = false;
  private long touchDownTime = 0L;

  // Radar Pulsing Animation
  private ValueAnimator pulseAnimator;
  private float pulseProgress = 0f;

  // Drawing Paints & Paths
  private Paint atmospherePaint;
  private Paint oceanPaint;
  private Paint gridPaint;
  private Paint landFillPaint;
  private Paint coastlinePaint;
  private Paint borderPaint;
  private Paint arcPaint;
  private Paint specularPaint;
  private Paint rimPaint;
  private Paint markerPaint;
  private Paint clusterTextPaint;

  private final Path sphereClipPath = new Path();
  private final RectF sphereRect = new RectF();

  public GlobeView(Context context) {
    super(context);
    init(context);
  }

  public GlobeView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public GlobeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    setFocusable(true);
    setClickable(true);

    // Initialize Paints
    atmospherePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    oceanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    gridPaint.setStyle(Paint.Style.STROKE);
    gridPaint.setColor(0x591E3A5F);
    gridPaint.setStrokeWidth(dpToPx(1f));

    landFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    landFillPaint.setStyle(Paint.Style.FILL);
    landFillPaint.setColor(0xFF192535);

    coastlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    coastlinePaint.setStyle(Paint.Style.STROKE);
    coastlinePaint.setColor(0x6B38BDF8);
    coastlinePaint.setStrokeWidth(dpToPx(1.2f));

    borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setColor(0x66334E68);
    borderPaint.setStrokeWidth(dpToPx(0.8f));

    arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    arcPaint.setStyle(Paint.Style.STROKE);
    arcPaint.setStrokeWidth(dpToPx(1.4f));

    specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    rimPaint.setStyle(Paint.Style.STROKE);
    rimPaint.setColor(0x8C38BDF8);
    rimPaint.setStrokeWidth(dpToPx(1.5f));

    markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    clusterTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    clusterTextPaint.setColor(Color.WHITE);
    clusterTextPaint.setTextSize(spToPx(10f));
    clusterTextPaint.setFakeBoldText(true);
    clusterTextPaint.setTextAlign(Paint.Align.CENTER);

    // Scale Gesture Detector for Pinch-to-Zoom
    scaleGestureDetector =
        new ScaleGestureDetector(
            context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
              @Override
              public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                zoom = Math.max(0.7f, Math.min(2.4f, zoom * factor));
                invalidate();
                return true;
              }
            });

    // Radar pulsing animator
    pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
    pulseAnimator.setDuration(2200);
    pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
    pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
    pulseAnimator.setInterpolator(new LinearInterpolator());
    pulseAnimator.addUpdateListener(
        animation -> {
          pulseProgress = (float) animation.getAnimatedValue();
          invalidate();
        });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (pulseAnimator != null && !pulseAnimator.isRunning()) {
      pulseAnimator.start();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (pulseAnimator != null) {
      pulseAnimator.cancel();
    }
  }

  public void setWorldMap(@Nullable WorldMapVector worldMap) {
    this.worldMap = worldMap;
    invalidate();
  }

  public void setArticles(@NonNull List<NewsArticle> newsArticles) {
    this.articles.clear();
    this.articles.addAll(newsArticles);
    invalidate();
  }

  public void setSelectedArticle(@Nullable NewsArticle article) {
    this.selectedArticle = article;
    invalidate();
  }

  public void setOnHotspotClickListener(@Nullable OnHotspotClickListener listener) {
    this.hotspotClickListener = listener;
  }

  /**
   * Smoothly animates pitch and yaw to focus on a target geographical coordinate.
   */
  public void rotateToLocation(double latitude, double longitude) {
    float targetPitch = (float) -latitude;
    float targetYaw = (float) -longitude;

    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(600);
    float startPitch = this.pitch;
    float startYaw = this.yaw;

    animator.addUpdateListener(
        a -> {
          float fraction = a.getAnimatedFraction();
          this.pitch = startPitch + (targetPitch - startPitch) * fraction;
          this.yaw = startYaw + (targetYaw - startYaw) * fraction;
          invalidate();
        });
    animator.start();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    scaleGestureDetector.onTouchEvent(event);

    float x = event.getX();
    float y = event.getY();

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        touchDownX = x;
        touchDownY = y;
        lastTouchX = x;
        lastTouchY = y;
        touchDownTime = System.currentTimeMillis();
        isDragging = false;
        if (getParent() != null) {
          getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;

      case MotionEvent.ACTION_MOVE:
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;

        if (Math.abs(x - touchDownX) > 8f || Math.abs(y - touchDownY) > 8f) {
          isDragging = true;
        }

        if (isDragging && !scaleGestureDetector.isInProgress()) {
          yaw += dx * 0.35f;
          pitch -= dy * 0.35f;

          // Clamp Pitch
          pitch = Math.max(-80f, Math.min(80f, pitch));
          invalidate();
        }

        lastTouchX = x;
        lastTouchY = y;
        return true;

      case MotionEvent.ACTION_UP:
        long elapsed = System.currentTimeMillis() - touchDownTime;
        float totalDist =
            (float) Math.hypot(x - touchDownX, y - touchDownY);

        if (!isDragging && totalDist < 20f && elapsed < 350) {
          handleTapEvent(x, y);
        }

        if (getParent() != null) {
          getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;

      case MotionEvent.ACTION_CANCEL:
        if (getParent() != null) {
          getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    return super.onTouchEvent(event);
  }

  private void handleTapEvent(float tapX, float tapY) {
    float hitRadius = dpToPx(28f);
    HotspotCluster clickedCluster = null;
    float closestDist = Float.MAX_VALUE;

    for (HotspotCluster cluster : projectedClusters) {
      float dist = (float) Math.hypot(cluster.getScreenX() - tapX, cluster.getScreenY() - tapY);
      if (dist < hitRadius && dist < closestDist) {
        closestDist = dist;
        clickedCluster = cluster;
      }
    }

    if (clickedCluster != null && hotspotClickListener != null) {
      if (clickedCluster.isCluster()) {
        hotspotClickListener.onClusterHotspotClick(clickedCluster.getArticles());
      } else {
        selectedArticle = clickedCluster.getPrimaryArticle();
        hotspotClickListener.onSingleHotspotClick(clickedCluster.getPrimaryArticle());
        invalidate();
      }
    }
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    int width = getWidth();
    int height = getHeight();
    if (width <= 0 || height <= 0) {
      return;
    }

    float centerX = width / 2f;
    float centerY = height / 2f;
    float radius = (Math.min(width, height) / 2f - dpToPx(16f)) * zoom;

    float pitchRad = (float) Math.toRadians(pitch);
    float yawRad = (float) Math.toRadians(yaw);

    // 1. Draw outer Rayleigh atmospheric glow
    atmospherePaint.setShader(
        new RadialGradient(
            centerX,
            centerY,
            radius * 1.15f,
            new int[] {0x5900E5FF, 0x260284C7, 0x0D0369A1, Color.TRANSPARENT},
            new float[] {0.85f, 0.92f, 0.97f, 1.0f},
            Shader.TileMode.CLAMP));
    canvas.drawCircle(centerX, centerY, radius * 1.15f, atmospherePaint);

    // Prepare Sphere Clip Path
    sphereRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    sphereClipPath.reset();
    sphereClipPath.addOval(sphereRect, Path.Direction.CW);

    canvas.save();
    canvas.clipPath(sphereClipPath);

    // 2. Oceanic Deep Blue Sphere Base
    oceanPaint.setShader(
        new RadialGradient(
            centerX - radius * 0.2f,
            centerY - radius * 0.2f,
            radius * 1.1f,
            new int[] {0xFF101B2E, 0xFF09101C, 0xFF04070D},
            new float[] {0.0f, 0.65f, 1.0f},
            Shader.TileMode.CLAMP));
    canvas.drawCircle(centerX, centerY, radius, oceanPaint);

    // 3. Parallels & Meridians Graticule
    drawGraticule(canvas, pitchRad, yawRad, radius, centerX, centerY);

    // 4. Real Continents & Country Borders (Natural Earth 110m)
    if (worldMap != null) {
      drawGeography(canvas, worldMap, pitchRad, yawRad, radius, centerX, centerY);
    }

    // 5. Strategic Intelligence Arcs
    drawIntelligenceArcs(canvas, pitchRad, yawRad, radius, centerX, centerY);

    // 6. Spherical 3D Directional Specular Lighting
    specularPaint.setShader(
        new RadialGradient(
            centerX - radius * 0.35f,
            centerY - radius * 0.35f,
            radius * 1.35f,
            new int[] {0x12FFFFFF, Color.TRANSPARENT, 0x99020617},
            new float[] {0.0f, 0.45f, 1.0f},
            Shader.TileMode.CLAMP));
    canvas.drawCircle(centerX, centerY, radius, specularPaint);

    canvas.restore();

    // 7. Atmospheric Rim Border
    canvas.drawCircle(centerX, centerY, radius, rimPaint);

    // 8. Cluster & Render Hotspots
    projectedClusters.clear();
    List<HotspotCluster> clusters =
        GlobeMath.clusterVisibleMarkers(
            articles, pitchRad, yawRad, radius, centerX, centerY, dpToPx(28f));
    projectedClusters.addAll(clusters);

    drawHotspots(canvas, clusters);
  }

  private void drawGraticule(
      Canvas canvas, float pitchRad, float yawRad, float radius, float cx, float cy) {
    Path path = new Path();

    // Parallels (Latitudes)
    int[] lats = new int[] {-60, -30, 0, 30, 60};
    for (int latDeg : lats) {
      float latR = (float) Math.toRadians(latDeg);
      path.reset();
      boolean started = false;
      for (int lonDeg = 0; lonDeg <= 360; lonDeg += 6) {
        float lonR = (float) Math.toRadians(lonDeg);
        float[] proj = GlobeMath.project3DPoint(latR, lonR, pitchRad, yawRad, radius, cx, cy);
        if (proj[2] > 0.01f) {
          if (!started) {
            path.moveTo(proj[0], proj[1]);
            started = true;
          } else {
            path.lineTo(proj[0], proj[1]);
          }
        } else {
          started = false;
        }
      }
      canvas.drawPath(path, gridPaint);
    }

    // Meridians (Longitudes)
    for (int lonDeg = 0; lonDeg < 360; lonDeg += 30) {
      float lonR = (float) Math.toRadians(lonDeg);
      path.reset();
      boolean started = false;
      for (int latDeg = -85; latDeg <= 85; latDeg += 5) {
        float latR = (float) Math.toRadians(latDeg);
        float[] proj = GlobeMath.project3DPoint(latR, lonR, pitchRad, yawRad, radius, cx, cy);
        if (proj[2] > 0.01f) {
          if (!started) {
            path.moveTo(proj[0], proj[1]);
            started = true;
          } else {
            path.lineTo(proj[0], proj[1]);
          }
        } else {
          started = false;
        }
      }
      canvas.drawPath(path, gridPaint);
    }
  }

  private void drawGeography(
      Canvas canvas,
      WorldMapVector mapData,
      float pitchRad,
      float yawRad,
      float radius,
      float cx,
      float cy) {
    Path landPath = new Path();

    // Land Rings
    for (float[] ring : mapData.getLandRings()) {
      landPath.reset();
      boolean pathStarted = false;
      int i = 0;
      int len = ring.length;
      while (i < len) {
        float lat = ring[i];
        float lon = ring[i + 1];
        i += 2;

        float latR = (float) Math.toRadians(lat);
        float lonR = (float) Math.toRadians(lon);
        float[] proj = GlobeMath.project3DPoint(latR, lonR, pitchRad, yawRad, radius, cx, cy);

        if (proj[2] > -0.05f) {
          if (!pathStarted) {
            landPath.moveTo(proj[0], proj[1]);
            pathStarted = true;
          } else {
            landPath.lineTo(proj[0], proj[1]);
          }
        } else {
          pathStarted = false;
        }
      }

      canvas.drawPath(landPath, landFillPaint);
      canvas.drawPath(landPath, coastlinePaint);
    }

    // Border Lines
    Path borderPath = new Path();
    for (float[] border : mapData.getBorderLines()) {
      borderPath.reset();
      boolean borderStarted = false;
      int i = 0;
      int len = border.length;
      while (i < len) {
        float lat = border[i];
        float lon = border[i + 1];
        i += 2;

        float latR = (float) Math.toRadians(lat);
        float lonR = (float) Math.toRadians(lon);
        float[] proj = GlobeMath.project3DPoint(latR, lonR, pitchRad, yawRad, radius, cx, cy);

        if (proj[2] > 0.02f) {
          if (!borderStarted) {
            borderPath.moveTo(proj[0], proj[1]);
            borderStarted = true;
          } else {
            borderPath.lineTo(proj[0], proj[1]);
          }
        } else {
          borderStarted = false;
        }
      }
      canvas.drawPath(borderPath, borderPaint);
    }
  }

  private void drawIntelligenceArcs(
      Canvas canvas, float pitchRad, float yawRad, float radius, float cx, float cy) {
    float[][] arcs =
        new float[][] {
          {50.45f, 30.52f, 38.90f, -77.03f}, // Kyiv <-> Washington
          {51.50f, -0.12f, 35.67f, 139.65f}, // London <-> Tokyo
          {46.20f, 6.14f, 1.35f, 103.81f}     // Geneva <-> Singapore
        };

    Path arcPath = new Path();
    for (float[] arc : arcs) {
      float fromLatR = (float) Math.toRadians(arc[0]);
      float fromLonR = (float) Math.toRadians(arc[1]);
      float toLatR = (float) Math.toRadians(arc[2]);
      float toLonR = (float) Math.toRadians(arc[3]);

      float[] p1 = GlobeMath.project3DPoint(fromLatR, fromLonR, pitchRad, yawRad, radius, cx, cy);
      float[] p2 = GlobeMath.project3DPoint(toLatR, toLonR, pitchRad, yawRad, radius, cx, cy);

      if (p1[2] > 0.05f || p2[2] > 0.05f) {
        arcPath.reset();
        arcPath.moveTo(p1[0], p1[1]);
        float midX = (p1[0] + p2[0]) / 2f;
        float midY = (p1[1] + p2[1]) / 2f - dpToPx(18f);
        arcPath.quadTo(midX, midY, p2[0], p2[1]);

        arcPaint.setShader(
            new LinearGradient(
                p1[0],
                p1[1],
                p2[0],
                p2[1],
                new int[] {0x7300E5FF, 0x4038BDF8},
                null,
                Shader.TileMode.CLAMP));
        canvas.drawPath(arcPath, arcPaint);
      }
    }
  }

  private void drawHotspots(Canvas canvas, List<HotspotCluster> clusters) {
    for (HotspotCluster cluster : clusters) {
      float ox = cluster.getScreenX();
      float oy = cluster.getScreenY();
      int catColor = GlobeMath.getCategoryColor(cluster.getPrimaryCategory());

      boolean isSelected =
          selectedArticle != null
              && cluster.getArticles().stream().anyMatch(a -> a.getId().equals(selectedArticle.getId()));

      if (cluster.isCluster()) {
        // Multi-article Cluster
        float pulseRadius = dpToPx(14f) + (dpToPx(8f) * pulseProgress);
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(adjustAlpha(catColor, (1f - pulseProgress) * 0.45f));
        canvas.drawCircle(ox, oy, pulseRadius, markerPaint);

        // Body
        markerPaint.setColor(0xEB0F172A);
        canvas.drawCircle(ox, oy, dpToPx(13f), markerPaint);

        // Border
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setColor(isSelected ? 0xFF00E5FF : catColor);
        markerPaint.setStrokeWidth(dpToPx(isSelected ? 2.5f : 1.8f));
        canvas.drawCircle(ox, oy, dpToPx(13f), markerPaint);

        // Text Count
        canvas.drawText(String.valueOf(cluster.getCount()), ox, oy + dpToPx(3.5f), clusterTextPaint);
      } else {
        // Single Article Hotspot
        float pulseRadius = dpToPx(10f) + (dpToPx(12f) * pulseProgress);
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(adjustAlpha(catColor, (1f - pulseProgress) * 0.45f));
        canvas.drawCircle(ox, oy, pulseRadius, markerPaint);

        // Halo
        markerPaint.setColor(adjustAlpha(catColor, 0.35f));
        canvas.drawCircle(ox, oy, dpToPx(8f), markerPaint);

        // Core
        markerPaint.setColor(catColor);
        canvas.drawCircle(ox, oy, dpToPx(4.5f), markerPaint);

        // White Center Eye
        markerPaint.setColor(Color.WHITE);
        canvas.drawCircle(ox, oy, dpToPx(2f), markerPaint);

        if (isSelected) {
          // Tactical Reticle
          markerPaint.setStyle(Paint.Style.STROKE);
          markerPaint.setColor(0xFF00E5FF);
          markerPaint.setStrokeWidth(dpToPx(1.8f));
          canvas.drawCircle(ox, oy, dpToPx(16f), markerPaint);
        }
      }
    }
  }

  private int adjustAlpha(int color, float factor) {
    int alpha = Math.round(Color.alpha(color) * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }

  private float dpToPx(float dp) {
    return dp * getResources().getDisplayMetrics().density;
  }

  private float spToPx(float sp) {
    return sp * getResources().getDisplayMetrics().scaledDensity;
  }
}
