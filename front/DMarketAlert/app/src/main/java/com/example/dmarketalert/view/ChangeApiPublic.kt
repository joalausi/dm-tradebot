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
import com.example.dmarketalert.viewModel.state.AuthState

class ChangeApiPublic : AppCompatActivity() {
    private lateinit var back: ImageView
    private lateinit var currentPublicApiText: TextView
    private lateinit var editTextNewPublicApi: EditText
    private lateinit var errorPublicApi: TextView
    private lateinit var buttonChangePublicApi: Button
    private val viewModel: AuthenticationViewModel by viewModels()
    private var nickname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_api_public)

        initViews()
        setupWindowInsets()
        setupClickListeners()
        observeViewModel()

        loadUserData()
    }

    private fun initViews() {
        back = findViewById(R.id.imageView_back7)
        currentPublicApiText = findViewById(R.id.textView_current_public_api)
        editTextNewPublicApi = findViewById(R.id.editText_new_public_API)
        errorPublicApi = findViewById(R.id.textView_error_public_API)
        buttonChangePublicApi = findViewById(R.id.button_public_API)
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> setLoading(true)

                is AuthState.Success -> {
                    setLoading(false)
                    val user = state.user
                    if (user != null) {
                        currentPublicApiText.text = user.apiPublic.ifBlank { "Not set" }
                    }
                }

                is AuthState.Error -> {
                    setLoading(false)
                    Toast.makeText(this, "Failed to load data: ${state.message}", Toast.LENGTH_LONG).show()
                }

                else -> setLoading(false)
            }
        }
    }

    private fun setupClickListeners() {
        back.setOnClickListener {
            finish()
        }

        buttonChangePublicApi.setOnClickListener {
            handleChangePublicApi()
        }
    }

    private fun handleChangePublicApi() {
        val newPublicApi = editTextNewPublicApi.text.toString().trim()

        val error = ValidationUtil.validatePublicApiKey(newPublicApi)
        if (error != null) {
            errorPublicApi.text = error
            ValidationUtil.showError(errorPublicApi)
            return
        }

        if (newPublicApi == currentPublicApiText.text.toString()) {
            Toast.makeText(this, "New key is the same as current", Toast.LENGTH_SHORT).show()
            return
        }

        ValidationUtil.hideError(errorPublicApi)
        setLoading(true)

        viewModel.updatePublicApiKey(nickname!!, newPublicApi) { result ->
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this, "Public API key changed successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        buttonChangePublicApi.isEnabled = !isLoading
        editTextNewPublicApi.isEnabled = !isLoading
        buttonChangePublicApi.text = if (isLoading) "Changing..." else "Apply"
    }

    private fun loadUserData() {
        nickname = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)

        if (nickname == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadUser(nickname!!)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}