package com.example.dmarketalert

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils // ✅ ВИПРАВЛЕНО: Правильний імпорт
import android.widget.ImageView

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val bot = findViewById<ImageView>(R.id.bot_logo)
        val arrows = findViewById<ImageView>(R.id.arrows_logo)

        val animBot = AnimationUtils.loadAnimation(this, R.anim.bot_animation)
        val animArrows = AnimationUtils.loadAnimation(this, R.anim.arrows_animation)

        bot.startAnimation(animBot)
        arrows.startAnimation(animArrows)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}