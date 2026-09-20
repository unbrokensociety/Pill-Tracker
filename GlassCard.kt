package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------------- */
/*  REAL LIQUID GLASS ENGINE                                                  */
/*  Real-time backdrop blur via GraphicsLayer recording + RenderEffect,       */
/*  plus refraction magnification, specular rim, sheen and tint overlays.     */
/*  Falls back gracefully to translucent glass on Android < 12 (API 31).      */
/* ------------------------------------------------------------------------- */

/** True when the device supports RenderEffect based backdrop blur (Android 12+). */
val BackdropBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Records everything drawn inside the modified node into [contentLayer] and
 * keeps drawing it normally. Attach to the content that should appear
 * blurred behind liquid glass panels (e.g. the pager behind the bottom bar).
 */
fun Modifier.glassSource(contentLayer: GraphicsLayer): Modifier =
    this.drawWithCache {
        onDrawWithContent {
            contentLayer.record {
                this@onDrawWithContent.drawContent()
            }
            drawLayer(contentLayer)
        }
    }

/**
 * Soft ambient "aurora" gradient decor drawn behind app content.
 * Gives the liquid glass real colorful content to refract and blur,
 * and lifts the overall look with a premium tinted depth.
 */
@Composable
fun Modifier.auroraBackdrop(): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val blobAlpha = if (isDark) 0.17f else 0.12f
    val washTop = if (isDark) Color(0x14FFFFFF) else Color.White.copy(alpha = 0.28f)

    return this.drawBehind {
        val w = size.width
        val h = size.height

        // very subtle vertical wash so flat backgrounds gain depth
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(washTop, Color.Transparent, Color.Transparent),
                startY = 0f,
                endY = h * 0.55f
            )
        )

        // soft color blobs (blurred beautifully by the liquid glass above)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = blobAlpha), Color.Transparent),
                center = Offset(0f, h * 0.10f),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(0f, h * 0.10f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = blobAlpha * 0.75f), Color.Transparent),
                center = Offset(w, h * 0.42f),
                radius = w * 0.70f
            ),
            radius = w * 0.70f,
            center = Offset(w, h * 0.42f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = blobAlpha * 0.65f), Color.Transparent),
                center = Offset(w * 0.35f, h),
                radius = w * 0.80f
            ),
            radius = w * 0.80f,
            center = Offset(w * 0.35f, h)
        )
    }
}

/**
 * THE real liquid glass panel.
 *
 * Draws a live blurred + refracted (magnified) copy of the content recorded by
 * [glassSource] behind this panel, then layers on top: glass tint, a top sheen
 * gradient, a diagonal specular streak, and a high-contrast specular rim.
 * On Android < 12 it falls back to a translucent tinted glass (still looks good).
 *
 * @param contentLayer shared layer recorded via [glassSource] on the background content.
 * @param sourceOriginProvider position (in root coordinates) of the node carrying [glassSource];
 *        used to translate the recorded drawing to this panel's position. May return null
 *        until the source has been positioned.
 * @param refraction subtle lens magnification of the blurred background (1f = none).
 * @param tint color cast of the glass. Use null for the theme default translucent surface.
 */
@Composable
fun LiquidGlassPanel(
    contentLayer: GraphicsLayer,
    sourceOriginProvider: () -> Offset?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    elevation: Dp = 16.dp,
    blurRadius: Dp = 24.dp,
    refraction: Float = 1.10f,
    tint: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val glassTint = tint ?: MaterialTheme.colorScheme.surface.copy(
        alpha = if (isDark) 0.40f else 0.26f
    )

    // Specular rim: bright at the top, softly lit at the bottom (Apple-like dual light)
    val rimTop = if (isDark) Color(1f, 1f, 1f, 0.34f) else Color(1f, 1f, 1f, 0.85f)
    val rimMid = if (isDark) Color(1f, 1f, 1f, 0.06f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
    val rimBottom = if (isDark) Color(1f, 1f, 1f, 0.16f) else Color(1f, 1f, 1f, 0.55f)
    val rimBrush = Brush.verticalGradient(listOf(rimTop, rimMid, rimBottom))

    val ambientShadowColor = if (isDark) Color(0x59000000) else Color(0x14000000)
    val spotShadowColor = if (isDark) Color(0x7A000000) else Color(0x29000000)

    val sheenTop = if (isDark) Color(1f, 1f, 1f, 0.09f) else Color(1f, 1f, 1f, 0.22f)
    val sheenMid = if (isDark) Color(1f, 1f, 1f, 0.03f) else Color(1f, 1f, 1f, 0.08f)
    val bottomShade = if (isDark) Color(0x33000000) else Color(0x0A000000)
    val streak = if (isDark) Color(1f, 1f, 1f, 0.05f) else Color(1f, 1f, 1f, 0.13f)

    var panelOrigin by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .onGloballyPositioned { panelOrigin = it.positionInRoot() }
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = ambientShadowColor,
                spotColor = spotShadowColor
            )
            .clip(shape)
            .then(
                if (BackdropBlurSupported) Modifier
                else Modifier.background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = if (isDark) 0.88f else 0.94f
                    )
                )
            )
    ) {
        if (BackdropBlurSupported) {
            // 1) Live blurred backdrop with subtle refraction magnification
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = BlurEffect(blurRadius.toPx(), blurRadius.toPx())
                    }
                    .drawBehind {
                        val srcOrigin = sourceOriginProvider() ?: return@drawBehind
                        val pos = panelOrigin ?: return@drawBehind
                        val dx = pos.x - srcOrigin.x
                        val dy = pos.y - srcOrigin.y
                        scale(
                            scale = refraction,
                            pivot = Offset(size.width * 0.5f, size.height * 0.5f)
                        ) {
                            translate(-dx, -dy) {
                                drawLayer(contentLayer)
                            }
                        }
                    }
            )
        }

        // 2) Glass tint + top sheen + bottom depth shading
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(color = glassTint)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(sheenTop, sheenMid, Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.8f
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bottomShade),
                            startY = size.height * 0.55f
                        )
                    )
                }
        )

        // 3) Diagonal specular streak (moving-light illusion)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    rotate(degrees = 16f, pivot = Offset(size.width * 0.5f, size.height * 0.5f)) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    streak,
                                    Color.Transparent,
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(-size.width * 0.25f, 0f),
                            size = Size(size.width * 1.5f, size.height)
                        )
                    }
                }
        )

        // 4) Specular rim on top
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = borderWidth, brush = rimBrush, shape = shape)
        )

        content()
    }
}

/* ------------------------------------------------------------------------- */
/*  Animation helpers                                                         */
/* ------------------------------------------------------------------------- */

/**
 * Staggered entrance animation for list items: fades + slides in with a
 * per-index delay. Great for medication cards and form sections.
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        if (!visibleState.targetState) {
            delay(index.coerceAtMost(8) * 40L)
            visibleState.targetState = true
        }
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(320, easing = EaseOutCubic)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 400f
                    ),
                    initialOffsetY = { it / 5 }
                ),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Gently floating icon used in empty states.
 */
@Composable
fun BobbingIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    circleSize: Dp = 64.dp,
    iconSize: Dp = 32.dp,
    circleAlpha: Float = 0.15f
) {
    val transition = rememberInfiniteTransition(label = "bob")
    val bobY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )
    val glow by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size(circleSize)
            .graphicsLayer {
                translationY = bobY.dp.toPx()
            }
            .clip(CircleShape)
            .background(tint.copy(alpha = circleAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = glow),
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Tactile interactions                                                      */
/* ------------------------------------------------------------------------- */

/**
 * Tactile spring press modifier for buttons, cards, and nav items.
 * Scales down smoothly on press, rebounds with organic spring physics
 * and can optionally trigger light haptic feedback.
 */
@Composable
fun Modifier.tactilePress(
    pressScale: Float = 0.94f,
    haptic: Boolean = false,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tactileScale"
    )

    if (haptic) {
        val view = LocalView.current
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        }
    }

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

/* ------------------------------------------------------------------------- */
/*  Legacy (non-blur) glass looks - kept as tasteful fallbacks / variants     */
/* ------------------------------------------------------------------------- */

/**
 * Core Liquid Glass Modifier applying Translucent Tinting,
 * Specular Edge Highlight (specular gradient rim), and Soft Ambient Shadow.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    customGlassColor: Color? = null,
    elevation: Dp = 10.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Specular Edge Highlight (rim gradient from Top light to Bottom-Right shadow)
    val specularTopLeft = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.22f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f)
    }
    val specularBottomRight = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.04f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(specularTopLeft, specularBottomRight)
    )

    val defaultGlassColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surface
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
 * Special Floating Island Glass Modifier specifically tuned for floating bars.
 * Soft translucent glass tinting and high-contrast specular rim.
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

/* ------------------------------------------------------------------------- */
/*  Glass building blocks                                                     */
/* ------------------------------------------------------------------------- */

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
        baseModifier.tactilePress(pressScale = 0.96f, haptic = true, onClick = onClick)
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        Column(
            modifier = Modifier.padding(contentPadding),
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

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = if (isDark) 0.22f else 0.14f))
            .border(
                width = 1.dp,
                color = tintColor.copy(alpha = if (isDark) 0.35f else 0.25f),
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
    val specularTopLeft = if (isDark) Color(1f, 1f, 1f, 0.30f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
    val specularBottomRight = if (isDark) Color(1f, 1f, 1f, 0.05f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(20.dp)
            ),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Volumetric Liquid Glass Floating Action Button with spring tactile bounce
 * and specular glow.
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
            .tactilePress(pressScale = 0.88f, haptic = true, onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
