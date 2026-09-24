package com.aistudio.meditracker.ui

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.meditracker.alarms.AlarmScheduler
import com.aistudio.meditracker.data.AppDatabase
import com.aistudio.meditracker.data.DailyScheduleView
import com.aistudio.meditracker.data.IntakeLog
import com.aistudio.meditracker.data.Medication
import com.aistudio.meditracker.data.MedicationRepository
import com.aistudio.meditracker.data.SettingsRepository
import com.aistudio.meditracker.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
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

    val alarmModeEnabled: StateFlow<Boolean> = settingsRepository.alarmModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val streakDays: StateFlow<Int> = repository.allIntakeLogDates.map { logEpochs ->
        val logDatesSet = logEpochs.map { epoch ->
            Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()

        val today = LocalDate.now()
        var streak = 0
        var checkDate = today

        if (!logDatesSet.contains(today)) {
            checkDate = today.minusDays(1)
        }

        while (logDatesSet.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        streak
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

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

    /** Courses still running: stock left and the end date has not passed. */
    val activeMedications: StateFlow<List<Medication>> = allMedications
        .map { list -> list.filterNot { MedicationRepository.isCourseFinished(it) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Finished courses — kept visible (and refindable/editable), no reminders. */
    val finishedMedications: StateFlow<List<Medication>> = allMedications
        .map { list -> list.filter { MedicationRepository.isCourseFinished(it) } }
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
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            val allSchedules = repository.getAllActiveScheduleViews()
            val medsById = repository.getAllMedicationsOnce().associateBy { it.id }
            allSchedules.forEach { view ->
                val sched = com.aistudio.meditracker.data.Schedule(
                    id = view.scheduleId,
                    medicationId = view.medicationId,
                    timeHour = view.timeHour,
                    timeMinute = view.timeMinute
                )
                if (enabled) {
                    // Course-finished medications get no new alarms — their
                    // stock is gone or the end date has passed.
                    val med = medsById[view.medicationId]
                    if (med != null && !MedicationRepository.isCourseFinished(med)) {
                        alarmScheduler.scheduleAlarm(sched, view.name)
                    }
                } else {
                    alarmScheduler.cancelAlarm(sched)
                }
            }
        }
    }

    // NOTE: island pinning and DND-bypassing sound are always on since
    // v2.5.3 — the toggles were removed from Settings, so there are no
    // setters for them here anymore.

    fun setAlarmMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAlarmMode(enabled) }
    }

    val lowStockMedications: StateFlow<List<Medication>> = repository.lowStockMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleLog(schedule: DailyScheduleView, isTaken: Boolean, sideEffectNote: String = "") {
        viewModelScope.launch {
            repository.toggleIntake(schedule, _selectedDate.value, isTaken, sideEffectNote)
            if (isTaken) {
                // The pinned reminder (island) and any pending snooze for this
                // dose are no longer relevant once it is logged as taken.
                try {
                    (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .cancel(schedule.scheduleId)
                } catch (_: Exception) {
                }
                alarmScheduler.cancelSnoozeAlarms(schedule.scheduleId)

                // That may have been the last pill: when the course just
                // finished, disarm the next scheduled reminder right away —
                // the history stays, the reminders stop.
                val med = repository.getMedicationById(schedule.medicationId)
                if (med != null && MedicationRepository.isCourseFinished(med)) {
                    alarmScheduler.cancelAlarm(
                        com.aistudio.meditracker.data.Schedule(
                            id = schedule.scheduleId,
                            medicationId = schedule.medicationId,
                            timeHour = schedule.timeHour,
                            timeMinute = schedule.timeMinute
                        )
                    )
                    alarmScheduler.cancelSnoozeAlarms(schedule.scheduleId)
                }
            }
        }
    }

    fun refillStock(medicationId: Int, amount: Int) {
        viewModelScope.launch {
            repository.refillStock(medicationId, amount)
            // A refill revives a finished course: re-arm its reminders so
            // the user does not have to edit anything to get them back.
            val med = repository.getMedicationById(medicationId)
            if (med != null && !MedicationRepository.isCourseFinished(med)) {
                repository.getSchedulesForMedication(medicationId).forEach { s ->
                    alarmScheduler.scheduleAlarm(s, med.name)
                }
            }
        }
    }

    fun addMedication(medication: Medication, times: List<Pair<Int, Int>>) {
        viewModelScope.launch {
            val createdSchedules = repository.addMedicationWithSchedules(medication, times)
            // A brand-new course could theoretically start already finished
            // (zero stock / past end date) — never arm alarms for it.
            val finished = MedicationRepository.isCourseFinished(medication)
            if (!finished) {
                createdSchedules.forEach { schedule ->
                    alarmScheduler.scheduleAlarm(schedule, medication.name)
                }
            }
        }
    }

    suspend fun getMedicationById(id: Int): Medication? {
        return repository.getMedicationById(id)
    }

    suspend fun getSchedulesForMedication(id: Int): List<com.aistudio.meditracker.data.Schedule> {
        return repository.getSchedulesForMedication(id)
    }

    fun updateMedication(medication: Medication, times: List<Pair<Int, Int>>) {
        viewModelScope.launch {
            val oldSchedules = repository.getSchedulesForMedication(medication.id)
            oldSchedules.forEach { alarmScheduler.cancelAlarm(it) }

            val newSchedules = repository.updateMedicationWithSchedules(medication, times)
            // Editing a finished course (e.g. refilling the stock or moving
            // the end date) re-activates it; while it stays finished, no
            // new alarms are armed.
            if (!MedicationRepository.isCourseFinished(medication)) {
                newSchedules.forEach { schedule ->
                    alarmScheduler.scheduleAlarm(schedule, medication.name)
                }
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            val schedules = repository.getSchedulesForMedication(medication.id)
            schedules.forEach { schedule ->
                alarmScheduler.cancelAlarm(schedule)
                alarmScheduler.cancelSnoozeAlarms(schedule.id)
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
            return MainViewModel(repository, alarmScheduler, settingsRepository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
