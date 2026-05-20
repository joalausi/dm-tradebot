package com.example.dmarketalert.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SettingsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "SettingsRepository"

    /**
     * Clear all user notification
     */
    suspend fun clearNotifications(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            val notificationsRef = db.collection("users")
                .document(userId)
                .collection("notifications")

            val documents = notificationsRef.get().await()

            // Delete all documents
            for (document in documents) {
                document.reference.delete().await()
            }

            Log.d(TAG, "Notifications cleared for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notifications", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all history about user targets
     */
    suspend fun clearHistory(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            val historyRef = db.collection("users")
                .document(userId)
                .collection("history")

            val documents = historyRef.get().await()

            // Delete all documents
            for (document in documents) {
                document.reference.delete().await()
            }

            Log.d(TAG, "History cleared for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all statistic(history + notification)
     */
    suspend fun clearAllStatistics(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            // Clear notification
            clearNotifications(userId).getOrThrow()

            // Clear history
            clearHistory(userId).getOrThrow()

            Log.d(TAG, "All statistics cleared for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all statistics", e)
            Result.failure(e)
        }
    }

    /**
     * Get count in notification
     */
    suspend fun getNotificationsCount(userId: String): Result<Int> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            val count = db.collection("users")
                .document(userId)
                .collection("notifications")
                .get()
                .await()
                .size()

            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notifications count", e)
            Result.failure(e)
        }
    }

    /**
     * Get count in history
     */
    suspend fun getHistoryCount(userId: String): Result<Int> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            val count = db.collection("users")
                .document(userId)
                .collection("history")
                .get()
                .await()
                .size()

            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history count", e)
            Result.failure(e)
        }
    }

    /**
     * Apply limit for notification (delete old, if limit is bigger, than number which user choose)
     */
    suspend fun applyNotificationLimit(userId: String, limit: Int): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            if (limit == 0) {
                return clearNotifications(userId)
            }

            val notificationsRef = db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

            val documents = notificationsRef.get().await()

            if (documents.size() > limit) {
                val toDelete = documents.documents.drop(limit)
                for (document in toDelete) {
                    document.reference.delete().await()
                }
                Log.d(TAG, "Applied notification limit: deleted ${toDelete.size} old notifications")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying notification limit", e)
            Result.failure(e)
        }
    }

    /**
     * Apply limit for history (delete old, if limit is bigger, than number which user choose)
     */
    suspend fun applyHistoryLimit(userId: String, limit: Int): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))

        return try {
            if (limit == 0) {
                // if limit = 0, clear all
                return clearHistory(userId)
            }

            val historyRef = db.collection("users")
                .document(userId)
                .collection("history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

            val documents = historyRef.get().await()

            if (documents.size() > limit) {
                val toDelete = documents.documents.drop(limit)
                for (document in toDelete) {
                    document.reference.delete().await()
                }
                Log.d(TAG, "Applied history limit: deleted ${toDelete.size} old records")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying history limit", e)
            Result.failure(e)
        }
    }
}