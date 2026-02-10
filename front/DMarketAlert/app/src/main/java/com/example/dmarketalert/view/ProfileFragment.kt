package com.example.dmarketalert.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.example.dmarketalert.viewModel.TargetStatisticsViewModel
import com.example.dmarketalert.viewModel.state.AuthState

class ProfileFragment : Fragment() {

    // UI elements
    private lateinit var userNickname: TextView
    private lateinit var nicknameText: TextView
    private lateinit var apiText: TextView
    private lateinit var editNickname: CardView
    private lateinit var editPassword: CardView
    private lateinit var editAPI: CardView

    private lateinit var currentTargetsText: TextView
    private lateinit var outbidTargetsText: TextView
    private lateinit var allTargetsText: TextView

    private lateinit var refreshData: CardView
    private lateinit var removeTargetStatistic: CardView
    private lateinit var exitAccount: CardView
    private lateinit var deleteAccount: CardView
    private lateinit var askQuestions: CardView
    private lateinit var license: CardView
    private lateinit var termsOfUse: CardView

    private val authViewModel: AuthenticationViewModel by activityViewModels()
    private val statisticsViewModel: TargetStatisticsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)

        val nickname = getUserNickname() ?: run {
            navigateToEnter()
            return view
        }

        loadUserData(nickname)
        observeAuthState()
        observeStatistics()
        setupClickListeners(nickname)

        return view
    }

    private fun initViews(view: View) {
        userNickname = view.findViewById(R.id.textView_user_nickname)
        nicknameText = view.findViewById(R.id.textView_nickname)
        apiText = view.findViewById(R.id.textView_API)
        editNickname = view.findViewById(R.id.CardView_nickname)
        editPassword = view.findViewById(R.id.CardView_password)
        editAPI = view.findViewById(R.id.CardView_API)
        currentTargetsText = view.findViewById(R.id.textView_current_targets)
        outbidTargetsText = view.findViewById(R.id.textView_outbid_targets)
        allTargetsText = view.findViewById(R.id.textView_all_targets)
        refreshData = view.findViewById(R.id.CardView_refresh_data)
        removeTargetStatistic = view.findViewById(R.id.CardView_remove_target_statistic)
        exitAccount = view.findViewById(R.id.CardView_exit_account)
        deleteAccount = view.findViewById(R.id.CardView_delete_account)
        askQuestions = view.findViewById(R.id.CardView_ask_questions)
        license = view.findViewById(R.id.CardView_gitHub)
        termsOfUse = view.findViewById(R.id.CardView_terms_of_use)
    }

    private fun loadUserData(nickname: String) {
        authViewModel.loadUser(nickname)
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> {
                    val user = state.user ?: return@observe
                    userNickname.text = user.nickname
                    nicknameText.text = user.nickname
                    apiText.text = getMaskedApiKey(user.apiHash)
                }

                is AuthState.Error -> {
                    showToast(state.message)
                }

                else -> Unit
            }
        }
    }

    private fun observeStatistics() {
        statisticsViewModel.statistics.observe(viewLifecycleOwner) { stats ->
            currentTargetsText.text = stats.currentTargets.toInt().toString()
            outbidTargetsText.text = stats.outbidTargets.toInt().toString()
            allTargetsText.text = stats.allTimeTargets.toInt().toString()
        }
    }

    private fun setupClickListeners(nickname: String) {
        editNickname.setOnClickListener {
            startActivity(Intent(requireContext(), ChangeNickname::class.java))
        }

        editPassword.setOnClickListener {
            val intent = Intent(requireContext(), CheckPasswordActivity::class.java)
            intent.putExtra("next_screen", "password")
            startActivity(intent)
        }

        editAPI.setOnClickListener {
            val intent = Intent(requireContext(), CheckPasswordActivity::class.java)
            intent.putExtra("next_screen", "api")
            startActivity(intent)
        }

        exitAccount.setOnClickListener {
            showExitDialog()
        }

        deleteAccount.setOnClickListener {
            showDeleteDialog(nickname)
        }

        refreshData.setOnClickListener {
            refreshFragment()
        }

        removeTargetStatistic.setOnClickListener {
            showRemoveTargetStatisticDialog()
        }

        askQuestions.setOnClickListener {
            openUrl("https://mail.google.com/mail/u/0/#inbox?compose=DmwnWtDpKkndvKVhKGbDfstxvdsPhRgsSjNZQpzxrpSTCpqpNkSnPxrFvBwlzNLnjsfRxNLgkvPq")
        }

        license.setOnClickListener {
            startActivity(Intent(requireContext(), License::class.java))
        }

        termsOfUse.setOnClickListener {
            startActivity(Intent(requireContext(), Terms_of_use_and_privacy_police::class.java))
        }
    }

    private fun showRemoveTargetStatisticDialog(){
        val dialogView = layoutInflater.inflate(R.layout.remove_target_statistic, null)
        val dialog = createDialog(dialogView)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_apply).setOnClickListener {
            resetStatistics()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showExitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.exit_account, null)
        val dialog = createDialog(dialogView)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.button_cancel2).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_apply2).setOnClickListener {
            clearPreferences()
            authViewModel.logout()
            navigateToEnter()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(nickname: String) {
        val dialogView = layoutInflater.inflate(R.layout.delete_account, null)
        val dialog = createDialog(dialogView)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.button_cancel3).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_apply3).setOnClickListener {
            authViewModel.deleteAccount(nickname) { result ->
                result
                    .onSuccess {
                        clearPreferences()
                        navigateToRegistration()
                        showToast("Account deleted successfully")
                    }
                    .onFailure {
                        showToast("Failed to delete account: ${it.message}")
                    }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun createDialog(dialogView: View): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
    }

    private fun refreshFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ProfileFragment())
            .commit()
    }

    private fun resetStatistics() {
        statisticsViewModel.resetStatistics()
    }

    private fun getUserNickname(): String? {
        return requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("nickname", null)
    }

    private fun clearPreferences() {
        requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun getMaskedApiKey(apiHash: String): String {
        return if (apiHash.length > 4) {
            "****${apiHash.takeLast(4)}"
        } else {
            "********"
        }
    }

    private fun navigateToEnter() {
        startActivity(Intent(requireContext(), EnterActivity::class.java))
        requireActivity().finish()
    }

    private fun navigateToRegistration() {
        startActivity(Intent(requireContext(), RegistrationActivity::class.java))
        requireActivity().finish()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}