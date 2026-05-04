package com.example.dmarketalert.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageManager {
    private const val PREF_NAME = "app_settings"
    private const val KEY_LANGUAGE = "language"

    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_UKRAINIAN = "uk"
    const val LANGUAGE_RUSSIAN = "ru"

    fun applyLanguage(context: Context){
        val language = getSavedLanguage(context)
        setLocale(context, language)
    }

    fun saveLanguage(context: Context, languageCode: String){
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getSavedLanguage(context: Context): String{
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    fun setLocale(context: Context, languageCode: String){
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }

    fun getLanguagePosition(languageCode: String): Int {
        return when (languageCode) {
            LANGUAGE_ENGLISH -> 0
            LANGUAGE_UKRAINIAN -> 1
            LANGUAGE_RUSSIAN -> 2
            else -> 0
        }
    }

    fun getLanguageCodeFromPosition(position: Int): String {
        return when (position) {
            0 -> LANGUAGE_ENGLISH
            1 -> LANGUAGE_UKRAINIAN
            2 -> LANGUAGE_RUSSIAN
            else -> LANGUAGE_ENGLISH
        }
    }
}