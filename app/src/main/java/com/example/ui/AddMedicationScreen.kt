package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.locale.findActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedColorIdx by remember { mutableStateOf(0) }
    var selectedStartDate by remember { mutableStateOf(LocalDate.now()) }
    val times = remember { mutableStateListOf<LocalTime>(LocalTime.of(8, 0)) }
    val context = LocalContext.current
    
    // Check if form is valid
    val isFormValid = name.isNotBlank() && dosage.isNotBlank() && times.isNotEmpty()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.add_med_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isFormValid) {
                        val startOfDayMillis = selectedStartDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                        val newMed = com.example.data.Medication(
                            name = name,
                            dosage = dosage,
                            color = selectedColorIdx,
                            timesPerDay = times.size,
                            startDate = startOfDayMillis
                        )
                        viewModel.addMedication(newMed, times.map { it.hour to it.minute })
                        onNavigateBack()
                    }
                },
                containerColor = if (isFormValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp),
                icon = { Icon(Icons.Filled.Check, stringResource(R.string.action_save)) },
                text = { Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Glass Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_med_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.add_med_name_label)) },
                            placeholder = { Text(stringResource(R.string.add_med_name_placeholder)) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text(stringResource(R.string.add_med_dosage_label)) },
                            placeholder = { Text(stringResource(R.string.add_med_dosage_placeholder)) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Start Date Picker Glass Card (Aligned calendar & text)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val activityContext = context.findActivity() ?: context
                        val datePicker = DatePickerDialog(
                            activityContext,
                            { _, y, m, d ->
                                selectedStartDate = LocalDate.of(y, m + 1, d)
                            },
                            selectedStartDate.year,
                            selectedStartDate.monthValue - 1,
                            selectedStartDate.dayOfMonth
                        )
                        datePicker.show()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.add_med_start_date_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedStartDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Color Selector Glass Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_med_color_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            predefinedColors.forEachIndexed { index, color ->
                                val isSelected = selectedColorIdx == index
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.25f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "colorCircleScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (index == 7) Color.DarkGray else Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorIdx = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = if (index == 7) Color.DarkGray else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Intake Times Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_med_time_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    TextButton(
                        onClick = { 
                            val nextTime = if (times.isNotEmpty()) {
                                val last = times.last()
                                last.plusHours(4)
                            } else {
                                LocalTime.of(8, 0)
                            }
                            times.add(nextTime)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_med_add_time), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // List of pill shaped times with interactive TimePickerDialogs & delete button
            if (times.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.add_med_empty_time_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                itemsIndexed(times) { index, time ->
                    Box(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Time Slot Glass Card
                            val formattedTime = String.format("%02d:%02d", time.hour, time.minute)
                            
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val activityContext = context.findActivity() ?: context
                                    val timePickerDialog = TimePickerDialog(
                                        activityContext,
                                        { _, selectedHour, selectedMinute ->
                                            times[index] = LocalTime.of(selectedHour, selectedMinute)
                                        },
                                        time.hour,
                                        time.minute,
                                        true
                                    )
                                    timePickerDialog.show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.add_med_intake_number, index + 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formattedTime,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Close/Delete button
                            IconButton(
                                onClick = { times.removeAt(index) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Delete time slot",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
