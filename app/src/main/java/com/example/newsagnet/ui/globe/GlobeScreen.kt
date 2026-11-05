package com.example.newsagnet.ui.globe

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newsagnet.model.NewsArticle
import com.example.newsagnet.model.NewsCategory
import com.example.newsagnet.viewmodel.GlobeViewModel
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * 3D Globe Spatial Situation Screen displaying interactive news hotspots on a dark tactical globe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobeScreen(
    viewModel: GlobeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var worldMapVector by remember { mutableStateOf<WorldMapVector?>(null) }

    LaunchedEffect(Unit) {
        worldMapVector = WorldMapRepository.loadWorldMap(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Global News 3D Globe",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Interactive Spatial News Hotspots & Clustering",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NewsCategory.entries) { category ->
                    val isSelected = category == uiState.selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = {
                            Text(
                                text = category.name.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3D Tactical Globe Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF070B14)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0EA5E9).copy(alpha = 0.35f),
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Globe3DCanvas(
                                articles = uiState.articles,
                                worldMap = worldMapVector,
                                pitch = uiState.rotationX,
                                yaw = uiState.rotationY,
                                zoom = uiState.zoom,
                                selectedArticle = uiState.selectedArticle,
                                onRotate = viewModel::onGlobeRotate,
                                onZoom = viewModel::onGlobeZoom,
                                onArticleSelected = viewModel::selectArticle,
                                onClusterSelected = viewModel::selectCluster
                            )

                            // Status Overlay Badges
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Drag • Pinch to Zoom",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            // Intelligence Hub Tag Overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF10B981).copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GLOBAL INTEL PROJECTION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }

                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    // Hotspots Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Geolocated Situation Feed (${uiState.articles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.errorMessage != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "Network error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = viewModel::loadGeoNews) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    // Articles list with coordinates
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.articles.forEach { article ->
                            HotspotListItem(
                                article = article,
                                isSelected = uiState.selectedArticle?.id == article.id,
                                onClick = { viewModel.selectArticle(article) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                // Floating Selected Hotspot Card / Cluster Sheet
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.selectedClusterArticles.isNotEmpty() || uiState.selectedArticle != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    val articlesToShow = if (uiState.selectedClusterArticles.isNotEmpty()) {
                        uiState.selectedClusterArticles
                    } else {
                        listOfNotNull(uiState.selectedArticle)
                    }

                    HotspotClusterDetailCard(
                        articles = articlesToShow,
                        onClose = viewModel::clearSelectedArticle,
                        onArticleClick = { article ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

/**
 * High-performance 3D Spherical Globe Canvas rendering real geographic continents,
 * country borders, dark satellite ocean aesthetics, and pulsating news markers.
 */
@Composable
fun Globe3DCanvas(
    articles: List<NewsArticle>,
    worldMap: WorldMapVector?,
    pitch: Float,
    yaw: Float,
    zoom: Float,
    selectedArticle: NewsArticle?,
    onRotate: (deltaPitch: Float, deltaYaw: Float) -> Unit,
    onZoom: (deltaZoom: Float) -> Unit,
    onArticleSelected: (NewsArticle) -> Unit,
    onClusterSelected: (List<NewsArticle>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stores screen projections of visible markers & clusters for tap detection
    val projectedClusters = remember { mutableListOf<ComposeHotspotCluster>() }

    // Pulsing animation for radar rings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onRotate(-pan.y * 0.35f, pan.x * 0.35f)
                    if (gestureZoom != 1.0f) {
                        onZoom((gestureZoom - 1.0f) * 0.75f)
                    }
                }
            }
            .pointerInput(articles, pitch, yaw, zoom) {
                detectTapGestures { tapOffset ->
                    val hitRadius = 42f
                    val hit = projectedClusters.firstOrNull { cluster ->
                        val dx = cluster.screenOffset.x - tapOffset.x
                        val dy = cluster.screenOffset.y - tapOffset.y
                        sqrt(dx * dx + dy * dy) < hitRadius
                    }
                    if (hit != null) {
                        if (hit.isCluster) {
                            onClusterSelected(hit.articles)
                        } else {
                            onArticleSelected(hit.articles.first())
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (size.minDimension / 2f - 20.dp.toPx()) * zoom

        val pitchRad = (pitch * PI / 180.0).toFloat()
        val yawRad = (yaw * PI / 180.0).toFloat()

        // 1. Draw outer atmospheric glow (Rayleigh scattering halo)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.35f),
                    Color(0xFF0284C7).copy(alpha = 0.15f),
                    Color(0xFF0369A1).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = radius * 1.15f
            ),
            radius = radius * 1.15f,
            center = Offset(centerX, centerY)
        )

        // Sphere clip path ensuring all geography and oceans cleanly stay on sphere
        val sphereClipPath = Path().apply {
            addOval(Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
        }

        clipPath(sphereClipPath) {
            // 2. Oceanic Deep Blue Sphere Base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF101B2E), // Subtle center illumination
                        Color(0xFF09101C),
                        Color(0xFF04070D)  // Deep oceanic edge
                    ),
                    center = Offset(centerX - radius * 0.2f, centerY - radius * 0.2f),
                    radius = radius * 1.1f
                ),
                radius = radius,
                center = Offset(centerX, centerY)
            )

            // 3. Draw Parallels (Latitudes) and Meridians (Longitudes) Graticule
            val gridColor = Color(0xFF1E3A5F).copy(alpha = 0.35f)
            for (latDeg in listOf(-60, -30, 0, 30, 60)) {
                val latR = (latDeg * PI / 180.0).toFloat()
                val parallelPath = Path()
                var started = false
                for (lonDeg in 0..360 step 6) {
                    val lonR = (lonDeg * PI / 180.0).toFloat()
                    val (sx, sy, sz) = GlobeMarkerClusterer.project3DPoint(latR, lonR, pitchRad, yawRad, radius, centerX, centerY)
                    if (sz > 0.01f) {
                        if (!started) {
                            parallelPath.moveTo(sx, sy)
                            started = true
                        } else {
                            parallelPath.lineTo(sx, sy)
                        }
                    } else {
                        started = false
                    }
                }
                drawPath(parallelPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            for (lonDeg in 0 until 360 step 30) {
                val lonR = (lonDeg * PI / 180.0).toFloat()
                val meridianPath = Path()
                var started = false
                for (latDeg in -85..85 step 5) {
                    val latR = (latDeg * PI / 180.0).toFloat()
                    val (sx, sy, sz) = GlobeMarkerClusterer.project3DPoint(latR, lonR, pitchRad, yawRad, radius, centerX, centerY)
                    if (sz > 0.01f) {
                        if (!started) {
                            meridianPath.moveTo(sx, sy)
                            started = true
                        } else {
                            meridianPath.lineTo(sx, sy)
                        }
                    } else {
                        started = false
                    }
                }
                drawPath(meridianPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // 4. Draw Real Continents and Landmasses (Natural Earth 110m)
            worldMap?.let { mapData ->
                val landFillColor = Color(0xFF192535)
                val coastlineColor = Color(0xFF38BDF8).copy(alpha = 0.42f)

                for (ring in mapData.landRings) {
                    val landPath = Path()
                    var pathStarted = false
                    var i = 0
                    val len = ring.size
                    while (i < len) {
                        val lat = ring[i]
                        val lon = ring[i + 1]
                        i += 2
                        val latR = (lat * PI / 180.0).toFloat()
                        val lonR = (lon * PI / 180.0).toFloat()
                        val (sx, sy, sz) = GlobeMarkerClusterer.project3DPoint(latR, lonR, pitchRad, yawRad, radius, centerX, centerY)

                        if (sz > -0.05f) {
                            if (!pathStarted) {
                                landPath.moveTo(sx, sy)
                                pathStarted = true
                            } else {
                                landPath.lineTo(sx, sy)
                            }
                        } else {
                            pathStarted = false
                        }
                    }

                    // Fill land polygon
                    drawPath(landPath, color = landFillColor)
                    // Stroke coastline outline
                    drawPath(landPath, color = coastlineColor, style = Stroke(width = 1.2.dp.toPx()))
                }

                // 5. Draw Country Sovereign Borders
                val borderColor = Color(0xFF334E68).copy(alpha = 0.40f)
                for (border in mapData.borderLines) {
                    val borderPath = Path()
                    var borderStarted = false
                    var i = 0
                    val len = border.size
                    while (i < len) {
                        val lat = border[i]
                        val lon = border[i + 1]
                        i += 2
                        val latR = (lat * PI / 180.0).toFloat()
                        val lonR = (lon * PI / 180.0).toFloat()
                        val (sx, sy, sz) = GlobeMarkerClusterer.project3DPoint(latR, lonR, pitchRad, yawRad, radius, centerX, centerY)

                        if (sz > 0.02f) {
                            if (!borderStarted) {
                                borderPath.moveTo(sx, sy)
                                borderStarted = true
                            } else {
                                borderPath.lineTo(sx, sy)
                            }
                        } else {
                            borderStarted = false
                        }
                    }
                    drawPath(borderPath, color = borderColor, style = Stroke(width = 0.8.dp.toPx()))
                }
            }

            // 6. Draw Global Intelligence Arcs (Hub to Hub connections)
            val intelligenceArcs = listOf(
                Pair(Pair(50.45f, 30.52f), Pair(38.90f, -77.03f)), // Kyiv <-> Washington
                Pair(Pair(51.50f, -0.12f), Pair(35.67f, 139.65f)), // London <-> Tokyo
                Pair(Pair(46.20f, 6.14f), Pair(1.35f, 103.81f))    // Geneva <-> Singapore
            )
            for ((fromCoord, toCoord) in intelligenceArcs) {
                val fromLatR = (fromCoord.first * PI / 180.0).toFloat()
                val fromLonR = (fromCoord.second * PI / 180.0).toFloat()
                val toLatR = (toCoord.first * PI / 180.0).toFloat()
                val toLonR = (toCoord.second * PI / 180.0).toFloat()

                val p1 = GlobeMarkerClusterer.project3DPoint(fromLatR, fromLonR, pitchRad, yawRad, radius, centerX, centerY)
                val p2 = GlobeMarkerClusterer.project3DPoint(toLatR, toLonR, pitchRad, yawRad, radius, centerX, centerY)

                if (p1.third > 0.05f || p2.third > 0.05f) {
                    val arcPath = Path()
                    arcPath.moveTo(p1.first, p1.second)
                    val midX = (p1.first + p2.first) / 2f
                    val midY = (p1.second + p2.second) / 2f - 18.dp.toPx()
                    arcPath.quadraticTo(midX, midY, p2.first, p2.second)
                    drawPath(
                        path = arcPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.45f),
                                Color(0xFF38BDF8).copy(alpha = 0.25f)
                            )
                        ),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }

            // 7. Spherical 3D Directional Lighting Overlay
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color.Transparent,
                        Color(0xFF020617).copy(alpha = 0.60f)
                    ),
                    center = Offset(centerX - radius * 0.35f, centerY - radius * 0.35f),
                    radius = radius * 1.35f
                ),
                radius = radius,
                center = Offset(centerX, centerY)
            )
        }

        // 8. Draw Atmospheric Rim Border
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.55f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 9. Cluster and Render Hotspots
        projectedClusters.clear()
        val clusters = GlobeMarkerClusterer.clusterVisibleMarkers(
            articles = articles,
            pitchRad = pitchRad,
            yawRad = yawRad,
            radius = radius,
            centerX = centerX,
            centerY = centerY,
            clusterThresholdPx = 54f
        )
        projectedClusters.addAll(clusters)

        for (cluster in clusters) {
            val offset = cluster.screenOffset
            val catColor = GlobeMarkerClusterer.getCategoryColor(cluster.primaryCategory)
            val isSelected = selectedArticle != null && cluster.articles.any { it.id == selectedArticle.id }

            if (cluster.isCluster) {
                // Multi-article Cluster Badge
                val pulseRadius = 14.dp.toPx() + (8.dp.toPx() * pulseProgress)
                drawCircle(
                    color = catColor.copy(alpha = (1f - pulseProgress) * 0.45f),
                    radius = pulseRadius,
                    center = offset
                )

                // Cluster Body
                drawCircle(
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    radius = 13.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = if (isSelected) Color(0xFF00E5FF) else catColor,
                    radius = 13.dp.toPx(),
                    center = offset,
                    style = Stroke(width = if (isSelected) 2.5.dp.toPx() else 1.8.dp.toPx())
                )

                // Draw Count Text
                val countText = cluster.articles.size.toString()
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(countText, offset.x, offset.y + 10f, paint)
                }
            } else {
                // Single Hotspot Marker
                val pulseRadius = 10.dp.toPx() + (12.dp.toPx() * pulseProgress)
                drawCircle(
                    color = catColor.copy(alpha = (1f - pulseProgress) * 0.45f),
                    radius = pulseRadius,
                    center = offset
                )

                // Halo
                drawCircle(
                    color = catColor.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = offset
                )

                // Core Dot
                drawCircle(
                    color = catColor,
                    radius = 4.5.dp.toPx(),
                    center = offset
                )

                // White Center Eye
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = offset
                )

                if (isSelected) {
                    // Tactical Target Reticle for Selected Item
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 16.dp.toPx(),
                        center = offset,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun HotspotListItem(
    article: NewsArticle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = GlobeMarkerClusterer.getCategoryColor(article.category)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(categoryColor, MaterialTheme.colorScheme.primary)
                )
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = categoryColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.locationName ?: "Global",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                    article.latitude?.let { lat ->
                        article.longitude?.let { lon ->
                            Text(
                                text = " (%.1f°, %.1f°)".format(lat, lon),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = " • ${article.sourceName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Floating Detail Card supporting both single verified hotspots and multi-news clusters.
 */
@Composable
private fun HotspotClusterDetailCard(
    articles: List<NewsArticle>,
    onClose: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    val first = articles.firstOrNull() ?: return
    val categoryColor = GlobeMarkerClusterer.getCategoryColor(first.category)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.2f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(categoryColor, Color(0xFF38BDF8))))
                    ) {
                        Text(
                            text = first.locationName ?: "Global Hotspot",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (articles.size > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${articles.size} Stories",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Articles in Cluster
            if (articles.size == 1) {
                val article = articles.first()
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Category: ${article.category.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onArticleClick(article) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read Original Article", color = Color.White)
                }
            } else {
                // Multi-story scrollable cluster list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(articles) { article ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                                .clickable { onArticleClick(article) }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = article.sourceName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Open",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Launch,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
