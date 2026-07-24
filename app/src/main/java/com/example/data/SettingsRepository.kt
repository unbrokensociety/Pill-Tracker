package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, BRAND }

class SettingsRepository(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_AVATAR_URI = stringPreferencesKey("user_avatar_uri")
        val ALARM_CLOCK_MODE = booleanPreferencesKey("alarm_clock_mode")
        val ALARM_REPEAT_COUNT = androidx.datastore.preferences.core.intPreferencesKey("alarm_repeat_count")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val LAST_SYNC_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("last_sync_timestamp")
        val PENDING_DELETION_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("pending_deletion_timestamp")
    }

    val userAvatarUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR_URI] ?: ""
    }

    val cloudSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CLOUD_SYNC_ENABLED] ?: true
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_ENABLED] = enabled
        }
    }

    val alarmClockModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ALARM_CLOCK_MODE] ?: false
    }

    val alarmRepeatCountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ALARM_REPEAT_COUNT] ?: 1
    }

    suspend fun updateUserProfile(name: String, avatarUri: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_AVATAR_URI] = avatarUri
        }
    }

    suspend fun setAlarmClockMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_CLOCK_MODE] = enabled
        }
    }

    suspend fun setAlarmRepeatCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_REPEAT_COUNT] = count
        }
    }

    val pendingDeletionTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PENDING_DELETION_TIMESTAMP] ?: 0L
    }

    suspend fun requestAccountDeletion(graceDays: Int = 30) {
        val targetTime = System.currentTimeMillis() + (graceDays.toLong() * 24 * 60 * 60 * 1000L)
        context.dataStore.edit { preferences ->
            preferences[PENDING_DELETION_TIMESTAMP] = targetTime
        }
    }

    suspend fun cancelAccountDeletion() {
        context.dataStore.edit { preferences ->
            preferences[PENDING_DELETION_TIMESTAMP] = 0L
        }
    }

    val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIMESTAMP] ?: 0L
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val mode = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(mode)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    val notificationsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETED] ?: false
    }

    val isGuestModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GUEST_MODE] ?: true
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL] ?: ""
    }

    suspend fun setAccountState(isGuest: Boolean, name: String, email: String, onboardingDone: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST_MODE] = isGuest
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
            preferences[IS_ONBOARDING_COMPLETED] = onboardingDone
        }
    }

    suspend fun signOutToGuest() {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST_MODE] = true
            preferences[USER_NAME] = ""
            preferences[USER_EMAIL] = ""
        }
    }
}
