package com.example.dmarketalert.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dmarketalert.R
import com.example.dmarketalert.util.ValidationUtil
import com.example.dmarketalert.viewModel.AuthenticationViewModel
import com.example.dmarketalert.viewModel.state.AuthState
import com.google.firebase.messaging.FirebaseMessaging

class RegistrationActivity : AppCompatActivity() {

    // UI elements
    private lateinit var urlText: TextView
    private lateinit var nicknameEdit: EditText
    private lateinit var errorNickname: TextView
    private lateinit var passwordEdit: EditText
    private lateinit var errorPassword: TextView
    private lateinit var apiPublicEdit: EditText
    private lateinit var errorApiPublic: TextView
    private lateinit var apiEdit: EditText
    private lateinit var errorApi: TextView
    private lateinit var signUpButton: Button
    private lateinit var logInText: TextView
    private lateinit var acceptRulesCheckBox: CheckBox
    private lateinit var rulesText: TextView
    private lateinit var progressBar: ProgressBar

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        initViews()
        setupWindowInsets()
        observeAuthState()
        setupClickListeners()
    }

    private fun initViews() {
        urlText = findViewById(R.id.textView_URL)
        nicknameEdit = findViewById(R.id.editText_nickname)
        errorNickname = findViewById(R.id.textView_error_nickname2)
        passwordEdit = findViewById(R.id.editText_password)
        errorPassword = findViewById(R.id.textView_error_password2)
        apiPublicEdit = findViewById(R.id.editText_API_Public)
        errorApiPublic = findViewById(R.id.textView_error_API_public)
        apiEdit = findViewById(R.id.editText_API_key)
        errorApi = findViewById(R.id.textView_error_API)
        signUpButton = findViewById(R.id.button_signUp)
        logInText = findViewById(R.id.textView_logIn)
        acceptRulesCheckBox = findViewById(R.id.checkBox)
        rulesText = findViewById(R.id.textView_rules2)
        // progressBar = findViewById(R.id.progressBar)
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
                    setLoadingState(false)
                    saveUserSession(state.user?.nickname)
                    showToast("Registration successful!")
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
        urlText.setOnClickListener {
            openUrl("https://dmarket.com/faq#startUsingTradingAPI")
        }

        signUpButton.setOnClickListener {
            handleSignUpClick()
        }

        logInText.setOnClickListener {
            navigateTo(EnterActivity::class.java)
        }

        rulesText.setOnClickListener {
            startActivity(Intent(this, Terms_of_use_and_privacy_police::class.java))
        }
    }

    private fun handleSignUpClick() {
        if (!acceptRulesCheckBox.isChecked) {
            showToast("You need to accept the Terms of Use and Privacy Policy", Toast.LENGTH_LONG)
            return
        }

        if (!validateAllFields()) return

        val nickname = nicknameEdit.text.toString().trim()
        val password = passwordEdit.text.toString().trim()
        val apiPublic = apiPublicEdit.text.toString().trim()
        val api = apiEdit.text.toString().trim()

        registerWithFcm(nickname, password, apiPublic, api)
    }

    private fun validateAllFields(): Boolean {
        var isValid = true

        val nicknameError = ValidationUtil.validateNickname(nicknameEdit.text.toString().trim())
        if (nicknameError != null) {
            errorNickname.text = nicknameError
            ValidationUtil.showError(errorNickname)
            isValid = false
        } else {
            ValidationUtil.hideError(errorNickname)
        }

        val passwordError = ValidationUtil.validatePassword(passwordEdit.text.toString().trim())
        if (passwordError != null) {
            errorPassword.text = passwordError
            ValidationUtil.showError(errorPassword)
            isValid = false
        } else {
            ValidationUtil.hideError(errorPassword)
        }

        val apiPublicError = ValidationUtil.validatePublicApiKey(apiPublicEdit.text.toString().trim())
        if (apiPublicError != null) {
            errorApiPublic.text = apiPublicError
            ValidationUtil.showError(errorApiPublic)
            isValid = false
        } else {
            ValidationUtil.hideError(errorApiPublic)
        }

        val apiError = ValidationUtil.validateApiKey(apiEdit.text.toString().trim())
        if (apiError != null) {
            errorApi.text = apiError
            ValidationUtil.showError(errorApi)
            isValid = false
        } else {
            ValidationUtil.hideError(errorApi)
        }

        return isValid
    }

    private fun registerWithFcm(nickname: String, password: String, apiPublic: String, api: String) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                viewModel.register(nickname, password, apiPublic, api)
                viewModel.saveFcmToken(nickname, token)
            }
            .addOnFailureListener {
                viewModel.register(nickname, password, apiPublic, api)
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        signUpButton.isEnabled = !isLoading
        nicknameEdit.isEnabled = !isLoading
        passwordEdit.isEnabled = !isLoading
        apiPublicEdit.isEnabled = !isLoading
        apiEdit.isEnabled = !isLoading
        acceptRulesCheckBox.isEnabled = !isLoading
        signUpButton.text = if (isLoading) "Registering..." else "Sign Up"
    }

    private fun saveUserSession(nickname: String?) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", true)
            .putString("nickname", nickname)
            .apply()
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