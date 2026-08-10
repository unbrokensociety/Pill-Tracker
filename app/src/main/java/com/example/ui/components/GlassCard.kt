package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Core Liquid Glass Modifier applying Translucent Tinting,
 * Specular Edge Highlight (1px specular gradient rim), and Soft Ambient Shadow.
 * Text and children composables inside remain 100% crisp and readable.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    customGlassColor: Color? = null,
    elevation: Dp = 12.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = isSystemInDarkTheme()

    // Specular Edge Highlight (1px Rim gradient from Top-Left light to Bottom-Right shadow)
    val specularTopLeft = if (isDark) Color(1.0f, 1.0f, 1.0f, 0.30f) else Color(1.0f, 1.0f, 1.0f, 0.80f)
    val specularBottomRight = if (isDark) Color(1.0f, 1.0f, 1.0f, 0.03f) else Color(1.0f, 1.0f, 1.0f, 0.15f)
    val borderBrush = Brush.linearGradient(
        colors = listOf(specularTopLeft, specularBottomRight)
    )

    // Liquid Glass Translucent Surface Colors (Light: 65% translucent white, Dark: 65% graphite)
    val defaultGlassColor = if (isDark) {
        Color(0xA61C1C1E) // rgba(28, 28, 30, 0.65)
    } else {
        Color(0xA6FFFFFF) // rgba(255, 255, 255, 0.65)
    }
    val glassColor = customGlassColor ?: defaultGlassColor

    val ambientShadowColor = if (isDark) Color(0x66000000) else Color(0x1A000000)
    val spotShadowColor = if (isDark) Color(0x80000000) else Color(0x1F000000)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambientShadowColor,
            spotColor = spotShadowColor
        )
        .clip(shape)
        .background(glassColor)
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = shape
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    glassAlpha: Float = 1.0f,
    contentPadding: Dp = 16.dp,
    elevation: Dp = 10.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val glassColor = if (isDark) {
        Color(0x1C, 0x1C, 0x1E, (255 * 0.65f * glassAlpha).toInt())
    } else {
        Color(0xFF, 0xFF, 0xFF, (255 * 0.65f * glassAlpha).toInt())
    }

    val baseModifier = modifier
        .liquidGlass(
            shape = shape,
            customGlassColor = glassColor,
            elevation = elevation
        )

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * Floating Mini Glass Circle for medication icon / status badges.
 */
@Composable
fun GlassCircleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val specularTopLeft = if (isDark) Color(1f, 1f, 1f, 0.40f) else Color(1f, 1f, 1f, 0.90f)
    val specularBottomRight = if (isDark) Color(1f, 1f, 1f, 0.05f) else Color(1f, 1f, 1f, 0.20f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = if (isDark) 0.22f else 0.14f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Liquid Glass Status Chip (e.g. Taken, Missed, Scheduled).
 */
@Composable
fun GlassChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val specularTopLeft = if (isDark) Color(1f, 1f, 1f, 0.45f) else Color(1f, 1f, 1f, 0.85f)
    val specularBottomRight = if (isDark) Color(1f, 1f, 1f, 0.05f) else Color(1f, 1f, 1f, 0.15f)

    Surface(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(20.dp)
            ),
        color = containerColor.copy(alpha = if (isDark) 0.35f else 0.22f),
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Volumetric Liquid Glass Floating Action Button with specular rim and vibrant glow.
 */
@Composable
fun GlassFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val specularTopLeft = Color(1f, 1f, 1f, 0.85f)
    val specularBottomRight = Color(1f, 1f, 1f, 0.20f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = containerColor.copy(alpha = 0.5f),
                spotColor = containerColor.copy(alpha = 0.8f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}



