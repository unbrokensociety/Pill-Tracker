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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
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
            val w = this.size.width
            val h = this.size.height

            when (formType) {
                FormType.TABLET -> {
                    // Volumetric round tablet pill with center score line
                    val radius = w * 0.42f
                    val center = Offset(w / 2f, h / 2f)

                    // 3D Base fill
                    drawCircle(
                        color = tint.copy(alpha = 0.35f),
                        radius = radius,
                        center = center
                    )
                    // Crisp boundary
                    drawCircle(
                        color = tint,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Glossy highlight arc on top-left edge
                    drawArc(
                        color = Color.White.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
                        size = Size(radius * 1.6f, radius * 1.6f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Score line across middle at 45 degree angle
                    val lineLen = radius * 0.70f
                    val dx = lineLen * 0.7071f
                    val dy = lineLen * 0.7071f
                    drawLine(
                        color = tint,
                        start = Offset(center.x - dx, center.y - dy),
                        end = Offset(center.x + dx, center.y + dy),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                FormType.CAPSULE -> {
                    // Classic 2-tone volumetric capsule angled at -45 degrees
                    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
                        val capWidth = w * 0.44f
                        val capHeight = h * 0.88f
                        val left = (w - capWidth) / 2f
                        val top = (h - capHeight) / 2f
                        val cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)

                        // Bottom half (tinted body)
                        drawRoundRect(
                            color = tint.copy(alpha = 0.30f),
                            topLeft = Offset(left, top + capHeight / 2f),
                            size = Size(capWidth, capHeight / 2f),
                            cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)
                        )

                        // Top half (solid cap)
                        drawRoundRect(
                            color = tint,
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

                        // Divide line
                        drawLine(
                            color = tint,
                            start = Offset(left, top + capHeight / 2f),
                            end = Offset(left + capWidth, top + capHeight / 2f),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Glossy streak
                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(left + capWidth * 0.3f, top + capHeight * 0.15f),
                            end = Offset(left + capWidth * 0.3f, top + capHeight * 0.4f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                FormType.PATCH -> {
                    // Realistic Medical Plaster / Band-Aid
                    rotate(degrees = -12f, pivot = Offset(w / 2f, h / 2f)) {
                        val patchW = w * 0.92f
                        val patchH = h * 0.48f
                        val left = (w - patchW) / 2f
                        val top = (h - patchH) / 2f
                        val radiusPx = 7.dp.toPx()

                        // Plaster skin body fill
                        drawRoundRect(
                            color = tint.copy(alpha = 0.30f),
                            topLeft = Offset(left, top),
                            size = Size(patchW, patchH),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                        // Plaster border
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(left, top),
                            size = Size(patchW, patchH),
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Central absorbent gauze pad (elevated 3D look)
                        val padW = patchW * 0.36f
                        val padLeft = (w - padW) / 2f
                        drawRoundRect(
                            color = tint.copy(alpha = 0.75f),
                            topLeft = Offset(padLeft, top),
                            size = Size(padW, patchH),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(padLeft, top),
                            size = Size(padW, patchH),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Perforated ventilation dots on adhesive wings
                        val dotRadius = 1.2.dp.toPx()
                        val leftWingX1 = left + patchW * 0.12f
                        val leftWingX2 = left + patchW * 0.22f
                        val rightWingX1 = left + patchW * 0.78f
                        val rightWingX2 = left + patchW * 0.88f
                        val dotY1 = top + patchH * 0.28f
                        val dotY2 = top + patchH * 0.72f

                        drawCircle(color = tint, radius = dotRadius, center = Offset(leftWingX1, dotY1))
                        drawCircle(color = tint, radius = dotRadius, center = Offset(leftWingX1, dotY2))
                        drawCircle(color = tint, radius = dotRadius, center = Offset(leftWingX2, (dotY1 + dotY2) / 2f))

                        drawCircle(color = tint, radius = dotRadius, center = Offset(rightWingX1, dotY1))
                        drawCircle(color = tint, radius = dotRadius, center = Offset(rightWingX1, dotY2))
                        drawCircle(color = tint, radius = dotRadius, center = Offset(rightWingX2, (dotY1 + dotY2) / 2f))
                    }
                }

                FormType.LIQUID -> {
                    // Syrup bottle with liquid level & scale
                    val bottleW = w * 0.54f
                    val bottleH = h * 0.62f
                    val left = (w - bottleW) / 2f
                    val top = h * 0.30f

                    // Bottle Cap
                    val capW = bottleW * 0.52f
                    val capH = h * 0.12f
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset((w - capW) / 2f, h * 0.14f),
                        size = Size(capW, capH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Bottle body base fill
                    drawRoundRect(
                        color = tint.copy(alpha = 0.20f),
                        topLeft = Offset(left, top),
                        size = Size(bottleW, bottleH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Liquid inside (60% full)
                    val liquidH = bottleH * 0.60f
                    drawRoundRect(
                        color = tint.copy(alpha = 0.75f),
                        topLeft = Offset(left + 1.5.dp.toPx(), top + bottleH - liquidH),
                        size = Size(bottleW - 3.dp.toPx(), liquidH - 1.dp.toPx()),
                        cornerRadius = CornerRadius(0f, 0f)
                    )

                    // Outer bottle border
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(bottleW, bottleH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Scale ticks
                    val tickLeft = left + 2.dp.toPx()
                    drawLine(color = tint, start = Offset(tickLeft, top + bottleH * 0.3f), end = Offset(tickLeft + 3.dp.toPx(), top + bottleH * 0.3f), strokeWidth = 1.2.dp.toPx())
                    drawLine(color = tint, start = Offset(tickLeft, top + bottleH * 0.5f), end = Offset(tickLeft + 4.dp.toPx(), top + bottleH * 0.5f), strokeWidth = 1.2.dp.toPx())
                    drawLine(color = tint, start = Offset(tickLeft, top + bottleH * 0.7f), end = Offset(tickLeft + 3.dp.toPx(), top + bottleH * 0.7f), strokeWidth = 1.2.dp.toPx())
                }

                FormType.DROPS -> {
                    // Volumetric liquid droplet
                    val dropPath = Path().apply {
                        moveTo(w / 2f, h * 0.10f)
                        cubicTo(
                            w * 0.88f, h * 0.50f,
                            w * 0.88f, h * 0.90f,
                            w / 2f, h * 0.90f
                        )
                        cubicTo(
                            w * 0.12f, h * 0.90f,
                            w * 0.12f, h * 0.50f,
                            w / 2f, h * 0.10f
                        )
                        close()
                    }

                    // Liquid fill
                    drawPath(path = dropPath, color = tint.copy(alpha = 0.65f))
                    // Outline
                    drawPath(
                        path = dropPath,
                        color = tint,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Glossy highlight arc
                    val highlightPath = Path().apply {
                        moveTo(w * 0.38f, h * 0.38f)
                        cubicTo(w * 0.28f, h * 0.52f, w * 0.28f, h * 0.70f, w * 0.36f, h * 0.80f)
                    }
                    drawPath(
                        path = highlightPath,
                        color = Color.White.copy(alpha = 0.7f),
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                FormType.INJECTION -> {
                    // Realistic Medical Syringe angled at -45 degrees
                    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
                        val bW = w * 0.30f
                        val bH = h * 0.44f
                        val bLeft = (w - bW) / 2f
                        val bTop = h * 0.30f

                        // 1. Needle Hub & Needle Tip (Top)
                        val hubW = bW * 0.45f
                        val hubH = h * 0.06f
                        val hubLeft = (w - hubW) / 2f
                        val hubTop = bTop - hubH

                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(hubLeft, hubTop),
                            size = Size(hubW, hubH),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )

                        // Needle tip
                        drawLine(
                            color = tint,
                            start = Offset(w / 2f, hubTop),
                            end = Offset(w / 2f, h * 0.08f),
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // 2. Main Barrel Body Fill (Glass look)
                        drawRoundRect(
                            color = tint.copy(alpha = 0.18f),
                            topLeft = Offset(bLeft, bTop),
                            size = Size(bW, bH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Fluid Fill inside barrel (60% full)
                        val fluidH = bH * 0.55f
                        val fluidTop = bTop + bH - fluidH
                        drawRect(
                            color = tint.copy(alpha = 0.70f),
                            topLeft = Offset(bLeft + 1.dp.toPx(), fluidTop),
                            size = Size(bW - 2.dp.toPx(), fluidH)
                        )

                        // Rubber Stopper / Piston line at top of fluid
                        drawRect(
                            color = tint,
                            topLeft = Offset(bLeft + 1.dp.toPx(), fluidTop - 2.dp.toPx()),
                            size = Size(bW - 2.dp.toPx(), 3.dp.toPx())
                        )

                        // Outer Barrel Border
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(bLeft, bTop),
                            size = Size(bW, bH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            style = Stroke(width = 1.8.dp.toPx())
                        )

                        // Measurement graduation lines on left side
                        val tickX1 = bLeft + 1.5.dp.toPx()
                        val tickX2 = bLeft + bW * 0.38f
                        for (i in 1..4) {
                            val tickY = bTop + bH * (i * 0.2f)
                            drawLine(
                                color = tint,
                                start = Offset(tickX1, tickY),
                                end = Offset(tickX2, tickY),
                                strokeWidth = 1.2.dp.toPx()
                            )
                        }

                        // Finger Flanges at bottom of barrel (wide horizontal bar)
                        val flangeW = bW * 1.6f
                        val flangeLeft = (w - flangeW) / 2f
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(flangeLeft, bTop + bH),
                            size = Size(flangeW, 3.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )

                        // Plunger Shaft
                        val shaftW = bW * 0.22f
                        val shaftLeft = (w - shaftW) / 2f
                        val shaftTop = bTop + bH + 3.dp.toPx()
                        val shaftBottom = h * 0.90f
                        drawRect(
                            color = tint,
                            topLeft = Offset(shaftLeft, shaftTop),
                            size = Size(shaftW, shaftBottom - shaftTop)
                        )

                        // Plunger Push Handle (Thumb Rest) at very bottom
                        val handleW = bW * 1.3f
                        val handleLeft = (w - handleW) / 2f
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(handleLeft, shaftBottom),
                            size = Size(handleW, 3.5.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }
                }

                FormType.SPRAY -> {
                    // Spray bottle with mist arcs
                    val sW = w * 0.42f
                    val sH = h * 0.50f
                    val left = w * 0.16f
                    val top = h * 0.40f

                    // Liquid fill in bottle
                    drawRoundRect(
                        color = tint.copy(alpha = 0.50f),
                        topLeft = Offset(left + 1.dp.toPx(), top + sH * 0.3f),
                        size = Size(sW - 2.dp.toPx(), sH * 0.7f - 1.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Bottle outline
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(sW, sH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Pump neck & Nozzle
                    drawLine(
                        color = tint,
                        start = Offset(left + sW / 2f, top),
                        end = Offset(left + sW / 2f, top - 6.dp.toPx()),
                        strokeWidth = 2.2.dp.toPx()
                    )
                    drawRect(
                        color = tint,
                        topLeft = Offset(left + sW / 2f - 2.dp.toPx(), top - 9.dp.toPx()),
                        size = Size(sW * 0.6f, 3.5.dp.toPx())
                    )

                    // Spray mist arcs radiating outward
                    val nozzleX = left + sW / 2f + sW * 0.6f
                    val nozzleY = top - 7.dp.toPx()

                    drawLine(
                        color = tint,
                        start = Offset(nozzleX, nozzleY),
                        end = Offset(nozzleX + 6.dp.toPx(), nozzleY - 4.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(nozzleX, nozzleY),
                        end = Offset(nozzleX + 8.dp.toPx(), nozzleY),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(nozzleX, nozzleY),
                        end = Offset(nozzleX + 6.dp.toPx(), nozzleY + 4.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

