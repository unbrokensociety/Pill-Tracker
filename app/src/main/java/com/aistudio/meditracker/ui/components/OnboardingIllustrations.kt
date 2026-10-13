package com.aistudio.meditracker.ui.components

/*
 * Onboarding v6 illustrations — quiet and native.
 *
 * Everything here is drawn from the app's own theme colors and reuses the
 * real UI atoms (FormTypeIcon), so the tutorial looks like the app itself,
 * not like a separate presentation. No gradients, no glow, no ambient
 * loops: an illustration moves only once, and only when the movement
 * explains something (the day ring fills, the island slides down).
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R

/* ────────────────────────────────────────────────────────────────
 * 1. Welcome — the day ring filling in, quietly
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun WelcomeArt(modifier: Modifier = Modifier) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val panelBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val chipBg = MaterialTheme.colorScheme.surface
    val chipText = MaterialTheme.colorScheme.onSurfaceVariant

    // One meaningful motion: the ring fills once, the way it does on the
    // home screen when a dose is marked.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fill.animateTo(0.75f, tween(900, easing = EaseOutCubic))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(panel)
            .border(1.dp, panelBorder, RoundedCornerShape(26.dp))
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(116.dp)) {
                val stroke = 9.dp.toPx()
                val inset = stroke / 2f + 1.dp.toPx()
                val arc = Size(size.width - inset * 2f, size.height - inset * 2f)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fill.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            FormTypeIcon(
                formKey = "capsule",
                tint = accent,
                size = 52.dp,
                iconSize = 25.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // A plain day: three intake times, nothing else.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("08:00", "13:00", "21:00").forEach { time ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipBg)
                        .border(1.dp, panelBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = chipText
                    )
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 2. Mini day ring — the interactive demo's progress companion
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun MiniDayRing(
    fraction: () -> Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 40.dp
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val animated by animateFloatAsState(
        targetValue = fraction().coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "miniDayRing"
    )

    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f + 1.dp.toPx()
            val arc = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        if (animated > 0.98f) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 3. Island notification — the real reminder
 *
 * The island lives in the system, so it keeps its honest dark chrome;
 * it slides down once (like a real incoming reminder) and stays still.
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun IslandNotificationMock(modifier: Modifier = Modifier) {
    val islandBg = Color(0xFF1B1F27)
    val islandBorder = Color.White.copy(alpha = 0.08f)
    val subText = Color.White.copy(alpha = 0.58f)
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 340f))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * -56f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(islandBg)
            .border(1.dp, islandBorder, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FormTypeIcon(
                formKey = "tablet",
                tint = accent,
                backgroundColor = Color.White.copy(alpha = 0.10f),
                size = 38.dp,
                iconSize = 19.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ob_mock_med),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.ob_mock_dose),
                    style = MaterialTheme.typography.bodySmall,
                    color = subText
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = stringResource(R.string.ob_mock_now),
                    style = MaterialTheme.typography.labelSmall,
                    color = subText
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MockAction(
                label = stringResource(R.string.notif_taken),
                filled = true,
                accent = accent,
                onAccent = onAccent,
                modifier = Modifier.weight(1f)
            )
            MockAction(
                label = stringResource(R.string.ob_mock_snooze),
                filled = false,
                accent = accent,
                onAccent = onAccent,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 4. Full-screen alarm — what happens if the island is missed
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun AlarmMockArt(modifier: Modifier = Modifier) {
    val screenBg = Color(0xFF14181F)
    val screenBorder = Color.White.copy(alpha = 0.07f)
    val subText = Color.White.copy(alpha = 0.58f)
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val alarmRed = Color(0xFFF87171)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(screenBg)
            .border(1.dp, screenBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                tint = alarmRed,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.ob_alarm_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.72f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "08:00",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FormTypeIcon(
                formKey = "tablet",
                tint = accent,
                backgroundColor = Color.White.copy(alpha = 0.10f),
                size = 32.dp,
                iconSize = 16.dp
            )
            Column {
                Text(
                    text = stringResource(R.string.ob_mock_med),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.ob_mock_dose),
                    style = MaterialTheme.typography.bodySmall,
                    color = subText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MockAction(
                label = stringResource(R.string.notif_taken),
                filled = true,
                accent = accent,
                onAccent = onAccent,
                modifier = Modifier.weight(1f)
            )
            MockAction(
                label = stringResource(R.string.ob_mock_snooze),
                filled = false,
                accent = accent,
                onAccent = onAccent,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

/* ── Shared: a plain mock action button ── */

@Composable
private fun MockAction(
    label: String,
    filled: Boolean,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) accent else Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) onAccent else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
