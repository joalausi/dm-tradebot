package com.example.dmarketalert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dmarketalert.model.NetworkResult
import com.example.dmarketalert.repository.MarketRepository
import com.example.dmarketalert.viewModel.state.HistoryUiState
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MarketRepository(application.applicationContext)

    private val _uiState = MutableLiveData<HistoryUiState>(HistoryUiState.Idle)
    val uiState: LiveData<HistoryUiState> = _uiState

    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    fun loadHistory() {
        viewModelScope.launch {
            repository.getTargetsHistory().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.value = HistoryUiState.Loading
                    }
                    is NetworkResult.Success -> {
                        val items = result.data ?: emptyList()
                        _uiState.value = if (items.isEmpty()) {
                            HistoryUiState.Empty
                        } else {
                            HistoryUiState.Success(items)
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = HistoryUiState.Error(
                            result.message ?: "Unknown error"
                        )
                    }
                }
            }
        }
    }
    fun refreshHistory() {
        _isRefreshing.value = true
        loadHistory()
        _isRefreshing.value = false
    }

    fun retry() {
        loadHistory()
    }
}