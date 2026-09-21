package com.aistudio.meditracker.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"
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

object OnboardingBus {
    var replayRequested by mutableStateOf(false)
        private set

    var tourActive by mutableStateOf(false)

    var pageRequested by mutableStateOf(-1)

    var addRequested by mutableStateOf(false)

    var backRequested by mutableStateOf(false)

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

    fun requestBack() {
        backRequested = true
    }

    fun consumeBack() {
        backRequested = false
    }

    fun tourFinished() {
        tourActive = false
    }
}

object CoachMarks {
    val rects = mutableStateMapOf<String, Rect>()
}

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

private enum class TourTap { NEXT, OPEN_CALENDAR, OPEN_ADD }

private data class TourStep(
    val tag: String,
    val wantPage: Int? = null,
    val wantAddScreen: Boolean = false,
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val tap: TourTap = TourTap.NEXT,
    val isLast: Boolean = false,
    val animatedIcon: Boolean = false
)

private fun buildTourSteps(): List<TourStep> = listOf(
    TourStep(
        tag = "home_hero",
        wantPage = 0,
        icon = Icons.Filled.Today,
        titleRes = R.string.tour_step_today_title,
        descRes = R.string.tour_step_today_desc,
        tap = TourTap.NEXT
    ),
    TourStep(
        tag = "home_list",
        wantPage = 0,
        icon = Icons.Filled.CheckCircle,
        titleRes = R.string.tour_step_dose_title,
        descRes = R.string.tour_step_dose_desc,
        tap = TourTap.NEXT
    ),
    TourStep(
        tag = "nav_island",
        wantPage = 0,
        icon = Icons.Filled.SwipeLeft,
        titleRes = R.string.tour_step_nav_title,
        descRes = R.string.tour_step_nav_desc,
        tap = TourTap.OPEN_CALENDAR,
        animatedIcon = true
    ),
    TourStep(
        tag = "calendar_content",
        wantPage = 1,
        icon = Icons.Filled.CalendarMonth,
        titleRes = R.string.tour_step_calendar_title,
        descRes = R.string.tour_step_calendar_desc,
        tap = TourTap.NEXT
    ),
    TourStep(
        tag = "meds_content",
        wantPage = 2,
        icon = Icons.AutoMirrored.Filled.List,
        titleRes = R.string.tour_step_meds_title,
        descRes = R.string.tour_step_meds_desc,
        tap = TourTap.NEXT
    ),
    TourStep(
        tag = "settings_content",
        wantPage = 3,
        icon = Icons.Filled.Settings,
        titleRes = R.string.tour_step_settings_title,
        descRes = R.string.tour_step_settings_desc,
        tap = TourTap.NEXT
    ),
    TourStep(
        tag = "fab_add",
        wantPage = 2,
        icon = Icons.Filled.AddCircle,
        titleRes = R.string.tour_step_add_title,
        descRes = R.string.tour_step_add_desc,
        tap = TourTap.OPEN_ADD,
        animatedIcon = true
    ),
    TourStep(
        tag = "add_form",
        wantAddScreen = true,
        icon = Icons.Filled.TouchApp,
        titleRes = R.string.tour_step_form_title,
        descRes = R.string.tour_step_form_desc,
        tap = TourTap.NEXT,
        isLast = true
    )
)

private val RectConverter = TwoWayConverter<Rect, AnimationVector4D>(
    convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
    convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) }
)

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    val steps = remember { buildTourSteps() }
    var phase by remember { mutableStateOf(0) }
    var stepIndex by remember { mutableStateOf(0) }

    SideEffect {
        OnboardingBus.tourActive = phase == 1
    }
    if (phase == 0) {
        BackHandler { onFinished() }
        WelcomeCard(
            onStart = {
                phase = 1
                stepIndex = 0
            },
            onSkip = onFinished
        )
    } else if (phase == 1) {
        CoachTour(
            steps = steps,
            stepIndex = stepIndex,
            onStepChange = { stepIndex = it },
            onFinish = { phase = 2 }
        )
    } else {
        BackHandler { onFinished() }
        FinishCard(
            onStart = onFinished
        )
    }
}

@Composable
private fun WelcomeCard(
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(360, easing = EaseOutCubic))
    }

    var userName by remember { mutableStateOf(OnboardingPrefs.getUserName(context) ?: "") }

    val drift = rememberInfiniteTransition(label = "onboardingDrift")
    val phase by drift.animateFloat(
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
                    val t = phase * 2f * Math.PI.toFloat()
                    translate(
                        w * (0.22f + 0.10f * cos(t)),
                        h * (0.16f + 0.08f * sin(t))
                    ) {
                        drawCircle(brush = blob1, radius = w * 0.75f, center = Offset.Zero)
                    }
                    translate(
                        w * (0.82f + 0.08f * sin(t)),
                        h * (0.30f + 0.10f * cos(t))
                    ) {
                        drawCircle(brush = blob2, radius = w * 0.65f, center = Offset.Zero)
                    }
                    translate(
                        w * (0.50f + 0.12f * cos(t * 0.7f)),
                        h * (0.92f + 0.06f * sin(t * 0.7f))
                    ) {
                        drawCircle(brush = blob3, radius = w * 0.70f, center = Offset.Zero)
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        val screenMaxHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
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
                        ) {
                            OnboardingPrefs.setUserName(context, userName)
                            onSkip()
                        }
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

            Spacer(modifier = Modifier.weight(0.55f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenMaxHeight - 96.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1D242F), Color(0xFF161B24))
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(26.dp)
            ) {
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
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
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
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.ob_welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ob_welcome_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                listOf(
                    R.string.ob_welcome_b1,
                    R.string.ob_welcome_b2,
                    R.string.ob_welcome_b3
                ).forEach { res ->
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
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.ob_name_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { if (it.length <= 24) userName = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ob_name_placeholder),
                            color = Color.White.copy(alpha = 0.38f)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.38f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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
                        ) {
                            OnboardingPrefs.setUserName(context, userName)
                            onStart()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.tour_start),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.PanTool,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.tour_interactive_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.45f))
        }
    }
}

@Composable
private fun FinishCard(
    onStart: () -> Unit
) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(360, easing = EaseOutCubic))
    }

    val checkScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(160)
        checkScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entrance.value
                scaleX = 0.94f + 0.06f * entrance.value
                scaleY = 0.94f + 0.06f * entrance.value
            }
            .background(Color.Black.copy(alpha = 0.92f))
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> change.consume() }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1D242F), Color(0xFF161B24))
                    )
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        scaleX = checkScale.value
                        scaleY = checkScale.value
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
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.tour_done_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tour_done_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))
            listOf(
                R.string.tour_done_b1,
                R.string.tour_done_b2
            ).forEach { res ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
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
                    ) { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tour_done_start),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachTour(
    steps: List<TourStep>,
    stepIndex: Int,
    onStepChange: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val step = steps[stepIndex]
    val density = LocalDensity.current

    var shownStep by remember { mutableStateOf(0) }
    var holeTarget by remember { mutableStateOf<Rect?>(null) }
    var nudge by remember { mutableStateOf(0) }

    val holeAnim = remember { Animatable(Rect(0f, 0f, 0f, 0f), RectConverter) }

    BackHandler(enabled = true) {
        if (stepIndex > 0) {
            if (step.wantAddScreen) OnboardingBus.requestBack()
            onStepChange(stepIndex - 1)
        } else {
            onFinish()
        }
    }

    LaunchedEffect(stepIndex) {
        if (step.wantAddScreen) {
            OnboardingBus.requestAdd()
        } else {
            step.wantPage?.let { OnboardingBus.requestPage(it) }
        }
        val tag = step.tag
        withTimeoutOrNull(900L) {
            snapshotFlow { CoachMarks.rects[tag] }.filterNotNull().first()
        }
        shownStep = stepIndex
    }

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

    val gap = 10.dp
    val arrowSize = 14.dp
    var tooltipH by remember { mutableStateOf(232.dp) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(holeTarget, stepIndex) {
                detectTapGestures { offset ->
                    val hole = holeTarget
                    if (hole != null && hole.inflate(12f).contains(offset)) {
                        when (step.tap) {
                            TourTap.NEXT -> {
                                if (step.isLast) onFinish() else onStepChange(stepIndex + 1)
                            }
                            TourTap.OPEN_CALENDAR -> {
                                OnboardingBus.requestPage(1)
                                onStepChange(stepIndex + 1)
                            }
                            TourTap.OPEN_ADD -> {
                                OnboardingBus.requestAdd()
                                onStepChange(stepIndex + 1)
                            }
                        }
                    } else {
                        nudge++
                    }
                }
            }
            .drawBehind {
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
                drawPath(path, Color.Black.copy(alpha = 0.88f * appear.value))
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
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f * pulseAlpha),
                topLeft = Offset(insetHole.left, insetHole.top),
                size = Size(insetHole.width, insetHole.height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 10.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f * pulseAlpha),
                topLeft = Offset(insetHole.left, insetHole.top),
                size = Size(insetHole.width, insetHole.height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 2.5.dp.toPx())
            )

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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
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
                        ) {
                            if (s.isLast) onFinish() else onStepChange(index + 1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            if (s.isLast) R.string.tour_finish else R.string.tour_next
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.tour_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
