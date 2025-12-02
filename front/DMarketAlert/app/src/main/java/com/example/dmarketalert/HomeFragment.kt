package com.example.dmarketalert

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class HomeFragment : Fragment() {

    private lateinit var addTarget: ImageView
    private lateinit var removeTarget: ImageView
    private lateinit var updatePage: ImageView
    private lateinit var checkAPI: ImageView

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

        addTarget.setOnClickListener {
            null
        }

        removeTarget.setOnClickListener {
            null
        }

        updatePage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .detach(this)
                .attach(this)
                .commit()
        }

        checkAPI.setOnClickListener {
            null
        }

        return view
    }
}