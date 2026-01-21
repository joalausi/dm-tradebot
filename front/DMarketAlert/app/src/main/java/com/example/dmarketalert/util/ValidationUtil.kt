package com.example.dmarketalert.util

import android.view.View
import android.widget.TextView

object ValidationUtil {

    //Show errors of animation
    fun showError(view: TextView) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(600).start()
    }

    // Hide animation error
    fun hideError(view: TextView) {
        view.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    //Operation with nickname
    fun validateNickname(nickname: String): String? {
        return when {
            nickname.isBlank() -> "Nickname cannot be empty"
            nickname.length < 3 -> "Nickname must be at least 3 characters"
            nickname.length > 20 -> "Nickname must be less than 20 characters"
            !nickname.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Nickname can only contain letters, numbers and underscore"
            else -> null
        }
    }

    //Validate of password
    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 8 -> "Password must be at least 8 characters"
            password.length > 128 -> "Password is too long"
            !password.any { it.isDigit() } -> "Password must contain at least one digit"
            !password.any { it.isLetter() } -> "Password must contain at least one letter"
            else -> null
        }
    }

    //Validate of API key
    fun validateApiKey(apiKey: String): String? {
        return when {
            apiKey.isBlank() -> "API key cannot be empty"
            apiKey.length < 10 -> "API key seems too short"
            else -> null
        }
    }
}