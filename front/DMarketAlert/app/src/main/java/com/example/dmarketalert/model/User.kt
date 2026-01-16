package com.example.dmarketalert.model

data class User(
    val nickname: String? = null,
    val password: String? = null,
    val api: String? = null,
    val fcmToken: String = "",
    val createdAt: Long = 0L
)
