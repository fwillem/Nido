package com.example.nido.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import com.example.nido.MainActivity
import com.example.nido.utils.TraceLogLevel.*

object LocaleUtils {
    private const val PREFS_NAME = "nido_prefs"
    private const val KEY_LANGUAGE = "app_language"

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
            return context
        }
    }

    fun setAppLocaleAndRestart(activity: Activity, language: String) {
        setLocale(activity, language)
        val intent = Intent(activity, MainActivity::class.java)
        // Passe un extra pour dire d’aller directement au LandingScreen
        intent.putExtra("force_landing", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.finish()
        activity.startActivity(intent)
        // (optionnel) Enlève l'animation
        activity.overridePendingTransition(0, 0)
    }

    fun saveLanguage(context: Context, language: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).commit()

        TRACE(VERBOSE) { "BOOOOO LocaleDebug saveLanguage: $language" }

        android.util.Log.d("BOOOOO LocaleDebug", "saveLanguage: $language")
    }


    fun getSavedLanguage(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null)
    }

    fun getSupportedLanguages(context: Context): List<AppLanguage> {
        val locales = mutableListOf<AppLanguage>()
        val res = context.resources
        for (lang in AppLanguage.values()) {
            val locale = Locale(lang.code)
            val config = Configuration(res.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)
            // Utilise la clé "locale_marker" qui existe uniquement dans les langues supportées
            val id = localizedContext.resources.getIdentifier("locale_marker", "string", context.packageName)
            if (id != 0) {
                locales.add(lang)
            }
        }
        return locales
    }

}
