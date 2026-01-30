package com.example.dmarketalert.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dmarketalert.R
import com.example.dmarketalert.util.ValidationUtil
import com.example.dmarketalert.viewModel.AuthenticationViewModel

class CheckPasswordActivity : AppCompatActivity() {
    private lateinit var back: ImageView
    private lateinit var editTextCheckPassword: EditText
    private lateinit var errorPassword: TextView
    private lateinit var buttonCheckPassword: Button

    private val viewModel: AuthenticationViewModel by viewModels()
    private var nickname: String? = null
    private var nextScreen: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_password)

        initViews()
        loadUserData()
        setupClickListeners()
        setupWindowInsets()
    }

    private fun initViews() {
        back = findViewById(R.id.imageView_back5)
        editTextCheckPassword = findViewById(R.id.editText_check_password)
        errorPassword = findViewById(R.id.textView_error_password_check)
        buttonCheckPassword = findViewById(R.id.button_check_password)
    }

    private fun loadUserData() {
        nickname = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)

        nextScreen = intent.getStringExtra("next_screen")

        if (nickname == null || nextScreen == null) {
            Toast.makeText(this, "Error: Invalid request", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        buttonCheckPassword.setOnClickListener {
            handleCheckPassword()
        }

        back.setOnClickListener{
            finish()
        }
    }

    private fun handleCheckPassword() {
        val password = editTextCheckPassword.text.toString().trim()

        if (password.isBlank()) {
            errorPassword.text = "Please, enter your current password"
            ValidationUtil.showError(errorPassword)
            return
        }

        ValidationUtil.hideError(errorPassword)
        setLoading(true)

        viewModel.verifyPassword(nickname!!, password) { result ->
            setLoading(false)
            result
                .onSuccess { isValid ->
                    if (isValid) {
                        navigateToNextScreen(password)
                    } else {
                        errorPassword.text = "Incorrect password"
                        ValidationUtil.showError(errorPassword)
                    }
                }
                .onFailure { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun navigateToNextScreen(verifiedPassword: String) {
        val intent = when (nextScreen) {
            "password" -> Intent(this, ChangePassword::class.java)
            "api" -> Intent(this, ChangeAPI::class.java)
            else -> {
                Toast.makeText(this, "Error: Unknown screen", Toast.LENGTH_SHORT).show()
                return
            }
        }

        intent.putExtra("verified_password", verifiedPassword)
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        buttonCheckPassword.isEnabled = !isLoading
        editTextCheckPassword.isEnabled = !isLoading
        buttonCheckPassword.text = if (isLoading) "Verifying..." else "Continue"
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}