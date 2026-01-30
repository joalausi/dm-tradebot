package com.example.dmarketalert.repository

import com.example.dmarketalert.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class Authentication {
    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")

    // Registration of user
    suspend fun registerUser(user: User): Result<Unit> {
        return try {
            val nickname = user.nickname.trim()

            if (nickname.isEmpty()) {
                return Result.failure(Exception("Invalid nickname"))
            }

            // Checking, is user created
            val doc = users.document(nickname).get().await()

            if (doc.exists()) {
                Result.failure(Exception("Nickname already taken"))
            } else {
                users.document(nickname).set(user).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Entering of user
    suspend fun loginUser(nickname: String, passwordHash: String): Result<User> {
        return try {
            val doc = users.document(nickname).get().await()
            val user = doc.toObject(User::class.java)

            when {
                user == null -> Result.failure(Exception("User not found"))
                user.passwordHash != passwordHash -> Result.failure(Exception("Wrong password"))
                else -> Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Loading of user data
    suspend fun getUser(nickname: String): Result<User> {
        return try {
            val doc = users.document(nickname).get().await()
            val user = doc.toObject(User::class.java)

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete user
    suspend fun deleteUser(nickname: String): Result<Unit> {
        return try {
            users.document(nickname).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updating of FCM token
    suspend fun updateFcmToken(nickname: String, token: String): Result<Unit> {
        return try {
            users.document(nickname)
                .update(mapOf(
                    "fcmToken" to token,
                    "updatedAt" to System.currentTimeMillis()
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updating of password
    suspend fun updatePassword(
        nickname: String,
        oldPasswordHash: String,
        newPasswordHash: String
    ): Result<Unit> {
        return try {
            val doc = users.document(nickname).get().await()
            val user = doc.toObject(User::class.java)

            when {
                user == null -> Result.failure(Exception("User not found"))
                user.passwordHash != oldPasswordHash -> Result.failure(Exception("Wrong current password"))
                else -> {
                    users.document(nickname)
                        .update(mapOf(
                            "passwordHash" to newPasswordHash,
                            "updatedAt" to System.currentTimeMillis()
                        ))
                        .await()
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updating of API key
    suspend fun updateApiKey(
        nickname: String,
        passwordHash: String,
        newApiHash: String
    ): Result<Unit> {
        return try {
            val doc = users.document(nickname).get().await()
            val user = doc.toObject(User::class.java)

            when {
                user == null -> Result.failure(Exception("User not found"))
                user.passwordHash != passwordHash -> Result.failure(Exception("Wrong password"))
                else -> {
                    users.document(nickname)
                        .update(mapOf(
                            "apiHash" to newApiHash,
                            "updatedAt" to System.currentTimeMillis()
                        ))
                        .await()
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updating of nickname
    suspend fun updateNickname(oldNickname: String, newNickname: String): Result<Unit> {
        return try {
            // Check if new nickname is already taken
            val existingDoc = users.document(newNickname).get().await()
            if (existingDoc.exists()) {
                return Result.failure(Exception("Nickname already taken"))
            }

            // Get old user data
            val oldDoc = users.document(oldNickname).get().await()
            val userData = oldDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            // Create new document with new nickname
            val updatedUser = userData.copy(
                nickname = newNickname,
                updatedAt = System.currentTimeMillis()
            )

            // Save to new document and delete old one
            users.document(newNickname).set(updatedUser).await()
            users.document(oldNickname).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Verify password
    suspend fun verifyPassword(nickname: String, passwordHash: String): Result<Boolean> {
        return try {
            val doc = users.document(nickname).get().await()
            val user = doc.toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            Result.success(user.passwordHash == passwordHash)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}