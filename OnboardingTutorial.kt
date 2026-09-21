package com.example.ui.components

/*
 * OnboardingTutorial v2 — ИНТЕРАКТИВНЫЙ тур по приложению.
 *
 *  • Первая страница — приветствие (что это за приложение).
 *  • Дальше — живые coach-marks ПОВЕРХ РЕАЛЬНОГО интерфейса:
 *    подсвечивается настоящая кнопка/зона, тап по подсветке
 *    программно выполняет то же действие (переключает страницу,
 *    открывает экран добавления) — человека реально «перекидывает».
 *  • Кнопка «Пропустить» — всегда сверху; шаги можно листать кнопкой
 *    «Дальше»; системный «назад» идёт по шагам.
 *  • Кнопка в Настройках перезапускает тур через OnboardingBus.
 *
 * Механика подсветки: экраны вешают теги через Modifier.coachTag("key")
 * — их рамки складываются в CoachMarks.rects; оверлей читает рамку
 * текущего шага и рисует затемнение с «дыркой» (Path + EvenOdd).
 */

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.flow.filter

/* ────────────────────────────────────────────────────────────────
 * Персистентность + сигналы навигации
 * ──────────────────────────────────────────────────────────────── */

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

    /** Имя для персонального приветствия (необязательно, только на устройстве). */
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
 * Шина туториала: повтор из Настройок + запросы навигации, которые
 * тур отдаёт главному экрану (переключить страницу / открыть добавление /
 * вернуться назад). MainScreen/MainPagerScreen слушают и выполняют.
 */
object OnboardingBus {
    var replayRequested by mutableStateOf(false)
        private set

    /** Тур активен — на это время блокируем свайпы пейджера. */
    var tourActive by mutableStateOf(false)

    /** Запрос «переключиться на страницу пейджера 0..3». */
    var pageRequested by mutableStateOf(-1)

    /** Запрос «открыть экран добавления лекарства». */
    var addRequested by mutableStateOf(false)

    /** Запрос «вернуться назад (закрыть экран добавления)». */
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

/* ────────────────────────────────────────────────────────────────
 * Coach-marks: реестр рамок реальных элементов UI
 * ──────────────────────────────────────────────────────────────── */

object CoachMarks {
    /** key → рамка элемента в координатах окна (обновляется onGloballyPositioned). */
    val rects = mutableStateMapOf<String, Rect>()
}

/**
 * Повесить на реальный элемент интерфейса, чтобы тур мог его подсветить:
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

/* ────────────────────────────────────────────────────────────────
 * Описание шагов тура
 * ──────────────────────────────────────────────────────────────── */

/** Что делает тап по подсвеченной зоне. */
private enum class TourTap { NEXT, OPEN_CALENDAR, OPEN_ADD }

private data class TourStep(
    val tag: String,          // ключ в CoachMarks
    val wantPage: Int? = null, // страницу пейджера выставить при входе в шаг
    val wantAddScreen: Boolean = false,
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val tap: TourTap = TourTap.NEXT,
    val isLast: Boolean = false,
    val animatedIcon: Boolean = false // иконка-«палец/свайп» живёт на месте
)

private val TOUR_ICONS = listOf(
    Icons.Filled.Today,
    Icons.Filled.SwipeLeft,
    Icons.Filled.CalendarMonth,
    Icons.AutoMirrored.Filled.List,
    Icons.Filled.Settings,
    Icons.Filled.AddCircle,
    Icons.Filled.TouchApp
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

/* ────────────────────────────────────────────────────────────────
 * Оверлей: приветствие → интерактивные шаги
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    val steps = remember { buildTourSteps() }
    var phase by remember { mutableStateOf(0) } // 0 = приветствие, 1 = тур
    var stepIndex by remember { mutableStateOf(0) }

    SideEffect {
        OnboardingBus.tourActive = phase == 1
    }
    if (phase == 0) {
        // Системный «назад» на приветствии = выйти из обучения
        BackHandler { onFinished() }
        WelcomeCard(
            onStart = {
                phase = 1
                stepIndex = 0
            },
            onSkip = onFinished
        )
    } else {
        CoachTour(
            steps = steps,
            stepIndex = stepIndex,
            onStepChange = { stepIndex = it },
            onFinish = onFinished
        )
    }
}

/* ── Приветствие ── */

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

    // Необязательное имя — оно попадёт в приветствие на главном экране
    var userName by remember { mutableStateOf(OnboardingPrefs.getUserName(context) ?: "") }

    // Живой фон: три медленно дрейфующих радиальных пятна
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
            // Почти непрозрачный фон: приложение за ним не просвечивает,
            // читается только карточка приветствия.
            .background(Color.Black.copy(alpha = 0.92f))
            .drawBehind {
                val t = phase * 2f * Math.PI.toFloat()
                fun blob(color: Color, cx: Float, cy: Float, radius: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = Offset(x = cx, y = cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(x = cx, y = cy)
                    )
                }
                blob(
                    color = roleColors[0].copy(alpha = 0.30f),
                    cx = size.width * (0.22f + 0.10f * kotlin.math.cos(t)),
                    cy = size.height * (0.16f + 0.08f * kotlin.math.sin(t)),
                    radius = size.width * 0.75f
                )
                blob(
                    color = roleColors[1].copy(alpha = 0.24f),
                    cx = size.width * (0.82f + 0.08f * kotlin.math.sin(t)),
                    cy = size.height * (0.30f + 0.10f * kotlin.math.cos(t)),
                    radius = size.width * 0.65f
                )
                blob(
                    color = roleColors[2].copy(alpha = 0.20f),
                    cx = size.width * (0.50f + 0.12f * kotlin.math.cos(t * 0.7f)),
                    cy = size.height * (0.92f + 0.06f * kotlin.math.sin(t * 0.7f)),
                    radius = size.width * 0.70f
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* поглощаем тапы по затемнению */ }
    ) {
        // Локальная копия ограничения по высоте — доступна во всех
        // вложенных лямбдах без танцев с неявными ресиверами.
        val screenMaxHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Верхняя строка: логотип + «Пропустить»
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

            // Карточка приветствия: на высоких экранах — как раньше,
            // на низких (ландшафт) — ограничена по высоте и скроллится изнутри
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
                // Дышащая иконка
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

                // Необязательное имя → персональное приветствие на главном
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

                // Кнопка «Начать тур»
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.45f))
        }
    }
}

/* ── Интерактивный тур ── */

@Composable
private fun CoachTour(
    steps: List<TourStep>,
    stepIndex: Int,
    onStepChange: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val step = steps[stepIndex]
    var nudge by remember { mutableStateOf(0) } // «пни» рамку, если тап мимо

    // Системный «назад»: по шагам, с последнего — выход.
    // ВАЖНО: если ТЕКУЩИЙ шаг живёт на экране добавления — сначала
    // закрываем его (requestBack), иначе тур вернётся на шаг «+»,
    // а экран добавления останется висеть поверх пейджера.
    BackHandler(enabled = true) {
        if (stepIndex > 0) {
            if (step.wantAddScreen) OnboardingBus.requestBack()
            onStepChange(stepIndex - 1)
        } else {
            onFinish()
        }
    }

    /* При входе в шаг — попросить главный экран поставить нужную страницу
       или открыть экран добавления. MainScreen слушает шину. */
    LaunchedEffect(stepIndex) {
        kotlinx.coroutines.delay(60) // кадр на композицию предыдущего шага
        if (step.wantAddScreen) {
            OnboardingBus.requestAdd()
        } else {
            step.wantPage?.let { OnboardingBus.requestPage(it) }
        }
    }

    /* Пульсирующая рамка подсветки */
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

    /* «Тапни сюда»: фаза расходящихся колец в центре дырки */
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

    val holeRect = CoachMarks.rects[step.tag]
    val density = LocalDensity.current
    val appear = remember(stepIndex) { Animatable(0f) }
    LaunchedEffect(stepIndex) {
        appear.animateTo(1f, tween(340, easing = EaseOutCubic))
    }

    // ГУСТОЙ scrim: приложение за оверлеем не просвечивает — «дырка»
    // подсветки остаётся единственным ярким пятном на экране.
    val scrimAlpha = (0.88f * appear.value)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(holeRect, stepIndex) {
                detectTapGestures { offset ->
                    if (holeRect != null && holeRect.inflate(12f).contains(offset)) {
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
                // Затемнение с «дыркой» над подсвеченным элементом
                val path = Path()
                path.fillType = PathFillType.EvenOdd
                path.addRect(Rect(0f, 0f, size.width, size.height))
                holeRect?.let { hole ->
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
        // Рамка вокруг дырки (двойная: тонкая яркая + широкая мягкая)
        holeRect?.let { hole ->
            val cornerRadius = 22.dp
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
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = appear.value
                        translationX = nudgeShake * 6f *
                            kotlin.math.sin(nudge * 12.9898f * 100f)
                    }
            ) {
                val insetHole = Rect(
                    left = hole.left - 8f,
                    top = hole.top - 8f,
                    right = hole.right + 8f,
                    bottom = hole.bottom + 8f
                )
                val rr = RoundRect(
                    insetHole,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
                // мягкое свечение
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f * pulseAlpha),
                    topLeft = Offset(insetHole.left, insetHole.top),
                    size = Size(insetHole.width, insetHole.height),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    style = Stroke(width = 10.dp.toPx())
                )
                // яркая рамка
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.95f * pulseAlpha),
                    topLeft = Offset(insetHole.left, insetHole.top),
                    size = Size(insetHole.width, insetHole.height),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // «Тапни сюда»: расходящееся кольцо + точка в центре дырки
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
        }

        // «Пропустить» — всегда сверху
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

        // ── Тултип шага: меряем РЕАЛЬНУЮ высоту карточки ──
        var tooltipH by remember { mutableStateOf(232.dp) }
        val arrowSize = 14.dp
        val gap = 10.dp

        // Куда ставить карточку: под дыркой (стрелка сверху смотрит на
        // кнопку), а если снизу не влезает — над дыркой (стрелка снизу).
        // Меряется реальная высота карточки — она зависит от текста шага,
        // поэтому константа вроде 230dp «на глаз» здесь не годится:
        // карточка может наехать на дырку или вылезти за экран.
        val tipTopTarget: Dp
        val belowHole: Boolean
        val arrowCx: Dp
        if (holeRect == null) {
            tipTopTarget = (maxHeight - tooltipH) / 2
            belowHole = true
            arrowCx = maxWidth / 2
        } else {
            with(density) {
                val holeBottom = holeRect.bottom.toDp()
                val holeTop = holeRect.top.toDp()
                val cx = holeRect.center.x.toDp()
                val fitsBelow =
                    holeBottom + tooltipH + gap + 28.dp < maxHeight - 24.dp
                if (fitsBelow) {
                    tipTopTarget = (holeBottom + gap)
                        .coerceAtMost(maxHeight - tooltipH - 24.dp)
                    belowHole = true
                } else {
                    tipTopTarget = (holeTop - tooltipH - gap)
                        .coerceAtLeast(100.dp)
                    belowHole = false
                }
                arrowCx = cx
            }
        }
        val topOffset by animateDpAsState(
            targetValue = tipTopTarget,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
            label = "tipTop"
        )
        val arrowCxSafe = arrowCx.coerceIn(36.dp, maxWidth - 36.dp)

        AnimatedContent(
            targetState = stepIndex,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = 0.dp, y = topOffset)
                .padding(horizontal = 20.dp),
            transitionSpec = {
                (
                    fadeIn(tween(280, easing = EaseOutCubic)) +
                        slideInVertically(tween(300, easing = EaseOutCubic)) { it / 4 }
                    ) togetherWith (
                    fadeOut(tween(150, easing = FastOutLinearInEasing)) +
                        slideOutVertically(tween(180)) { -it / 6 }
                    )
            },
            label = "coachTooltip"
        ) { index ->
            val s = steps[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = appear.value }
                    // Реальная высота карточки → позиция пересчитывается
                    .onGloballyPositioned { coords ->
                        with(density) { tooltipH = coords.size.height.toDp() }
                    }
                    // Стрелка-ромб к подсвеченной кнопке (рисуется ДО clip,
                    // поэтому не срезается скруглением карточки)
                    .drawBehind {
                        val a = arrowSize.toPx()
                        // координаты AnimatedContent сдвинуты на 20dp паддинг
                        val cx = (arrowCxSafe - 20.dp).toPx()
                            .coerceIn(a, size.width - a)
                        val tipColor = Color(0xFF1D242F)
                        val path = Path()
                        if (belowHole) {
                            // карточка ПОД дыркой: остриё вверх, из верхнего ребра
                            path.moveTo(cx, -a / 2f)
                            path.lineTo(cx + a / 2f, a / 2f)
                            path.lineTo(cx, a * 1.5f)
                            path.lineTo(cx - a / 2f, a / 2f)
                        } else {
                            // карточка НАД дыркой: остриё вниз, из нижнего ребра
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
                    // Иконка шага: «палец»/«свайп» слегка живые
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
                    // Счётчик шага
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

                // Точки прогресса тура
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

                // Кнопка шага
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

                // Подсказка «тапни по подсвеченному»
                Text(
                    text = stringResource(R.string.tour_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
