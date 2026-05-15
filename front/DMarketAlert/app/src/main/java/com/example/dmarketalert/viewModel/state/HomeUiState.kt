package com.example.dmarketalert.viewModel.state

import com.example.dmarketalert.model.MarketItem

sealed class HomeUiState {
    object Idle : HomeUiState()

    object Loading : HomeUiState()

    data class Success(val items: List<MarketItem>) : HomeUiState()

    data class Error(val message: String) : HomeUiState()

    object Empty : HomeUiState()
}
