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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val schedules by viewModel.dailySchedules.collectAsState()
    val logs by viewModel.todayIntakeLogs.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()

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
            StreakBanner(streakDays = streakDays)

            // Horizontal Date strip
            val dateStrip = remember {
                (-2..2).map { LocalDate.now().plusDays(it.toLong()) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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

            AnimatedContent(
                targetState = selectedDate,
                transitionSpec = {
                    val isAfter = targetState.isAfter(initialState)
                    if (isAfter) {
                        (slideInHorizontally { width -> width / 5 } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))).togetherWith(
                            slideOutHorizontally { width -> -width / 5 } + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                        )
                    } else {
                        (slideInHorizontally { width -> -width / 5 } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))).togetherWith(
                            slideOutHorizontally { width -> width / 5 } + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                        )
                    }.using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                label = "dayContentTransition",
                modifier = Modifier.fillMaxSize()
            ) { currDate ->
                if (schedules.isEmpty()) {
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
                            top = 16.dp,
                            bottom = bottomPadding + 88.dp // Ensures content scrolls fully above bottom navigation
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(schedules, key = { it.scheduleId }) { schedule ->
                            val isTaken = remember(logs, schedule.scheduleId) { 
                                logs.any { it.scheduleId == schedule.scheduleId } 
                            }
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

@Composable
fun DateItem(
    date: LocalDate, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isToday = remember(date) { date == LocalDate.now() }
    
    val animatedBg by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        label = "dateBg"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dateText"
    )

    val borderStroke = when {
        isSelected -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        isToday -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        else -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }

    // Fully localized short day initials (e.g. Пн, Вт, Ср, Mon, Tue etc.)
    val localizedDay = remember(date) {
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        border = borderStroke,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = localizedDay,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = animatedContentColor.copy(alpha = if (isSelected) 0.8f else 0.6f)
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
    onToggle: (Boolean) -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isTaken) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        label = "cardColor"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isTaken) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        },
        label = "borderColor"
    )

    val animatedIconColor by animateColorAsState(
        targetValue = if (isTaken) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "iconColor"
    )

    val animatedIconBg by animateColorAsState(
        targetValue = if (isTaken) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        },
        label = "iconBg"
    )

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

    val formattedTime = remember(schedule.timeHour, schedule.timeMinute) {
        String.format("%02d:%02d", schedule.timeHour, schedule.timeMinute)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onToggle(!isTaken) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = animatedColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, animatedBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High fidelity beautifully color-coded pill capsule
            val medColor = remember(schedule.color, schedule.name) {
                val predefinedColors = listOf(
                    Color(0xFFE57373), // Coral Red
                    Color(0xFF42A5F5), // Sky Blue
                    Color(0xFF66BB6A), // Fresh Green
                    Color(0xFFFFA726), // Sunset Orange
                    Color(0xFFAB47BC), // Rich Purple
                    Color(0xFF26A69A), // Teal Green
                    Color(0xFFEC407A), // Rose Pink
                    Color(0xFFFFCA28)  // Sunflower Yellow
                )
                val cVal = schedule.color
                if (cVal >= 0 && cVal < predefinedColors.size) {
                    predefinedColors[cVal]
                } else {
                    val idx = Math.abs(schedule.name.hashCode()) % predefinedColors.size
                    predefinedColors[idx]
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(medColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 12.dp)
                        .scale(1.1f)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(medColor)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Time Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isTaken) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
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
            
            // Check Circle Toggle representation
            IconButton(
                onClick = { onToggle(!isTaken) },
                modifier = Modifier
                    .scale(checkButtonScale)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(animatedIconBg)
                    .border(
                        width = if (isTaken) 0.dp else 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            ) {
                if (isTaken) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.status_taken),
                        tint = animatedIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

@Composable
fun StreakBanner(streakDays: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔥",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (streakDays > 0) "Серия приёма: $streakDays ${getDaysSuffix(streakDays)} 🔥" else "Серия приёма: 0 дней",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (streakDays > 0) "Отличный результат! Не пропускайте приём" else "Отметьте приём сегодня, чтобы запустить серию",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getDaysSuffix(days: Int): String {
    val remainder10 = days % 10
    val remainder100 = days % 100
    return when {
        remainder100 in 11..19 -> "дней"
        remainder10 == 1 -> "день"
        remainder10 in 2..4 -> "дня"
        else -> "дней"
    }
}
