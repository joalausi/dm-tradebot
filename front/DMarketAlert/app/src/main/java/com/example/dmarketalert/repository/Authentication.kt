package com.example.dmarketalert.repository

import com.example.dmarketalert.model.User
import com.google.firebase.firestore.FirebaseFirestore

class Authentication {

    private val firestore = FirebaseFirestore.getInstance()
    private val userCollection = firestore.collection("users")

    fun registerUser(user: User, onResult: (Boolean, String?) -> Unit) {
        val nickname = user.nickname ?: return onResult(false, "Invalid nickname")

        userCollection.document(nickname).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onResult(false, "Nickname is taken")
                } else {
                    userCollection.document(nickname).set(user)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { onResult(false, it.message) }
                }
            }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun loginUser(
        nickname: String,
        password: String,
        onResult: (Boolean, User?) -> Unit
    ) {
        userCollection.document(nickname).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(false, null)
                    return@addOnSuccessListener
                }

                val user = doc.toObject(User::class.java)
                if (user?.password == password) {
                    onResult(true, user)
                } else {
                    onResult(false, null)
                }
            }
            .addOnFailureListener {
                onResult(false, null)
            }
    }

    fun updateFcmToken(nickname: String, token: String) {
        userCollection.document(nickname)
            .update("fcmToken", token)
    }
}