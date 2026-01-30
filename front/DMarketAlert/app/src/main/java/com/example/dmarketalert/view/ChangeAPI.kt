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

class ChangeAPI : AppCompatActivity() {

    private lateinit var back: ImageView
    private lateinit var currentAPIText: TextView
    private lateinit var editTextNewAPI: EditText
    private lateinit var errorAPI: TextView
    private lateinit var buttonChangeAPI: Button

    private val viewModel: AuthenticationViewModel by viewModels()
    private var nickname: String? = null
    private var verifiedPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_api)

        initViews()
        loadUserData()
        setupClickListeners()
        setupWindowInsets()
    }

    private fun initViews() {
        back = findViewById(R.id.imageView_back2)
        currentAPIText = findViewById(R.id.textView_current_api)
        editTextNewAPI = findViewById(R.id.editText_change_API)
        errorAPI = findViewById(R.id.textView_error_api_change)
        buttonChangeAPI = findViewById(R.id.button_change_api)
    }

    private fun loadUserData() {
        nickname = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)

        verifiedPassword = intent.getStringExtra("verified_password")

        if (nickname == null || verifiedPassword == null) {
            Toast.makeText(this, "Error: Invalid request", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        back.setOnClickListener {
            finish()
        }

        buttonChangeAPI.setOnClickListener {
            handleChangeAPI()
        }
    }

    private fun handleChangeAPI() {
        val newAPI = editTextNewAPI.text.toString().trim()

        // Validate
        val error = ValidationUtil.validateApiKey(newAPI)
        if (error != null) {
            errorAPI.text = error
            ValidationUtil.showError(errorAPI)
            return
        }

        ValidationUtil.hideError(errorAPI)
        setLoading(true)

        viewModel.updateApiKey(nickname!!, verifiedPassword!!, newAPI) { result ->
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this, "API key changed successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadCurrentAPI() {
        viewModel.loadUser(nickname!!)

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Success -> {
                    val user = state.user ?: return@observe

                    val maskedApi = if (user.apiHash.length > 8) {
                        "********${user.apiHash.takeLast(8)}"
                    } else {
                        user.apiHash
                    }

                    currentAPIText.text = maskedApi
                }

                is AuthState.Error -> {
                    currentAPIText.text = "********"
                }

                else -> Unit
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        buttonChangeAPI.isEnabled = !isLoading
        editTextNewAPI.isEnabled = !isLoading
        buttonChangeAPI.text = if (isLoading) "Changing..." else "Apply"
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}