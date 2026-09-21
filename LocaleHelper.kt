package com.aistudio.meditracker.ui.locale

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANG = "selected_language"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "system") ?: "system"
    }

    fun setLanguage(context: Context, lang: String): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANG, lang) }
        return updateResources(context, lang)
    }

    fun getEffectiveLanguage(context: Context): String {
        val selected = getLanguage(context)
        if (selected == "system") {
            return systemLanguage(context)
        }
        return selected
    }

    // BUG FIX: the deprecated Locale(String) constructor and the pre-Nougat

    // Configuration.locales and Locale.Builder are always available.
    private fun systemLanguage(context: Context): String {
        val sysLocale = context.resources.configuration.locales[0]
        val sysLang = sysLocale.language.lowercase()
        return if (sysLang == "uk" || sysLang == "ru") sysLang else "en"
    }

    fun updateResources(context: Context, lang: String): Context {
        val effectiveLang = if (lang == "system") systemLanguage(context) else lang

        val locale = Locale.Builder().setLanguage(effectiveLang).build()
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getLocalizedContext(context: Context): Context {
        return updateResources(context, getLanguage(context))
    }
}

fun Context.findActivity(): android.app.Activity? {
    var cur = this
    while (cur is android.content.ContextWrapper) {
        if (cur is android.app.Activity) return cur
        cur = cur.baseContext
    }
    return null
}
