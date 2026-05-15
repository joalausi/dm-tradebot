package com.example.dmarketalert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dmarketalert.model.NetworkResult
import com.example.dmarketalert.repository.MarketRepository
import com.example.dmarketalert.viewModel.state.HomeUiState
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MarketRepository(application.applicationContext)

    private val _uiState = MutableLiveData<HomeUiState>(HomeUiState.Idle)
    val uiState: LiveData<HomeUiState> = _uiState

    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    fun loadActiveTargets() {
        viewModelScope.launch {
            repository.getActiveTargets().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.value = HomeUiState.Loading
                    }
                    is NetworkResult.Success -> {
                        val items = result.data ?: emptyList()
                        _uiState.value = if (items.isEmpty()) {
                            HomeUiState.Empty
                        } else {
                            HomeUiState.Success(items)
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = HomeUiState.Error(
                            result.message ?: "Unknown error occurred"
                        )
                    }
                }
            }
        }
    }

    fun refreshTargets() {
        _isRefreshing.value = true

        viewModelScope.launch {
            repository.refreshTargets().collect { result ->
                _isRefreshing.value = false

                when (result) {
                    is NetworkResult.Loading -> {
                        // SwipeRefreshLayout
                    }
                    is NetworkResult.Success -> {
                        val items = result.data ?: emptyList()
                        _uiState.value = if (items.isEmpty()) {
                            HomeUiState.Empty
                        } else {
                            HomeUiState.Success(items)
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = HomeUiState.Error(
                            result.message ?: "Failed to refresh"
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        loadActiveTargets()
    }
}