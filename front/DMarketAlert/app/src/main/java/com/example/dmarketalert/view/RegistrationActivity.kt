package com.example.dmarketalert.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dmarketalert.R
import com.example.dmarketalert.databinding.ActivityRegistrationBinding
import com.example.dmarketalert.util.ValidationUtil
import com.example.dmarketalert.viewModel.AuthenticationViewModel
import com.example.dmarketalert.viewModel.state.AuthState
import com.google.firebase.messaging.FirebaseMessaging

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding
    private val viewModel: AuthenticationViewModel by viewModels()

    private lateinit var url: TextView
    private lateinit var nickname_edit: EditText
    private lateinit var error_nickname: TextView
    private lateinit var password_edit: EditText
    private lateinit var error_password: TextView
    private lateinit var api_edit: EditText
    private lateinit var error_API: TextView
    private lateinit var signUp: Button
    private lateinit var logIn: TextView
    private lateinit var acceptRules: CheckBox
    private lateinit var rules: TextView
    private lateinit var progressBar: ProgressBar // Progress bar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupListeners()
        observeAuthState()
        setupInsets()
    }

    private fun initViews() {
        url = findViewById(R.id.textView_URL)
        nickname_edit = findViewById(R.id.editText_nickname)
        error_nickname = findViewById(R.id.textView_error_nickname2)
        password_edit = findViewById(R.id.editText_password)
        error_password = findViewById(R.id.textView_error_password2)
        api_edit = findViewById(R.id.editText_API_key)
        error_API = findViewById(R.id.textView_error_API)
        signUp = findViewById(R.id.button_signUp)
        logIn = findViewById(R.id.textView_logIn)
        acceptRules = findViewById(R.id.checkBox)
        rules = findViewById(R.id.textView_rules2)
        // progressBar = findViewById(R.id.progressBar) // Need to add a progress bar
    }

    private fun setupListeners() {
        url.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://dmarket.com/faq#startUsingTradingAPI")
                )
            )
        }

        signUp.setOnClickListener {
            if (!acceptRules.isChecked) {
                Toast.makeText(
                    this,
                    "You need to accept the Terms of Use and Privacy Policy",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (!validateInputs()) return@setOnClickListener

            val nickname = nickname_edit.text.toString().trim()
            val password = password_edit.text.toString().trim()
            val api = api_edit.text.toString().trim()

            // Getting of FCM token and send to the registration
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                viewModel.register(
                    nickname = nickname,
                    password = password,
                    api = api
                )

                // Saving of FCM token
                viewModel.saveFcmToken(nickname, token)
            }.addOnFailureListener {
                // If token not taken, anyway registered
                viewModel.register(
                    nickname = nickname,
                    password = password,
                    api = api
                )
            }
        }

        logIn.setOnClickListener {
            startActivity(Intent(this, EnterActivity::class.java))
            finish()
        }

        rules.setOnClickListener {
            startActivity(Intent(this, Terms_of_use_and_privacy_police::class.java))
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    setLoading(true)
                }

                is AuthState.Success -> {
                    setLoading(false)

                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("nickname", state.user?.nickname)
                        .apply()

                    Toast.makeText(
                        this,
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

                is AuthState.Error -> {
                    setLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }

                else -> {
                    setLoading(false)
                }
            }
        }
    }

    //Validate of all fields
    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate of nickname
        val nicknameError = ValidationUtil.validateNickname(
            nickname_edit.text.toString().trim()
        )
        if (nicknameError != null) {
            error_nickname.text = nicknameError
            ValidationUtil.showError(error_nickname)
            isValid = false
        } else {
            ValidationUtil.hideError(error_nickname)
        }

        // Validate of password
        val passwordError = ValidationUtil.validatePassword(
            password_edit.text.toString().trim()
        )
        if (passwordError != null) {
            error_password.text = passwordError
            ValidationUtil.showError(error_password)
            isValid = false
        } else {
            ValidationUtil.hideError(error_password)
        }

        // Validate of API
        val apiError = ValidationUtil.validateApiKey(
            api_edit.text.toString().trim()
        )
        if (apiError != null) {
            error_API.text = apiError
            ValidationUtil.showError(error_API)
            isValid = false
        } else {
            ValidationUtil.hideError(error_API)
        }

        return isValid
    }

    //Show/hide progress bar
    private fun setLoading(isLoading: Boolean) {
        // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        signUp.isEnabled = !isLoading
        nickname_edit.isEnabled = !isLoading
        password_edit.isEnabled = !isLoading
        api_edit.isEnabled = !isLoading
        acceptRules.isEnabled = !isLoading

        signUp.text = if (isLoading) "Registering..." else "Sign Up"
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }
}