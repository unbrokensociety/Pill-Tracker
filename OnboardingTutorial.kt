package com.aistudio.meditracker.ui.components

/*
 * Onboarding v7 — rebuilt from scratch.
 *
 * ACT 1 · Four intro slides on the app's own background:
 *     1. Welcome — what the app does, in one sentence (by name, if known)
 *     2. Try it — the real dose-card gesture, live on the slide
 *     3. Reminders — the island, then the full-screen alarm fallback
 *     4. One last thing — the notification permission, asked honestly
 *
 * ACT 2 · A three-step guided tour over the REAL interface:
 *     the "Today" header, the navigation island, the "+" button.
 *     No whole-screen spotlights, no pulsing frames, no arrows: a quiet
 *     scrim with one clean hole marks the element, a solid card placed
 *     right below (or above) it explains it in one sentence, and the
 *     button inside the card moves on. Between steps the overlay fades
 *     out and back in — the screen behind never jumps.
 *
 * Replay from Settings ("Показати навчання знову") skips the intro and
 * opens the tour directly — the intro is for the very first launch.
 *
 * Elements opt into the tour with Modifier.coachTag("key"); the overlay
 * reads their window frames from CoachMarks.
 */

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/* ────────────────────────────────────────────────────────────────
 * Persistence + signals
 * ──────────────────────────────────────────────────────────────── */

object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"
    private const val KEY_PERMISSION_ASKED = "notification_permission_asked"
    private const val KEY_USER_NAME = "user_name"

    fun isCompleted(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setCompleted(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun reset(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, false).apply()
    }

    /** True once the POST_NOTIFICATIONS dialog has been shown (onboarding or fallback). */
    fun isPermissionAsked(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERMISSION_ASKED, false)
    }

    fun setPermissionAsked(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
    }

    /** Optional display name for the home greeting (set in Settings). */
    fun getUserName(context: android.content.Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_USER_NAME, null)?.trim()
        return if (name.isNullOrEmpty()) null else name.take(24)
    }

    fun setUserName(context: android.content.Context, name: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val trimmed = name?.trim()?.take(24)
        if (trimmed.isNullOrEmpty()) {
            prefs.edit().remove(KEY_USER_NAME).apply()
        } else {
            prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
        }
    }
}

/**
 * Tutorial bus: replay from Settings + navigation requests that the tour
 * hands to the main screen (open the add screen; page switching is kept
 * for compatibility). MainScreen listens and performs them.
 */
object OnboardingBus {
    var replayRequested by mutableStateOf(false)
        private set

    /** Tour is active — pager swipes are blocked for that time. */
    var tourActive by mutableStateOf(false)

    /** Request "switch to pager page 0..3". */
    var pageRequested by mutableStateOf(-1)

    /** Request "open the add-medication screen". */
    var addRequested by mutableStateOf(false)

    /** Replay from Settings: jump straight to the tour, skip the intro. */
    var replaySkipIntro by mutableStateOf(false)
        private set

    fun requestReplay(context: android.content.Context) {
        OnboardingPrefs.reset(context)
        replaySkipIntro = true
        replayRequested = true
    }

    /** Consumed by the overlay once the initial phase has been picked. */
    fun consumeReplaySkipIntro() {
        replaySkipIntro = false
    }

    fun consume() {
        replayRequested = false
    }

    fun requestPage(page: Int) {
        pageRequested = page
    }

    fun consumePage() {
        pageRequested = -1
    }

    fun requestAdd() {
        addRequested = true
    }

    fun consumeAdd() {
        addRequested = false
    }

    fun tourFinished() {
        tourActive = false
    }
}

/* ────────────────────────────────────────────────────────────────
 * Coach marks: registry of real UI element frames
 * ──────────────────────────────────────────────────────────────── */

object CoachMarks {
    /** key -> element frame in window coordinates (via onGloballyPositioned). */
    val rects = mutableStateMapOf<String, Rect>()
}

/**
 * Attach to a real UI element so the tour can highlight it:
 *   Modifier.coachTag("home_hero")
 */
fun Modifier.coachTag(key: String): Modifier {
    return this.onGloballyPositioned { coordinates ->
        val rect = coordinates.boundsInWindow()
        if (rect.width > 1f && rect.height > 1f && rect.left >= 0f) {
            CoachMarks.rects[key] = rect
        } else {
            CoachMarks.rects.remove(key)
        }
    }
}

private fun notificationsGranted(context: android.content.Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
    return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

/* ────────────────────────────────────────────────────────────────
 * Tour steps — three, all small real elements on the home page
 * ──────────────────────────────────────────────────────────────── */

private data class TourStep(
    val tag: String,               // key in CoachMarks
    val titleRes: Int,
    val descRes: Int,
    val primaryRes: Int,           // main button label
    val secondaryRes: Int? = null, // optional second button (always exits)
    val primaryOpensAdd: Boolean = false
)

private fun buildTourSteps(): List<TourStep> = listOf(
    TourStep(
        tag = "home_hero",
        titleRes = R.string.ob_step_today_title,
        descRes = R.string.ob_step_today_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "nav_island",
        titleRes = R.string.ob_step_nav_title,
        descRes = R.string.ob_step_nav_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "fab_add",
        titleRes = R.string.ob_step_add_title,
        descRes = R.string.ob_step_add_desc,
        primaryRes = R.string.ob_add_now,
        secondaryRes = R.string.ob_later,
        primaryOpensAdd = true
    )
)

/* ────────────────────────────────────────────────────────────────
 * Overlay: intro slides -> coach tour
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    // Replay from Settings opens the tour directly.
    var phase by remember { mutableStateOf(if (OnboardingBus.replaySkipIntro) 1 else 0) }

    LaunchedEffect(Unit) { OnboardingBus.consumeReplaySkipIntro() }
    SideEffect { OnboardingBus.tourActive = phase == 1 }

    if (phase == 0) {
        IntroSlides(
            onStartTour = { phase = 1 },
            onSkip = onFinished
        )
    } else {
        CoachTour(
            steps = remember { buildTourSteps() },
            onFinish = onFinished
        )
    }
}

/* ────────────────────────────────────────────────────────────────
 * Act 1: intro slides (welcome / try it / reminders / permission)
 * ──────────────────────────────────────────────────────────────── */

private const val INTRO_SLIDE_COUNT = 4

@Composable
private fun IntroSlides(
    onStartTour: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { INTRO_SLIDE_COUNT })
    val scope = rememberCoroutineScope()

    // System "back": previous slide, or leave the tutorial from the first one.
    BackHandler(enabled = true) {
        val page = pagerState.currentPage
        if (page > 0) {
            scope.launch { pagerState.animateScrollToPage(page - 1) }
        } else {
            onSkip()
        }
    }

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(280, easing = EaseOutCubic))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = entrance.value }
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            // Top row: app mark + "Skip".
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.size(9.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.ob_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSkip() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // The slides themselves: swipeable, one idea per slide.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 12.dp
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                    ) {
                        when (page) {
                            0 -> WelcomeSlide()
                            1 -> DemoSlide()
                            2 -> ReminderSlide()
                            else -> PermissionSlide()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slide dots — quiet, centered.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(INTRO_SLIDE_COUNT) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (active) 20.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Swipe hint: only meaningful on the first slide.
            if (pagerState.currentPage == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ob_swipe_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main button: next slide, or start the hands-on part.
            PrimaryButton(
                labelRes = if (pagerState.currentPage == INTRO_SLIDE_COUNT - 1) {
                    R.string.ob_start_tour
                } else {
                    R.string.ob_next
                },
                height = 52.dp
            ) {
                val page = pagerState.currentPage
                if (page >= INTRO_SLIDE_COUNT - 1) {
                    onStartTour()
                } else {
                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            }
        }
    }
}

/* ── Slide 1: welcome ── */

@Composable
private fun WelcomeSlide() {
    val context = LocalContext.current
    val userName = remember { OnboardingPrefs.getUserName(context) }

    WelcomeArt()

    Spacer(modifier = Modifier.height(26.dp))
    SlideTitle(R.string.ob_welcome_title)
    if (userName != null) {
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.ob_welcome_name, userName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(modifier = Modifier.height(9.dp))
    SlideBody(R.string.ob_welcome_desc)
}

/* ── Slide 2: the real gesture, on a real card ── */

@Composable
private fun DemoSlide() {
    var demoTaken by remember { mutableStateOf(false) }

    SlideTitle(R.string.ob_demo_title)
    Spacer(modifier = Modifier.height(9.dp))
    SlideBody(R.string.ob_demo_desc)
    Spacer(modifier = Modifier.height(20.dp))

    DemoDoseCard(taken = demoTaken, onToggle = { demoTaken = !demoTaken })

    Spacer(modifier = Modifier.height(16.dp))

    // The hint reacts to the user's action — the day ring fills exactly
    // like the real one on the home screen when a dose is marked.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniDayRing(fraction = { if (demoTaken) 1f else 0f })
        AnimatedContent(
            targetState = demoTaken,
            transitionSpec = {
                (fadeIn(tween(220, easing = EaseOutCubic)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(tween(120)))
            },
            label = "demoHint"
        ) { taken ->
            Text(
                text = stringResource(if (taken) R.string.ob_demo_hint_done else R.string.ob_demo_hint_idle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A faithful miniature of the home screen dose card: same GlassCard, same
 * icon badge, same chips, same check circle with the same haptic tick.
 */
@Composable
private fun DemoDoseCard(
    taken: Boolean,
    onToggle: () -> Unit
) {
    val hapticView = LocalView.current

    val cardScale by animateFloatAsState(
        targetValue = if (taken) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "demoCardScale"
    )
    val checkBg by animateColorAsState(
        targetValue = if (taken) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        },
        animationSpec = tween(280, easing = EaseOutCubic),
        label = "demoCheckBg"
    )
    val checkBorder by animateColorAsState(
        targetValue = if (taken) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        },
        animationSpec = tween(280, easing = EaseOutCubic),
        label = "demoCheckBorder"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        onClick = {
            hapticView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            onToggle()
        },
        glassAlpha = if (taken) 0.85f else 1.0f,
        elevation = if (taken) 6.dp else 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormTypeIcon(
                formKey = "tablet",
                tint = MaterialTheme.colorScheme.primary,
                size = 56.dp,
                iconSize = 26.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(
                        text = stringResource(R.string.ob_demo_chip_time),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    GlassChip(
                        text = stringResource(R.string.ob_demo_chip_dose),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.ob_mock_med),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.ob_mock_dose),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The star of the show: a working check circle, same as home.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(checkBg)
                    .border(
                        width = if (taken) 0.dp else 2.dp,
                        color = checkBorder,
                        shape = CircleShape
                    )
                    .clickable {
                        hapticView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggle()
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = taken,
                    transitionSpec = {
                        (
                            scaleIn(
                                initialScale = 0.4f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(tween(180))
                            ).togetherWith(
                            scaleOut(targetScale = 0.4f, animationSpec = tween(140)) +
                                fadeOut(tween(140))
                        )
                    },
                    label = "demoCheck"
                ) { checked ->
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(if (checked) 26.dp else 22.dp)
                    )
                }
            }
        }
    }
}

/* ── Slide 3: how a reminder looks (island + full-screen alarm) ── */

@Composable
private fun ReminderSlide() {
    SlideTitle(R.string.ob_reminder_title)
    Spacer(modifier = Modifier.height(9.dp))
    SlideBody(R.string.ob_reminder_desc)
    Spacer(modifier = Modifier.height(16.dp))

    IslandNotificationMock()

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.ob_reminder_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    AlarmMockArt()

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.ob_reminder_alarm),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/* ── Slide 4: the notification permission, asked honestly ── */

@Composable
private fun PermissionSlide() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(notificationsGranted(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        granted = notificationsGranted(context)
    }

    SlideTitle(R.string.ob_perm_title)
    Spacer(modifier = Modifier.height(9.dp))
    SlideBody(R.string.ob_perm_desc)

    Spacer(modifier = Modifier.height(24.dp))

    // The state morphs in place once the permission is granted.
    AnimatedContent(
        targetState = granted,
        transitionSpec = {
            (
                fadeIn(tween(260, easing = EaseOutCubic)) +
                    scaleIn(initialScale = 0.94f, animationSpec = tween(260, easing = EaseOutCubic))
                ) togetherWith (
                fadeOut(tween(140, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(140))
                )
        },
        label = "permissionState"
    ) { isGranted ->
        if (isGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.ob_perm_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else {
            PrimaryButton(
                labelRes = R.string.ob_perm_button,
                height = 50.dp
            ) {
                OnboardingPrefs.setPermissionAsked(context)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.ob_perm_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ── Shared slide building blocks ── */

@Composable
private fun SlideTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SlideBody(bodyRes: Int) {
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Solid primary button — the same quiet style the app uses. */
@Composable
private fun PrimaryButton(
    labelRes: Int,
    height: Dp = 52.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/* ────────────────────────────────────────────────────────────────
 * Act 2: guided tour over the real interface
 *
 * One step, one element, one sentence. The overlay is a scrim with a
 * single clean hole; the card sits right next to the hole and carries
 * the only button. No pulses, no arrows, no cross-screen flights:
 * between steps the overlay simply fades out and back in.
 * ──────────────────────────────────────────────────────────────── */

@Composable
private fun CoachTour(
    steps: List<TourStep>,
    onFinish: () -> Unit
) {
    val density = LocalDensity.current
    var stepIndex by remember { mutableStateOf(0) }
    var hole by remember { mutableStateOf(Rect.Zero) }

    val fade = remember { Animatable(0f) }

    val step = steps[stepIndex]

    fun performPrimary() {
        if (step.primaryOpensAdd) {
            OnboardingBus.requestAdd()
            onFinish()
        } else if (stepIndex < steps.size - 1) {
            stepIndex += 1
        } else {
            onFinish()
        }
    }

    // System "back": previous step, or leave the tutorial.
    BackHandler(enabled = true) {
        if (stepIndex > 0) {
            stepIndex -= 1
        } else {
            onFinish()
        }
    }

    // Step lifecycle: fade the spotlight out (if shown), take the new
    // element's frame, fade back in. The screen behind never jumps —
    // the swap happens while the overlay is invisible.
    LaunchedEffect(stepIndex) {
        if (fade.value > 0f) {
            fade.animateTo(0f, tween(170, easing = FastOutLinearInEasing))
        }
        val rect = withTimeoutOrNull(1500L) {
            snapshotFlow { CoachMarks.rects[steps[stepIndex].tag] }
                .filterNotNull()
                .firstOrNull()
        }
        hole = rect ?: Rect.Zero
        fade.animateTo(1f, tween(300, easing = EaseOutCubic))
    }

    // Keep the hole glued to the element in case its frame shifts.
    LaunchedEffect(stepIndex) {
        snapshotFlow { CoachMarks.rects[steps[stepIndex].tag] }
            .filterNotNull()
            .collect { rect ->
                if (rect != hole) hole = rect
            }
    }

    var tooltipH by remember { mutableStateOf(210.dp) }
    val gap = 18.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Consume everything: the interface below is read-only during
            // the tour; the card's buttons are the way forward.
            .pointerInput(Unit) { detectTapGestures { } }
            .drawBehind {
                val f = fade.value
                if (f <= 0.01f) return@drawBehind
                val h = hole
                // Scrim with one rounded hole above the highlighted element.
                val path = Path()
                path.fillType = PathFillType.EvenOdd
                path.addRect(Rect(0f, 0f, size.width, size.height))
                if (!h.isEmpty) {
                    val pad = 10.dp.toPx()
                    val corner = 22.dp.toPx()
                    path.addRoundRect(
                        RoundRect(
                            rect = Rect(h.left - pad, h.top - pad, h.right + pad, h.bottom + pad),
                            cornerRadius = CornerRadius(corner, corner)
                        )
                    )
                }
                drawPath(path, Color.Black.copy(alpha = 0.52f * f))
                // A thin quiet ring — no pulsing.
                if (!h.isEmpty) {
                    val pad = 10.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.65f * f),
                        topLeft = Offset(h.left - pad, h.top - pad),
                        size = Size(h.width + pad * 2f, h.height + pad * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
    ) {
        val maxHPx = with(density) { maxHeight.toPx() }

        // "Skip" — always available in the top-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .graphicsLayer { alpha = fade.value }
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onFinish() }
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                text = stringResource(R.string.ob_skip),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Step card: solid surface, overline with the step number, title,
        // one sentence, one button. Positioned below the hole when it
        // fits, above it otherwise; centered when there is no hole yet.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val tipHPx = tooltipH.toPx()
                    val gapPx = gap.toPx()
                    val y: Float = if (hole.isEmpty) {
                        (maxHPx - tipHPx) / 2f
                    } else {
                        val fitsBelow =
                            hole.bottom + gapPx + tipHPx + 24.dp.toPx() <= maxHPx
                        if (fitsBelow) {
                            hole.bottom + gapPx
                        } else {
                            (hole.top - gapPx - tipHPx).coerceAtLeast(64.dp.toPx())
                        }
                    }
                    IntOffset(0, y.roundToInt())
                }
                .graphicsLayer { alpha = fade.value }
                .onGloballyPositioned { coords ->
                    with(density) { tooltipH = coords.size.height.toDp() }
                }
                .padding(horizontal = 20.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = stringResource(R.string.ob_step_of, stepIndex + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = stringResource(step.descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Primary action of the step.
            CoachButton(
                labelRes = step.primaryRes,
                filled = true
            ) { performPrimary() }

            // Optional secondary action ("later") — always exits the tour.
            step.secondaryRes?.let { secondaryRes ->
                Spacer(modifier = Modifier.height(6.dp))
                CoachButton(
                    labelRes = secondaryRes,
                    filled = false
                ) { onFinish() }
            }
        }
    }
}

/** Tour card buttons: solid primary, or a quiet outlined variant. */
@Composable
private fun CoachButton(
    labelRes: Int,
    filled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (filled) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val fg = if (filled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (filled) 46.dp else 42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (filled) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp)
                    )
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}
