package com.example.dmarketalert.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dmarketalert.R
import com.example.dmarketalert.util.ValidationUtil
import com.example.dmarketalert.viewModel.AuthenticationViewModel
import com.example.dmarketalert.viewModel.state.AuthState
import com.google.firebase.messaging.FirebaseMessaging

class EnterActivity : AppCompatActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    private lateinit var url: TextView
    private lateinit var nickname_edit: EditText
    private lateinit var error_nickname: TextView
    private lateinit var password_edit: EditText
    private lateinit var error_password: TextView
    private lateinit var logIn: Button
    private lateinit var create_account: TextView
    private lateinit var progressBar: ProgressBar // ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enter)

        initViews()
        setupListeners()
        observeAuthState()
        setupInsets()
    }

    private fun initViews() {
        url = findViewById(R.id.textView_URL2)
        nickname_edit = findViewById(R.id.editText_nickname2)
        error_nickname = findViewById(R.id.textView_error_nickname)
        password_edit = findViewById(R.id.editText_password2)
        error_password = findViewById(R.id.textView_error_password)
        logIn = findViewById(R.id.button_logIn)
        create_account = findViewById(R.id.textView_create_account)
        // progressBar = findViewById(R.id.progressBar2) // ProgressBar
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

        logIn.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val nickname = nickname_edit.text.toString().trim()
            val password = password_edit.text.toString().trim()

            viewModel.login(nickname, password)
        }

        create_account.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    setLoading(true)
                }

                is AuthState.Success -> {
                    val user = state.user ?: return@observe

                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("nickname", user.nickname)
                        .apply()

                    // Update FCM token, when user entering again
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        viewModel.saveFcmToken(user.nickname, token)
                    }

                    setLoading(false)

                    Toast.makeText(
                        this,
                        "Welcome back, ${user.nickname}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

                is AuthState.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    setLoading(false)
                }
            }
        }
    }

    //Validate of fields for entering
    private fun validateInputs(): Boolean {
        var isValid = true

        // Checking fields
        if (nickname_edit.text.isNullOrBlank()) {
            error_nickname.text = "Please enter your nickname"
            ValidationUtil.showError(error_nickname)
            isValid = false
        } else {
            ValidationUtil.hideError(error_nickname)
        }

        if (password_edit.text.isNullOrBlank()) {
            error_password.text = "Please enter your password"
            ValidationUtil.showError(error_password)
            isValid = false
        } else {
            ValidationUtil.hideError(error_password)
        }

        return isValid
    }

    //Showing of loading indicators
    private fun setLoading(isLoading: Boolean) {
        // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        logIn.isEnabled = !isLoading
        nickname_edit.isEnabled = !isLoading
        password_edit.isEnabled = !isLoading

        logIn.text = if (isLoading) "Logging in..." else "Log In"
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