package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

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
            return values().find { it.key.equals(key, ignoreCase = true) } ?: CAPSULE
        }
    }
}

/**
 * Redrawn high-fidelity medication form icons.
 *
 * Every icon is vector-drawn with layered gradients (volumetric fill +
 * gloss highlight + bold outline) instead of flat alpha fills, so pills,
 * bottles, drops and syringes read as little 3D objects at any size.
 * All geometry is relative to the canvas size, strokes stay crisp.
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
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
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

/* ------------------------------------------------------------------------- */
/*  Tablet — volumetric round pill with radial shading + gloss arc            */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawTablet(tint: Color) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h / 2f)
    val radius = w * 0.42f
    val light = lerp(tint, Color.White, 0.45f)
    val dark = lerp(tint, Color.Black, 0.18f)

    // Volumetric body: light source top-left
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(light, tint, dark),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.45f),
            radius = radius * 1.35f
        ),
        radius = radius,
        center = center
    )

    // Crisp outline
    drawCircle(
        color = tint,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // Glossy highlight arc on the top-left edge
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.15f)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius * 0.1f, center.y - radius * 0.9f)
        ),
        startAngle = 195f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
        size = Size(radius * 1.64f, radius * 1.64f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Score line across the middle (embossed: dark under-stroke + light top-stroke)
    val lineLen = radius * 0.72f
    val dx = lineLen * 0.7071f
    val dy = lineLen * 0.7071f
    drawLine(
        color = dark.copy(alpha = 0.55f),
        start = Offset(center.x - dx + 0.7.dp.toPx(), center.y - dy + 0.7.dp.toPx()),
        end = Offset(center.x + dx + 0.7.dp.toPx(), center.y + dy + 0.7.dp.toPx()),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = lerp(tint, Color.White, 0.55f),
        start = Offset(center.x - dx, center.y - dy),
        end = Offset(center.x + dx, center.y + dy),
        strokeWidth = 2.2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/* ------------------------------------------------------------------------- */
/*  Capsule — two-tone gradient capsule with cap seam and gloss streak        */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawCapsule(tint: Color) {
    val w = size.width
    val h = size.height
    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
        val capWidth = w * 0.46f
        val capHeight = h * 0.88f
        val left = (w - capWidth) / 2f
        val top = (h - capHeight) / 2f
        val cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)
        val capLight = lerp(tint, Color.White, 0.38f)
        val bodyLight = lerp(tint, Color.White, 0.55f)
        val dark = lerp(tint, Color.Black, 0.22f)

        // Bottom half — translucent gradient body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(bodyLight.copy(alpha = 0.75f), tint.copy(alpha = 0.45f)),
                startY = top + capHeight / 2f,
                endY = top + capHeight
            ),
            topLeft = Offset(left, top + capHeight / 2f),
            size = Size(capWidth, capHeight / 2f),
            cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)
        )

        // Top half — solid gradient cap
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(capLight, tint),
                startY = top,
                endY = top + capHeight / 2f
            ),
            topLeft = Offset(left, top),
            size = Size(capWidth, capHeight / 2f),
            cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)
        )

        // Outer border
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(capWidth, capHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = 2.dp.toPx())
        )

        // Cap seam (embossed divider)
        drawLine(
            color = dark.copy(alpha = 0.5f),
            start = Offset(left, top + capHeight / 2f + 0.7.dp.toPx()),
            end = Offset(left + capWidth, top + capHeight / 2f + 0.7.dp.toPx()),
            strokeWidth = 2.2.dp.toPx()
        )
        drawLine(
            color = lerp(tint, Color.White, 0.5f),
            start = Offset(left, top + capHeight / 2f),
            end = Offset(left + capWidth, top + capHeight / 2f),
            strokeWidth = 2.dp.toPx()
        )

        // Glossy streak along the cap
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.05f)
                ),
                startY = top + capHeight * 0.12f,
                endY = top + capHeight * 0.42f
            ),
            start = Offset(left + capWidth * 0.26f, top + capHeight * 0.12f),
            end = Offset(left + capWidth * 0.26f, top + capHeight * 0.42f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Tiny bottom reflection
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(left + capWidth * 0.60f, top + capHeight * 0.68f),
            end = Offset(left + capWidth * 0.60f, top + capHeight * 0.84f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Liquid — syrup bottle with gradient liquid, cap and label                 */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawLiquidBottle(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.40f)
    val liquidTop = lerp(tint, Color.White, 0.22f)

    val bottleW = w * 0.54f
    val bottleH = h * 0.60f
    val left = (w - bottleW) / 2f
    val top = h * 0.32f

    // Cap with gradient
    val capW = bottleW * 0.52f
    val capH = h * 0.11f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(light, tint),
            startY = h * 0.14f,
            endY = h * 0.14f + capH
        ),
        topLeft = Offset((w - capW) / 2f, h * 0.14f),
        size = Size(capW, capH),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Neck
    val neckW = bottleW * 0.34f
    drawRect(
        color = tint.copy(alpha = 0.85f),
        topLeft = Offset((w - neckW) / 2f, h * 0.25f),
        size = Size(neckW, h * 0.07f)
    )

    // Glass body fill
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                tint.copy(alpha = 0.10f),
                tint.copy(alpha = 0.22f),
                tint.copy(alpha = 0.10f)
            ),
            startX = left,
            endX = left + bottleW
        ),
        topLeft = Offset(left, top),
        size = Size(bottleW, bottleH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Liquid inside (65% full, gradient + soft surface)
    val liquidH = bottleH * 0.65f
    val liquidTopY = top + bottleH - liquidH
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(liquidTop.copy(alpha = 0.85f), tint.copy(alpha = 0.92f)),
            startY = liquidTopY,
            endY = top + bottleH
        ),
        topLeft = Offset(left + 1.5.dp.toPx(), liquidTopY),
        size = Size(bottleW - 3.dp.toPx(), liquidH),
        cornerRadius = CornerRadius(0f, 0f)
    )
    // Meniscus highlight
    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(left + 2.5.dp.toPx(), liquidTopY + 1.dp.toPx()),
        end = Offset(left + bottleW - 2.5.dp.toPx(), liquidTopY + 1.dp.toPx()),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Label band
    val labelY = top + bottleH * 0.48f
    drawRect(
        color = Color.White.copy(alpha = 0.28f),
        topLeft = Offset(left + 1.5.dp.toPx(), labelY),
        size = Size(bottleW - 3.dp.toPx(), bottleH * 0.18f)
    )
    drawLine(
        color = tint,
        start = Offset(left + bottleW * 0.30f, labelY + bottleH * 0.07f),
        end = Offset(left + bottleW * 0.70f, labelY + bottleH * 0.07f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = tint.copy(alpha = 0.7f),
        start = Offset(left + bottleW * 0.36f, labelY + bottleH * 0.12f),
        end = Offset(left + bottleW * 0.64f, labelY + bottleH * 0.12f),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Outer bottle border
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(bottleW, bottleH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    // Glossy reflection on the right edge of the glass
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(left + bottleW - 3.5.dp.toPx(), top + 3.dp.toPx()),
        end = Offset(left + bottleW - 3.5.dp.toPx(), top + bottleH - 3.dp.toPx()),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/* ------------------------------------------------------------------------- */
/*  Drops — volumetric teardrop with big gloss                                */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawDroplet(tint: Color) {
    val w = size.width
    val h = size.height

    val dropPath = Path().apply {
        moveTo(w / 2f, h * 0.08f)
        cubicTo(
            w * 0.90f, h * 0.48f,
            w * 0.90f, h * 0.92f,
            w / 2f, h * 0.92f
        )
        cubicTo(
            w * 0.10f, h * 0.92f,
            w * 0.10f, h * 0.48f,
            w / 2f, h * 0.08f
        )
        close()
    }

    // Volumetric fill: light at the top fading into saturated tint
    drawPath(
        path = dropPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                lerp(tint, Color.White, 0.40f),
                tint,
                lerp(tint, Color.Black, 0.12f)
            ),
            startY = h * 0.08f,
            endY = h * 0.92f
        )
    )

    // Outline
    drawPath(
        path = dropPath,
        color = tint,
        style = Stroke(width = 2.dp.toPx())
    )

    // Big glossy curve on the left side
    val highlightPath = Path().apply {
        moveTo(w * 0.36f, h * 0.36f)
        cubicTo(w * 0.24f, h * 0.52f, w * 0.24f, h * 0.72f, w * 0.33f, h * 0.82f)
    }
    drawPath(
        path = highlightPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.25f)),
            startY = h * 0.36f,
            endY = h * 0.82f
        ),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Small sharp sparkle top-right
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = 1.3.dp.toPx(),
        center = Offset(w * 0.68f, h * 0.30f)
    )
}

/* ------------------------------------------------------------------------- */
/*  Injection — detailed syringe with gradient barrel and fluid               */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawSyringe(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.35f)

    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
        val barrelW = w * 0.34f
        val barrelH = h * 0.48f
        val barrelLeft = (w - barrelW) / 2f
        val barrelTop = h * 0.28f

        // 1. Needle
        val needleTop = h * 0.05f
        val needleBottom = barrelTop - h * 0.05f
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.6f), tint),
                startY = needleTop,
                endY = needleBottom
            ),
            start = Offset(w / 2f, needleBottom),
            end = Offset(w / 2f, needleTop),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Beveled tip highlight
        drawLine(
            color = Color.White,
            start = Offset(w / 2f, needleTop + 1.dp.toPx()),
            end = Offset(w / 2f, needleTop + 3.dp.toPx()),
            strokeWidth = 1.1.dp.toPx()
        )

        // 2. Luer-lock hub
        val hubW = barrelW * 0.50f
        val hubH = h * 0.06f
        val hubLeft = (w - hubW) / 2f
        val hubTop = barrelTop - hubH
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(light, tint),
                startX = hubLeft,
                endX = hubLeft + hubW
            ),
            topLeft = Offset(hubLeft, hubTop),
            size = Size(hubW, hubH),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )

        // 3. Glass barrel
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    tint.copy(alpha = 0.14f),
                    tint.copy(alpha = 0.30f),
                    tint.copy(alpha = 0.14f)
                ),
                startX = barrelLeft,
                endX = barrelLeft + barrelW
            ),
            topLeft = Offset(barrelLeft, barrelTop),
            size = Size(barrelW, barrelH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // 4. Fluid dose with gradient
        val fluidH = barrelH * 0.58f
        val fluidTop = barrelTop + barrelH - fluidH
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.30f), tint),
                startY = fluidTop,
                endY = fluidTop + fluidH
            ),
            topLeft = Offset(barrelLeft + 1.5.dp.toPx(), fluidTop),
            size = Size(barrelW - 3.dp.toPx(), fluidH)
        )

        // 5. Piston stopper on top of the fluid
        val stopperH = 4.5.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(barrelLeft + 1.dp.toPx(), fluidTop - stopperH),
            size = Size(barrelW - 2.dp.toPx(), stopperH),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // 6. Graduation marks
        val markX1 = barrelLeft + 2.dp.toPx()
        val markLongX2 = barrelLeft + barrelW * 0.42f
        val markShortX2 = barrelLeft + barrelW * 0.28f
        for (i in 1..8) {
            val markY = barrelTop + barrelH * (i * 0.11f)
            val isLong = (i % 2 == 0)
            drawLine(
                color = if (markY >= fluidTop) Color.White.copy(alpha = 0.9f) else tint,
                start = Offset(markX1, markY),
                end = Offset(if (isLong) markLongX2 else markShortX2, markY),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // Glossy glass reflection along the right edge
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(barrelLeft + barrelW - 3.dp.toPx(), barrelTop + 3.dp.toPx()),
            end = Offset(barrelLeft + barrelW - 3.dp.toPx(), barrelTop + barrelH - 3.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )

        // Barrel outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(barrelLeft, barrelTop),
            size = Size(barrelW, barrelH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // 7. Finger flange
        val flangeW = barrelW * 1.75f
        val flangeLeft = (w - flangeW) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(light, tint, light),
                startX = flangeLeft,
                endX = flangeLeft + flangeW
            ),
            topLeft = Offset(flangeLeft, barrelTop + barrelH),
            size = Size(flangeW, 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 8. Plunger shaft
        val shaftW = barrelW * 0.24f
        val shaftLeft = (w - shaftW) / 2f
        val shaftTop = barrelTop + barrelH + 4.dp.toPx()
        val shaftBottom = h * 0.92f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(lerp(tint, Color.White, 0.5f), tint),
                startX = shaftLeft,
                endX = shaftLeft + shaftW
            ),
            topLeft = Offset(shaftLeft, shaftTop),
            size = Size(shaftW, shaftBottom - shaftTop)
        )

        // 9. Thumb disc
        val discW = barrelW * 1.40f
        val discLeft = (w - discW) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(light, tint, light),
                startX = discLeft,
                endX = discLeft + discW
            ),
            topLeft = Offset(discLeft, shaftBottom),
            size = Size(discW, 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Spray — bottle with pump, nozzle and radiating mist                       */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawSprayBottle(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.38f)

    val sW = w * 0.42f
    val sH = h * 0.50f
    val left = w * 0.14f
    val top = h * 0.40f

    // Liquid fill (gradient, 70% full)
    val liquidTopY = top + sH * 0.30f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(lerp(tint, Color.White, 0.25f), tint),
            startY = liquidTopY,
            endY = top + sH
        ),
        topLeft = Offset(left + 1.dp.toPx(), liquidTopY),
        size = Size(sW - 2.dp.toPx(), sH * 0.70f - 1.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Bottle glass body (translucent side shading)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                tint.copy(alpha = 0.12f),
                tint.copy(alpha = 0.26f),
                tint.copy(alpha = 0.12f)
            ),
            startX = left,
            endX = left + sW
        ),
        topLeft = Offset(left, top),
        size = Size(sW, sH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Bottle outline
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(sW, sH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.8.dp.toPx())
    )

    // Pump neck
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(light, tint),
            startY = top - 6.dp.toPx(),
            endY = top
        ),
        start = Offset(left + sW / 2f, top),
        end = Offset(left + sW / 2f, top - 6.dp.toPx()),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Pump head + nozzle pointing right
    val headH = 3.5.dp.toPx()
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(light, tint),
            startX = left + sW / 2f - sW * 0.30f,
            endX = left + sW / 2f + sW * 0.34f
        ),
        topLeft = Offset(left + sW / 2f - sW * 0.30f, top - 9.dp.toPx()),
        size = Size(sW * 0.64f, headH),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(left + sW / 2f + sW * 0.30f, top - 8.6.dp.toPx()),
        size = Size(sW * 0.32f, 2.6.dp.toPx()),
        cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
    )

    // Radiating mist: fading dots + streaks
    val nozzleX = left + sW / 2f + sW * 0.62f
    val nozzleY = top - 7.5.dp.toPx()
    val mistDot = 1.1.dp.toPx()
    val mistColor = tint
    // streaks
    drawLine(color = mistColor, start = Offset(nozzleX + 2.dp.toPx(), nozzleY - 1.dp.toPx()), end = Offset(nozzleX + 8.dp.toPx(), nozzleY - 5.dp.toPx()), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = mistColor, start = Offset(nozzleX + 2.dp.toPx(), nozzleY), end = Offset(nozzleX + 10.dp.toPx(), nozzleY), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = mistColor, start = Offset(nozzleX + 2.dp.toPx(), nozzleY + 1.dp.toPx()), end = Offset(nozzleX + 8.dp.toPx(), nozzleY + 5.dp.toPx()), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
    // fading dots further out
    drawCircle(color = mistColor.copy(alpha = 0.80f), radius = mistDot, center = Offset(nozzleX + 10.dp.toPx(), nozzleY - 6.dp.toPx()))
    drawCircle(color = mistColor.copy(alpha = 0.65f), radius = mistDot, center = Offset(nozzleX + 12.dp.toPx(), nozzleY - 1.dp.toPx()))
    drawCircle(color = mistColor.copy(alpha = 0.70f), radius = mistDot, center = Offset(nozzleX + 11.dp.toPx(), nozzleY + 4.dp.toPx()))
    drawCircle(color = mistColor.copy(alpha = 0.45f), radius = mistDot, center = Offset(nozzleX + 14.dp.toPx(), nozzleY - 8.dp.toPx()))
    drawCircle(color = mistColor.copy(alpha = 0.40f), radius = mistDot, center = Offset(nozzleX + 15.dp.toPx(), nozzleY + 2.dp.toPx()))

    // Glossy reflection on the bottle
    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(left + sW - 3.dp.toPx(), top + 4.dp.toPx()),
        end = Offset(left + sW - 3.dp.toPx(), top + sH - 4.dp.toPx()),
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/* ------------------------------------------------------------------------- */
/*  Patch — medical plaster with gauze pad and perforated wings               */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawPatch(tint: Color) {
    val w = size.width
    val h = size.height
    val light = lerp(tint, Color.White, 0.30f)

    rotate(degrees = -12f, pivot = Offset(w / 2f, h / 2f)) {
        val patchW = w * 0.92f
        val patchH = h * 0.48f
        val left = (w - patchW) / 2f
        val top = (h - patchH) / 2f
        val radiusPx = 7.dp.toPx()

        // Plaster body with vertical gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(light.copy(alpha = 0.85f), tint.copy(alpha = 0.55f)),
                startY = top,
                endY = top + patchH
            ),
            topLeft = Offset(left, top),
            size = Size(patchW, patchH),
            cornerRadius = CornerRadius(radiusPx, radiusPx)
        )

        // Border
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(patchW, patchH),
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(width = 2.dp.toPx())
        )

        // Central absorbent gauze pad (elevated look)
        val padW = patchW * 0.36f
        val padLeft = (w - padW) / 2f
        val padTop = top + 1.5.dp.toPx()
        val padH = patchH - 3.dp.toPx()
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(tint, Color.White, 0.65f), lerp(tint, Color.White, 0.35f)),
                startY = padTop,
                endY = padTop + padH
            ),
            topLeft = Offset(padLeft, padTop),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Woven crosshatch on the gauze
        val step = 2.6.dp.toPx()
        var x = padLeft + step
        while (x < padLeft + padW - 0.5.dp.toPx()) {
            drawLine(
                color = tint.copy(alpha = 0.35f),
                start = Offset(x, padTop + 1.dp.toPx()),
                end = Offset(x, padTop + padH - 1.dp.toPx()),
                strokeWidth = 0.8.dp.toPx()
            )
            x += step
        }
        var y = padTop + step
        while (y < padTop + padH - 0.5.dp.toPx()) {
            drawLine(
                color = tint.copy(alpha = 0.35f),
                start = Offset(padLeft + 1.dp.toPx(), y),
                end = Offset(padLeft + padW - 1.dp.toPx(), y),
                strokeWidth = 0.8.dp.toPx()
            )
            y += step
        }
        drawRoundRect(
            color = tint,
            topLeft = Offset(padLeft, padTop),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Perforated ventilation dots on the adhesive wings
        val dotRadius = 1.1.dp.toPx()
        val wingCols = listOf(0.10f, 0.17f, 0.24f)
        val wingRows = listOf(0.30f, 0.55f, 0.80f)
        for (fx in wingCols) {
            for (fy in wingRows) {
                drawCircle(color = tint, radius = dotRadius, center = Offset(left + patchW * fx, top + patchH * fy))
                drawCircle(color = tint, radius = dotRadius, center = Offset(left + patchW * (1f - fx), top + patchH * fy))
            }
        }
    }
}
