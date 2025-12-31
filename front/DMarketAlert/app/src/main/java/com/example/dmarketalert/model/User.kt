package com.example.dmarketalert.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity (tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nickname: String,
    val password: String,
    val api: String
)
