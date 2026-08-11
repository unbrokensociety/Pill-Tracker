package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun isPowerSaveMode(): Boolean {
    val ctx = LocalContext.current
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return pm.isPowerSaveMode
}

/**
 * Tactile spring press modifier for buttons, cards, and nav items.
 * Scales down smoothly on press and rebounds with organic spring physics.
 */
@Composable
fun Modifier.tactilePress(
    pressScale: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "tactileScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Core Liquid Glass Modifier applying Translucent Tinting,
 * Specular Edge Highlight (1px specular gradient rim), and Soft Ambient Shadow.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    customGlassColor: Color? = null,
    elevation: Dp = 10.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Specular Edge Highlight (1px Rim gradient from Top-Left light to Bottom-Right shadow)
    val specularTopLeft = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.22f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)
    }
    val specularBottomRight = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.04f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(specularTopLeft, specularBottomRight)
    )

    val defaultGlassColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }
    val glassColor = customGlassColor ?: defaultGlassColor

    val ambientShadowColor = if (isDark) Color(0x66000000) else Color(0x0D000000)
    val spotShadowColor = if (isDark) Color(0x80000000) else Color(0x1A000000)

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

/**
 * Special Floating Island Glass Modifier specifically tuned for the Bottom Navigation Capsule.
 * Combines hardware backdrop blur (API 31+), soft translucent glass tinting, and high-contrast specular rim.
 */
@Composable
fun Modifier.islandGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 16.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val specularTopLeft = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.30f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)
    }
    val specularBottomRight = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.05f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(specularTopLeft, specularBottomRight)
    )

    val glassColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }

    val ambientShadowColor = if (isDark) Color(0x66000000) else Color(0x12000000)
    val spotShadowColor = if (isDark) Color(0x80000000) else Color(0x24000000)

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
    elevation: Dp = 8.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val defaultSurface = MaterialTheme.colorScheme.surface
    val glassColor = if (isDark) {
        defaultSurface.copy(alpha = 0.85f * glassAlpha)
    } else {
        defaultSurface.copy(alpha = 0.96f * glassAlpha)
    }

    val baseModifier = modifier
        .liquidGlass(
            shape = shape,
            customGlassColor = glassColor,
            elevation = elevation
        )

    val finalModifier = if (onClick != null) {
        baseModifier.tactilePress(pressScale = 0.96f, onClick = onClick)
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val specularTopLeft = if (isDark) Color(1f, 1f, 1f, 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
    val specularBottomRight = if (isDark) Color(1f, 1f, 1f, 0.05f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = if (isDark) 0.25f else 0.16f))
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val specularTopLeft = if (isDark) Color(1f, 1f, 1f, 0.40f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
    val specularBottomRight = if (isDark) Color(1f, 1f, 1f, 0.05f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    Surface(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(20.dp)
            ),
        color = containerColor.copy(alpha = if (isDark) 0.35f else 0.18f),
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
 * Volumetric Liquid Glass Floating Action Button with spring tactile bounce and specular glow.
 */
@Composable
fun GlassFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    val specularTopLeft = Color(1f, 1f, 1f, 0.85f)
    val specularBottomRight = Color(1f, 1f, 1f, 0.20f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = containerColor.copy(alpha = 0.4f),
                spotColor = containerColor.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.88f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(24.dp)
            )
            .tactilePress(pressScale = 0.88f, onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
