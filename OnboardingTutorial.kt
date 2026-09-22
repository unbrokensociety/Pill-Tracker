package com.aistudio.meditracker.ui.components

/*
 * Onboarding v3 — three short acts:
 *
 *   1) Swipeable intro slides: what the app does, how a reminder looks
 *      (a preview of the real notification island) and privacy.
 *   2) A contextual notification-permission step: the system dialog is
 *      only shown after the user knows why reminders need it.
 *   3) Two coach-mark steps over the REAL interface (the scrim is light
 *      so the app stays visible): where today's doses live and the "+"
 *      button that opens the real add form.
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
    val animatedIcon: Boolean = false,
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
        animatedIcon = true,
        titleRes = R.string.ob_step_doses_title,
        descRes = R.string.ob_step_doses_desc,
        primaryRes = R.string.ob_next
    ),
    TourStep(
        tag = "fab_add",
        wantPage = 0,
        icon = Icons.Filled.AddCircle,
        animatedIcon = true,
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
 * Act 1 + 2: intro slides (welcome / reminder / privacy / permission)
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
        entrance.animateTo(1f, tween(360, easing = EaseOutCubic))
    }

    // Living backdrop: three slowly drifting radial blobs.
    val drift = rememberInfiniteTransition(label = "onboardingDrift")
    val blobPhase by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "onboardingPhase"
    )
    val roleColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entrance.value
                scaleX = 0.94f + 0.06f * entrance.value
                scaleY = 0.94f + 0.06f * entrance.value
            }
            .background(Color.Black.copy(alpha = 0.92f))
            .drawWithCache {
                val w = size.width
                val h = size.height
                val blob1 = Brush.radialGradient(
                    colors = listOf(roleColors[0].copy(alpha = 0.30f), Color.Transparent),
                    center = Offset.Zero,
                    radius = w * 0.75f
                )
                val blob2 = Brush.radialGradient(
                    colors = listOf(roleColors[1].copy(alpha = 0.24f), Color.Transparent),
                    center = Offset.Zero,
                    radius = w * 0.65f
                )
                val blob3 = Brush.radialGradient(
                    colors = listOf(roleColors[2].copy(alpha = 0.20f), Color.Transparent),
                    center = Offset.Zero,
                    radius = w * 0.70f
                )
                onDrawBehind {
                    val t = blobPhase * 2f * Math.PI.toFloat()
                    translate(w * (0.22f + 0.10f * cos(t)), h * (0.16f + 0.08f * sin(t))) {
                        drawCircle(brush = blob1, radius = w * 0.75f, center = Offset.Zero)
                    }
                    translate(w * (0.82f + 0.08f * sin(t)), h * (0.30f + 0.10f * cos(t))) {
                        drawCircle(brush = blob2, radius = w * 0.65f, center = Offset.Zero)
                    }
                    translate(w * (0.50f + 0.12f * cos(t * 0.7f)), h * (0.92f + 0.06f * sin(t * 0.7f))) {
                        drawCircle(brush = blob3, radius = w * 0.70f, center = Offset.Zero)
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* absorb taps on the backdrop */ }
    ) {
        // Local height cap available to nested lambdas without receiver tricks.
        val screenMaxHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            // Top row: logo + "Skip".
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.13f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSkip() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ob_skip),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The slides themselves: swipeable, one idea per slide.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 16.dp
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .heightIn(max = screenMaxHeight - 236.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1D242F), Color(0xFF161B24))
                                )
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(26.dp)
                    ) {
                        when (page) {
                            0 -> WelcomeSlide()
                            1 -> ReminderSlide()
                            2 -> PrivacySlide()
                            else -> PermissionSlide()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slide dots.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(INTRO_SLIDE_COUNT) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (active) 18.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.22f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Swipe hint: only meaningful on the first slide.
            if (pagerState.currentPage == 0) {
                Text(
                    text = stringResource(R.string.ob_swipe_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main button: next slide, or start the hands-on part.
            GradientButton(
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
    // Breathing app icon.
    val breathe = rememberInfiniteTransition(label = "breathe")
    val scale by breathe.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "breatheScale"
    )
    SlideIconBadge(
        icon = Icons.Filled.Medication,
        scale = { scale }
    )

    Spacer(modifier = Modifier.height(20.dp))
    SlideTitle(R.string.ob_welcome_title)
    Spacer(modifier = Modifier.height(8.dp))
    SlideBody(R.string.ob_welcome_desc)
    Spacer(modifier = Modifier.height(18.dp))
    SlideBullet(R.string.ob_welcome_b1)
    Spacer(modifier = Modifier.height(10.dp))
    SlideBullet(R.string.ob_welcome_b2)
    Spacer(modifier = Modifier.height(10.dp))
    SlideBullet(R.string.ob_welcome_b3)
}

/* ── Slide 2: how a reminder looks ── */

@Composable
private fun ReminderSlide() {
    SlideIconBadge(icon = Icons.Filled.Notifications)

    Spacer(modifier = Modifier.height(18.dp))
    SlideTitle(R.string.ob_reminder_title)
    Spacer(modifier = Modifier.height(8.dp))
    SlideBody(R.string.ob_reminder_desc)
    Spacer(modifier = Modifier.height(18.dp))

    ReminderIslandMock()

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.ob_reminder_explain),
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.72f)
    )
}

/** A still preview of the real reminder island with its action buttons. */
@Composable
private fun ReminderIslandMock() {
    // Gentle float so the card reads as "alive".
    val float = rememberInfiniteTransition(label = "islandFloat")
    val floatY by float.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "islandFloatY"
    )
    // Pulsing "now" marker, like the live chronometer in the real reminder.
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
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = floatY }
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF232B38), Color(0xFF1A212C))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
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
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
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
                    color = Color.White.copy(alpha = 0.55f)
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
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Filled "taken" action — the primary path.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                Text(
                    text = stringResource(R.string.notif_taken),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            // Tonal "snooze" action.
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ob_mock_snooze),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/* ── Slide 3: privacy ── */

@Composable
private fun PrivacySlide() {
    // Slow float for the lock badge.
    val float = rememberInfiniteTransition(label = "lockFloat")
    val offsetY by float.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "lockFloatY"
    )

    SlideIconBadge(
        icon = Icons.Filled.Lock,
        translationY = { offsetY }
    )

    Spacer(modifier = Modifier.height(18.dp))
    SlideTitle(R.string.ob_privacy_title)
    Spacer(modifier = Modifier.height(8.dp))
    SlideBody(R.string.ob_privacy_desc)
    Spacer(modifier = Modifier.height(18.dp))

    // Three compact fact chips.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            R.string.ob_privacy_c1,
            R.string.ob_privacy_c2,
            R.string.ob_privacy_c3
        ).forEach { res ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ── Slide 4: notification permission ── */

@Composable
private fun PermissionSlide() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(notificationsGranted(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        granted = notificationsGranted(context)
    }

    // A bell that rocks gently — reminders are the point of the app.
    val rock = rememberInfiniteTransition(label = "bellRock")
    val angle by rock.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "bellAngle"
    )
    SlideIconBadge(
        icon = Icons.Filled.Notifications,
        rotationZ = { angle },
        rotationPivotTop = true
    )

    Spacer(modifier = Modifier.height(18.dp))
    SlideTitle(R.string.ob_perm_title)
    Spacer(modifier = Modifier.height(8.dp))
    SlideBody(R.string.ob_perm_desc)
    Spacer(modifier = Modifier.height(20.dp))

    // Success state morphs in place once the permission is granted.
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
                    .background(Color(0xFF1E3A2F))
                    .border(1.dp, Color(0xFF3F8F6A), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF6FCF97),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.ob_perm_granted),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD7F5E4)
                )
            }
        } else {
            Column {
                GradientButton(
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
    }
}

/* ── Shared slide building blocks ── */

@Composable
private fun SlideIconBadge(
    icon: ImageVector,
    scale: () -> Float = { 1f },
    translationY: () -> Float = { 0f },
    rotationZ: () -> Float = { 0f },
    rotationPivotTop: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
                this.translationY = translationY()
                this.rotationZ = rotationZ()
                if (rotationPivotTop) {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.15f)
                }
            }
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
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun SlideTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
    )
}

@Composable
private fun SlideBody(bodyRes: Int) {
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.82f)
    )
}

@Composable
private fun SlideBullet(bulletRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(bulletRes),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun GradientButton(
    labelRes: Int,
    height: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

/* ────────────────────────────────────────────────────────────────
 * Act 3: coach tour over the real interface
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
            kotlinx.coroutines.delay(450)
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
            val rMin = kotlin.math.min(hole.width, hole.height) / 2f
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
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFinish() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = stringResource(R.string.ob_skip),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = appear.value }
                    .onGloballyPositioned { coords ->
                        with(density) { tooltipH = coords.size.height.toDp() }
                    }
                    // Arrow pointing at the hole (drawn before clip so the
                    // rounded corners do not cut it off).
                    .drawBehind {
                        val hole = holeAnim.value
                        val a = arrowSize.toPx()
                        val cx = if (hole.isEmpty) {
                            size.width / 2f
                        } else {
                            val screenCx = (hole.left + hole.right) / 2f - 20.dp.toPx()
                            screenCx.coerceIn(a, size.width - a)
                        }
                        val tipColor = Color(0xFF1D242F)
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1D242F), Color(0xFF161B24))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Step icon: the "hand" and "plus" icons wiggle slightly.
                    val wiggle = rememberInfiniteTransition(label = "wiggle")
                    val wiggleX by wiggle.animateFloat(
                        initialValue = -7f,
                        targetValue = 7f,
                        animationSpec = infiniteRepeatable(
                            tween(700, easing = FastOutLinearInEasing),
                            RepeatMode.Reverse
                        ),
                        label = "wiggleX"
                    )
                    val wiggleScale by wiggle.animateFloat(
                        initialValue = 0.97f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            tween(900, easing = FastOutLinearInEasing),
                            RepeatMode.Reverse
                        ),
                        label = "wiggleScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                if (s.animatedIcon) {
                                    translationX = wiggleX
                                    scaleX = wiggleScale
                                    scaleY = wiggleScale
                                }
                            }
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
                            imageVector = s.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(s.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.ob_step_of, index + 1, steps.size
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(s.descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tour progress dots.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { i ->
                        val active = i == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    width = if (active) 18.dp else 6.dp,
                                    height = 6.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.22f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary action of the step.
                GradientButton(
                    labelRes = s.primaryRes,
                    height = 48.dp
                ) { performPrimary() }

                // Optional secondary action ("later") — always exits the tour.
                s.secondaryRes?.let { secondaryRes ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onFinish() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(secondaryRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.ob_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
