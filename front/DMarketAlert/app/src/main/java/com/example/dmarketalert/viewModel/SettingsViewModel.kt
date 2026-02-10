package com.example.dmarketalert.viewModel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dmarketalert.model.AppSettings
import com.example.dmarketalert.repository.SettingsRepository
import com.example.dmarketalert.util.SettingsManager
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager
    private val repository = SettingsRepository()

    // LiveData settings observation
    private val _settings = MutableLiveData<AppSettings>()
    val settings: LiveData<AppSettings> = _settings

    // LiveData for operation status
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    // LiveData for statistic
    private val _notificationsCount = MutableLiveData<Int>()
    val notificationsCount: LiveData<Int> = _notificationsCount

    private val _historyCount = MutableLiveData<Int>()
    val historyCount: LiveData<Int> = _historyCount

    init {
        loadSettings()
    }

    /**
     * Load settings
     */
    fun loadSettings() {
        val settings = settingsManager.getSettings(getApplication())
        _settings.value = settings
    }

    /**
     * Load statistic
     */
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

    // === UPDATE METHODS ===

    fun updateLanguage(language: String) {
        settingsManager.setLanguage(getApplication(), language)
        loadSettings()
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
        loadSettings()
    }

    fun updateNotificationDelay(delay: Int) {
        settingsManager.setNotificationDelay(getApplication(), delay)
        loadSettings()
    }

    fun updateNotificationLimit(userId: String, limit: Int) {
        settingsManager.setNotificationLimit(getApplication(), limit)
        loadSettings()

        // Apply limit for the notifications
        viewModelScope.launch {
            repository.applyNotificationLimit(userId, limit).onSuccess {
                _operationStatus.postValue(OperationStatus.Success("Notification limit applied"))
                loadStatistics(userId)
            }.onFailure { error ->
                _operationStatus.postValue(OperationStatus.Error(error.message ?: "Error applying limit"))
            }
        }
    }

    fun updateHistoryLimit(userId: String, limit: Int) {
        settingsManager.setHistoryLimit(getApplication(), limit)
        loadSettings()

        // Apply limit for the history
        viewModelScope.launch {
            repository.applyHistoryLimit(userId, limit).onSuccess {
                _operationStatus.postValue(OperationStatus.Success("History limit applied"))
                loadStatistics(userId)
            }.onFailure { error ->
                _operationStatus.postValue(OperationStatus.Error(error.message ?: "Error applying limit"))
            }
        }
    }

    // === ACTIONS ===

    fun resetSettings() {
        settingsManager.resetSettings(getApplication())
        loadSettings()
        _operationStatus.value = OperationStatus.Success("Settings reset to defaults")
    }

    fun clearNotifications(userId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading

            repository.clearNotifications(userId).onSuccess {
                _operationStatus.postValue(OperationStatus.Success("Notifications cleared"))
                loadStatistics(userId)
            }.onFailure { error ->
                _operationStatus.postValue(OperationStatus.Error(error.message ?: "Error clearing notifications"))
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
                _operationStatus.postValue(OperationStatus.Error(error.message ?: "Error clearing history"))
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
                _operationStatus.postValue(OperationStatus.Error(error.message ?: "Error removing statistics"))
            }
        }
    }

    /**
     * Apply theme for the app
     */
    private fun applyTheme(theme: String) {
        when (theme) {
            AppSettings.THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppSettings.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppSettings.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    /**
     * Remove operation status
     */
    fun resetOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }

    // === SEALED CLASS FOR OPERATION STATUS ===

    sealed class OperationStatus {
        object Idle : OperationStatus()
        object Loading : OperationStatus()
        data class Success(val message: String) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }
}