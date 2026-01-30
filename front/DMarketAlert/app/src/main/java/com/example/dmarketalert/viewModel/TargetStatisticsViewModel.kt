package com.example.dmarketalert.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class TargetStatistics(
    val currentTargets: Float = 0f,
    val outbidTargets: Float = 0f,
    val allTimeTargets: Float = 0f
)

class TargetStatisticsViewModel : ViewModel() {

    private val _statistics = MutableLiveData<TargetStatistics>(
        TargetStatistics(
            currentTargets = 10f,
            outbidTargets = 3f,
            allTimeTargets = 25f
        )
    )

    val statistics: LiveData<TargetStatistics> = _statistics

    fun updateStatistics(current: Float, outbid: Float, allTime: Float) {
        _statistics.value = TargetStatistics(current, outbid, allTime)
    }

    fun resetStatistics() {
        _statistics.value = TargetStatistics(0f, 0f, 0f)
    }

    fun incrementCurrent() {
        val current = _statistics.value ?: return
        _statistics.value = current.copy(
            currentTargets = current.currentTargets + 1,
            allTimeTargets = current.allTimeTargets + 1
        )
    }

    fun decrementCurrent() {
        val current = _statistics.value ?: return
        if (current.currentTargets > 0) {
            _statistics.value = current.copy(
                currentTargets = current.currentTargets - 1
            )
        }
    }

    fun incrementOutbid() {
        val current = _statistics.value ?: return
        _statistics.value = current.copy(
            outbidTargets = current.outbidTargets + 1,
            currentTargets = maxOf(0f, current.currentTargets - 1)
        )
    }
}