package com.example.dmarketalert.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.example.dmarketalert.R
import androidx.fragment.app.activityViewModels
import com.example.dmarketalert.viewModel.AuthenticationViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.math.E

class ProfileFragment : Fragment() {
    private lateinit var nickname_text: TextView
    private lateinit var user_nickname: TextView
    private lateinit var password_text: TextView
    private lateinit var nickname_button: CardView
    private lateinit var password_button: CardView
    private lateinit var api_button: CardView
    private lateinit var api_text: TextView
    private lateinit var remove_target_statistic: CardView
    private lateinit var exit_account: CardView
    private lateinit var delete_account: CardView
    private lateinit var ask_questions: CardView
    private lateinit var license: CardView
    private lateinit var rules: CardView

    private val viewModel: AuthenticationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        nickname_text = view.findViewById(R.id.textView_nickname)
        user_nickname = view.findViewById(R.id.textView_user_nickname)
        password_text = view.findViewById(R.id.textView_password)
        nickname_button = view.findViewById(R.id.CardView_nickname)
        password_button = view.findViewById(R.id.CardView_password)
        api_button = view.findViewById(R.id.CardView_API)
        api_text = view.findViewById(R.id.textView_API)
        remove_target_statistic = view.findViewById(R.id.CardView_remove_target_statistic)
        exit_account = view.findViewById(R.id.CardView_exit_account)
        delete_account = view.findViewById(R.id.CardView_delete_account)
        ask_questions = view.findViewById(R.id.CardView_ask_questions)
        license = view.findViewById(R.id.CardView_gitHub)
        rules = view.findViewById(R.id.CardView_terms_of_use)

        val prefs = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val nickname = prefs.getString("nickname", null)
        if (nickname.isNullOrEmpty()) {
            startActivity(Intent(requireContext(), EnterActivity::class.java))
            requireActivity().finish()
            return view
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(nickname)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(com.example.dmarketalert.model.User::class.java)
                if (user != null) {
                    user_nickname. text = user.nickname ?: ""
                    nickname_text.text = user.nickname ?: ""
                    password_text.text = user.password ?: ""
                    api_text.text = user.api ?: ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }

        nickname_button.setOnClickListener {
            startActivity(Intent(requireContext(), ChangeNickname::class.java))
        }

        password_button.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePassword::class.java))
        }

        api_button.setOnClickListener {
            startActivity(Intent(requireContext(), ChangeAPI::class.java))
        }

        remove_target_statistic.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.remove_target_statistic, null)
            val cancel: Button = dialogView.findViewById(R.id.button_cancel)
            val apply: Button = dialogView.findViewById(R.id.button_apply)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            cancel.setOnClickListener {
                dialog.dismiss()
            }

            apply.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        exit_account.setOnClickListener {
            val dialogView2 = layoutInflater.inflate(R.layout.exit_account, null)
            val cancel2: Button = dialogView2.findViewById(R.id.button_cancel2)
            val apply2: Button = dialogView2.findViewById(R.id.button_apply2)

            val dialog2 = AlertDialog.Builder(requireContext())
                .setView(dialogView2)
                .create()

            cancel2.setOnClickListener {
                dialog2.dismiss()
            }

            apply2.setOnClickListener {
                val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("isLoggedIn", false).remove("nickname").apply()

                startActivity(Intent(requireContext(), EnterActivity::class.java))
                requireActivity().finish()
                dialog2.dismiss()
            }

            dialog2.show()
            dialog2.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        delete_account.setOnClickListener {
            val dialogView3 = layoutInflater.inflate(R.layout.delete_account, null)
            val cancel3: Button = dialogView3.findViewById(R.id.button_cancel3)
            val apply3: Button = dialogView3.findViewById(R.id.button_apply3)

            val dialog3 = AlertDialog.Builder(requireContext())
                .setView(dialogView3)
                .create()

            cancel3.setOnClickListener {
                dialog3.dismiss()
            }

            apply3.setOnClickListener {
                val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val nickname = prefs.getString("nickname", "") ?: ""

                FirebaseFirestore.getInstance().collection("users").document(nickname).delete()
                    .addOnSuccessListener {
                        prefs.edit().putBoolean("isLoggedIn", false).remove("nickname").apply()
                        startActivity(Intent(requireContext(), RegistrationActivity::class.java))
                        requireActivity().finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show()
                    }
                dialog3.dismiss()
            }

            dialog3.show()
            dialog3.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        ask_questions.setOnClickListener {
            val url = "https://mail.google.com/mail/u/0/#inbox?compose=jrjtXVXdPMxgqnjgXVKgHCbpVhhGPzLJbtrJlvwTMSBhqwmLQRRpcKhzTStntLkhcBxkDfTq"
            val intent4 = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent4)
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