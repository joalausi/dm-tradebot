package com.example.dmarketalert.viewModel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dmarketalert.model.AppSettings
import com.example.dmarketalert.repository.SettingsRepository
import com.example.dmarketalert.repository.local.AppDatabase
import com.example.dmarketalert.util.SettingsManager
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application){
    private val settingsManager = SettingsManager
    private val repository = SettingsRepository()
    private val _settings = MutableLiveData<AppSettings>()
    val settings: LiveData<AppSettings> = _settings

    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    private val _restartActivity = MutableLiveData<Boolean>()
    val restartActivity: LiveData<Boolean> = _restartActivity

    private val _notificationsCount = MutableLiveData<Int>()
    val notificationsCount: LiveData<Int> = _notificationsCount

    private val _historyCount = MutableLiveData<Int>()
    val historyCount: LiveData<Int> = _historyCount

    init {
        loadSettings()
    }

    fun loadSettings() {
        val settings = settingsManager.getSettings(getApplication())
        _settings.value = settings
    }

    fun loadStatistics(userId: String) {
        viewModelScope.launch {
            repository.getNotificationsCount(userId).onSuccess { count ->
                _notificationsCount.postValue(count)
            }
            repository.getHistoryCount(userId).onSuccess { count ->
                _historyCount.postValue(count)
            }
        }
    }

    // ===== UPDATE SETTINGS =====

    fun updateLanguage(language: String) {
        val currentLanguage = settingsManager.getLanguage(getApplication())
        if (currentLanguage == language) return

        settingsManager.setLanguage(getApplication(), language)
        loadSettings()
        _restartActivity.value = true
    }

    fun updateCurrency(currency: String) {
        settingsManager.setCurrency(getApplication(), currency)
        loadSettings()
    }

    fun updateTheme(theme: String) {
        settingsManager.setTheme(getApplication(), theme)
        applyTheme(theme)
        loadSettings()
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        settingsManager.setNotificationEnabled(getApplication(), enabled)
        loadSettings()
    }

    fun updateOutbidNotifications(enabled: Boolean) {
        settingsManager.setOutbidNotification(getApplication(), enabled)
        loadSettings()
    }

    fun updateApiErrorNotifications(enabled: Boolean) {
        settingsManager.setApiErrorsNotification(getApplication(), enabled)
        loadSettings()
    }

    fun updateNotificationMode(mode: String) {
        settingsManager.setNotificationMode(getApplication(), mode)
        applyNotificationMode(mode)
        loadSettings()
    }

    fun updateNotificationDelay(delay: Int) {
        settingsManager.setNotificationDelay(getApplication(), delay)
        loadSettings()
    }

    fun updateNotificationLimit(userId: String, limit: Int) {
        settingsManager.setNotificationLimit(getApplication(), limit)
        loadSettings()
        viewModelScope.launch {
            try {
                if (limit > 0){
                    AppDatabase.getDatabase(getApplication()).notificationDao().applyLimit(limit)
                } else {
                    AppDatabase.getDatabase(getApplication()).notificationDao().clearAll()
                }

                repository.applyNotificationLimit(userId, limit)
                _operationStatus.postValue(OperationStatus.Success("Notification limit applied"))
                loadStatistics(userId)
            } catch (e: Exception){
                _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error applying limit"))
            }
        }
    }

    fun updateHistoryLimit(userId: String, limit: Int) {
        settingsManager.setHistoryLimit(getApplication(), limit)
        loadSettings()
        viewModelScope.launch {
            repository.applyHistoryLimit(userId, limit).onSuccess {
                _operationStatus.postValue(OperationStatus.Success("History limit applied"))
                loadStatistics(userId)
            }.onFailure { error ->
                _operationStatus.postValue(
                    OperationStatus.Error(error.message ?: "Error applying limit")
                )
            }
        }
    }

    // ===== ACTIONS =====

    fun resetSettings() {
        settingsManager.resetSettings(getApplication())
        applyTheme(AppSettings.THEME_SYSTEM)
        loadSettings()
        _operationStatus.value = OperationStatus.Success("Settings reset to defaults")
    }

    fun clearNotifications(userId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            try {
                AppDatabase.getDatabase(getApplication()).notificationDao().clearAll()

                repository.clearNotifications(userId)
                _operationStatus.postValue(OperationStatus.Success("Notifications cleared"))
                loadStatistics(userId)
            } catch (e: Exception){
                _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error clearing notifications"))
            }
        }
    }

    fun clearHistory(userId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            repository.clearHistory(userId).onSuccess {
                _operationStatus.postValue(OperationStatus.Success("History cleared"))
                loadStatistics(userId)
            }.onFailure { error ->
                _operationStatus.postValue(
                    OperationStatus.Error(error.message ?: "Error clearing history")
                )
            }
        }
    }

    fun removeAllStatistics(userId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            repository.clearAllStatistics(userId).onSuccess {
                _operationStatus.postValue(OperationStatus.Success("All statistics removed"))
                loadStatistics(userId)
            }.onFailure { error ->
                _operationStatus.postValue(
                    OperationStatus.Error(error.message ?: "Error removing statistics")
                )
            }
        }
    }

    // ===== PRIVATE HELPING FUNCTIONS =====

    private fun applyTheme(theme: String) {
        when (theme) {
            AppSettings.THEME_SYSTEM ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppSettings.THEME_LIGHT ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppSettings.THEME_DARK ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun applyNotificationMode(mode: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager =
                getApplication<Application>()
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = when (mode) {
                AppSettings.NOTIFICATION_SOUND -> "dm_alert_sound"
                AppSettings.NOTIFICATION_VIBRATION -> "dm_alert_vibration"
                AppSettings.NOTIFICATION_SILENT -> "dm_alert_silent"
                else -> "dm_alert_sound"
            }
            settingsManager.setNotificationMode(getApplication(), mode)
        }
    }

    fun resetRestartFlag() {
        _restartActivity.value = false
    }

    fun resetOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }

    sealed class OperationStatus {
        object Idle : OperationStatus()
        object Loading : OperationStatus()
        data class Success(val message: String) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }
}