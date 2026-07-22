package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    glassAlpha: Float = 0.45f,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    
    val glassBorder = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            )
        )
    }

    val glassBg = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = glassAlpha),
                MaterialTheme.colorScheme.surface.copy(alpha = glassAlpha * 0.75f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = glassAlpha)
            )
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassBg)
            .border(BorderStroke(1.2.dp, glassBorder), shape)
            .then(clickableModifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
