package com.example.dmarketalert.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.AuthenticationViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enter)

        url = findViewById(R.id.textView_URL2)
        nickname_edit = findViewById(R.id.editText_nickname2)
        error_nickname = findViewById(R.id.textView_error_nickname)
        password_edit = findViewById(R.id.editText_password2)
        error_password = findViewById(R.id.textView_error_password)
        logIn = findViewById(R.id.button_logIn)
        create_account = findViewById(R.id.textView_create_account)

        url.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/faq#startUsingTradingAPI")
            )
            startActivity(intent)
        }

        fun validEdit(): Boolean {
            var isValid = true

            // check nickname
            if (nickname_edit.text.isNullOrEmpty()) {
                isValid = false
                error_nickname.visibility = View.VISIBLE
                error_nickname.alpha = 0f
                error_nickname.animate().alpha(1f).setDuration(600).start()
            } else {
                error_nickname.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        error_nickname.visibility = View.GONE
                    }
                    .start()
            }

            // check password
            if (password_edit.text.isNullOrEmpty()) {
                isValid = false
                error_password.visibility = View.VISIBLE
                error_password.alpha = 0f
                error_password.animate().alpha(1f).setDuration(600).start()
            } else {
                error_password.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        error_password.visibility = View.GONE
                    }
                    .start()
            }
            return isValid
        }

        viewModel.loginResult.observe(this) { (success, user) ->
            if (success && user != null) {

                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("nickname", user.nickname)
                    .apply()

                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    viewModel.saveFcmToken(user.nickname!!, token)
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid nickname or password", Toast.LENGTH_SHORT).show()
            }
        }

        logIn.setOnClickListener {
            val nickname = nickname_edit.text.toString().trim()
            val password = password_edit.text.toString().trim()

            if (nickname.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(nickname, password)
            }
        }

        create_account.setOnClickListener {
            val intent3 = Intent(this, RegistrationActivity::class.java)
            startActivity(intent3)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
