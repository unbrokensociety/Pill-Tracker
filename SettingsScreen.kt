package com.aistudio.meditracker.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aistudio.meditracker.R
import com.aistudio.meditracker.data.ThemeMode
import com.aistudio.meditracker.ui.components.GlassCard
import com.aistudio.meditracker.ui.components.OnboardingBus
import com.aistudio.meditracker.ui.components.OnboardingPrefs
import com.aistudio.meditracker.ui.components.PrivacyPolicyDialog
import com.aistudio.meditracker.ui.components.TermsOfServiceDialog
import com.aistudio.meditracker.ui.components.UpdateBus
import com.aistudio.meditracker.ui.components.coachTag
import com.aistudio.meditracker.ui.locale.LocaleHelper
import com.aistudio.meditracker.ui.locale.findActivity

/**
 * Компактный экран настроек.
 *
 * Структура — 5 карточек вместо 8:
 *  1) Прогрес прийому — одна строка с кольцом
 *  2) Сповіщення — 2 тумблера (островок и критический звук всегда включены)
 *  3) Вигляд — тема, рідке скло (тумблер) та мова
 *  4) Основне — ім'я, повтор навчання, перевірка оновлень (група рядків)
 *  5) Про застосунок — версія, опис, юридичні документи
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val alarmMode by viewModel.alarmModeEnabled.collectAsState()
    val glassEnabled by viewModel.liquidGlassEnabled.collectAsState()

    val medications by viewModel.allMedications.collectAsState()
    val schedules by viewModel.dailySchedules.collectAsState()
    val logs by viewModel.todayIntakeLogs.collectAsState()

    val context = LocalContext.current

    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }
    var fullScreenIntentGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                (notificationManager?.canUseFullScreenIntent() ?: true)
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    fullScreenIntentGranted = notificationManager?.canUseFullScreenIntent() ?: true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsOfService by remember { mutableStateOf(false) }

    var userName by remember { mutableStateOf(OnboardingPrefs.getUserName(context)) }
    var showNameDialog by remember { mutableStateOf(false) }

    if (showNameDialog) {
        NameEditDialog(
            initial = userName,
            onSave = { name ->
                userName = name
                OnboardingPrefs.setUserName(context, name)
            },
            onDismiss = { showNameDialog = false }
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }

    if (showTermsOfService) {
        TermsOfServiceDialog(onDismiss = { showTermsOfService = false })
    }

    // Today's compliance ratio for the compact progress ring
    val totalSchedules = schedules.size
    val takenSchedules = schedules.count { s -> logs.any { it.scheduleId == s.scheduleId } }
    val ratio = if (totalSchedules > 0) takenSchedules.toFloat() / totalSchedules else 0f
    val ratioPercent = (ratio * 100).toInt()

    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "statsProgressAnim"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .coachTag("settings_content"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = bottomPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─────────────────────────────────────────────────────────
            // 1. Прогрес прийому — компактная одна строка
            // ─────────────────────────────────────────────────────────
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(58.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val ringColor = MaterialTheme.colorScheme.primary
                            val ringTrack = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            val ringAccent = MaterialTheme.colorScheme.tertiary
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 6.dp.toPx()
                                val inset = stroke / 2f + 2.dp.toPx()
                                val arcSize = Size(
                                    size.width - inset * 2f,
                                    size.height - inset * 2f
                                )
                                val tl = Offset(inset, inset)
                                drawArc(
                                    color = ringTrack,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = tl,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(ringColor, ringAccent, ringColor)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedRatio,
                                    useCenter = false,
                                    topLeft = tl,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$ratioPercent%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_stats_today_ratio),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$takenSchedules / $totalSchedules",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_stats_active_meds, medications.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────
            // 2. Сповіщення — 2 тумблери в одній картці.
            // Островок и критический звук всегда включены — с v2.5.3
            // отдельные тумблеры больше не нужны.
            // ─────────────────────────────────────────────────────────
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Filled.Notifications,
                            title = stringResource(R.string.settings_notifications),
                            subtitle = stringResource(
                                if (notificationsEnabled) R.string.settings_notif_active
                                else R.string.settings_notif_disabled
                            ),
                            checked = notificationsEnabled,
                            onToggle = { viewModel.setNotifications(!notificationsEnabled) }
                        )

                        RowDivider()

                        val alarmSubtitle = if (alarmMode && !fullScreenIntentGranted) {
                            stringResource(R.string.settings_alarm_blocked)
                        } else {
                            stringResource(R.string.settings_alarm_mode_desc)
                        }
                        SettingsToggleRow(
                            icon = Icons.Filled.Alarm,
                            title = stringResource(R.string.settings_alarm_mode),
                            subtitle = alarmSubtitle,
                            checked = alarmMode,
                            onToggle = {
                                when {
                                    !alarmMode -> {
                                        viewModel.setAlarmMode(true)
                                        if (!fullScreenIntentGranted) {
                                            openFullScreenIntentSettings(context)
                                        }
                                    }
                                    fullScreenIntentGranted -> viewModel.setAlarmMode(false)
                                    else -> openFullScreenIntentSettings(context)
                                }
                            }
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────
            // 3. Вигляд — тема + рідке скло + мова
            // ─────────────────────────────────────────────────────────
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // ── Тема ──
                        SectionHeader(
                            icon = Icons.Filled.Palette,
                            title = stringResource(R.string.settings_theme)
                        )
                        val themes = listOf(
                            Triple(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_system), Icons.Filled.BrightnessAuto),
                            Triple(ThemeMode.LIGHT, stringResource(R.string.settings_theme_light), Icons.Filled.LightMode),
                            Triple(ThemeMode.DARK, stringResource(R.string.settings_theme_dark), Icons.Filled.DarkMode),
                            Triple(ThemeMode.BRAND, stringResource(R.string.settings_theme_brand), Icons.Filled.BlurOn)
                        )
                        ChipGroup(
                            items = themes,
                            label = { it.second },
                            icon = { it.third },
                            isSelected = { it.first == themeMode },
                            onSelect = { viewModel.setTheme(it.first) }
                        )

                        RowDivider(inset = 0.dp)

                        // ── Рідке скло (iOS-стиль) ──
                        SettingsToggleRow(
                            icon = Icons.Filled.AutoAwesome,
                            title = stringResource(R.string.settings_glass_title),
                            subtitle = stringResource(R.string.settings_glass_desc),
                            checked = glassEnabled,
                            onToggle = { viewModel.setLiquidGlass(!glassEnabled) }
                        )

                        RowDivider(inset = 0.dp)

                        // ── Мова ──
                        SectionHeader(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.settings_language)
                        )
                        val languages = listOf(
                            "system" to stringResource(R.string.settings_lang_system),
                            "uk" to stringResource(R.string.settings_lang_uk),
                            "en" to stringResource(R.string.settings_lang_en),
                            "ru" to stringResource(R.string.settings_lang_ru)
                        )
                        val currentLanguage = LocaleHelper.getLanguage(context)
                        ChipGroup(
                            items = languages,
                            label = { it.second },
                            icon = { null },
                            isSelected = { it.first == currentLanguage },
                            onSelect = { (code, _) ->
                                if (code != currentLanguage) {
                                    LocaleHelper.setLanguage(context, code)
                                    context.findActivity()?.recreate()
                                }
                            }
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────
            // 4. Основне — ім'я / навчання / оновлення (група рядків)
            // ─────────────────────────────────────────────────────────
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 6.dp) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Filled.Person,
                            title = stringResource(R.string.settings_name_title),
                            subtitle = userName
                                ?: stringResource(R.string.settings_name_empty),
                            onClick = { showNameDialog = true }
                        )

                        RowDivider()

                        SettingsActionRow(
                            icon = Icons.Filled.School,
                            title = stringResource(R.string.ob_settings_replay_title),
                            subtitle = stringResource(R.string.ob_settings_replay_desc),
                            onClick = { OnboardingBus.requestReplay(context) }
                        )

                        RowDivider()

                        SettingsActionRow(
                            icon = Icons.Filled.CloudDownload,
                            title = stringResource(R.string.upd_check_title),
                            subtitle = stringResource(R.string.upd_check_desc),
                            onClick = { UpdateBus.requestCheck() }
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────
            // 5. Про застосунок — версія + юридичні документи
            // ─────────────────────────────────────────────────────────
            item {
                val appVersionName = remember(context) {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        "v${pInfo.versionName}"
                    } catch (e: Exception) {
                        "v2.5.4"
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                icon = Icons.Filled.Info,
                                title = stringResource(R.string.settings_about)
                            )
                            Text(
                                text = appVersionName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = stringResource(R.string.settings_about_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        RowDivider()

                        SettingsActionRow(
                            icon = Icons.Filled.Info,
                            title = stringResource(R.string.privacy_policy_title),
                            subtitle = stringResource(R.string.privacy_policy_desc),
                            onClick = { showPrivacyPolicy = true }
                        )

                        SettingsActionRow(
                            icon = Icons.Filled.Info,
                            title = stringResource(R.string.terms_of_service_title),
                            subtitle = stringResource(R.string.terms_of_service_desc),
                            onClick = { showTermsOfService = true }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Переиспользуемые компактные компоненты
// ═══════════════════════════════════════════════════════════════

/** Маленький заголовок секции: 30dp бейдж + полужирный заголовок. */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Ряд-действие: бейдж + заголовок + подзаголовок + шеврон вправо. */
@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Компактный ряд с переключателем. */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

/** Внутрикарточный разделитель с отступом под иконку ряда. */
@Composable
private fun RowDivider(inset: androidx.compose.ui.unit.Dp = 46.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = inset),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

/** Один чип выбора: скруглённая пилюля с пружинной анимацией. */
@Composable
private fun SelectChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipScale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selectChipScale"
    )
    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Группа чипов на FlowRow: длинные подписи переносятся на новую строку
 * целыми чипами — раскладка безопасна в любой локали и при любом
 * масштабе системного шрифта.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(
    items: List<T>,
    label: (T) -> String,
    icon: ((T) -> ImageVector?)?,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            SelectChip(
                label = label(item),
                icon = icon?.invoke(item),
                selected = isSelected(item),
                onClick = { onSelect(item) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Диалоги и системные переходы
// ═══════════════════════════════════════════════════════════════

@Composable
private fun NameEditDialog(
    initial: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.name_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= 24) value = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.name_dialog_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.name_dialog_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = value.trim()
                    onSave(trimmed.ifEmpty { null })
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.name_dialog_save))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!initial.isNullOrEmpty()) {
                    TextButton(
                        onClick = {
                            onSave(null)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.name_dialog_clear),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.name_dialog_cancel))
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Opens the screen where the user allows the full-screen alarm presentation.
private fun openFullScreenIntentSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse("package:" + context.packageName)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
