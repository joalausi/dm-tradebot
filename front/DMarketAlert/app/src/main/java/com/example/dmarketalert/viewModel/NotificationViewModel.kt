package com.example.dmarketalert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.dmarketalert.repository.local.AppDatabase
import com.example.dmarketalert.viewModel.state.NotificationState
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).notificationDao()
    val notificationState = MediatorLiveData<NotificationState>()

    init {
        notificationState.value = NotificationState.Loading

        notificationState.addSource(dao.getAllNotification()) { list ->
            notificationState.value = if (list.isNullOrEmpty()) {
                NotificationState.Empty
            } else {
                NotificationState.Success(list)
            }
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}