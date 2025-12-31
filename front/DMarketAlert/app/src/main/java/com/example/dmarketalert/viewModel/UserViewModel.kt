package com.example.dmarketalert.viewModel

import android.content.Intent
import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dmarketalert.model.User
import com.example.dmarketalert.repository.UserRepository
import com.example.dmarketalert.view.MainActivity
import kotlinx.coroutines.launch
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.viewModelFactory

class UserViewModel(private val repository: UserRepository): ViewModel() {
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    fun registerUser(nickname: String, password: String, api: String){
        viewModelScope.launch{
            val newUser = User(nickname = nickname, password = password, api = api)
            repository.saveUser(newUser)
            _user.value = newUser
        }
    }
    fun loadUser(){
        viewModelScope.launch{
            _user.value = repository.getUser()
        }
    }
}