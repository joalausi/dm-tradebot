package com.example.dmarketalert.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.AuthenticationViewModel
import com.example.dmarketalert.viewModel.state.AuthState

class ProfileFragment : Fragment() {

    private lateinit var userNickname: TextView
    private lateinit var apiText: TextView

    private lateinit var exitAccount: CardView
    private lateinit var deleteAccount: CardView

    private val viewModel: AuthenticationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)

        val nickname = getNickname()
        if (nickname == null) {
            goToEnter()
            return view
        }

        // Download user data
        viewModel.loadUser(nickname)

        observeViewModel()
        setupClickListeners(nickname)

        return view
    }

    private fun initViews(view: View) {
        userNickname = view.findViewById(R.id.textView_user_nickname)
        apiText = view.findViewById(R.id.textView_API)
        exitAccount = view.findViewById(R.id.CardView_exit_account)
        deleteAccount = view.findViewById(R.id.CardView_delete_account)
    }

    private fun getNickname(): String? {
        return requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)
    }

    private fun setupClickListeners(nickname: String) {
        exitAccount.setOnClickListener { showExitDialog() }
        deleteAccount.setOnClickListener { showDeleteDialog(nickname) }

        view?.findViewById<CardView>(R.id.CardView_ask_questions)?.setOnClickListener {
            openMail()
        }

        view?.findViewById<CardView>(R.id.CardView_gitHub)?.setOnClickListener {
            startActivity(Intent(requireContext(), License::class.java))
        }

        view?.findViewById<CardView>(R.id.CardView_terms_of_use)?.setOnClickListener {
            startActivity(
                Intent(requireContext(), Terms_of_use_and_privacy_police::class.java)
            )
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> {
                    val user = state.user ?: return@observe
                    userNickname.text = user.nickname

                    val maskedApi = if (user.apiHash.length > 4) {
                        "••••${user.apiHash.takeLast(4)}"
                    } else {
                        "••••••••"
                    }
                    apiText.text = maskedApi
                }

                is AuthState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                else -> Unit
            }
        }
    }

    private fun showExitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.exit_account, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.setBackgroundColor(Color.TRANSPARENT)

        dialogView.findViewById<Button>(R.id.button_cancel2).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_apply2).setOnClickListener {
            clearPrefs()
            viewModel.logout()
            goToEnter()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(nickname: String) {
        val dialogView = layoutInflater.inflate(R.layout.delete_account, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.button_cancel3).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.setBackgroundColor(Color.TRANSPARENT)

        dialogView.findViewById<Button>(R.id.button_apply3).setOnClickListener {
            viewModel.deleteAccount(nickname) { result ->
                result
                    .onSuccess {
                        clearPrefs()
                        goToRegistration()
                        Toast.makeText(
                            requireContext(),
                            "Account deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .onFailure {
                        Toast.makeText(
                            requireContext(),
                            "Failed to delete account: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearPrefs() {
        requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun goToEnter() {
        startActivity(Intent(requireContext(), EnterActivity::class.java))
        requireActivity().finish()
    }

    private fun goToRegistration() {
        startActivity(Intent(requireContext(), RegistrationActivity::class.java))
        requireActivity().finish()
    }

    private fun openMail() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://mail.google.com/")
        )
        startActivity(intent)
    }
}