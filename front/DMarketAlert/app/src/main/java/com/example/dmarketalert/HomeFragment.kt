package com.example.dmarketalert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class HomeFragment : Fragment() {
    private lateinit var addTarget: ImageView
    private lateinit var removeTarget: ImageView
    private lateinit var updatePage: ImageView
    private lateinit var checkAPI: ImageView
    private lateinit var aboutApp: Button
    private lateinit var aboutDevelopers: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // inizializaton of an elements of the home screen
        addTarget = view.findViewById(R.id.imageView_add_target)
        removeTarget = view.findViewById(R.id.imageView_remove_target)
        updatePage = view.findViewById(R.id.imageView_update_page)
        checkAPI = view.findViewById(R.id.imageView_check_API)
        aboutApp = view.findViewById(R.id.button_about_app)
        aboutDevelopers = view.findViewById(R.id.button_about_developers)

        addTarget.setOnClickListener {
            val intent1 = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/ingame-items/item-list/csgo-skins?exchangeTab=myTargets"))
            startActivity(intent1)
        }

        removeTarget.setOnClickListener {
            val intent2 = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/ingame-items/item-list/csgo-skins?exchangeTab=myTargets"))
            startActivity(intent2)
        }

        updatePage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        checkAPI.setOnClickListener {
            val intent3 = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/ru/ingame-items/item-list/csgo-skins?utm_source=google&utm_medium=cpc&utm_campaign=dm_new_brand-ua_s&gclid=Cj0KCQiAubrJBhCbARIsAHIdxD9iQgymE9e0Xf1OyoNVeUuAmbHiM7ecPEBO_oniDF8EqwVkkQVi-QsaAq1yEALw_wcB&cheapestBySteamAnalyst=true"))
            startActivity(intent3)
        }

        aboutApp.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.about_app_view, null)
            val cross: ImageView = dialogView.findViewById(R.id.imageView_cross5)
            val license: TextView = dialogView.findViewById(R.id.textView_license)
            val rules: TextView = dialogView.findViewById(R.id.textView_rules)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            cross.setOnClickListener {
                dialog.dismiss()
            }

            license.setOnClickListener {
                val dialogView2 = layoutInflater.inflate(R.layout.license, null)
                val cross3: ImageView = dialogView2.findViewById(R.id.imageView_cross2)

                val dialog2 = AlertDialog.Builder(requireContext())
                    .setView(dialogView2)
                    .create()

                cross3.setOnClickListener {
                    dialog2.dismiss()
                }

                dialog2.show()
            }

            rules.setOnClickListener {
                val dialogView3 = layoutInflater.inflate(R.layout.terms_of_use, null)
                val cross4: ImageView = dialogView3.findViewById(R.id.imageView_cross3)

                val dialog3 = AlertDialog.Builder(requireContext())
                    .setView(dialogView3)
                    .create()

                cross4.setOnClickListener {
                    dialog3.dismiss()
                }

                dialog3.show()
            }

            dialog.show()
        }

        aboutDevelopers.setOnClickListener {
            val dialogView4 = layoutInflater.inflate(R.layout.about_developers_view, null)
            val cross2: ImageView = dialogView4.findViewById(R.id.imageView_cross)
            val gitHub: TextView = dialogView4.findViewById(R.id.textView_GitHub_link)
            val email: TextView = dialogView4.findViewById(R.id.textView_email_support)

            val dialog4 = AlertDialog.Builder(requireContext())
                .setView(dialogView4)
                .create()

            cross2.setOnClickListener {
                dialog4.dismiss()
            }

            gitHub.setOnClickListener {
                val intent4 = Intent(
                    Intent.ACTION_VIEW, Uri.parse("https://github.com/joalausi/dm-tradebot")
                )
                startActivity(intent4)
            }

            email.setOnClickListener {
                val intent5 = Intent(
                    Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/mail/u/0/#inbox/FMfcgzQdzctzCgNQzDnMGnhmkjddKBKj?compose=CllgCHrjFRSldGrKQTdQLzfgdVlXHFsxvPFtncpGLZPmjXMmKqcHzrsLLhqvVMpSwTPTbdVkzMg")
                )
                startActivity(intent5)
            }

            dialog4.show()
        }
        return view
    }
}