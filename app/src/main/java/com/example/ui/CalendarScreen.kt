package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

import com.example.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailySchedules by viewModel.dailySchedules.collectAsState()
    val logs by viewModel.todayIntakeLogs.collectAsState()
    
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.nav_calendar),
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
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = bottomPadding + 88.dp // Fully scroll behind bottom navigation dock
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Calendar Card - Material You glassmorphic round style
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Month Selector Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentMonth = currentMonth.minusMonths(1) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                            }
                            
                            val monthName = remember(currentMonth) {
                                currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }
                            Text(
                                text = "$monthName ${currentMonth.year}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            IconButton(
                                onClick = { currentMonth = currentMonth.plusMonths(1) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Localized dynamic days of week (Mon - Sun)
                        val daysOfWeek = remember {
                            listOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                                DayOfWeek.SATURDAY,
                                DayOfWeek.SUNDAY
                            ).map { d -> 
                                d.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(40.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Calendar Grid of Days
                        AnimatedContent(
                            targetState = currentMonth,
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))).togetherWith(
                                        slideOutHorizontally { width -> -width } + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                    )
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                    )
                                }.using(
                                    SizeTransform(clip = false)
                                )
                            },
                            label = "monthTransition"
                        ) { targetMonth ->
                            val firstDayOfWeek = remember(targetMonth) { targetMonth.atDay(1).dayOfWeek.value } // 1 (Mon) -> 7 (Sun)
                            val daysInMonth = remember(targetMonth) { targetMonth.lengthOfMonth() }
                            val firstWeekOffset = firstDayOfWeek - 1
                            
                            val gridDates = remember(targetMonth) {
                                val list = mutableListOf<LocalDate?>()
                                for (i in 0 until firstWeekOffset) {
                                    list.add(null)
                                }
                                for (day in 1..daysInMonth) {
                                    list.add(targetMonth.atDay(day))
                                }
                                while (list.size % 7 != 0) {
                                    list.add(null)
                                }
                                list
                            }

                            Column {
                                gridDates.chunked(7).forEach { weekDates ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        weekDates.forEach { dayDate ->
                                            if (dayDate == null) {
                                                Spacer(modifier = Modifier.width(40.dp).height(40.dp))
                                            } else {
                                                val isSelected = dayDate == selectedDate
                                                val isToday = dayDate == LocalDate.now()
                                                CalendarDayCell(
                                                    dayNumber = dayDate.dayOfMonth,
                                                    isSelected = isSelected,
                                                    isToday = isToday,
                                                    onClick = { viewModel.setSelectedDate(dayDate) }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            // Selected Day Label & Compliance Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedSelectedDate = remember(selectedDate) {
                        selectedDate.dayOfMonth.toString() + " " +
                                selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    }
                    Text(
                        text = formattedSelectedDate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            // List of medications and intake status for the selected day
            if (dailySchedules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.home_no_schedules),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(dailySchedules, key = { it.scheduleId }) { schedule ->
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

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        label = "cellBg"
    )
    
    val animatedText by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "cellText"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(animatedBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = animatedText
        )
    }
}
