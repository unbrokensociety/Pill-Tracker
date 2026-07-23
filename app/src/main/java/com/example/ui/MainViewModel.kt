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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.map

import com.example.data.CloudSyncRepository
import com.example.data.UserAccount
import com.example.data.UserRepository

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MedicationRepository,
    private val userRepository: UserRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    val lastSyncTimestamp: StateFlow<Long> = settingsRepository.lastSyncTimestampFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    fun triggerCloudSync(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncRepository.syncToCloud()
            onResult(result.getOrDefault("Sync complete"))
        }
    }

    fun triggerCloudRestore(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncRepository.restoreFromCloud()
            onResult(result.getOrDefault("Restore complete"))
        }
    }

    val allSavedUsers: StateFlow<List<UserAccount>> = userRepository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


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

    val isOnboardingCompleted: StateFlow<Boolean> = settingsRepository.isOnboardingCompletedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true // Default true to prevent flicker, will update on initial collection
    )

    val isGuestMode: StateFlow<Boolean> = settingsRepository.isGuestModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val userName: StateFlow<String> = settingsRepository.userNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val userEmail: StateFlow<String> = settingsRepository.userEmailFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
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

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
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
        }
    }

    fun refillStock(medicationId: Int, amount: Int) {
        viewModelScope.launch {
            repository.refillStock(medicationId, amount)
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

    fun loginOrRegister(name: String, email: String, provider: String = "EMAIL", passwordHash: String = "") {
        viewModelScope.launch {
            val userAccount = UserAccount(
                email = email,
                name = name,
                authProvider = provider,
                passwordHash = passwordHash,
                lastLoginAt = System.currentTimeMillis()
            )
            userRepository.saveUser(userAccount)
            settingsRepository.setAccountState(
                isGuest = false,
                name = name,
                email = email,
                onboardingDone = true
            )
        }
    }

    fun loginWithGoogle(email: String, name: String, avatarUrl: String) {
        viewModelScope.launch {
            val userAccount = UserAccount(
                email = email,
                name = name,
                authProvider = "GOOGLE",
                avatarUrl = avatarUrl,
                lastLoginAt = System.currentTimeMillis()
            )
            userRepository.saveUser(userAccount)
            settingsRepository.setAccountState(
                isGuest = false,
                name = name,
                email = email,
                onboardingDone = true
            )
        }
    }

    fun switchAccount(email: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByEmail(email)
            if (user != null) {
                userRepository.updateLastLogin(email)
                settingsRepository.setAccountState(
                    isGuest = false,
                    name = user.name,
                    email = user.email,
                    onboardingDone = true
                )
            }
        }
    }

    fun skipAuthAsGuest() {
        viewModelScope.launch {
            settingsRepository.setAccountState(
                isGuest = true,
                name = "",
                email = "",
                onboardingDone = true
            )
        }
    }

    fun signOutToGuest() {
        viewModelScope.launch {
            settingsRepository.signOutToGuest()
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
            val userRepository = UserRepository(database.userDao())
            val alarmScheduler = AlarmScheduler(context)
            val settingsRepository = SettingsRepository(context)
            val cloudSyncRepository = CloudSyncRepository(context, database.medicationDao(), settingsRepository)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userRepository, alarmScheduler, settingsRepository, cloudSyncRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

