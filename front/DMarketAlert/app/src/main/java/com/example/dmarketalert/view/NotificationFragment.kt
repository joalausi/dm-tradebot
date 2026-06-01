package com.example.dmarketalert.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.NotificationViewModel
import com.example.dmarketalert.viewModel.adapter_viewHolder.NotificationAdapter
import com.example.dmarketalert.viewModel.state.NotificationState

class NotificationFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var imageError: ImageView
    private lateinit var textError: TextView
    private lateinit var imageEmpty: ImageView
    private lateinit var textEmpty: TextView

    private val viewModel: NotificationViewModel by viewModels()
    private val adapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)
        initViews(view)
        setupRecyclerView()
        observeViewModel()
        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView_notification)
        progressBar = view.findViewById(R.id.progressBar_notification)
        imageError = view.findViewById(R.id.imageView_error_notification)
        textError = view.findViewById(R.id.textView_error_notification)
        imageEmpty = view.findViewById(R.id.imageView_empty_notification)
        textEmpty = view.findViewById(R.id.textView_empty_notification)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.notificationState.observe(viewLifecycleOwner) { state ->
            updateVisibility(
                loading = state is NotificationState.Loading,
                success = state is NotificationState.Success,
                empty = state is NotificationState.Empty,
                error = state is NotificationState.Error
            )

            when (state) {
                is NotificationState.Success -> {
                    adapter.submitList(state.list)
                }
                is NotificationState.Error -> {
                }
                else -> {}
            }
        }
    }

    private fun updateVisibility(loading: Boolean, success: Boolean, empty: Boolean, error: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (success) View.VISIBLE else View.GONE
        imageEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        imageError.visibility = if (error) View.VISIBLE else View.GONE
        textError.visibility = if (error) View.VISIBLE else View.GONE
    }
}