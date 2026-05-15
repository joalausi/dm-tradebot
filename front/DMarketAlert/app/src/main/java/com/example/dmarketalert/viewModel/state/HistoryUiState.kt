package com.example.dmarketalert.viewModel.state

import com.example.dmarketalert.model.MarketItem

sealed class HistoryUiState {
    object Idle : HistoryUiState()
    object Loading : HistoryUiState()
    data class Success(val items: List<MarketItem>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
    object Empty : HistoryUiState()
}