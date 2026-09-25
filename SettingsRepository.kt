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
        val ALARM_MODE = booleanPreferencesKey("alarm_mode")
        val LIQUID_GLASS = booleanPreferencesKey("liquid_glass")
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

    // NOTE: the reminder is always pinned in the island and always routes
    // through the DND-bypassing channel (with automatic fallback to the
    // normal channel while policy access is missing). These are no longer
    // user-facing toggles — v2.5.3 removed them from Settings.

    // Full-screen alarm presentation instead of a plain heads-up notification.
    val alarmModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ALARM_MODE] ?: true
    }

    suspend fun setAlarmMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_MODE] = enabled
        }
    }

    // iOS-grade liquid glass material — the user's master switch.
    // It unlocks the full lens only on powerful devices; the performance
    // governor still downgrades quality if frames start to stutter.
    val liquidGlassFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LIQUID_GLASS] ?: true
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LIQUID_GLASS] = enabled
        }
    }
}
