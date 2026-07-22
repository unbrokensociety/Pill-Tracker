package com.example.ui.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANG = "selected_language"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "ru") ?: "ru"
    }

    fun setLanguage(context: Context, lang: String): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang).apply()
        return updateResources(context, lang)
    }

    fun updateResources(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
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
