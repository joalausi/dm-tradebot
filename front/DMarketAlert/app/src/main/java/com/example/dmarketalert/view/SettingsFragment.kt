package com.example.dmarketalert.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import com.example.dmarketalert.R

private lateinit var gitHub: CardView
private lateinit var disclaimer: CardView
private lateinit var tech_support: CardView
private lateinit var license: CardView
private lateinit var rules: CardView
class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        gitHub = view.findViewById(R.id.CardView_license_in_profile)
        disclaimer = view.findViewById(R.id.CardView_disclamer)

        tech_support = view.findViewById(R.id.CardView_tech_support)
        license = view.findViewById(R.id.CardView_license)
        rules = view.findViewById(R.id.CardView_rules)

        gitHub.setOnClickListener {
            val url1 = "https://github.com/joalausi/dm-tradebot.git"
            val intent9 = Intent(Intent.ACTION_VIEW, Uri.parse(url1))
            startActivity(intent9)
        }

        tech_support.setOnClickListener {
            val url2 = "https://mail.google.com/mail/u/0/#inbox?compose=jrjtXVXdPMxgqnjgXVKgHCbpVhhGPzLJbtrJlvwTMSBhqwmLQRRpcKhzTStntLkhcBxkDfTq"
            val intent10 = Intent(Intent.ACTION_VIEW, Uri.parse(url2))
            startActivity(intent10)
        }

        license.setOnClickListener {
            startActivity(Intent(requireContext(), License::class.java))
        }

        rules.setOnClickListener {
            startActivity(Intent(requireContext(), Terms_of_use_and_privacy_police::class.java))
        }

        return view
    }
}