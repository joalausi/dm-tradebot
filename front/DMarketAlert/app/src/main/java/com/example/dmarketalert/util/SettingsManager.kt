package com.example.dmarketalert.util

import android.content.Context
import android.content.SharedPreferences
import com.example.dmarketalert.model.AppSettings

object SettingsManager {
    private const val PREFS_NAME = "app_settings"

    // Keys
    private const val KEY_LANGUAGE = "language"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_THEME = "theme"

    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_OUTBID_NOTIFICATIONS = "outbid_notifications"
    private const val KEY_API_ERRORS = "api_errors_notifications"
    private const val KEY_NOTIFICATION_MODE = "notification_mode"
    private const val KEY_NOTIFICATION_DELAY = "notification_delay"

    private const val KEY_NOTIFICATIONS_LIMIT = "notifications_limit"
    private const val KEY_HISTORY_LIMIT = "history_limit"

    private const val KEY_LAST_UPDATE = "last_update"

    // Default values
    private const val DEFAULT_LANGUAGE = AppSettings.LANGUAGE_ENGLISH
    private const val DEFAULT_CURRENCY = AppSettings.CURRENCY_USD
    private const val DEFAULT_THEME = AppSettings.THEME_SYSTEM
    private const val DEFAULT_NOTIFICATION_MODE = AppSettings.NOTIFICATION_SOUND
    private const val DEFAULT_NOTIFICATION_DELAY = AppSettings.DELAY_0_HOURS
    private const val DEFAULT_NOTIFICATIONS_LIMIT = 50
    private const val DEFAULT_HISTORY_LIMIT = 100

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get all settings
     */
    fun getSettings(context: Context): AppSettings {
        val prefs = getPrefs(context)

        return AppSettings(
            language = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE,
            currency = prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY,
            theme = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME,

            notificationEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
            outBidNotification = prefs.getBoolean(KEY_OUTBID_NOTIFICATIONS, true),
            apiNotification = prefs.getBoolean(KEY_API_ERRORS, true),
            notificationMode = prefs.getString(KEY_NOTIFICATION_MODE, DEFAULT_NOTIFICATION_MODE)
                ?: DEFAULT_NOTIFICATION_MODE,
            notificationDelay = prefs.getInt(KEY_NOTIFICATION_DELAY, DEFAULT_NOTIFICATION_DELAY),

            limitOfNotification = prefs.getInt(KEY_NOTIFICATIONS_LIMIT, DEFAULT_NOTIFICATIONS_LIMIT),
            limitOfHistory = prefs.getInt(KEY_HISTORY_LIMIT, DEFAULT_HISTORY_LIMIT),

            lastUpdate = prefs.getLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        )
    }

    /**
     * Save settings
     */
    fun saveSettings(context: Context, settings: AppSettings) {
        getPrefs(context).edit().apply {
            putString(KEY_LANGUAGE, settings.language)
            putString(KEY_CURRENCY, settings.currency)
            putString(KEY_THEME, settings.theme)

            putBoolean(KEY_NOTIFICATIONS, settings.notificationEnabled)
            putBoolean(KEY_OUTBID_NOTIFICATIONS, settings.outBidNotification)
            putBoolean(KEY_API_ERRORS, settings.apiNotification)
            putString(KEY_NOTIFICATION_MODE, settings.notificationMode)
            putInt(KEY_NOTIFICATION_DELAY, settings.notificationDelay)

            putInt(KEY_NOTIFICATIONS_LIMIT, settings.limitOfNotification)
            putInt(KEY_HISTORY_LIMIT, settings.limitOfHistory)

            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())

            apply()
        }
    }

    // === GETTERS ===

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun getCurrency(context: Context): String {
        return getPrefs(context).getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }

    fun getTheme(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun isOutbidNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_OUTBID_NOTIFICATIONS, true)
    }

    fun isApiErrorsNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_API_ERRORS, true)
    }

    fun getNotificationMode(context: Context): String {
        return getPrefs(context).getString(KEY_NOTIFICATION_MODE, DEFAULT_NOTIFICATION_MODE)
            ?: DEFAULT_NOTIFICATION_MODE
    }

    fun getNotificationDelay(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOTIFICATION_DELAY, DEFAULT_NOTIFICATION_DELAY)
    }

    fun getNotificationLimit(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOTIFICATIONS_LIMIT, DEFAULT_NOTIFICATIONS_LIMIT)
    }

    fun getHistoryLimit(context: Context): Int {
        return getPrefs(context).getInt(KEY_HISTORY_LIMIT, DEFAULT_HISTORY_LIMIT)
    }

    fun getLastUpdate(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_UPDATE, System.currentTimeMillis())
    }

    // === SETTERS ===

    fun setLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, language).apply()
        updateLastUpdate(context)
    }

    fun setCurrency(context: Context, currency: String) {
        getPrefs(context).edit().putString(KEY_CURRENCY, currency).apply()
        updateLastUpdate(context)
    }

    fun setTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply()
        updateLastUpdate(context)
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()

        if (!enabled) {
            setOutbidNotification(context, false)
            setApiErrorsNotification(context, false)
        }

        updateLastUpdate(context)
    }

    fun setOutbidNotification(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_OUTBID_NOTIFICATIONS, enabled).apply()
        updateLastUpdate(context)
    }

    fun setApiErrorsNotification(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_API_ERRORS, enabled).apply()
        updateLastUpdate(context)
    }

    fun setNotificationMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_NOTIFICATION_MODE, mode).apply()
        updateLastUpdate(context)
    }

    fun setNotificationDelay(context: Context, delay: Int) {
        getPrefs(context).edit().putInt(KEY_NOTIFICATION_DELAY, delay).apply()
        updateLastUpdate(context)
    }

    fun setNotificationLimit(context: Context, limit: Int) {
        getPrefs(context).edit().putInt(KEY_NOTIFICATIONS_LIMIT, limit).apply()
        updateLastUpdate(context)
    }

    fun setHistoryLimit(context: Context, limit: Int) {
        getPrefs(context).edit().putInt(KEY_HISTORY_LIMIT, limit).apply()
        updateLastUpdate(context)
    }

    /**
     * Update text of last app update
     */
    private fun updateLastUpdate(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
    }

    /**
     * Reset settings
     */
    fun resetSettings(context: Context) {
        getPrefs(context).edit().clear().apply()
        updateLastUpdate(context)
    }

    /**
     * Processing of settings initialization
     */
    fun isInitialized(context: Context): Boolean {
        return getPrefs(context).contains(KEY_LANGUAGE)
    }
}