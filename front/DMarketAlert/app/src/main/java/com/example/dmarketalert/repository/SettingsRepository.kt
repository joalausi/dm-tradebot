package com.example.dmarketalert.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

class SettingsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "SettingsRepository"

    // ===== PRIVATE HELPING FUNCTION =====

    private suspend fun clearCollection(userId: String, collectionName: String): Result<Unit> {

        if (userId.isBlank()) return Result.failure(
            IllegalArgumentException("User ID cannot be empty")
        )

        return try {
            val collectionRef = db.collection("users")
                .document(userId)
                .collection(collectionName)

            val documents = collectionRef.get().await()

            if (documents.isEmpty) {
                return Result.success(Unit)
            }
            val batchSize = 500
            val docList = documents.documents
            docList.chunked(batchSize).forEach { chunk ->
                val batch: WriteBatch = db.batch()

                chunk.forEach { document ->
                    batch.delete(document.reference)
                }

                batch.commit().await()
            }

            Log.d(TAG, "Collection '$collectionName' cleared for user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing collection '$collectionName'", e)
            Result.failure(e)
        }
    }

    // ===== PUBLIC FUNCTIONS =====

    suspend fun clearNotifications(userId: String): Result<Unit> =
        clearCollection(userId, "notifications")

    suspend fun clearHistory(userId: String): Result<Unit> =
        clearCollection(userId, "history")

    suspend fun clearAllStatistics(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(
            IllegalArgumentException("User ID cannot be empty")
        )
        return try {
            clearNotifications(userId).getOrThrow()
            clearHistory(userId).getOrThrow()
            Log.d(TAG, "All statistics cleared for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all statistics", e)
            Result.failure(e)
        }
    }

    suspend fun getNotificationsCount(userId: String): Result<Int> {
        if (userId.isBlank()) return Result.failure(
            IllegalArgumentException("User ID cannot be empty")
        )
        return try {
            val count = db.collection("users")
                .document(userId)
                .collection("notifications")
                .get().await().size()
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notifications count", e)
            Result.failure(e)
        }
    }

    suspend fun getHistoryCount(userId: String): Result<Int> {
        if (userId.isBlank()) return Result.failure(
            IllegalArgumentException("User ID cannot be empty")
        )
        return try {
            val count = db.collection("users")
                .document(userId)
                .collection("history")
                .get().await().size()
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history count", e)
            Result.failure(e)
        }
    }

    // ===== LIMITS =====

    suspend fun applyNotificationLimit(userId: String, limit: Int): Result<Unit> {
        if (userId.isBlank()) return Result.failure(
            IllegalArgumentException("User ID cannot be empty")
        )
        if (limit == 0) return clearNotifications(userId)

        return try {
            val documents = db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()

            deleteExcess(documents.documents, limit)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying notification limit", e)
            Result.failure(e)
        }
    }

    suspend fun applyHistoryLimit(userId: String, limit: Int): Result<Unit> {
        if (userId.isBlank()) return Result.failure(
            IllegalArgumentException("User ID cannot be empty")
        )
        if (limit == 0) return clearHistory(userId)

        return try {
            val documents = db.collection("users")
                .document(userId)
                .collection("history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()

            deleteExcess(documents.documents, limit)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying history limit", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteExcess(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        limit: Int
    ) {
        if (documents.size <= limit) return

        val toDelete = documents.drop(limit)

        toDelete.chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }

        Log.d(TAG, "Deleted ${toDelete.size} excess documents")
    }
}