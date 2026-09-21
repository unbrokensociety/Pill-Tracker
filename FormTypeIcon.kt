package com.aistudio.meditracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R

enum class FormType(val key: String, val stringRes: Int) {
    CAPSULE("capsule", R.string.form_capsule),
    TABLET("tablet", R.string.form_tablet),
    LIQUID("liquid", R.string.form_liquid),
    DROPS("drops", R.string.form_drops),
    INJECTION("injection", R.string.form_injection),
    SPRAY("spray", R.string.form_spray),
    PATCH("patch", R.string.form_patch);

    companion object {
        fun fromKey(key: String): FormType {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: CAPSULE
        }
    }
}

/**
 * High-fidelity medication form icons — v3.
 *
 * Redrawn for crispness: every glyph now fills ~90% of the canvas, keeps one
 * consistent outline weight, and drops micro-details that turned to mush at
 * 20–24dp. Volumetric shading (light top-left → saturated bottom-right) and
 * one confident gloss stroke per icon make them read as tiny 3D objects.
 */
@Composable
fun FormTypeIcon(
    formKey: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = tint.copy(alpha = 0.18f),
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val formType = FormType.fromKey(formKey)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val rim = tint.copy(alpha = if (isDark) 0.32f else 0.24f)

    // radial badge highlight (needs px conversion for the gradient center)
    val density = LocalDensity.current
    val badgeRadiusPx = with(density) { size.toPx() } * 0.9f
    val badgeCenter = Offset(badgeRadiusPx * 0.39f, badgeRadiusPx * 0.33f)
    val badgeLight = lerp(backgroundColor, Color.White, if (isDark) 0.04f else 0.35f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(badgeLight, backgroundColor),
                    center = badgeCenter,
                    radius = badgeRadiusPx
                )
            )
            .border(width = 1.dp, color = rim, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            when (formType) {
                FormType.TABLET -> drawTablet(tint)
                FormType.CAPSULE -> drawCapsule(tint)
                FormType.LIQUID -> drawLiquidBottle(tint)
                FormType.DROPS -> drawDroplet(tint)
                FormType.INJECTION -> drawSyringe(tint)
                FormType.SPRAY -> drawSprayBottle(tint)
                FormType.PATCH -> drawPatch(tint)
            }
        }
    }
}

/* Reading "luminance" for the radial badge background */
private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

/* ------------------------------------------------------------------------- */
/*  Tablet — bold round pill with score line                                  */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawTablet(tint: Color) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h / 2f)
    val radius = w * 0.46f
    val light = lerp(tint, Color.White, 0.50f)
    val mid = tint
    val dark = lerp(tint, Color.Black, 0.16f)
    val outline = 2.2.dp.toPx()

    // Volumetric body: light source top-left
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(light, mid, dark),
            center = Offset(center.x - radius * 0.32f, center.y - radius * 0.40f),
            radius = radius * 1.40f
        ),
        radius = radius,
        center = center
    )

    // Crisp outline
    drawCircle(
        color = tint,
        radius = radius - outline / 2f,
        center = center,
        style = Stroke(width = outline)
    )

    // Bold horizontal score line (embossed: dark under-stroke + light top-stroke)
    val half = radius * 0.66f
    drawLine(
        color = dark.copy(alpha = 0.55f),
        start = Offset(center.x - half + 0.8.dp.toPx(), center.y + 0.8.dp.toPx()),
        end = Offset(center.x + half + 0.8.dp.toPx(), center.y + 0.8.dp.toPx()),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = lerp(tint, Color.White, 0.62f),
        start = Offset(center.x - half, center.y),
        end = Offset(center.x + half, center.y),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Confident gloss arc across the upper edge
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.90f), Color.White.copy(alpha = 0.10f)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius * 0.2f, center.y - radius * 0.8f)
        ),
        startAngle = 200f,
        sweepAngle = 80f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
        size = Size(radius * 1.56f, radius * 1.56f),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
    )
}

/* ------------------------------------------------------------------------- */
/*  Capsule — two-tone diagonal capsule                                       */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawCapsule(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.55f)
    val outline = 2.2.dp.toPx()

    rotate(degrees = -30f, pivot = Offset(w / 2f, h / 2f)) {
        val capWidth = w * 0.50f
        val capHeight = h * 0.96f
        val left = (w - capWidth) / 2f
        val top = (h - capHeight) / 2f
        val half = top + capHeight / 2f
        val corner = CornerRadius(capWidth / 2f, capWidth / 2f)

        // Lower half — tinted translucent body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(light.copy(alpha = 0.80f), tint.copy(alpha = 0.55f)),
                startY = half,
                endY = top + capHeight
            ),
            topLeft = Offset(left, half),
            size = Size(capWidth, capHeight / 2f),
            cornerRadius = corner
        )

        // Upper half — solid gradient cap (saturated)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.30f), tint),
                startY = top,
                endY = half
            ),
            topLeft = Offset(left, top),
            size = Size(capWidth, capHeight / 2f),
            cornerRadius = corner
        )

        // Bold outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(capWidth, capHeight),
            cornerRadius = corner,
            style = Stroke(width = outline)
        )

        // Cap seam (embossed divider)
        drawLine(
            color = lerp(tint, Color.Black, 0.20f),
            start = Offset(left, half + 0.8.dp.toPx()),
            end = Offset(left + capWidth, half + 0.8.dp.toPx()),
            strokeWidth = 2.8.dp.toPx()
        )
        drawLine(
            color = lerp(tint, Color.White, 0.55f),
            start = Offset(left, half),
            end = Offset(left + capWidth, half),
            strokeWidth = 2.4.dp.toPx()
        )

        // One confident gloss streak along the cap
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.10f)
                ),
                startY = top + capHeight * 0.10f,
                endY = top + capHeight * 0.46f
            ),
            start = Offset(left + capWidth * 0.24f, top + capHeight * 0.10f),
            end = Offset(left + capWidth * 0.24f, top + capHeight * 0.46f),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Liquid — syrup bottle with shoulders, cap and liquid                      */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawLiquidBottle(tint: Color) {
    val w = size.width
    val h = size.height
    val outline = 2.2.dp.toPx()

    val bottleW = w * 0.62f
    val bottleH = h * 0.68f
    val left = (w - bottleW) / 2f
    val top = h * 0.30f
    val bodyCorner = CornerRadius(5.dp.toPx(), 5.dp.toPx())

    // Cap (rounded, gradient)
    val capW = bottleW * 0.46f
    val capH = h * 0.12f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(lerp(tint, Color.White, 0.30f), tint),
            startY = h * 0.10f,
            endY = h * 0.10f + capH
        ),
        topLeft = Offset((w - capW) / 2f, h * 0.10f),
        size = Size(capW, capH),
        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
    )

    // Neck
    val neckW = bottleW * 0.34f
    drawRect(
        color = tint.copy(alpha = 0.85f),
        topLeft = Offset((w - neckW) / 2f, h * 0.22f),
        size = Size(neckW, h * 0.08f)
    )

    // Glass body (translucent side shading)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                tint.copy(alpha = 0.12f),
                tint.copy(alpha = 0.28f),
                tint.copy(alpha = 0.12f)
            ),
            startX = left,
            endX = left + bottleW
        ),
        topLeft = Offset(left, top),
        size = Size(bottleW, bottleH),
        cornerRadius = bodyCorner
    )

    // Liquid inside (70% full) with rounded meniscus
    val liquidH = bottleH * 0.70f
    val liquidTopY = top + bottleH - liquidH
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(lerp(tint, Color.White, 0.28f), tint),
            startY = liquidTopY,
            endY = top + bottleH
        ),
        topLeft = Offset(left + outline, liquidTopY),
        size = Size(bottleW - outline * 2f, liquidH),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    // Meniscus highlight
    drawLine(
        color = Color.White.copy(alpha = 0.65f),
        start = Offset(left + 4.dp.toPx(), liquidTopY + 1.2.dp.toPx()),
        end = Offset(left + bottleW - 4.dp.toPx(), liquidTopY + 1.2.dp.toPx()),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Label band with a dosage mark
    val labelY = top + bottleH * 0.42f
    drawRect(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(left + outline, labelY),
        size = Size(bottleW - outline * 2f, bottleH * 0.22f)
    )
    drawLine(
        color = tint,
        start = Offset(left + bottleW * 0.32f, labelY + bottleH * 0.09f),
        end = Offset(left + bottleW * 0.68f, labelY + bottleH * 0.09f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = tint.copy(alpha = 0.7f),
        start = Offset(left + bottleW * 0.40f, labelY + bottleH * 0.15f),
        end = Offset(left + bottleW * 0.60f, labelY + bottleH * 0.15f),
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Outer bottle border
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(bottleW, bottleH),
        cornerRadius = bodyCorner,
        style = Stroke(width = outline)
    )

    // One glossy reflection along the right edge
    drawLine(
        color = Color.White.copy(alpha = 0.60f),
        start = Offset(left + bottleW - 3.5.dp.toPx(), top + 3.dp.toPx()),
        end = Offset(left + bottleW - 3.5.dp.toPx(), top + bottleH - 3.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/* ------------------------------------------------------------------------- */
/*  Drops — bold teardrop with companion drop                                 */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawDroplet(tint: Color) {
    val w = size.width
    val h = size.height
    val outline = 2.2.dp.toPx()

    // Main teardrop — slightly offset left so the small drop fits
    scale(scale = 0.88f, pivot = Offset(w * 0.44f, h * 0.5f)) {
        val dropPath = Path().apply {
            moveTo(w / 2f, h * 0.04f)
            cubicTo(
                w * 0.96f, h * 0.48f,
                w * 0.96f, h * 0.94f,
                w / 2f, h * 0.94f
            )
            cubicTo(
                w * 0.04f, h * 0.94f,
                w * 0.04f, h * 0.48f,
                w / 2f, h * 0.04f
            )
            close()
        }

        // Volumetric fill
        drawPath(
            path = dropPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerp(tint, Color.White, 0.42f),
                    tint,
                    lerp(tint, Color.Black, 0.14f)
                ),
                startY = h * 0.04f,
                endY = h * 0.94f
            )
        )
        drawPath(
            path = dropPath,
            color = tint,
            style = Stroke(width = outline)
        )

        // Bold glossy curve on the left side
        val highlightPath = Path().apply {
            moveTo(w * 0.38f, h * 0.34f)
            cubicTo(w * 0.24f, h * 0.52f, w * 0.24f, h * 0.74f, w * 0.34f, h * 0.84f)
        }
        drawPath(
            path = highlightPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.25f)),
                startY = h * 0.34f,
                endY = h * 0.84f
            ),
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Companion mini-drop (reads as plural "drops")
    val miniPath = Path().apply {
        val cx = w * 0.84f
        val cy = h * 0.30f
        val r = w * 0.13f
        moveTo(cx, cy - r * 1.6f)
        cubicTo(cx + r, cy - r * 0.4f, cx + r, cy + r * 0.6f, cx, cy + r)
        cubicTo(cx - r, cy + r * 0.6f, cx - r, cy - r * 0.4f, cx, cy - r * 1.6f)
        close()
    }
    drawPath(
        path = miniPath,
        color = lerp(tint, Color.White, 0.25f)
    )
    drawPath(
        path = miniPath,
        color = tint,
        style = Stroke(width = 1.6.dp.toPx())
    )

    // Sparkle
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = 1.6.dp.toPx(),
        center = Offset(w * 0.60f, h * 0.22f)
    )
}

/* ------------------------------------------------------------------------- */
/*  Injection — clean syringe, diagonal                                        */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawSyringe(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.40f)
    val outline = 2.2.dp.toPx()

    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
        val barrelW = w * 0.42f
        val barrelH = h * 0.46f
        val barrelLeft = (w - barrelW) / 2f
        val barrelTop = h * 0.26f

        // 1. Needle
        val needleTop = h * 0.03f
        val needleBottom = barrelTop - h * 0.03f
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.65f), tint),
                startY = needleTop,
                endY = needleBottom
            ),
            start = Offset(w / 2f, needleBottom),
            end = Offset(w / 2f, needleTop),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Beveled tip glint
        drawLine(
            color = Color.White,
            start = Offset(w / 2f, needleTop + 1.dp.toPx()),
            end = Offset(w / 2f, needleTop + 3.5.dp.toPx()),
            strokeWidth = 1.2.dp.toPx()
        )

        // 2. Luer-lock hub
        val hubW = barrelW * 0.56f
        val hubH = h * 0.07f
        val hubLeft = (w - hubW) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(light, tint),
                startX = hubLeft,
                endX = hubLeft + hubW
            ),
            topLeft = Offset(hubLeft, barrelTop - hubH),
            size = Size(hubW, hubH),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )

        // 3. Glass barrel
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    tint.copy(alpha = 0.16f),
                    tint.copy(alpha = 0.34f),
                    tint.copy(alpha = 0.16f)
                ),
                startX = barrelLeft,
                endX = barrelLeft + barrelW
            ),
            topLeft = Offset(barrelLeft, barrelTop),
            size = Size(barrelW, barrelH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // 4. Fluid dose
        val fluidH = barrelH * 0.55f
        val fluidTop = barrelTop + barrelH - fluidH
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.32f), tint),
                startY = fluidTop,
                endY = fluidTop + fluidH
            ),
            topLeft = Offset(barrelLeft + outline, fluidTop),
            size = Size(barrelW - outline * 2f, fluidH)
        )

        // 5. Piston stopper
        val stopperH = 5.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(barrelLeft + 1.2.dp.toPx(), fluidTop - stopperH),
            size = Size(barrelW - 2.4.dp.toPx(), stopperH),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // 6. Three bold graduation marks (long/short/long)
        val markX1 = barrelLeft + 2.5.dp.toPx()
        val marksY = listOf(0.22f, 0.45f, 0.68f)
        marksY.forEachIndexed { i, f ->
            val markY = barrelTop + barrelH * f
            val xEnd = barrelLeft + barrelW * if (i == 1) 0.28f else 0.42f
            drawLine(
                color = if (markY >= fluidTop) Color.White.copy(alpha = 0.95f) else tint,
                start = Offset(markX1, markY),
                end = Offset(xEnd, markY),
                strokeWidth = 1.8.dp.toPx()
            )
        }

        // Glossy reflection along the right edge
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(barrelLeft + barrelW - 3.dp.toPx(), barrelTop + 3.dp.toPx()),
            end = Offset(barrelLeft + barrelW - 3.dp.toPx(), barrelTop + barrelH - 3.dp.toPx()),
            strokeWidth = 1.8.dp.toPx()
        )

        // Barrel outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(barrelLeft, barrelTop),
            size = Size(barrelW, barrelH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = outline)
        )

        // 7. Finger flange
        val flangeW = barrelW * 1.85f
        val flangeLeft = (w - flangeW) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(light, tint, light),
                startX = flangeLeft,
                endX = flangeLeft + flangeW
            ),
            topLeft = Offset(flangeLeft, barrelTop + barrelH),
            size = Size(flangeW, 4.5.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 8. Plunger shaft
        val shaftW = barrelW * 0.26f
        val shaftLeft = (w - shaftW) / 2f
        val shaftTop = barrelTop + barrelH + 4.5.dp.toPx()
        val shaftBottom = h * 0.90f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(lerp(tint, Color.White, 0.55f), tint),
                startX = shaftLeft,
                endX = shaftLeft + shaftW
            ),
            topLeft = Offset(shaftLeft, shaftTop),
            size = Size(shaftW, shaftBottom - shaftTop)
        )

        // 9. Thumb disc
        val discW = barrelW * 1.50f
        val discLeft = (w - discW) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(light, tint, light),
                startX = discLeft,
                endX = discLeft + discW
            ),
            topLeft = Offset(discLeft, shaftBottom),
            size = Size(discW, 4.5.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Spray — pump bottle with bold mist                                        */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawSprayBottle(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.42f)
    val outline = 2.2.dp.toPx()

    val sW = w * 0.52f
    val sH = h * 0.56f
    val left = w * 0.10f
    val top = h * 0.38f
    val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())

    // Liquid fill (75%)
    val liquidTopY = top + sH * 0.25f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(lerp(tint, Color.White, 0.28f), tint),
            startY = liquidTopY,
            endY = top + sH
        ),
        topLeft = Offset(left + outline, liquidTopY),
        size = Size(sW - outline * 2f, sH * 0.75f - outline),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Translucent glass body
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                tint.copy(alpha = 0.14f),
                tint.copy(alpha = 0.30f),
                tint.copy(alpha = 0.14f)
            ),
            startX = left,
            endX = left + sW
        ),
        topLeft = Offset(left, top),
        size = Size(sW, sH),
        cornerRadius = corner
    )

    // Outline
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(sW, sH),
        cornerRadius = corner,
        style = Stroke(width = outline)
    )

    // Pump neck
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(light, tint),
            startY = top - 7.dp.toPx(),
            endY = top
        ),
        start = Offset(left + sW / 2f, top),
        end = Offset(left + sW / 2f, top - 7.dp.toPx()),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Pump head + nozzle pointing right
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(light, tint),
            startX = left + sW / 2f - sW * 0.32f,
            endX = left + sW / 2f + sW * 0.36f
        ),
        topLeft = Offset(left + sW / 2f - sW * 0.32f, top - 11.dp.toPx()),
        size = Size(sW * 0.68f, 4.2.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(left + sW / 2f + sW * 0.30f, top - 10.4.dp.toPx()),
        size = Size(sW * 0.34f, 3.dp.toPx()),
        cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
    )

    // Bold radiating mist: 3 streaks + fading dots
    val nozzleX = left + sW / 2f + sW * 0.66f
    val nozzleY = top - 9.dp.toPx()
    drawLine(color = tint, start = Offset(nozzleX + 2.dp.toPx(), nozzleY - 1.5.dp.toPx()), end = Offset(nozzleX + 9.dp.toPx(), nozzleY - 6.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = tint, start = Offset(nozzleX + 2.dp.toPx(), nozzleY), end = Offset(nozzleX + 12.dp.toPx(), nozzleY), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = tint, start = Offset(nozzleX + 2.dp.toPx(), nozzleY + 1.5.dp.toPx()), end = Offset(nozzleX + 9.dp.toPx(), nozzleY + 6.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    val mistDot = 1.5.dp.toPx()
    drawCircle(color = tint.copy(alpha = 0.75f), radius = mistDot, center = Offset(nozzleX + 12.dp.toPx(), nozzleY - 7.dp.toPx()))
    drawCircle(color = tint.copy(alpha = 0.60f), radius = mistDot, center = Offset(nozzleX + 14.dp.toPx(), nozzleY - 1.dp.toPx()))
    drawCircle(color = tint.copy(alpha = 0.65f), radius = mistDot, center = Offset(nozzleX + 13.dp.toPx(), nozzleY + 5.dp.toPx()))

    // Glossy reflection
    drawLine(
        color = Color.White.copy(alpha = 0.60f),
        start = Offset(left + sW - 3.5.dp.toPx(), top + 4.dp.toPx()),
        end = Offset(left + sW - 3.5.dp.toPx(), top + sH - 4.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/* ------------------------------------------------------------------------- */
/*  Patch — plaster with gauze pad and ventilation dots                        */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawPatch(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.34f)
    val outline = 2.2.dp.toPx()

    rotate(degrees = -12f, pivot = Offset(w / 2f, h / 2f)) {
        val patchW = w * 0.96f
        val patchH = h * 0.52f
        val left = (w - patchW) / 2f
        val top = (h - patchH) / 2f
        val radius = 9.dp.toPx()
        val corner = CornerRadius(radius, radius)

        // Plaster body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(light.copy(alpha = 0.90f), tint.copy(alpha = 0.58f)),
                startY = top,
                endY = top + patchH
            ),
            topLeft = Offset(left, top),
            size = Size(patchW, patchH),
            cornerRadius = corner
        )

        // Border
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(patchW, patchH),
            cornerRadius = corner,
            style = Stroke(width = outline)
        )

        // Central gauze pad (elevated)
        val padW = patchW * 0.34f
        val padLeft = (w - padW) / 2f
        val padTop = top + 2.dp.toPx()
        val padH = patchH - 4.dp.toPx()
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.70f), lerp(tint, Color.White, 0.38f)),
                startY = padTop,
                endY = padTop + padH
            ),
            topLeft = Offset(padLeft, padTop),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Woven cross on the gauze (2 + 2 confident strokes)
        val crossStroke = 1.6.dp.toPx()
        val crossColor = tint.copy(alpha = 0.55f)
        val cx1 = padLeft + padW * 0.30f
        val cx2 = padLeft + padW * 0.70f
        val cy1 = padTop + padH * 0.30f
        val cy2 = padTop + padH * 0.70f
        drawLine(color = crossColor, start = Offset(cx1, cy1), end = Offset(cx1, cy2), strokeWidth = crossStroke, cap = StrokeCap.Round)
        drawLine(color = crossColor, start = Offset(cx2, cy1), end = Offset(cx2, cy2), strokeWidth = crossStroke, cap = StrokeCap.Round)
        drawLine(color = crossColor, start = Offset(cx1, cy1), end = Offset(cx2, cy1), strokeWidth = crossStroke, cap = StrokeCap.Round)
        drawLine(color = crossColor, start = Offset(cx1, cy2), end = Offset(cx2, cy2), strokeWidth = crossStroke, cap = StrokeCap.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(padLeft, padTop),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.6.dp.toPx())
        )

        // Ventilation dots on the wings (2 per wing, bold)
        val dotRadius = 1.6.dp.toPx()
        val wingCols = listOf(0.13f, 0.24f)
        val wingRows = listOf(0.34f, 0.66f)
        for (fx in wingCols) {
            for (fy in wingRows) {
                drawCircle(color = tint, radius = dotRadius, center = Offset(left + patchW * fx, top + patchH * fy))
                drawCircle(color = tint, radius = dotRadius, center = Offset(left + patchW * (1f - fx), top + patchH * fy))
            }
        }
    }
}
