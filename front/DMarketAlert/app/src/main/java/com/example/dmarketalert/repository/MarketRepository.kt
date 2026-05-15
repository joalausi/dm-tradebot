package com.example.dmarketalert.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.dmarketalert.model.MarketItem
import com.example.dmarketalert.model.NetworkResult
import com.example.dmarketalert.model.remote.RetrofitClient
import com.example.dmarketalert.model.toDomainModel
import com.example.dmarketalert.util.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class MarketRepository(private val context: Context) {

    private val apiService = RetrofitClient.apiService
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private fun getApiKey(): String {
        return sharedPreferences.getString("api_key", "") ?: ""
    }

    fun getActiveTargets(): Flow<NetworkResult<List<MarketItem>>> = flow {
        emit(NetworkResult.Loading())

        if (!NetworkHelper.isNetworkAvailable(context)) {
            emit(NetworkResult.Error("No internet connection"))
            return@flow
        }

        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            emit(NetworkResult.Error("API key not found. Please log in"))
            return@flow
        }

        try {
            val response = apiService.getActiveTargets("Bearer $apiKey")
            emit(handleResponse(response))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getTargetsHistory(): Flow<NetworkResult<List<MarketItem>>> = flow {
        emit(NetworkResult.Loading())

        if (!NetworkHelper.isNetworkAvailable(context)) {
            emit(NetworkResult.Error("No internet connection"))
            return@flow
        }

        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            emit(NetworkResult.Error("API key not found. Please log in."))
            return@flow
        }

        try {
            val response = apiService.getTargetsHistory("Bearer $apiKey")
            emit(handleResponse(response))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)

    fun refreshTargets(): Flow<NetworkResult<List<MarketItem>>> = flow {
        emit(NetworkResult.Loading())

        if (!NetworkHelper.isNetworkAvailable(context)) {
            emit(NetworkResult.Error("No internet connection"))
            return@flow
        }

        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            emit(NetworkResult.Error("API key not found"))
            return@flow
        }

        try {
            val response = apiService.refreshTargets("Bearer $apiKey")
            emit(handleResponse(response))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun validateApiKey(apiKey: String): NetworkResult<Boolean> {
        return try {
            if (!NetworkHelper.isNetworkAvailable(context)) {
                NetworkResult.Error("No internet connection")
            } else {
                val response = apiService.validateApiKey("Bearer $apiKey")
                if (response.isSuccessful) {
                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Error("Invalid API key")
                }
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Validation failed")
        }
    }

    private fun handleResponse(response: Response<com.example.dmarketalert.model.TargetsApiResponse>): NetworkResult<List<MarketItem>> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    val items = body.items.map { it.toDomainModel() }
                    NetworkResult.Success(items)
                } else {
                    NetworkResult.Error("Empty response from server")
                }
            }
            response.code() == 401 -> NetworkResult.Error("Unauthorized. Please check your API key.")
            response.code() == 404 -> NetworkResult.Error("Endpoint not found")
            response.code() >= 500 -> NetworkResult.Error("Server error. Please try again later.")
            else -> NetworkResult.Error("Error: ${response.code()} ${response.message()}")
        }
    }
}