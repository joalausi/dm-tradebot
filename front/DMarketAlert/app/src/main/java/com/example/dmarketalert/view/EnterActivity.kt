package com.example.dmarketalert.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    // UI elements
    private lateinit var url: TextView
    private lateinit var nicknameEdit: EditText
    private lateinit var errorNickname: TextView
    private lateinit var passwordEdit: EditText
    private lateinit var errorPassword: TextView
    private lateinit var logInButton: Button
    private lateinit var createAccountText: TextView
    private lateinit var progressBar: ProgressBar

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enter)

        initViews()
        setupWindowInsets()
        observeAuthState()
        setupClickListeners()
    }

    private fun initViews() {
        url = findViewById(R.id.textView_URL2)
        nicknameEdit = findViewById(R.id.editText_nickname2)
        errorNickname = findViewById(R.id.textView_error_nickname)
        passwordEdit = findViewById(R.id.editText_password2)
        errorPassword = findViewById(R.id.textView_error_password)
        logInButton = findViewById(R.id.button_logIn)
        createAccountText = findViewById(R.id.textView_create_account)
        // progressBar = findViewById(R.id.progressBar2)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> setLoadingState(true)

                is AuthState.Success -> {
                    val user = state.user ?: return@observe
                    setLoadingState(false)
                    saveUserSession(user.nickname)
                    updateFcmToken(user.nickname)
                    showToast("Welcome back, ${user.nickname}!")
                    navigateToMain()
                }

                is AuthState.Error -> {
                    setLoadingState(false)
                    showToast(state.message, Toast.LENGTH_LONG)
                }

                else -> setLoadingState(false)
            }
        }
    }

    private fun setupClickListeners() {
        url.setOnClickListener {
            openUrl("https://dmarket.com/faq#startUsingTradingAPI")
        }

        logInButton.setOnClickListener {
            handleLoginClick()
        }

        createAccountText.setOnClickListener {
            navigateTo(RegistrationActivity::class.java)
        }
    }

    private fun handleLoginClick() {
        if (!validateFields()) return

        val nickname = nicknameEdit.text.toString().trim()
        val password = passwordEdit.text.toString().trim()

        viewModel.login(nickname, password)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (nicknameEdit.text.isNullOrBlank()) {
            errorNickname.text = "Please enter your nickname"
            ValidationUtil.showError(errorNickname)
            isValid = false
        } else {
            ValidationUtil.hideError(errorNickname)
        }

        if (passwordEdit.text.isNullOrBlank()) {
            errorPassword.text = "Please enter your password"
            ValidationUtil.showError(errorPassword)
            isValid = false
        } else {
            ValidationUtil.hideError(errorPassword)
        }

        return isValid
    }

    private fun setLoadingState(isLoading: Boolean) {
        // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        logInButton.isEnabled = !isLoading
        nicknameEdit.isEnabled = !isLoading
        passwordEdit.isEnabled = !isLoading
        logInButton.text = if (isLoading) "Logging in..." else "Log In"
    }

    private fun saveUserSession(nickname: String) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", true)
            .putString("nickname", nickname)
            .apply()
    }

    private fun updateFcmToken(nickname: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModel.saveFcmToken(nickname, token)
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateTo(activity: Class<*>) {
        startActivity(Intent(this, activity))
        finish()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}