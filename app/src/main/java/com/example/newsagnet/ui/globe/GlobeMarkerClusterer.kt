package com.example.newsagnet.ui.globe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.newsagnet.model.NewsArticle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents an aggregated spatial cluster or single news marker projected on screen.
 */
data class ComposeHotspotCluster(
    val articles: List<NewsArticle>,
    val screenOffset: Offset,
    val depthZ: Float,
    val primaryCategory: String,
    val isCluster: Boolean = articles.size > 1
)

object GlobeMarkerClusterer {

    /**
     * Category colors matching intelligence visualization standards:
     * Politics: Orange, Economy: Green, Technology: Cyan, Military: Red, Disaster: Yellow, Other: Gray
     */
    fun getCategoryColor(category: String): Color {
        return when (category.lowercase()) {
            "politics" -> Color(0xFFF97316)
            "economy", "business" -> Color(0xFF10B981)
            "technology" -> Color(0xFF06B6D4)
            "military" -> Color(0xFFEF4444)
            "disaster", "health" -> Color(0xFFF59E0B)
            "world", "science" -> Color(0xFF38BDF8)
            else -> Color(0xFF94A3B8)
        }
    }

    /**
     * Projects news articles to 3D spherical coordinates and aggregates nearby points into clusters.
     */
    fun clusterVisibleMarkers(
        articles: List<NewsArticle>,
        pitchRad: Float,
        yawRad: Float,
        radius: Float,
        centerX: Float,
        centerY: Float,
        clusterThresholdPx: Float = 60f
    ): List<ComposeHotspotCluster> {
        val visiblePoints = mutableListOf<Triple<Offset, Float, NewsArticle>>()

        for (article in articles) {
            val lat = article.latitude
            val lon = article.longitude
            if (lat != null && lon != null) {
                val latRad = (lat * PI / 180.0).toFloat()
                val lonRad = (lon * PI / 180.0).toFloat()
                val (sx, sy, sz) = project3DPoint(latRad, lonRad, pitchRad, yawRad, radius, centerX, centerY)

                if (sz > 0.04f) { // Front facing hemisphere
                    visiblePoints.add(Triple(Offset(sx, sy), sz, article))
                }
            }
        }

        val clusters = mutableListOf<ComposeHotspotCluster>()
        val visited = BooleanArray(visiblePoints.size)

        for (i in visiblePoints.indices) {
            if (visited[i]) continue

            val group = mutableListOf<NewsArticle>()
            var sumX = visiblePoints[i].first.x
            var sumY = visiblePoints[i].first.y
            var avgZ = visiblePoints[i].second
            group.add(visiblePoints[i].third)
            visited[i] = true

            for (j in i + 1 until visiblePoints.size) {
                if (visited[j]) continue
                val dx = visiblePoints[i].first.x - visiblePoints[j].first.x
                val dy = visiblePoints[i].first.y - visiblePoints[j].first.y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < clusterThresholdPx) {
                    group.add(visiblePoints[j].third)
                    sumX += visiblePoints[j].first.x
                    sumY += visiblePoints[j].first.y
                    avgZ = (avgZ + visiblePoints[j].second) / 2f
                    visited[j] = true
                }
            }

            val centerOffset = Offset(sumX / group.size, sumY / group.size)
            val primaryCat = group.first().category
            clusters.add(
                ComposeHotspotCluster(
                    articles = group,
                    screenOffset = centerOffset,
                    depthZ = avgZ,
                    primaryCategory = primaryCat,
                    isCluster = group.size > 1
                )
            )
        }

        return clusters
    }

    /**
     * Projects spherical coordinate (lat, lon) to screen (x, y, z) after yaw and pitch rotations.
     */
    fun project3DPoint(
        latRad: Float,
        lonRad: Float,
        pitchRad: Float,
        yawRad: Float,
        radius: Float,
        centerX: Float,
        centerY: Float
    ): Triple<Float, Float, Float> {
        val x0 = cos(latRad) * sin(lonRad)
        val y0 = -sin(latRad)
        val z0 = cos(latRad) * cos(lonRad)

        // Rotate Yaw (around Y axis)
        val x1 = x0 * cos(yawRad) + z0 * sin(yawRad)
        val z1 = -x0 * sin(yawRad) + z0 * cos(yawRad)
        val y1 = y0

        // Rotate Pitch (around X axis)
        val y2 = y1 * cos(pitchRad) - z1 * sin(pitchRad)
        val z2 = y1 * sin(pitchRad) + z1 * cos(pitchRad)
        val x2 = x1

        val screenX = centerX + x2 * radius
        val screenY = centerY + y2 * radius

        return Triple(screenX, screenY, z2)
    }
}
