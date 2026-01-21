package com.example.dmarketalert.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dmarketalert.model.User
import com.example.dmarketalert.repository.Authentication
import com.example.dmarketalert.util.SecurityUtil
import com.example.dmarketalert.viewModel.state.AuthState
import kotlinx.coroutines.launch

class AuthenticationViewModel : ViewModel() {
    private val repository = Authentication()
    val authState = MutableLiveData<AuthState>(AuthState.Idle)

    // Registration
    fun register(nickname: String, password: String, api: String) {
        if (nickname.isBlank() || password.isBlank() || api.isBlank()) {
            authState.value = AuthState.Error("All fields are required")
            return
        }

        authState.value = AuthState.Loading

        viewModelScope.launch {
            val user = User(
                nickname = nickname,
                passwordHash = SecurityUtil.sha256(password),
                apiHash = SecurityUtil.sha256(api),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = repository.registerUser(user)
            result
                .onSuccess {
                    authState.postValue(AuthState.Success(user))
                }
                .onFailure {
                    authState.postValue(
                        AuthState.Error(it.message ?: "Registration failed")
                    )
                }
        }
    }

    // Enter
    fun login(nickname: String, password: String) {
        authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = repository.loginUser(nickname, SecurityUtil.sha256(password))
            result
                .onSuccess { user ->
                    authState.postValue(AuthState.Success(user))
                }
                .onFailure {
                    authState.postValue(
                        AuthState.Error(it.message ?: "Login failed")
                    )
                }
        }
    }

    // Download user data
    fun loadUser(nickname: String) {
        authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = repository.getUser(nickname)
            result
                .onSuccess { user ->
                    authState.postValue(AuthState.Success(user))
                }
                .onFailure {
                    authState.postValue(
                        AuthState.Error(it.message ?: "Failed to load user")
                    )
                }
        }
    }

    // Delete an account
    fun deleteAccount(nickname: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteUser(nickname)
            onResult(result)
        }
    }

    // Updating of FCM token
    fun saveFcmToken(nickname: String, token: String) {
        viewModelScope.launch {
            repository.updateFcmToken(nickname, token)
        }
    }

    // Updating of password
    fun updatePassword(
        nickname: String,
        oldPassword: String,
        newPassword: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updatePassword(
                nickname,
                SecurityUtil.sha256(oldPassword),
                SecurityUtil.sha256(newPassword)
            )
            onResult(result)
        }
    }

    // Updating of API key
    fun updateApiKey(
        nickname: String,
        password: String,
        newApiKey: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateApiKey(
                nickname,
                SecurityUtil.sha256(password),
                SecurityUtil.sha256(newApiKey)
            )
            onResult(result)
        }
    }

    // Exit from an account
    fun logout() {
        authState.value = AuthState.Idle
    }
}