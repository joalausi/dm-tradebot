package com.example.dmarketalert.model

/**
 * General model of user data, saving in FireStore Database.
 * Password&API - is hash, not real values.
 */
data class User(
    val nickname: String = "",
    val passwordHash: String = "",
    val apiPublic: String = "",
    val apiHash: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
