package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.NotificationsActive
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

import androidx.compose.material.icons.filled.Remove
import java.time.Instant

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.FormType
import com.example.ui.components.FormTypeIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddMedicationScreen(
    editingMedicationId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedColorIdx by remember { mutableStateOf(0) }
    var selectedStartDate by remember { mutableStateOf(LocalDate.now()) }
    var formTypeKey by remember { mutableStateOf("capsule") }
    var scheduleTypeKey by remember { mutableStateOf("daily") } // daily, interval, as_needed
    var intervalDaysVal by remember { mutableStateOf(2) }
    var trackStockEnabled by remember { mutableStateOf(true) }
    var stockCountInput by remember { mutableStateOf("30") }
    var lowStockThresholdInput by remember { mutableStateOf("5") }

    val times = remember { mutableStateListOf<LocalTime>(LocalTime.of(8, 0)) }
    val context = LocalContext.current

    LaunchedEffect(editingMedicationId) {
        if (editingMedicationId != null) {
            val med = viewModel.getMedicationById(editingMedicationId)
            if (med != null) {
                name = med.name
                dosage = med.dosage
                notes = med.notes
                selectedColorIdx = med.color
                formTypeKey = med.formType
                scheduleTypeKey = med.scheduleType
                intervalDaysVal = med.intervalDays
                trackStockEnabled = med.trackStock
                stockCountInput = med.stockCount.toString()
                lowStockThresholdInput = med.lowStockThreshold.toString()

                selectedStartDate = Instant.ofEpochMilli(med.startDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val scheds = viewModel.getSchedulesForMedication(editingMedicationId)
                times.clear()
                scheds.forEach { s ->
                    times.add(LocalTime.of(s.timeHour, s.timeMinute))
                }
            }
        }
    }
    
    val isDarkTheme = isSystemInDarkTheme()
    
    // Check if form is valid
    val isFormValid = name.isNotBlank() && dosage.isNotBlank() && (scheduleTypeKey == "as_needed" || times.isNotEmpty())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(if (editingMedicationId != null) R.string.edit_med_title else R.string.add_med_title),
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
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                        onClick = {
                            if (isFormValid) {
                                val startOfDayMillis = selectedStartDate
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()

                                val medToSave = com.example.data.Medication(
                                    id = editingMedicationId ?: 0,
                                    name = name,
                                    dosage = dosage,
                                    notes = notes,
                                    color = selectedColorIdx,
                                    timesPerDay = if (scheduleTypeKey == "as_needed") 0 else times.size,
                                    startDate = startOfDayMillis,
                                    stockCount = stockCountInput.toIntOrNull() ?: 30,
                                    lowStockThreshold = lowStockThresholdInput.toIntOrNull() ?: 5,
                                    formType = formTypeKey,
                                    scheduleType = scheduleTypeKey,
                                    intervalDays = intervalDaysVal,
                                    trackStock = trackStockEnabled
                                )

                                val timesToSave = if (scheduleTypeKey == "as_needed") emptyList() else times.map { it.hour to it.minute }

                                if (editingMedicationId != null) {
                                    viewModel.updateMedication(medToSave, timesToSave)
                                } else {
                                    viewModel.addMedication(medToSave, timesToSave)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
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
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.add_med_notes_label)) },
                            placeholder = { Text(stringResource(R.string.add_med_notes_placeholder)) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            singleLine = true
                        )

                        // Quick Food Presets
                        val foodPresets = listOf(
                            stringResource(R.string.food_before_meal),
                            stringResource(R.string.food_with_meal),
                            stringResource(R.string.food_after_meal),
                            stringResource(R.string.food_before_bed)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            foodPresets.forEach { preset ->
                                val isSelected = notes.contains(preset)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        notes = if (notes.isBlank()) {
                                            preset
                                        } else if (notes.contains(preset)) {
                                            notes.replace(preset, "").replace(", ,", ",").trim(',', ' ')
                                        } else {
                                            "$notes, $preset"
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = preset,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Form Type Selector Glass Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_med_form_type_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(FormType.values()) { formType ->
                                val isSelected = formTypeKey.equals(formType.key, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { formTypeKey = formType.key },
                                    label = {
                                        Text(
                                            text = stringResource(formType.stringRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        FormTypeIcon(
                                            formKey = formType.key,
                                            size = 26.dp,
                                            iconSize = 14.dp,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Schedule Frequency Selector Glass Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_med_schedule_type_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val chipColors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                            val chipBorderDaily = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = scheduleTypeKey == "daily"
                            )
                            val chipBorderInterval = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = scheduleTypeKey == "interval"
                            )
                            val chipBorderAsNeeded = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = scheduleTypeKey == "as_needed"
                            )

                            FilterChip(
                                selected = scheduleTypeKey == "daily",
                                onClick = { scheduleTypeKey = "daily" },
                                label = {
                                    Text(
                                        text = stringResource(R.string.sched_daily),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = chipColors,
                                border = chipBorderDaily,
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = scheduleTypeKey == "interval",
                                onClick = { scheduleTypeKey = "interval" },
                                label = {
                                    Text(
                                        text = stringResource(R.string.sched_interval, intervalDaysVal),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = chipColors,
                                border = chipBorderInterval,
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = scheduleTypeKey == "as_needed",
                                onClick = { scheduleTypeKey = "as_needed" },
                                label = {
                                    Text(
                                        text = stringResource(R.string.sched_as_needed),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = chipColors,
                                border = chipBorderAsNeeded,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (scheduleTypeKey == "interval") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.sched_interval_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (intervalDaysVal > 2) intervalDaysVal-- },
                                        enabled = intervalDaysVal > 2
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Decrease interval",
                                            tint = if (intervalDaysVal > 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    Text(
                                        text = "$intervalDaysVal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { if (intervalDaysVal < 30) intervalDaysVal++ },
                                        enabled = intervalDaysVal < 30
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Increase interval",
                                            tint = if (intervalDaysVal < 30) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stock Tracker Glass Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.stock_track_label),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = trackStockEnabled,
                                onCheckedChange = { trackStockEnabled = it }
                            )
                        }

                        if (trackStockEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = stockCountInput,
                                    onValueChange = { stockCountInput = it.filter { char -> char.isDigit() } },
                                    label = { Text(stringResource(R.string.stock_total_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                OutlinedTextField(
                                    value = lowStockThresholdInput,
                                    onValueChange = { lowStockThresholdInput = it.filter { char -> char.isDigit() } },
                                    label = { Text(stringResource(R.string.stock_low_warning)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Start Date Picker Glass Card (Aligned calendar & text)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val activityContext = context.findActivity() ?: context
                        val dialogTheme = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog else android.R.style.Theme_DeviceDefault_Light_Dialog
                        val datePicker = DatePickerDialog(
                            activityContext,
                            dialogTheme,
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

                        val predefinedColors = com.example.ui.theme.MedicationColors.predefinedColors

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
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorIdx = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
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
                                    val dialogTheme = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog else android.R.style.Theme_DeviceDefault_Light_Dialog
                                    val timePickerDialog = TimePickerDialog(
                                        activityContext,
                                        dialogTheme,
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
