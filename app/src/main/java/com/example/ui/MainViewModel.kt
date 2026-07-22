package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarms.AlarmScheduler
import com.example.data.AppDatabase
import com.example.data.DailyScheduleView
import com.example.data.IntakeLog
import com.example.data.Medication
import com.example.data.MedicationRepository
import com.example.data.SettingsRepository
import com.example.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )
    
    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Using flatMapLatest so whenever selectedDate changes, we query new data
    val dailySchedules: StateFlow<List<DailyScheduleView>> = _selectedDate
        .flatMapLatest { date -> repository.getDailySchedules(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val todayIntakeLogs: StateFlow<List<IntakeLog>> = _selectedDate
        .flatMapLatest { date -> repository.getIntakeLogsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMedications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun toggleLog(schedule: DailyScheduleView, isTaken: Boolean) {
        viewModelScope.launch {
            repository.toggleIntake(schedule, _selectedDate.value, isTaken)
        }
    }

    fun addMedication(medication: Medication, times: List<Pair<Int, Int>>) {
        viewModelScope.launch {
            val createdSchedules = repository.addMedicationWithSchedules(medication, times)
            createdSchedules.forEach { schedule ->
                alarmScheduler.scheduleAlarm(schedule, medication.name)
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            val schedules = repository.getSchedulesForMedication(medication.id)
            schedules.forEach { schedule ->
                alarmScheduler.cancelAlarm(schedule)
            }
            repository.deleteMedication(medication)
        }
    }
}

class MainViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = MedicationRepository(database.medicationDao())
            val alarmScheduler = AlarmScheduler(context)
            val settingsRepository = SettingsRepository(context)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, alarmScheduler, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
