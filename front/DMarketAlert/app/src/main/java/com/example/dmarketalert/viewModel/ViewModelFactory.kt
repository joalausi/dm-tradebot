package com.example.dmarketalert.viewModel
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.dmarketalert.model.AppDatabase
import com.example.dmarketalert.repository.UserRepository

class ViewModelFactory(context: Context): ViewModelProvider.Factory {
    private val userDao = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app_database"
    ).build().userDao()

    private val repository = UserRepository(userDao)

    override fun <T: ViewModel> create(modelClass: Class<T>): T{
        return UserViewModel(repository) as T
    }
}