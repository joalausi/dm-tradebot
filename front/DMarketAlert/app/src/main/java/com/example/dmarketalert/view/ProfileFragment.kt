package com.example.dmarketalert.view

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
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModel
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.UserViewModel
import com.example.dmarketalert.viewModel.ViewModelFactory
import androidx.fragment.app.activityViewModels
import com.example.dmarketalert.databinding.RemoveTargetStatisticBinding
import com.example.dmarketalert.view.ChangeAPI
import com.example.dmarketalert.view.ChangePassword

class ProfileFragment : Fragment() {
    private val viewModel: UserViewModel by activityViewModels{
        ViewModelFactory(requireContext())
    }
    private lateinit var nickname_text: TextView
    private lateinit var user_nickname: TextView
    private lateinit var password_text: TextView
    private lateinit var nickname: CardView
    private lateinit var password: CardView
    private lateinit var api: CardView
    private lateinit var api_text: TextView
    private lateinit var remove_target_statistic: CardView
    private lateinit var exit_account: CardView
    private lateinit var delete_accont: CardView
    private lateinit var ask_questions: CardView
    private lateinit var license: CardView
    private lateinit var rules: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        nickname_text = view.findViewById(R.id.textView_nickname)
        user_nickname = view.findViewById(R.id.textView_user_nickname)
        password_text = view.findViewById(R.id.textView_password)
        nickname = view.findViewById(R.id.CardView_nickname)
        password = view.findViewById(R.id.CardView_password)
        api = view.findViewById(R.id.CardView_API)
        api_text = view.findViewById(R.id.textView_API)
        remove_target_statistic = view.findViewById(R.id.CardView_remove_target_statistic)
        exit_account = view.findViewById(R.id.CardView_exit_account)
        delete_accont = view.findViewById(R.id.CardView_delete_account)
        ask_questions = view.findViewById(R.id.CardView_ask_questions)
        license = view.findViewById(R.id.CardView_license)
        rules = view.findViewById(R.id.CardView_terms_of_use)


        nickname.setOnClickListener {
            startActivity(Intent(requireContext(), ChangeNickname::class.java))
        }

        password.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePassword::class.java))
        }

        api.setOnClickListener {
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
                val intent = Intent(requireContext(), EnterActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
                dialog2.dismiss()
            }

            dialog2.show()
            dialog2.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        delete_accont.setOnClickListener {
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
                val intent2 = Intent(requireContext(), RegistrationActivity::class.java)
                startActivity(intent2)
                requireActivity().finish()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        viewModel.user.observe(viewLifecycleOwner){user ->
            user?.let {
                nickname_text.text = it.nickname
                user_nickname.text = it.nickname
                password_text.text = it.password
                api_text.text = it.api
            }
        }
        viewModel.loadUser()
    }
}