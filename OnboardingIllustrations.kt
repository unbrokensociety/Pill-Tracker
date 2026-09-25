package com.aistudio.meditracker.ui.components

/*
 * Onboarding v5 — hand-drawn, animated illustrations.
 *
 * Every intro slide leads with a living picture instead of a plain icon:
 *   WelcomeArt          — glowing capsule with an orbit of sparkles
 *   AddFormArt          — phone mock of the real add-medication form
 *   MiniDayRing         — small progress ring used by the interactive demo
 *   IslandNotificationMock — the real reminder island preview
 *   AlarmMockArt        — the full-screen alarm that backs the island up
 *   StockBottleArt      — a bottle that counts the package down
 *   CourseLifecycleArt  — active -> done -> history kept
 *   CalendarMonthArt    — a month grid that fills with taken/missed days
 *   ShieldArt           — pulsing privacy shield
 *
 * Everything is drawn with Canvas + composables (no image assets), so the
 * art stays crisp on every density, follows the theme accent colors and
 * adds zero APK weight.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* ────────────────────────────────────────────────────────────────
 * Shared palette (matches the onboarding panel colors used app-wide)
 * ──────────────────────────────────────────────────────────────── */

private val ArtPanelTop = Color(0xFF1D242F)
private val ArtPanelBottom = Color(0xFF161B24)
private val ArtInnerTop = Color(0xFF232B38)
private val ArtInnerBottom = Color(0xFF1A212C)
private val ArtScreen = Color(0xFF0F141B)
private val ArtGreen = Color(0xFF6FCF97)
private val ArtOrange = Color(0xFFFF8A65)
private val ArtRed = Color(0xFFEF5350)
private val ArtWhite = Color.White

/** Slow 0..1 breathing value shared by several arts. */
@Composable
private fun rememberBreath(periodMs: Int = 1800): Float {
    val transition = rememberInfiniteTransition(label = "artBreath")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(periodMs, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "artBreathValue"
    ).value
}

/** Rounded dark panel every illustration lives in. */
@Composable
private fun ArtPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(ArtPanelTop, ArtPanelBottom)))
            .border(1.dp, ArtWhite.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(contentPadding)
    ) { content() }
}

/** Gradient pill button used inside the mock previews. */
@Composable
private fun MockAction(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (filled) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                } else {
                    Modifier
                        .background(ArtWhite.copy(alpha = 0.10f))
                        .border(1.dp, ArtWhite.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold,
            color = if (filled) ArtWhite else ArtWhite.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/* ────────────────────────────────────────────────────────────────
 * 1. Welcome — breathing capsule with an orbit of sparkles
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun WelcomeArt(modifier: Modifier = Modifier) {
    val glow = rememberBreath(2200)
    // Captured in composable context — Canvas lambdas are not composable.
    val artPrimary = MaterialTheme.colorScheme.primary
    val artTertiary = MaterialTheme.colorScheme.tertiary
    val spin = rememberInfiniteTransition(label = "welcomeSpin")
    val orbit by spin.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "welcomeOrbit"
    )
    val twinkle = rememberInfiniteTransition(label = "welcomeTwinkle")
    val twinkleA by twinkle.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(1300, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "welcomeTwinkleA"
    )

    ArtPanel(modifier.height(210.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val base = size.minDimension

            // Soft radial glow behind the capsule.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        artPrimary.copy(alpha = 0.20f + 0.12f * glow),
                        Color.Transparent
                    ),
                    center = c,
                    radius = base * 0.55f
                ),
                radius = base * 0.55f,
                center = c
            )

            // Elliptical orbit + three sparkles riding it.
            val orbitR = base * 0.40f
            drawCircle(
                color = ArtWhite.copy(alpha = 0.07f),
                radius = orbitR,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            val twoPi = 2f * PI.toFloat()
            repeat(3) { i ->
                val ang = orbit * twoPi + i * twoPi / 3f
                val pos = Offset(
                    c.x + orbitR * cos(ang),
                    c.y + orbitR * 0.38f * sin(ang)
                )
                drawCircle(
                    color = ArtWhite.copy(alpha = 0.20f + 0.55f * twinkleA),
                    radius = (2.4f + 1.2f * twinkleA).dp.toPx(),
                    center = pos
                )
            }

            // The capsule: two gradient halves, gentle vertical float.
            rotate(degrees = -24f, pivot = c) {
                val capW = 118.dp.toPx()
                val capH = 44.dp.toPx()
                val half = Offset(c.x - capW / 2f, c.y - capH / 2f)
                val radius = CornerRadius(capH / 2f, capH / 2f)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            artPrimary,
                            artPrimary.copy(alpha = 0.82f)
                        )
                    ),
                    topLeft = half,
                    size = Size(capW * 0.56f, capH),
                    cornerRadius = radius
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            artTertiary.copy(alpha = 0.92f),
                            artTertiary
                        )
                    ),
                    topLeft = Offset(half.x + capW * 0.44f, half.y),
                    size = Size(capW * 0.56f, capH),
                    cornerRadius = radius
                )
                // Shine line.
                drawRoundRect(
                    color = ArtWhite.copy(alpha = 0.35f),
                    topLeft = Offset(half.x + capW * 0.10f, half.y + capH * 0.22f),
                    size = Size(capW * 0.20f, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 2. Add form — a miniature of the real "new medication" screen
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun AddFormArt(modifier: Modifier = Modifier) {
    // Blinking text cursor + pulsing CTA highlight.
    val cursor = rememberInfiniteTransition(label = "addCursor")
    val cursorA by cursor.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            tween(650, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "addCursorA"
    )
    val cta = rememberBreath(1500)

    ArtPanel(modifier.height(230.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
        PhoneFrame(modifier = Modifier.align(Alignment.Center)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                // Title bar.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Medication,
                            contentDescription = null,
                            tint = ArtWhite,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    MockBar(width = 0.42f, height = 7.dp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name field with a blinking cursor.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(ArtWhite.copy(alpha = 0.07f))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MockBar(width = 0.34f, height = 6.dp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 2.dp, height = 9.dp)
                            .background(ArtWhite.copy(alpha = 0.25f + 0.75f * cursorA))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Real form-type icons from the app itself.
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FormTypeIcon(formKey = "tablet", size = 30.dp, iconSize = 16.dp)
                    FormTypeIcon(formKey = "capsule", size = 30.dp, iconSize = 16.dp)
                    FormTypeIcon(formKey = "liquid", size = 30.dp, iconSize = 16.dp)
                    FormTypeIcon(formKey = "injection", size = 30.dp, iconSize = 16.dp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dose rows.
                MockBar(width = 0.55f, height = 6.dp, alpha = 0.35f)
                Spacer(modifier = Modifier.height(6.dp))
                MockBar(width = 0.38f, height = 6.dp, alpha = 0.22f)

                Spacer(modifier = Modifier.height(10.dp))

                // Times of day.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MockTimeChip("08:00")
                    MockTimeChip("14:00")
                    MockTimeChip("20:00")
                }

                Spacer(modifier = Modifier.weight(1f))

                // CTA with a traveling highlight.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val sweepW = size.width * 0.35f
                        val x = (cta * 1.6f - 0.3f) * size.width
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, ArtWhite.copy(alpha = 0.30f), Color.Transparent),
                                startX = x - sweepW,
                                endX = x + sweepW
                            ),
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ArtWhite
                    )
                }
            }
        }
    }
}

/** A thin rounded bar that stands in for a line of text. */
@Composable
private fun MockBar(
    width: Float,
    height: Dp,
    alpha: Float = 0.45f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(ArtWhite.copy(alpha = alpha))
    )
}

@Composable
private fun MockTimeChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(ArtWhite.copy(alpha = 0.10f))
            .border(1.dp, ArtWhite.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ArtWhite.copy(alpha = 0.85f)
        )
    }
}

/** Small phone mock frame with a notch; children paint the screen. */
@Composable
private fun PhoneFrame(
    modifier: Modifier = Modifier,
    screen: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .width(168.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(ArtInnerTop, ArtInnerBottom)))
            .border(1.dp, ArtWhite.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
    ) {
        // Screen surface.
        Box(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(ArtScreen)
        ) {
            screen()
        }
        // Notch.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(width = 44.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ArtWhite.copy(alpha = 0.16f))
        )
    }
}

/* ────────────────────────────────────────────────────────────────
 * 3. Mini day ring — the interactive demo's progress companion
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun MiniDayRing(
    fraction: () -> Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 40.dp
) {
    val animated by animateFloatAsState(
        targetValue = fraction().coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "miniDayRing"
    )
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        val ring = MaterialTheme.colorScheme.primary
        val ringTrack = ArtWhite.copy(alpha = 0.14f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f + 1.dp.toPx()
            val arc = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = ringTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ring,
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 4. Island notification — the real reminder, floating
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun IslandNotificationMock(modifier: Modifier = Modifier) {
    val float = rememberInfiniteTransition(label = "islandFloat")
    val floatY by float.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "islandFloatY"
    )
    val nowPulse = rememberInfiniteTransition(label = "nowPulse")
    val nowAlpha by nowPulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "nowAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = floatY }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(ArtInnerTop, ArtInnerBottom)))
            .border(1.dp, ArtWhite.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Medication,
                    contentDescription = null,
                    tint = ArtWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ob_mock_med),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArtWhite
                )
                Text(
                    text = stringResource(R.string.ob_mock_dose),
                    style = MaterialTheme.typography.bodySmall,
                    color = ArtWhite.copy(alpha = 0.55f)
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = nowAlpha))
                )
                Text(
                    text = stringResource(R.string.ob_mock_now),
                    style = MaterialTheme.typography.labelSmall,
                    color = ArtWhite.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MockAction(label = stringResource(R.string.notif_taken), filled = true, modifier = Modifier.weight(1f))
            MockAction(label = stringResource(R.string.ob_mock_snooze), filled = false, modifier = Modifier.weight(1.2f))
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 5. Full-screen alarm — what happens if the island is missed
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun AlarmMockArt(modifier: Modifier = Modifier) {
    // Blinking colon like a real alarm clock.
    val blink = rememberInfiniteTransition(label = "alarmBlink")
    val colonA by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "alarmColonA"
    )
    val ringPulse = rememberBreath(1100)

    ArtPanel(modifier.height(216.dp), contentPadding = PaddingValues(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(ArtScreen)
                .border(1.dp, ArtRed.copy(alpha = 0.25f + 0.20f * ringPulse), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = ArtRed,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(R.string.ob_alarm_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ArtWhite.copy(alpha = 0.70f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Big clock: 08 : 00 with a blinking colon.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "08",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArtWhite
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArtWhite.copy(alpha = colonA),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = "00",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArtWhite
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = ArtWhite,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.ob_mock_med),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArtWhite
                    )
                    Text(
                        text = stringResource(R.string.ob_mock_dose),
                        style = MaterialTheme.typography.bodySmall,
                        color = ArtWhite.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MockAction(label = stringResource(R.string.notif_taken), filled = true, modifier = Modifier.weight(1f))
                MockAction(label = stringResource(R.string.ob_mock_snooze), filled = false, modifier = Modifier.weight(1.2f))
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 6. Stock bottle — counting the package down
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun StockBottleArt(modifier: Modifier = Modifier) {
    val warn = rememberBreath(1100)
    // Captured in composable context — Canvas lambdas are not composable.
    val artPrimary = MaterialTheme.colorScheme.primary
    val artTertiary = MaterialTheme.colorScheme.tertiary

    ArtPanel(modifier.height(170.dp), contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bottle with the remaining count on the label.
            Box(
                modifier = Modifier.size(width = 66.dp, height = 118.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Cap.
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                ArtWhite.copy(alpha = 0.34f),
                                ArtWhite.copy(alpha = 0.20f)
                            )
                        ),
                        topLeft = Offset(w * 0.30f, 0f),
                        size = Size(w * 0.40f, h * 0.10f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // Body.
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                artPrimary.copy(alpha = 0.38f),
                                artTertiary.copy(alpha = 0.22f)
                            )
                        ),
                        topLeft = Offset(w * 0.06f, h * 0.13f),
                        size = Size(w * 0.88f, h * 0.87f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                    drawRoundRect(
                        color = ArtWhite.copy(alpha = 0.22f),
                        topLeft = Offset(w * 0.06f, h * 0.13f),
                        size = Size(w * 0.88f, h * 0.87f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(1.dp.toPx())
                    )
                    // Label plate.
                    drawRoundRect(
                        color = ArtWhite.copy(alpha = 0.12f),
                        topLeft = Offset(w * 0.14f, h * 0.40f),
                        size = Size(w * 0.72f, h * 0.34f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
                Text(
                    text = "4",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArtWhite,
                    modifier = Modifier.padding(top = 34.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stock_remaining, 4),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ArtWhite.copy(alpha = 0.90f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stock bar: almost empty.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(ArtWhite.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.13f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(ArtRed, ArtOrange))
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = ArtOrange.copy(alpha = warn),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.ob_stock_warn_line),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ArtOrange.copy(alpha = 0.55f + 0.45f * warn)
                    )
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────
 * 7. Course lifecycle — active -> done -> history kept
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun CourseLifecycleArt(modifier: Modifier = Modifier) {
    ArtPanel(modifier.height(150.dp), contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LifecycleChip(
                icon = Icons.Filled.Medication,
                tint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.ob_life_active),
                modifier = Modifier.weight(1f)
            )
            LifecycleArrow()
            LifecycleChip(
                icon = Icons.Filled.CheckCircle,
                tint = ArtGreen,
                label = stringResource(R.string.ob_life_done),
                modifier = Modifier.weight(1f)
            )
            LifecycleArrow()
            LifecycleChip(
                icon = Icons.Filled.History,
                tint = ArtOrange,
                label = stringResource(R.string.ob_life_history),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LifecycleChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = ArtWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun LifecycleArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = ArtWhite.copy(alpha = 0.30f),
        modifier = Modifier.size(16.dp)
    )
}

/* ────────────────────────────────────────────────────────────────
 * 8. Calendar month — history fills in day by day
 * ──────────────────────────────────────────────────────────────── */

private const val CAL_TAKEN_LAST = 17   // indexes 0..17 are taken days
private const val CAL_MISSED_AT = 11    // one missed day
private const val CAL_TODAY_AT = 18     // today, the rest is upcoming

@Composable
fun CalendarMonthArt(modifier: Modifier = Modifier) {
    // Staggered entrance: days light up one after another.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(1500, easing = FastOutLinearInEasing))
    }
    val todayPulse = rememberBreath(1300)

    ArtPanel(modifier.height(226.dp), contentPadding = PaddingValues(14.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) { col ->
                            val index = row * 7 + col
                            DayCell(
                                index = index,
                                progress = entrance.value,
                                todayPulse = todayPulse,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendDot(color = ArtGreen, filled = true, label = stringResource(R.string.ob_hist_taken))
                LegendDot(color = ArtOrange, filled = false, label = stringResource(R.string.ob_hist_missed))
                LegendDot(color = ArtWhite.copy(alpha = 0.35f), filled = false, label = stringResource(R.string.ob_hist_future))
            }
        }
    }
}

@Composable
private fun DayCell(
    index: Int,
    progress: Float,
    todayPulse: Float,
    modifier: Modifier = Modifier
) {
    // Per-cell staggered reveal.
    val appear = ((progress - index * 0.028f) / 0.16f).coerceIn(0f, 1f)
    val isTaken = index <= CAL_TAKEN_LAST && index != CAL_MISSED_AT
    val isMissed = index == CAL_MISSED_AT
    val isToday = index == CAL_TODAY_AT

    Box(
        modifier = modifier
            .height(26.dp)
            .graphicsLayer { alpha = appear }
            .clip(RoundedCornerShape(7.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f + 0.10f * todayPulse)
                    else -> ArtWhite.copy(alpha = 0.06f)
                }
            )
            .then(
                if (isToday) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + 0.3f * todayPulse),
                        RoundedCornerShape(7.dp)
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isTaken -> Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(ArtGreen)
            )
            isMissed -> Box(
                modifier = Modifier
                    .size(11.dp)
                    .border(2.dp, ArtOrange, CircleShape)
            )
            isToday -> Icon(
                imageVector = Icons.Filled.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            else -> Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ArtWhite.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    filled: Boolean,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (filled) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .border(2.dp, color, CircleShape)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ArtWhite.copy(alpha = 0.65f)
        )
    }
}

/* ────────────────────────────────────────────────────────────────
 * 9. Shield — privacy, pulsing
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun ShieldArt(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "shieldPulse")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "shieldPhase"
    )
    val breathe = rememberBreath(2000)
    // Captured in composable context — Canvas lambdas are not composable.
    val artPrimary = MaterialTheme.colorScheme.primary
    val artTertiary = MaterialTheme.colorScheme.tertiary

    ArtPanel(modifier.height(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val base = size.minDimension

            // Expanding rings.
            repeat(2) { i ->
                val p = (phase + i * 0.5f) % 1f
                drawCircle(
                    color = artPrimary.copy(alpha = (1f - p) * 0.28f),
                    radius = base * (0.16f + 0.30f * p),
                    center = c,
                    style = Stroke(2.dp.toPx())
                )
            }

            // Shield silhouette.
            val shieldW = base * 0.30f
            val shieldH = base * 0.38f
            val left = c.x - shieldW / 2f
            val top = c.y - shieldH / 2f
            val path = Path().apply {
                moveTo(c.x, top)
                cubicTo(
                    c.x + shieldW * 0.28f, top + shieldH * 0.06f,
                    c.x + shieldW * 0.44f, top + shieldH * 0.14f,
                    c.x + shieldW * 0.46f, top + shieldH * 0.22f
                )
                // Right side down to the bottom point.
                cubicTo(
                    c.x + shieldW * 0.48f, top + shieldH * 0.55f,
                    c.x + shieldW * 0.30f, top + shieldH * 0.82f,
                    c.x, top + shieldH
                )
                // Mirror to the left.
                cubicTo(
                    c.x - shieldW * 0.30f, top + shieldH * 0.82f,
                    c.x - shieldW * 0.48f, top + shieldH * 0.55f,
                    c.x - shieldW * 0.46f, top + shieldH * 0.22f
                )
                cubicTo(
                    c.x - shieldW * 0.44f, top + shieldH * 0.14f,
                    c.x - shieldW * 0.28f, top + shieldH * 0.06f,
                    c.x, top
                )
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(
                        artPrimary.copy(alpha = 0.75f + 0.20f * breathe),
                        artTertiary.copy(alpha = 0.70f)
                    ),
                    startY = top,
                    endY = top + shieldH
                )
            )
            drawPath(
                path = path,
                color = ArtWhite.copy(alpha = 0.30f),
                style = Stroke(1.5.dp.toPx())
            )

            // Check mark.
            val check = Path().apply {
                moveTo(c.x - shieldW * 0.17f, c.y + shieldH * 0.02f)
                lineTo(c.x - shieldW * 0.045f, c.y + shieldH * 0.15f)
                lineTo(c.x + shieldW * 0.19f, c.y - shieldH * 0.13f)
            }
            drawPath(
                path = check,
                color = ArtWhite,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
