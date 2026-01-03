package com.example.dmarketalert.view

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import com.example.dmarketalert.R
import com.example.dmarketalert.view.Terms_of_use_and_privacy_police

class RegistrationActivity : AppCompatActivity() {
    private lateinit var url: TextView
    private lateinit var nickname_edit: EditText
    private lateinit var error_nickname: TextView
    private lateinit var password_edit: EditText
    private lateinit var error_password: TextView
    private lateinit var len_password: TextView
    private lateinit var api_edit: EditText
    private lateinit var error_API: TextView
    private lateinit var signUp: Button
    private lateinit var logIn: TextView
    private lateinit var acceptRules: CheckBox
    private lateinit var rules: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        url = findViewById(R.id.textView_URL)
        nickname_edit = findViewById(R.id.editText_nickname)
        error_nickname = findViewById(R.id.textView_error_nickname2)
        password_edit = findViewById(R.id.editText_password)
        error_password = findViewById(R.id.textView_error_password2)
        len_password = findViewById(R.id.textView_password_len)
        api_edit = findViewById(R.id.editText_API_key)
        error_API = findViewById(R.id.textView_error_API)
        signUp = findViewById(R.id.button_signUp)
        logIn = findViewById(R.id.textView_logIn)
        acceptRules = findViewById(R.id.checkBox)
        rules = findViewById(R.id.textView_rules2)

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
                error_nickname.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .start()
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
                error_password.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .start()
            } else {
                error_password.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        error_password.visibility = View.GONE
                    }
                    .start()
            }
            // check API
            if (api_edit.text.isNullOrEmpty()) {
                isValid = false
                error_API.visibility = View.VISIBLE
                error_API.alpha = 0f
                error_API.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .start()
            } else {
                error_API.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        error_API.visibility = View.GONE
                    }
                    .start()
            }
            return isValid
        }

        signUp.setOnClickListener {
            if (!acceptRules.isChecked) {
                Toast.makeText(this, "You need to accept the Terms of Use and Privacy Policy", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (nickname_edit.text.isNotEmpty() && password_edit.text.isNotEmpty() && api_edit.text.isNotEmpty()){
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("isLoggedIn", true).apply()

                val intent2 = Intent(this, MainActivity::class.java)
                startActivity(intent2)
                finish()
            } else {
                validEdit()
            }
        }

        logIn.setOnClickListener {
            val intent3 = Intent(this, EnterActivity::class.java)
            startActivity(intent3)
            finish()
        }

        rules.setOnClickListener {
            startActivity(Intent(this, Terms_of_use_and_privacy_police::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
