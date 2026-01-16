package com.example.dmarketalert.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dmarketalert.model.User
import com.example.dmarketalert.repository.Authentication

class AuthenticationViewModel : ViewModel() {

    private val repository = Authentication()

    val registrationResult = MutableLiveData<Pair<Boolean, String?>>()
    val loginResult = MutableLiveData<Pair<Boolean, User?>>()

    fun register(user: User) {
        repository.registerUser(user) { success, error ->
            registrationResult.postValue(success to error)
        }
    }

    fun login(nickname: String, password: String) {
        repository.loginUser(nickname, password) { success, user ->
            loginResult.postValue(success to user)
        }
    }

    fun saveFcmToken(nickname: String, token: String) {
        repository.updateFcmToken(nickname, token)
    }
}