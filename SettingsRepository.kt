package com.aistudio.meditracker.data

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
        val PERSISTENT_REMINDER = booleanPreferencesKey("persistent_reminder")
        val CRITICAL_ALERTS = booleanPreferencesKey("critical_alerts")
        val ALARM_MODE = booleanPreferencesKey("alarm_mode")
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

    // The reminder notification stays pinned (island/shade) until the dose is taken.
    val persistentReminderFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PERSISTENT_REMINDER] ?: true
    }

    suspend fun setPersistentReminder(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERSISTENT_REMINDER] = enabled
        }
    }

    // Reminder sound routed through the DND-bypassing channel. Requires
    // notification policy access to actually ring while Do Not Disturb is on.
    val criticalAlertsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CRITICAL_ALERTS] ?: false
    }

    suspend fun setCriticalAlerts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_ALERTS] = enabled
        }
    }

    // Full-screen alarm presentation instead of a plain heads-up notification.
    val alarmModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ALARM_MODE] ?: true
    }

    suspend fun setAlarmMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_MODE] = enabled
        }
    }
}
