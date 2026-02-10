package com.example.dmarketalert.model

data class AppSettings(
    // General settings
    val language: String = "en",
    val currency: String = "USD",
    val theme: String = "system",

    // Notification settings
    val notificationEnabled: Boolean = true,
    val outBidNotification: Boolean = true,
    val apiNotification: Boolean = true,
    val notificationMode: String = "sound",
    val notificationDelay: Int = 0, // hours

    // App options
    val limitOfNotification: Int = 50,
    val limitOfHistory: Int = 100,

    // About app
    val lastUpdate: Long = System.currentTimeMillis()
) {
    companion object {
        // Language options
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_UKRAINIAN = "ua"
        const val LANGUAGE_RUSSIAN = "ru"

        // Currency options
        const val CURRENCY_USD = "USD"
        const val CURRENCY_UAH = "UAH"
        const val CURRENCY_EUR = "EUR"

        // Theme options
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        // Notification mode options
        const val NOTIFICATION_SOUND = "sound"
        const val NOTIFICATION_VIBRATION = "vibration"
        const val NOTIFICATION_SILENT = "silent"

        // Notification delay options (hours)
        const val DELAY_0_HOURS = 0
        const val DELAY_1_HOUR = 1
        const val DELAY_5_HOURS = 5
        const val DELAY_10_HOURS = 10
        const val DELAY_1_DAY = 24
        const val DELAY_3_DAYS = 72
        const val DELAY_7_DAYS = 168

        // Limit options
        val LIMIT_OPTIONS = listOf(0, 10, 25, 50, 100)
    }

    /**
     * Get text of notification delay
     */
    fun getDelayText(): String {
        return when (notificationDelay) {
            0 -> "0 hours"
            1 -> "1 hour"
            5 -> "5 hours"
            10 -> "10 hours"
            24 -> "1 day"
            72 -> "3 days"
            168 -> "7 days"
            else -> "$notificationDelay hours"
        }
    }

    /**
     * Get symbol of currency
     */
    fun getCurrencySymbol(): String {
        return when (currency) {
            CURRENCY_USD -> "$"
            CURRENCY_UAH -> "₴"
            CURRENCY_EUR -> "€"
            else -> "$"
        }
    }

    /**
     * Get currency
     */
    fun getCurrencyDisplay(): String {
        return when (currency) {
            CURRENCY_USD -> "USD$"
            CURRENCY_UAH -> "UAH₴"
            CURRENCY_EUR -> "EUR€"
            else -> "USD$"
        }
    }

    /**
     * Get language
     */
    fun getLanguageDisplay(): String {
        return when (language) {
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_UKRAINIAN -> "Українська"
            LANGUAGE_RUSSIAN -> "Русский"
            else -> "English"
        }
    }

    /**
     * Get theme
     */
    fun getThemeDisplay(): String {
        return when (theme) {
            THEME_SYSTEM -> "System"
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            else -> "System"
        }
    }

    /**
     * Get name of mode
     */
    fun getNotificationModeDisplay(): String {
        return when (notificationMode) {
            NOTIFICATION_SOUND -> "Sound"
            NOTIFICATION_VIBRATION -> "Vibration"
            NOTIFICATION_SILENT -> "Silent"
            else -> "Sound"
        }
    }
}