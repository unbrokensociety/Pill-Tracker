package com.example.ui.components

/*
 * OnboardingTutorial — детальный тур по функциям приложения.
 *
 *  • Показывается один раз при ПЕРВОМ входе (флаг хранится в
 *    SharedPreferences — та же схема, что и язык в LocaleHelper,
 *    без зависимостей от DataStore/репозиториев).
 *  • Сверху всегда доступна кнопка «Пропустить».
 *  • 6 страниц: приветствие, «Сегодня», календарь, список лекарств,
 *    добавление лекарства, напоминания и настройки.
 *  • Кнопка в Настройках (внизу) запускает тур повторно через
 *    OnboardingBus — MainActivity слушает запрос и показывает оверлей.
 *
 * Визуал — в стилистике приложения: стеклянная карточка, мягкие
 * пружины, тактильные нажатия, живой градиентный фон.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.cos
import kotlin.math.sin

/* ────────────────────────────────────────────────────────────────
 * Персистентность + сигнал повторного запуска
 * ──────────────────────────────────────────────────────────────── */

object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"

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
}

/**
 * Шина «показать обучение снова»: SettingsScreen вызывает requestReplay(),
 * MainActivity слушает replayRequested через snapshotFlow и открывает тур.
 */
object OnboardingBus {
    var replayRequested by mutableStateOf(false)
        private set

    fun requestReplay(context: android.content.Context) {
        OnboardingPrefs.reset(context)
        replayRequested = true
    }

    fun consume() {
        replayRequested = false
    }
}

/* ────────────────────────────────────────────────────────────────
 * Данные страниц
 * ──────────────────────────────────────────────────────────────── */

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val bulletsRes: List<Int>,
    // Пара цветов градиента по ролям: 0=primary, 1=tertiary, 2=secondary
    val gradientRoles: Pair<Int, Int>
)

private fun buildOnboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        icon = Icons.Filled.Medication,
        titleRes = R.string.ob_welcome_title,
        descRes = R.string.ob_welcome_desc,
        bulletsRes = listOf(
            R.string.ob_welcome_b1,
            R.string.ob_welcome_b2,
            R.string.ob_welcome_b3
        ),
        gradientRoles = 0 to 1
    ),
    OnboardingPage(
        icon = Icons.Filled.Today,
        titleRes = R.string.ob_home_title,
        descRes = R.string.ob_home_desc,
        bulletsRes = listOf(
            R.string.ob_home_b1,
            R.string.ob_home_b2,
            R.string.ob_home_b3
        ),
        gradientRoles = 1 to 2
    ),
    OnboardingPage(
        icon = Icons.Filled.CalendarMonth,
        titleRes = R.string.ob_calendar_title,
        descRes = R.string.ob_calendar_desc,
        bulletsRes = listOf(
            R.string.ob_calendar_b1,
            R.string.ob_calendar_b2,
            R.string.ob_calendar_b3
        ),
        gradientRoles = 2 to 0
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.List,
        titleRes = R.string.ob_meds_title,
        descRes = R.string.ob_meds_desc,
        bulletsRes = listOf(
            R.string.ob_meds_b1,
            R.string.ob_meds_b2,
            R.string.ob_meds_b3
        ),
        gradientRoles = 0 to 2
    ),
    OnboardingPage(
        icon = Icons.Filled.AddCircle,
        titleRes = R.string.ob_add_title,
        descRes = R.string.ob_add_desc,
        bulletsRes = listOf(
            R.string.ob_add_b1,
            R.string.ob_add_b2,
            R.string.ob_add_b3
        ),
        gradientRoles = 1 to 0
    ),
    OnboardingPage(
        icon = Icons.Filled.NotificationsActive,
        titleRes = R.string.ob_reminders_title,
        descRes = R.string.ob_reminders_desc,
        bulletsRes = listOf(
            R.string.ob_reminders_b1,
            R.string.ob_reminders_b2,
            R.string.ob_reminders_b3
        ),
        gradientRoles = 2 to 1
    )
)

/* ────────────────────────────────────────────────────────────────
 * Оверлей обучения
 * ──────────────────────────────────────────────────────────────── */

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    val pages = remember { buildOnboardingPages() }
    val pageCount = pages.size
    var current by remember { mutableStateOf(0) }

    // Системная кнопка «назад»: по страницам, с последней — выход
    BackHandler(enabled = true) {
        if (current > 0) current-- else onFinished()
    }

    // Появление оверлея: мягкий fade + scale
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(360, easing = EaseOutCubic))
    }

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

    // Перехват касаний, чтобы клики не проваливались под оверлей
    val scrimInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entrance.value
                scaleX = 0.94f + 0.06f * entrance.value
                scaleY = 0.94f + 0.06f * entrance.value
            }
            .background(Color.Black.copy(alpha = 0.58f))
            .drawBehind {
                val t = phase * 2f * Math.PI.toFloat()
                fun blob(
                    color: Color,
                    cx: Float,
                    cy: Float,
                    radius: Float
                ) {
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
                    cx = size.width * (0.22f + 0.10f * cos(t)),
                    cy = size.height * (0.16f + 0.08f * sin(t)),
                    radius = size.width * 0.75f
                )
                blob(
                    color = roleColors[1].copy(alpha = 0.24f),
                    cx = size.width * (0.82f + 0.08f * sin(t)),
                    cy = size.height * (0.30f + 0.10f * cos(t)),
                    radius = size.width * 0.65f
                )
                blob(
                    color = roleColors[2].copy(alpha = 0.20f),
                    cx = size.width * (0.50f + 0.12f * cos(t * 0.7f)),
                    cy = size.height * (0.92f + 0.06f * sin(t * 0.7f)),
                    radius = size.width * 0.70f
                )
            }
            .clickable(
                interactionSource = scrimInteraction,
                indication = null
            ) { /* поглощаем тапы по затемнению */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Верхняя строка: логотип + «Пропустить» ──
            OnboardingTopRow(onSkip = onFinished)

            Spacer(modifier = Modifier.weight(0.55f))

            // ── Карточка текущей страницы ──
            AnimatedContent(
                targetState = current,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (
                        slideInHorizontally(animationSpec = tween(320, easing = EaseOutCubic)) { full ->
                            direction * full / 3
                        } + fadeIn(animationSpec = tween(260, easing = EaseOutCubic)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(260, easing = EaseOutCubic))
                        ) togetherWith (
                        slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { full ->
                            -direction * full / 3
                        } + fadeOut(animationSpec = tween(180)) +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(200))
                        )
                },
                label = "onboardingPage"
            ) { pageIndex ->
                OnboardingPageCard(
                    page = pages[pageIndex],
                    roleColors = roleColors
                )
            }

            Spacer(modifier = Modifier.weight(0.45f))

            // ── Индикатор шага ──
            Text(
                text = stringResource(R.string.ob_step_of, current + 1, pageCount),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Точки прогресса ──
            OnboardingDots(
                pageCount = pageCount,
                current = current,
                onPageSelected = { current = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Кнопки навигации ──
            OnboardingButtonsRow(
                isFirst = current == 0,
                isLast = current == pageCount - 1,
                onBack = { if (current > 0) current-- },
                onNext = {
                    if (current < pageCount - 1) current++ else onFinished()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/* ── Верхняя строка: логотип слева, «Пропустить» справа ── */

@Composable
private fun OnboardingTopRow(onSkip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Мини-логотип
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

        // Кнопка «Пропустить» — всегда сверху
        val skipInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.13f))
                .clickable(
                    interactionSource = skipInteraction,
                    indication = null
                ) { onSkip() }
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                text = stringResource(R.string.ob_skip),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
    }
}

/* ── Карточка страницы: иконка в градиенте, заголовок, описание, буллеты ── */

@Composable
private fun OnboardingPageCard(
    page: OnboardingPage,
    roleColors: List<Color>
) {
    val startColor = roleColors[page.gradientRoles.first]
    val endColor = roleColors[page.gradientRoles.second]

    // Лёгкое «дыхание» иконки
    val bobTransition = rememberInfiniteTransition(label = "onboardingBob")
    val bob by bobTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingBobValue"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 26.dp,
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Композиция иконки: свечение + градиентный скруглённый квадрат
            // + две декоративные «пилюли» по углам
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .drawBehind {
                            val glowRadius = size.width * 0.72f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        startColor.copy(alpha = 0.38f),
                                        Color.Transparent
                                    ),
                                    center = Offset(x = size.width / 2f, y = size.height / 2f),
                                    radius = glowRadius
                                ),
                                radius = glowRadius,
                                center = Offset(x = size.width / 2f, y = size.height / 2f)
                            )
                        }
                        .graphicsLayer {
                            translationY = bob * 5.dp.toPx()
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(listOf(startColor, endColor))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = stringResource(page.titleRes),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                }

                // Декоративные «пилюли»
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer { translationY = bob * 3.dp.toPx() }
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(endColor.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .graphicsLayer { translationY = bob * -4.dp.toPx() }
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(startColor.copy(alpha = 0.45f))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(page.descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Буллеты: галочка в кружке + текст
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                page.bulletsRes.forEach { bulletRes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(startColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = startColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = stringResource(bulletRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/* ── Точки прогресса ── */

@Composable
private fun OnboardingDots(
    pageCount: Int,
    current: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 24.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dotWidth$index"
            )
            val alpha by animateFloatAsState(
                targetValue = if (active) 1f else 0.32f,
                animationSpec = tween(220),
                label = "dotAlpha$index"
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = alpha))
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onPageSelected(index) }
            )
        }
    }
}

/* ── Ряд кнопок: Назад / Далее · Начать ── */

@Composable
private fun OnboardingButtonsRow(
    isFirst: Boolean,
    isLast: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isFirst) {
            val backInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null
                    ) { onBack() }
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Text(
                    text = stringResource(R.string.ob_back),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Главная кнопка: градиент + тактильная пружина
        Box(
            modifier = Modifier
                .weight(1f)
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
                .tactilePress(pressScale = 0.95f, haptic = true, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(
                    if (isLast) R.string.ob_start else R.string.ob_next
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
