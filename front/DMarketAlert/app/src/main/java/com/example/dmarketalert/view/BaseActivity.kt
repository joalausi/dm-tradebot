package com.example.dmarketalert.view

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.dmarketalert.model.AppSettings
import com.example.dmarketalert.util.SettingsManager

open class BaseActivity: AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = SettingsManager.getLanguage(newBase)
        val localizedContext = SettingsManager.applyLanguage(newBase, language)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeFromSettings()
        super.onCreate(savedInstanceState)
    }

    private fun applyThemeFromSettings() {
        val theme = SettingsManager.getTheme(this)

        when (theme) {
            AppSettings.THEME_SYSTEM ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            // FOLLOW_SYSTEM

            AppSettings.THEME_LIGHT ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            // MODE_NIGHT_NO

            AppSettings.THEME_DARK ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            // MODE_NIGHT_YES
        }
    }

    fun restartForLanguageChange() {
        recreate()
    }
}