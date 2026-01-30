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

class ChangeNickname : AppCompatActivity() {

    private lateinit var back: ImageView
    private lateinit var currentNicknameText: TextView
    private lateinit var editTextChangeNickname: EditText
    private lateinit var errorNickname: TextView
    private lateinit var buttonChangeNickname: Button

    private val viewModel: AuthenticationViewModel by viewModels()
    private var oldNickname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_nickname)

        initViews()
        loadCurrentNickname()
        setupClickListeners()
        setupWindowInsets()
    }

    private fun initViews() {
        back = findViewById(R.id.imageView_back3)
        currentNicknameText = findViewById(R.id.textView_current_nickname)
        editTextChangeNickname = findViewById(R.id.editText_change_nickname)
        errorNickname = findViewById(R.id.textView_error_nickname_change)
        buttonChangeNickname = findViewById(R.id.button_change_nickname)
    }

    private fun loadCurrentNickname() {
        oldNickname = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)

        if (oldNickname == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentNicknameText.text = oldNickname
    }

    private fun setupClickListeners() {
        back.setOnClickListener {
            finish()
        }

        buttonChangeNickname.setOnClickListener {
            handleChangeNickname()
        }
    }

    private fun handleChangeNickname() {
        val newNickname = editTextChangeNickname.text.toString().trim()

        // Validate
        val error = ValidationUtil.validateNickname(newNickname)
        if (error != null) {
            errorNickname.text = error
            ValidationUtil.showError(errorNickname)
            return
        }

        if (newNickname == oldNickname) {
            Toast.makeText(this, "New nickname is the same as current", Toast.LENGTH_SHORT).show()
            return
        }

        ValidationUtil.hideError(errorNickname)
        setLoading(true)

        viewModel.updateNickname(oldNickname!!, newNickname) { result ->
            setLoading(false)
            result
                .onSuccess {
                    // Update SharedPreferences
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("nickname", newNickname)
                        .apply()

                    Toast.makeText(this, "Nickname changed successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        buttonChangeNickname.isEnabled = !isLoading
        editTextChangeNickname.isEnabled = !isLoading
        buttonChangeNickname.text = if (isLoading) "Changing..." else "Apply"
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}