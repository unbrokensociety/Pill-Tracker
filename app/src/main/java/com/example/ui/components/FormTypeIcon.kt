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
    backgroundColor: Color = tint.copy(alpha = 0.15f),
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
                    // Round pill tablet with a score line in the middle
                    val radius = w * 0.42f
                    val center = Offset(w / 2f, h / 2f)
                    
                    // Outer tablet ring/fill
                    drawCircle(
                        color = tint.copy(alpha = 0.25f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = tint,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Score line across middle at 45 degree angle
                    val lineLen = radius * 0.75f
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
                    // Two-tone capsule pill shape angled at -45 degrees
                    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
                        val capWidth = w * 0.44f
                        val capHeight = h * 0.88f
                        val left = (w - capWidth) / 2f
                        val top = (h - capHeight) / 2f
                        val cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)

                        // Outer capsule outline
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(left, top),
                            size = Size(capWidth, capHeight),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Top half fill
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(left, top),
                            size = Size(capWidth, capHeight / 2f),
                            cornerRadius = CornerRadius(capWidth / 2f, capWidth / 2f)
                        )

                        // Center divide line
                        drawLine(
                            color = tint,
                            start = Offset(left, top + capHeight / 2f),
                            end = Offset(left + capWidth, top + capHeight / 2f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                FormType.PATCH -> {
                    // Band-aid adhesive plaster with central pad and ventilation dots
                    val patchW = w * 0.88f
                    val patchH = h * 0.50f
                    val left = (w - patchW) / 2f
                    val top = (h - patchH) / 2f

                    drawRoundRect(
                        color = tint.copy(alpha = 0.2f),
                        topLeft = Offset(left, top),
                        size = Size(patchW, patchH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(patchW, patchH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    val padW = patchW * 0.38f
                    val padLeft = (w - padW) / 2f
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(padLeft, top),
                        size = Size(padW, patchH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val dotRadius = 1.2.dp.toPx()
                    val dotY1 = top + patchH * 0.30f
                    val dotY2 = top + patchH * 0.70f
                    val leftDotX = left + patchW * 0.15f
                    val rightDotX = left + patchW * 0.85f

                    drawCircle(color = tint, radius = dotRadius, center = Offset(leftDotX, dotY1))
                    drawCircle(color = tint, radius = dotRadius, center = Offset(leftDotX, dotY2))
                    drawCircle(color = tint, radius = dotRadius, center = Offset(rightDotX, dotY1))
                    drawCircle(color = tint, radius = dotRadius, center = Offset(rightDotX, dotY2))
                }

                FormType.LIQUID -> {
                    // Syrup bottle with liquid level line
                    val bottleW = w * 0.52f
                    val bottleH = h * 0.65f
                    val left = (w - bottleW) / 2f
                    val top = h * 0.28f

                    val capW = bottleW * 0.5f
                    val capH = h * 0.12f
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset((w - capW) / 2f, h * 0.12f),
                        size = Size(capW, capH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(bottleW, bottleH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    drawRoundRect(
                        color = tint.copy(alpha = 0.45f),
                        topLeft = Offset(left + 2.dp.toPx(), top + bottleH * 0.4f),
                        size = Size(bottleW - 4.dp.toPx(), bottleH * 0.6f - 2.dp.toPx()),
                        cornerRadius = CornerRadius(0f, 0f)
                    )
                }

                FormType.DROPS -> {
                    // Liquid droplet teardrop shape
                    val dropPath = Path().apply {
                        moveTo(w / 2f, h * 0.12f)
                        cubicTo(
                            w * 0.85f, h * 0.52f,
                            w * 0.85f, h * 0.88f,
                            w / 2f, h * 0.88f
                        )
                        cubicTo(
                            w * 0.15f, h * 0.88f,
                            w * 0.15f, h * 0.52f,
                            w / 2f, h * 0.12f
                        )
                        close()
                    }
                    drawPath(path = dropPath, color = tint.copy(alpha = 0.25f))
                    drawPath(
                        path = dropPath,
                        color = tint,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                FormType.INJECTION -> {
                    // Syringe vector
                    rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
                        val bW = w * 0.28f
                        val bH = h * 0.55f
                        val bLeft = (w - bW) / 2f
                        val bTop = h * 0.25f

                        drawRect(
                            color = tint,
                            topLeft = Offset(bLeft, bTop),
                            size = Size(bW, bH),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w / 2f, bTop),
                            end = Offset(w / 2f, h * 0.08f),
                            strokeWidth = 1.8.dp.toPx()
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w / 2f, bTop + bH),
                            end = Offset(w / 2f, h * 0.92f),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = tint,
                            start = Offset(bLeft - 2.dp.toPx(), h * 0.92f),
                            end = Offset(bLeft + bW + 2.dp.toPx(), h * 0.92f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                FormType.SPRAY -> {
                    // Spray bottle with mist arcs
                    val sW = w * 0.40f
                    val sH = h * 0.52f
                    val left = w * 0.18f
                    val top = h * 0.38f

                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(sW, sH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    drawLine(
                        color = tint,
                        start = Offset(left + sW / 2f, top),
                        end = Offset(left + sW / 2f, top - 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawLine(
                        color = tint,
                        start = Offset(left + sW + 3.dp.toPx(), top - 2.dp.toPx()),
                        end = Offset(left + sW + 9.dp.toPx(), top - 6.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(left + sW + 4.dp.toPx(), top + 2.dp.toPx()),
                        end = Offset(left + sW + 11.dp.toPx(), top + 2.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(left + sW + 3.dp.toPx(), top + 6.dp.toPx()),
                        end = Offset(left + sW + 9.dp.toPx(), top + 10.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
