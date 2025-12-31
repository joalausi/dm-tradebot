package com.example.dmarketalert.repository

import com.example.dmarketalert.model.User
import com.example.dmarketalert.model.UserDao

class UserRepository(private val userDao: UserDao) {
    suspend fun saveUser(user: User) = userDao.insert(user)
    suspend fun getUser(): User? = userDao.getUser()

}