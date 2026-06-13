package com.example.dmarketalert.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.HistoryViewModel
import com.example.dmarketalert.viewModel.adapter_viewHolder.MarketItemAdapter
import com.example.dmarketalert.viewModel.state.HistoryUiState

class HistoryFragment : Fragment() {
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: MarketItemAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var errorImage: ImageView
    private lateinit var emptyText: TextView
    private lateinit var emptyImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        observeViewModel()

        if (savedInstanceState == null) {
            viewModel.loadHistory()
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView_history)
        progressBar = view.findViewById(R.id.progressBar_history)
        errorText = view.findViewById(R.id.textView_error)
        errorImage = view.findViewById(R.id.imageView_Error)
        emptyImage = view.findViewById(R.id.imageView_history)
        emptyText = view.findViewById(R.id.textView_empty)
    }

    private fun setupRecyclerView() {
        adapter = MarketItemAdapter { item ->
            Toast.makeText(requireContext(), "History: ${item.title}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            handleUiState(state)
        }
    }

    private fun handleUiState(state: HistoryUiState) {
        // Скидаємо все одразу — потім вмикаємо тільки потрібне
        updateUi(
            showRecycler   = state is HistoryUiState.Success,
            showProgress   = state is HistoryUiState.Loading,
            showError      = state is HistoryUiState.Error,
            showEmpty      = state is HistoryUiState.Empty
        )

        if (state is HistoryUiState.Success) {
            adapter.submitList(state.items)
        }
    }

    private fun updateUi(
        showRecycler: Boolean,
        showProgress: Boolean,
        showError: Boolean,
        showEmpty: Boolean
    ) {
        recyclerView.visibility = if (showRecycler) View.VISIBLE else View.GONE
        progressBar.visibility  = if (showProgress) View.VISIBLE else View.GONE
        errorImage.visibility   = if (showError)    View.VISIBLE else View.GONE
        errorText.visibility    = if (showError)    View.VISIBLE else View.GONE
        emptyImage.visibility   = if (showEmpty)    View.VISIBLE else View.GONE
        emptyText.visibility    = if (showEmpty)    View.VISIBLE else View.GONE
    }
}