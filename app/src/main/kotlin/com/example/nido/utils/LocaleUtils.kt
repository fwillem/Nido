package com.example.nido.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.nido.utils.TraceLogLevel.*
import java.util.Locale

/**
 * Locale management helpers.
 * Note: all hard relaunches go through [hardRestartApp] for consistency.
 */
object LocaleUtils {
    private const val PREFS_NAME = "nido_prefs"
    private const val KEY_LANGUAGE = "app_language"

    /**
     * Apply the given language to the provided [context].
     * Returns the context adjusted for the new configuration.
     *
     * Important: this does NOT persist the language. Call [saveLanguage] for that.
     */
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = Configuration(resources.configuration)

        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
            context
        }
    }

    /**
     * Persist language choice (synchronously) so it survives app restarts.
     */
    fun saveLanguage(context: Context, language: String) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).commit()

        TRACE(VERBOSE) { "LocaleUtils.saveLanguage: $language" }
        android.util.Log.d("LocaleUtils", "saveLanguage: $language")
    }

    /**
     * Read previously saved language, or null if none.
     */
    fun getSavedLanguage(context: Context): String? {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null)
    }

    /**
     * Helper used when language is changed in-app:
     * - apply locale to current context
     * - perform a hard app relaunch using [hardRestartApp]
     */
    fun setAppLocaleAndHardRestart(activity: Activity, language: String) {
        TRACE(INFO) { "LocaleUtils.setAppLocaleAndHardRestart: $language" }
        setLocale(activity, language)
        hardRestartApp(activity, forceLanding = true)
    }

    /**
     * Enumerate supported languages by checking for a sentinel string resource ("locale_marker")
     * present only in localized resource sets. This keeps the list data-driven.
     */
    fun getSupportedLanguages(context: Context): List<AppLanguage> {
        val locales = mutableListOf<AppLanguage>()
        val res = context.resources
        for (lang in AppLanguage.values()) {
            val locale = Locale(lang.code)
            val config = Configuration(res.configuration)
            config.setLocale(locale)
            val localized = context.createConfigurationContext(config)
            // The "locale_marker" string should exist only when that locale is truly supported.
            val id = localized.resources.getIdentifier("locale_marker", "string", context.packageName)
            if (id != 0) {
                locales.add(lang)
            }
        }
        return locales
    }
}
