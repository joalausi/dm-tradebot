package com.example.dmarketalert.view

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.content.ContextCompat
import com.example.dmarketalert.R
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity() {
    private lateinit var bottomNavigationView1: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // FCM-token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TEST", "Error", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TEST", "Your FCM token: $token")
        }

        setupNotificationChannel()

        bottomNavigationView1 = findViewById(R.id.bottomnavigatonview)
        bottomNavigationView1.itemBackground = null
        bottomNavigationView1.backgroundTintList = null
        bottomNavigationView1.itemIconTintList =
            ContextCompat.getColorStateList(this, R.color.nav_item_color)
        bottomNavigationView1.itemTextColor =
            ContextCompat.getColorStateList(this, R.color.white)

        bottomNavigationView1.selectedItemId = R.id.bottom_home

        bottomNavigationView1.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_history -> { replaceFragments(HistoryFragment()); true }
                R.id.bottom_notification -> { replaceFragments(NotificationFragment()); true }
                R.id.bottom_home -> { replaceFragments(HomeFragment()); true }
                R.id.bottom_settings -> { replaceFragments(SettingsFragment()); true }
                R.id.bottom_profile -> { replaceFragments(ProfileFragment()); true }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val notificationManager =
                getSystemService(android.app.NotificationManager::class.java)

            val soundChannel = android.app.NotificationChannel(
                "dm_alert_sound",
                "DM Alert (Sound)",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Price alerts with sound"
                enableVibration(false)
            }

            val vibrationChannel = android.app.NotificationChannel(
                "dm_alert_vibration",
                "DM Alert (Vibration)",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Price alerts with vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val silentChannel = android.app.NotificationChannel(
                "dm_alert_silent",
                "DM Alert (Silent)",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent price alerts"
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(soundChannel)
            notificationManager.createNotificationChannel(vibrationChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    private fun replaceFragments(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}