package com.example.newsagnet.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation destinations for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object News : Screen("news", "News", Icons.AutoMirrored.Filled.Article)
    object Globe : Screen("globe", "Globe", Icons.Filled.Public)
    object AiChat : Screen("ai_chat", "AI Intel", Icons.Filled.ChatBubble)


    companion object {
        val items = listOf(News, Globe, AiChat)
    }
}
