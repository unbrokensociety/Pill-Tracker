package com.aistudio.meditracker.ui.components

/*
 * Onboarding v6 — plain, native, human.
 *
 * ACT 1 · Four swipeable intro slides on the app's own background, set in
 * the app's own colors and components (no dark overlay, no gradients):
 *     1. Welcome — what the app does, in one sentence (by name, if given)
 *     2. Try it — the real dose-card gesture, right on the slide
 *     3. Reminders — the island, then the full-screen alarm fallback
 *     4. One last thing — the notification permission, honestly asked
 *
 * ACT 2 · Five coach-mark steps over the REAL interface: today's doses,
 * the navigation island, the Calendar (history), Settings, and finally
 * the "+" button that opens the real add form.
 *
 * The tour always ends with an action, not a "congratulations" screen:
 * either "add now" (the add form opens for real) or "later" (home).
 *
 * Coach-mark mechanics: screens publish their frames through
 * Modifier.coachTag("key"); the overlay cuts a hole in the scrim above
 * the current step's frame and animates the hole between steps.
 */

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
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
 * hands to the main screen (switch pager page / open the add screen).
 * MainScreen listens and performs them.
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

    fun requestReplay(context: android.content.Context) {
        OnboardingPrefs.reset(context)
        replayRequested = true
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
 *   Modifier.coachTag("home_list")
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
 * Tour steps (the hands-on part)
 * ──────────────────────────────────────────────────────────────── */

private data class TourStep(
    val tag: String,                // key in CoachMarks
    val wantPage: Int,              // pager page to show when the step starts
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val primaryRes: Int,            // main button label
    val secondaryRes: Int? = null,  // optional second button (always exits the tour)
    val primaryOpensAdd: Boolean = false // primary action opens the real add form
)

private fun buildTourSteps(): List<TourStep> = listOf(
    TourStep(
        tag = "home_list",
        wantPage = 0,
        icon = Icons.Filled.TouchApp,
        titleRes = R.string.ob_step_doses_title,
        descRes = R.string.ob_step_doses_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "nav_island",
        wantPage = 0,
        icon = Icons.Filled.SwapHoriz,
        titleRes = R.string.ob_step_nav_title,
        descRes = R.string.ob_step_nav_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "calendar_content",
        wantPage = 1,
        icon = Icons.Filled.CalendarMonth,
        titleRes = R.string.ob_step_cal_title,
        descRes = R.string.ob_step_cal_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "settings_content",
        wantPage = 3,
        icon = Icons.Filled.Tune,
        titleRes = R.string.ob_step_settings_title,
        descRes = R.string.ob_step_settings_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "fab_add",
        wantPage = 0,
        icon = Icons.Filled.AddCircle,
        titleRes = R.string.ob_step_add_title,
        descRes = R.string.ob_step_add_desc,
        primaryRes = R.string.ob_add_now,
        secondaryRes = R.string.ob_later,
        primaryOpensAdd = true
    )
)

private val RectConverter = TwoWayConverter<Rect, AnimationVector4D>(
    convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
    convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) }
)

/* ────────────────────────────────────────────────────────────────
 * Overlay: intro slides -> coach tour
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    var phase by remember { mutableStateOf(0) } // 0 = slides, 1 = coach tour

    SideEffect {
        OnboardingBus.tourActive = phase == 1
    }

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

            // Slide dots — quiet, left-aligned with the content.
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

    Spacer(modifier = Modifier.height(24.dp))
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
            // min, not fixed: at larger system font scales a two-line
            // label must grow the button instead of being cut off.
            .heightIn(min = height)
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
 * Act 2: coach tour over the real interface
 * ──────────────────────────────────────────────────────────────── */

@Composable
private fun CoachTour(
    steps: List<TourStep>,
    onFinish: () -> Unit
) {
    val density = LocalDensity.current
    var stepIndex by remember { mutableStateOf(0) }
    var shownStep by remember { mutableStateOf(0) }
    var holeTarget by remember { mutableStateOf<Rect?>(null) }
    var nudge by remember { mutableStateOf(0) }

    val holeAnim = remember { Animatable(Rect(0f, 0f, 0f, 0f), RectConverter) }

    val step = steps[stepIndex]

    // System "back": previous step, or leave the tutorial.
    BackHandler(enabled = true) {
        if (stepIndex > 0) {
            stepIndex -= 1
        } else {
            onFinish()
        }
    }

    // Ask the main screen for the page the step lives on, then wait a
    // moment for the tag's frame to appear.
    LaunchedEffect(stepIndex) {
        OnboardingBus.requestPage(step.wantPage)
        val tag = step.tag
        withTimeoutOrNull(900L) {
            snapshotFlow { CoachMarks.rects[tag] }.firstOrNull()
        }
        shownStep = stepIndex
    }

    // Track the highlighted element's frame (tall elements are capped
    // so the hole never swallows the whole screen).
    LaunchedEffect(shownStep) {
        val tag = steps[shownStep].tag
        snapshotFlow { CoachMarks.rects[tag] }
            .filterNotNull()
            .collect { r ->
                val maxHoleH = with(density) { 300.dp.toPx() }
                holeTarget = if (r.height > maxHoleH) {
                    Rect(r.left, r.top, r.right, r.top + maxHoleH)
                } else {
                    r
                }
            }
    }

    LaunchedEffect(holeTarget) {
        val target = holeTarget ?: return@LaunchedEffect
        if (holeAnim.value.isEmpty) {
            holeAnim.snapTo(target)
        } else {
            holeAnim.animateTo(
                target,
                spring(dampingRatio = 0.88f, stiffness = 460f)
            )
        }
    }

    // Pulsing highlight frame.
    val pulse = rememberInfiniteTransition(label = "coachPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "coachPulseAlpha"
    )

    // "Tap here": expanding rings in the center of the hole.
    val tapPulse = rememberInfiniteTransition(label = "tapPulse")
    val tapPhase by tapPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "tapPhase"
    )

    val appear = remember(shownStep) { Animatable(0f) }
    LaunchedEffect(shownStep) {
        appear.animateTo(1f, tween(360, easing = EaseOutCubic))
    }

    val nudgeShake by animateFloatAsState(
        targetValue = if (nudge > 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f),
        label = "nudgeShake"
    )
    LaunchedEffect(nudge) {
        if (nudge > 0) {
            delay(450)
            nudge = 0
        }
    }

    // LIGHT scrim: the real interface stays visible behind the overlay —
    // the tooltip card has its own solid background for readability.
    val scrimAlpha = 0.40f * appear.value

    val gap = 10.dp
    val arrowSize = 14.dp
    var tooltipH by remember { mutableStateOf(232.dp) }

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(holeTarget, stepIndex) {
                detectTapGestures { offset ->
                    val hole = holeTarget
                    if (hole != null && hole.inflate(12f).contains(offset)) {
                        performPrimary()
                    } else {
                        nudge++
                    }
                }
            }
            .drawBehind {
                // Scrim with a hole above the highlighted element.
                val hole = holeAnim.value
                val path = Path()
                path.fillType = PathFillType.EvenOdd
                path.addRect(Rect(0f, 0f, size.width, size.height))
                if (!hole.isEmpty) {
                    val cornerRadius = 22.dp.toPx()
                    path.addRoundRect(
                        RoundRect(
                            left = hole.left - 8f,
                            top = hole.top - 8f,
                            right = hole.right + 8f,
                            bottom = hole.bottom + 8f,
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    )
                }
                drawPath(path, Color.Black.copy(alpha = scrimAlpha))
            }
    ) {
        val maxHPx = with(density) { maxHeight.toPx() }
        val topMinPx = with(density) { 100.dp.toPx() }
        val bottomGuardPx = with(density) { 24.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = appear.value
                    translationX = nudgeShake * 6f * sin(nudge * 12.9898f * 100f)
                }
        ) {
            val hole = holeAnim.value
            if (hole.isEmpty) return@Canvas
            val cornerRadius = 22.dp.toPx()
            val insetHole = Rect(
                left = hole.left - 8f,
                top = hole.top - 8f,
                right = hole.right + 8f,
                bottom = hole.bottom + 8f
            )
            // Soft glow.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f * pulseAlpha),
                topLeft = Offset(insetHole.left, insetHole.top),
                size = Size(insetHole.width, insetHole.height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 10.dp.toPx())
            )
            // Bright frame.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f * pulseAlpha),
                topLeft = Offset(insetHole.left, insetHole.top),
                size = Size(insetHole.width, insetHole.height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 2.5.dp.toPx())
            )
            // "Tap here" rings + center dot.
            val c = hole.center
            val rMin = min(hole.width, hole.height) / 2f
            val ringR = rMin * (0.34f + 0.62f * tapPhase)
            drawCircle(
                color = Color.White.copy(alpha = (1f - tapPhase) * 0.50f),
                radius = ringR,
                center = c,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = 5.dp.toPx(),
                center = c
            )
        }

        // "Skip" — always in the top-right corner.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
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
        }

        // Step tooltip: the card measures its real height and positions
        // itself below the hole (or above it when there is no room).
        AnimatedContent(
            targetState = shownStep,
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val hole = holeAnim.value
                    val tipHPx = tooltipH.toPx()
                    val gapPx = gap.toPx()
                    val y: Float = if (hole.isEmpty) {
                        (maxHPx - tipHPx) / 2f
                    } else {
                        val fitsBelow =
                            hole.bottom + gapPx + tipHPx + bottomGuardPx < maxHPx
                        if (fitsBelow) {
                            (hole.bottom + gapPx)
                                .coerceAtMost(maxHPx - tipHPx - bottomGuardPx)
                        } else {
                            (hole.top - tipHPx - gapPx).coerceAtLeast(topMinPx)
                        }
                    }
                    IntOffset(0, y.roundToInt())
                }
                .padding(horizontal = 20.dp),
            transitionSpec = {
                (
                    fadeIn(tween(260, easing = EaseOutCubic)) +
                        scaleIn(
                            initialScale = 0.97f,
                            animationSpec = tween(260, easing = EaseOutCubic)
                        )
                    ) togetherWith (
                    fadeOut(tween(140, easing = FastOutLinearInEasing)) +
                        scaleOut(targetScale = 0.98f, animationSpec = tween(140))
                    )
            },
            label = "coachTooltip"
        ) { index ->
            val s = steps[index]
            // Colors are read in composable context (never inside drawBehind).
            val tipColor = MaterialTheme.colorScheme.surface
            val titleColor = MaterialTheme.colorScheme.onSurface
            val subColor = MaterialTheme.colorScheme.onSurfaceVariant
            // Long localized descriptions plus two buttons can outgrow a
            // small screen — cap the card and scroll its content instead
            // of clipping the buttons out of reach.
            val maxTipHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = appear.value }
                    .onGloballyPositioned { coords ->
                        with(density) { tooltipH = coords.size.height.toDp() }
                    }
                    .heightIn(max = maxTipHeight)
                    // Arrow stays on the non-scrolling window (drawn before
                    // the scroll node) so it is never clipped by it.
                    .drawBehind {
                        val hole = holeAnim.value
                        val a = arrowSize.toPx()
                        val cx = if (hole.isEmpty) {
                            size.width / 2f
                        } else {
                            val screenCx = (hole.left + hole.right) / 2f - 20.dp.toPx()
                            screenCx.coerceIn(a, size.width - a)
                        }
                        val path = Path()
                        val fitsBelow = if (hole.isEmpty) true else {
                            hole.bottom + gap.toPx() + size.height + 24.dp.toPx() < maxHPx
                        }
                        if (fitsBelow) {
                            path.moveTo(cx, -a / 2f)
                            path.lineTo(cx + a / 2f, a / 2f)
                            path.lineTo(cx, a * 1.5f)
                            path.lineTo(cx - a / 2f, a / 2f)
                        } else {
                            path.moveTo(cx, size.height + a / 2f)
                            path.lineTo(cx + a / 2f, size.height - a / 2f)
                            path.lineTo(cx, size.height - a * 1.5f)
                            path.lineTo(cx - a / 2f, size.height - a / 2f)
                        }
                        path.close()
                        drawPath(path, tipColor)
                    }
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(22.dp))
                    .background(tipColor)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = s.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Text(
                        text = stringResource(s.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(
                            R.string.ob_step_of, index + 1, steps.size
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = subColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(s.descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary action of the step.
                CoachButton(
                    labelRes = s.primaryRes,
                    filled = true
                ) { performPrimary() }

                // Optional secondary action ("later") — always exits the tour.
                s.secondaryRes?.let { secondaryRes ->
                    Spacer(modifier = Modifier.height(8.dp))
                    CoachButton(
                        labelRes = secondaryRes,
                        filled = false
                    ) { onFinish() }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.ob_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Tooltip buttons: solid primary, or a quiet outlined variant. */
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
            .heightIn(min = if (filled) 46.dp else 42.dp)
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
