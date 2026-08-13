package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyScheduleView
import com.example.data.IntakeLog
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

import com.example.ui.components.GlassCard
import com.example.ui.components.liquidGlass
import com.example.ui.components.GlassCircleIcon
import com.example.ui.components.GlassChip
import com.example.ui.components.tactilePress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val schedules by viewModel.dailySchedules.collectAsState()
    val logs by viewModel.todayIntakeLogs.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()

    val context = LocalContext.current
    val alarmScheduler = remember { com.example.alarms.AlarmScheduler(context.applicationContext) }
    var snoozeScheduleToPrompt by remember { mutableStateOf<DailyScheduleView?>(null) }

    if (snoozeScheduleToPrompt != null) {
        val sched = snoozeScheduleToPrompt!!
        AlertDialog(
            onDismissRequest = { snoozeScheduleToPrompt = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.snooze_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = sched.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                alarmScheduler.scheduleSnooze(sched.scheduleId, sched.name, 15)
                                snoozeScheduleToPrompt = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.snooze_15m))
                        }
                        OutlinedButton(
                            onClick = {
                                alarmScheduler.scheduleSnooze(sched.scheduleId, sched.name, 30)
                                snoozeScheduleToPrompt = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.snooze_30m))
                        }
                        OutlinedButton(
                            onClick = {
                                alarmScheduler.scheduleSnooze(sched.scheduleId, sched.name, 60)
                                snoozeScheduleToPrompt = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.snooze_60m))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { snoozeScheduleToPrompt = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.nav_today),
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
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            
            // Streak motivation banner
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                StreakBanner(streakDays = streakDays)
            }

            val lowStockMeds by viewModel.lowStockMedications.collectAsState()
            AnimatedVisibility(
                visible = lowStockMeds.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
            ) {
                LowStockBanner(
                    lowStockMeds = lowStockMeds,
                    onRefill = { medId -> viewModel.refillStock(medId, 30) }
                )
            }

            // Horizontal Date strip
            val dateStrip = remember(LocalDate.now()) {
                (-2..2).map { LocalDate.now().plusDays(it.toLong()) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dateStrip.forEach { d ->
                    DateItem(
                        date = d,
                        isSelected = d == selectedDate,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setSelectedDate(d) }
                    )
                }
            }

            val visibleSchedules = remember(schedules) {
                schedules.filter { it.scheduleType != "as_needed" }
            }

            val prnSchedules = remember(schedules) {
                schedules.filter { it.scheduleType == "as_needed" }
            }

            AnimatedContent(
                targetState = selectedDate,
                transitionSpec = {
                    val isAfter = targetState.isAfter(initialState)
                    val slideDir = if (isAfter) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    (slideIntoContainer(
                        towards = slideDir,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220)) + scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(220, easing = EaseOutCubic)
                    )).togetherWith(
                        slideOutOfContainer(
                            towards = slideDir,
                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                        ) + fadeOut(animationSpec = tween(160)) + scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                        )
                    )
                },
                contentAlignment = Alignment.TopCenter,
                label = "dayContentTransition",
                modifier = Modifier.fillMaxSize()
            ) { currDate ->
                if (visibleSchedules.isEmpty() && prnSchedules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding + 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = 28.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.home_no_schedules),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = bottomPadding + 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleSchedules, key = { it.scheduleId }) { schedule ->
                            val isTaken = remember(logs, schedule.scheduleId) { 
                                logs.any { it.scheduleId == schedule.scheduleId } 
                            }
                            Box(
                                modifier = Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            ) {
                                MedicationCard(
                                    schedule = schedule,
                                    isTaken = isTaken,
                                    onToggle = { taken -> viewModel.toggleLog(schedule, taken) },
                                    onSnooze = { snoozeScheduleToPrompt = schedule }
                                )
                            }
                        }

                        if (prnSchedules.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.adherence_as_needed_section),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(prnSchedules, key = { "prn_${it.scheduleId}" }) { schedule ->
                                val isTaken = remember(logs, schedule.scheduleId) { 
                                    logs.any { it.scheduleId == schedule.scheduleId } 
                                }
                                Box(
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                ) {
                                    MedicationCard(
                                        schedule = schedule,
                                        isTaken = isTaken,
                                        onToggle = { taken -> viewModel.toggleLog(schedule, taken) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateItem(
    date: LocalDate, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isToday = remember(date) { date == LocalDate.now() }
    
    val animatedContentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dateText"
    )

    val customGlassColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> null
    }

    // Fully localized short day initials (e.g. Пн, Вт, Ср, Mon, Tue etc.)
    val localizedDay = remember(date) {
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    val dateScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dateScale"
    )

    Box(
        modifier = modifier
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                customGlassColor = customGlassColor,
                elevation = if (isSelected) 10.dp else 4.dp
            )
            .tactilePress(pressScale = 0.90f, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = dateScale
                    scaleY = dateScale
                }
        ) {
            Text(
                text = localizedDay,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = animatedContentColor.copy(alpha = if (isSelected) 0.9f else 0.65f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = animatedContentColor
            )
        }
    }
}

@Composable
fun MedicationCard(
    schedule: DailyScheduleView,
    isTaken: Boolean,
    onToggle: (Boolean) -> Unit,
    onSnooze: () -> Unit = {}
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isTaken) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val checkButtonScale by animateFloatAsState(
        targetValue = if (isTaken) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkButtonScale"
    )

    val formattedTime = remember(schedule.timeHour, schedule.timeMinute, schedule.scheduleType) {
        if (schedule.scheduleType == "as_needed" || schedule.timeHour < 0) {
            null
        } else {
            String.format("%02d:%02d", schedule.timeHour, schedule.timeMinute)
        }
    }

    val medColor = remember(schedule.color, schedule.name) {
        com.example.ui.theme.MedicationColors.getColor(schedule.color, schedule.name)
    }

    val checkBgColor by animateColorAsState(
        targetValue = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 280, easing = EaseOutCubic),
        label = "checkBgColor"
    )

    val checkBorderColor by animateColorAsState(
        targetValue = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 280, easing = EaseOutCubic),
        label = "checkBorderColor"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        shape = RoundedCornerShape(24.dp),
        onClick = { onToggle(!isTaken) },
        glassAlpha = if (isTaken) 0.85f else 1.0f,
        elevation = if (isTaken) 6.dp else 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FormType icon inside circle badge
            com.example.ui.components.FormTypeIcon(
                formKey = schedule.formType,
                tint = medColor,
                backgroundColor = medColor.copy(alpha = 0.18f),
                size = 52.dp,
                iconSize = 24.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Glass Time Pill
                    if (formattedTime != null) {
                        GlassChip(
                            text = formattedTime,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        GlassChip(
                            text = stringResource(R.string.sched_as_needed),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    // Stock Tag if enabled
                    if (schedule.trackStock) {
                        val isLow = schedule.stockCount <= schedule.lowStockThreshold
                        GlassChip(
                            text = if (isLow) stringResource(R.string.stock_low_tag, schedule.stockCount) else stringResource(R.string.stock_pcs, schedule.stockCount),
                            containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status Chip
                    if (isTaken) {
                        GlassChip(
                            text = stringResource(R.string.status_taken),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = schedule.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // High visibility check circle button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(checkButtonScale)
                    .clip(CircleShape)
                    .background(checkBgColor)
                    .border(
                        width = if (isTaken) 0.dp else 2.dp,
                        color = checkBorderColor,
                        shape = CircleShape
                    )
                    .clickable { onToggle(!isTaken) },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isTaken,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(200)))
                            .togetherWith(scaleOut(targetScale = 0.4f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
                    },
                    label = "checkIconAnim"
                ) { taken ->
                    if (taken) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.status_taken),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.status_taken),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakBanner(streakDays: Int) {
    val daysSuffix = remember(streakDays) {
        val rem10 = streakDays % 10
        val rem100 = streakDays % 100
        when {
            rem100 in 11..19 -> R.string.streak_day_5
            rem10 == 1 -> R.string.streak_day_1
            rem10 in 2..4 -> R.string.streak_day_2_4
            else -> R.string.streak_day_5
        }
    }

    val titleText = if (streakDays > 0) {
        stringResource(R.string.streak_title, streakDays, stringResource(daysSuffix))
    } else {
        stringResource(R.string.streak_zero_title)
    }

    val subText = if (streakDays > 0) {
        stringResource(R.string.streak_sub_active)
    } else {
        stringResource(R.string.streak_sub_zero)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleIcon(
                size = 44.dp,
                tintColor = Color(0xFFFF9800)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LowStockBanner(
    lowStockMeds: List<com.example.data.Medication>,
    onRefill: (Int) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 8.dp,
        contentPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠️", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.stock_low_warning),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            lowStockMeds.forEach { med ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${med.name}: ${stringResource(R.string.stock_remaining, med.stockCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { onRefill(med.id) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.stock_refill_30),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

