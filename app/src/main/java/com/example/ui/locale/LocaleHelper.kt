package com.example.ui.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
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
        prefs.edit().putString(KEY_LANG, lang).apply()
        return updateResources(context, lang)
    }

    fun getEffectiveLanguage(context: Context): String {
        val selected = getLanguage(context)
        if (selected == "system") {
            val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            val sysLang = sysLocale.language.lowercase()
            return if (sysLang == "uk" || sysLang == "ru") sysLang else "en"
        }
        return selected
    }

    fun updateResources(context: Context, lang: String): Context {
        val effectiveLang = if (lang == "system") {
            val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            val sysLang = sysLocale.language.lowercase()
            if (sysLang == "uk" || sysLang == "ru") sysLang else "en"
        } else {
            lang
        }

        val locale = Locale(effectiveLang)
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

