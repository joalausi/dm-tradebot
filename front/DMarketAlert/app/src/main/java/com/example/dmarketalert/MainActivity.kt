package com.example.dmarketalert

import androidx.fragment.app.Fragment
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView1: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // initialization of navigation view
        bottomNavigationView1 = findViewById(R.id.bottomnavigatonview)
        bottomNavigationView1.itemBackground = null
        bottomNavigationView1.backgroundTintList = null
        bottomNavigationView1.itemIconTintList = ContextCompat.getColorStateList(this, R.color.nav_item_color)
        bottomNavigationView1.itemTextColor = ContextCompat.getColorStateList(this, R.color.white)

        //fragment by default
        bottomNavigationView1.selectedItemId = R.id.bottom_home

        //replace fragments of navigation menu
        bottomNavigationView1.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_history -> {
                    replaceFragments(HistoryFragment())
                    true // 'true' means you've consumed the event
                }
                R.id.bottom_notification -> {
                    replaceFragments(NotificationFragment())
                    true
                }
                R.id.bottom_home -> {
                    replaceFragments(HomeFragment())
                    true
                }
                R.id.bottom_settings -> {
                    replaceFragments(SettingsFragment())
                    true
                }
                R.id.bottom_profile -> {
                    replaceFragments(ProfileFragment())
                    true
                }
                else -> false // Return 'false' if the item ID is not handled
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun replaceFragments(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.frame_container, fragment)
            .commit()
    }
}