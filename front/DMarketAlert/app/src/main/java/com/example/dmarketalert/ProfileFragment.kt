package com.example.dmarketalert

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class ProfileFragment : Fragment() {

    private lateinit var nickname_text: TextView
    private lateinit var user_nickname: TextView
    private lateinit var password_text: TextView
    private lateinit var api_text: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        nickname_text = view.findViewById(R.id.textView_nickname)
        user_nickname = view.findViewById(R.id.textView_user_nickname)
        password_text = view.findViewById(R.id.textView_password)
        api_text = view.findViewById(R.id.textView_API)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        val nickname = arguments?.getString("nickname_key")
        val password = arguments?.getString("password_key")
        val api = arguments?.getString("api_key")

        nickname_text.text = nickname
        user_nickname.text = nickname
        password_text.text = password
        api_text.text = api
    }
}