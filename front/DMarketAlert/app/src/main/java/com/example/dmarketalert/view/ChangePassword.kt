package com.example.dmarketalert.view

import android.content.Context
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

class ChangePassword : AppCompatActivity() {

    private lateinit var back: ImageView
    private lateinit var currentPassword: TextView
    private lateinit var editTextNewPassword: EditText
    private lateinit var errorNewPassword: TextView
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var errorConfirmPassword: TextView
    private lateinit var buttonChangePassword: Button

    private val viewModel: AuthenticationViewModel by viewModels()
    private var nickname: String? = null
    private var verifiedPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        initViews()
        loadUserData()
        setupClickListeners()
        setupWindowInsets()
    }

    private fun initViews() {
        back = findViewById(R.id.imageView_back4)
        currentPassword = findViewById(R.id.textView_current_password)
        editTextNewPassword = findViewById(R.id.editText_change_password)
        errorNewPassword = findViewById(R.id.textView_error_password_change)
        editTextConfirmPassword = findViewById(R.id.editText_confirm_password)
        errorConfirmPassword = findViewById(R.id.textView_error_password_confirm)
        buttonChangePassword = findViewById(R.id.button_change_password)
    }

    private fun loadUserData() {
        nickname = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)

        verifiedPassword = intent.getStringExtra("verified_password")

        if (nickname == null || verifiedPassword == null) {
            Toast.makeText(this, "Error: Invalid request", Toast.LENGTH_SHORT).show()
            finish()
        }

        currentPassword.text = verifiedPassword
    }

    private fun setupClickListeners() {
        back.setOnClickListener {
            finish()
        }

        buttonChangePassword.setOnClickListener {
            handleChangePassword()
        }
    }

    private fun handleChangePassword() {
        val newPassword = editTextNewPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()

        var isValid = true

        // Validate new password
        val newPasswordError = ValidationUtil.validatePassword(newPassword)
        if (newPasswordError != null) {
            errorNewPassword.text = newPasswordError
            ValidationUtil.showError(errorNewPassword)
            isValid = false
        } else {
            ValidationUtil.hideError(errorNewPassword)
        }

        // Check if passwords match
        if (newPassword != confirmPassword) {
            errorConfirmPassword.text = "Passwords do not match"
            ValidationUtil.showError(errorConfirmPassword)
            isValid = false
        } else {
            ValidationUtil.hideError(errorConfirmPassword)
        }

        if (!isValid) return

        // Check if new password is different from current
        if (newPassword == verifiedPassword) {
            Toast.makeText(this, "New password is the same as current", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewModel.updatePassword(nickname!!, verifiedPassword!!, newPassword) { result ->
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        buttonChangePassword.isEnabled = !isLoading
        editTextNewPassword.isEnabled = !isLoading
        editTextConfirmPassword.isEnabled = !isLoading
        buttonChangePassword.text = if (isLoading) "Changing..." else "Aplly"
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}