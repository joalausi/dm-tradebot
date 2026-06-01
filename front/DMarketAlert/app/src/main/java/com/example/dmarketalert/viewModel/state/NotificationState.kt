package com.example.dmarketalert.viewModel.state

import com.example.dmarketalert.model.local.NotificationEntity

sealed class NotificationState {
    object Loading: NotificationState()
    data class Success(val list: List<NotificationEntity>) : NotificationState()
    object Empty: NotificationState()
    data class Error(val message: String) : NotificationState()
}