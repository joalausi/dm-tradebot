package com.example.dmarketalert.view

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.HomeViewModel
import com.example.dmarketalert.viewModel.TargetStatisticsViewModel
import com.example.dmarketalert.viewModel.adapter_viewHolder.MarketItemAdapter
import com.example.dmarketalert.viewModel.state.HomeUiState
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var addTarget: ImageView
    private lateinit var removeTarget: ImageView
    private lateinit var updatePage: ImageView
    private lateinit var checkAPI: ImageView
    private lateinit var pieChart: PieChart
    private lateinit var currentTargetsText: TextView
    private lateinit var outbidTargetsText: TextView
    private lateinit var allTargetsText: TextView
    private lateinit var errorText: TextView
    private lateinit var emptyText: TextView
    private lateinit var emptyImage: ImageView
    private lateinit var errorImage: ImageView
    private val statisticsViewModel: TargetStatisticsViewModel by activityViewModels()

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: MarketItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        observeStatistics()
        observeViewModel()
        setupRecyclerView()
        setupClickListeners()

        if (savedInstanceState == null){
            viewModel.loadActiveTargets()
        }

        return view
    }

    private fun initViews(view: View) {
        errorImage = view.findViewById(R.id.imageView_targetsError)
        emptyImage = view.findViewById(R.id.imageView_targetsEmpty)
        errorText = view.findViewById(R.id.textView_Error)
        emptyText = view.findViewById(R.id.textView_Empty)
        recyclerView = view.findViewById(R.id.recyclerView_homeFragment)
        progressBar = view.findViewById(R.id.progressBar_homeFragment)
        addTarget = view.findViewById(R.id.imageView_add_target)
        removeTarget = view.findViewById(R.id.imageView_remove_target)
        updatePage = view.findViewById(R.id.imageView_update_page)
        checkAPI = view.findViewById(R.id.imageView_check_API)
        pieChart = view.findViewById(R.id.piechart)
        currentTargetsText = view.findViewById(R.id.textView_current_targets_inBox)
        outbidTargetsText = view.findViewById(R.id.textView_outbid_targets_inBox)
        allTargetsText = view.findViewById(R.id.textView_all_targets_inBox)
    }

    private fun setupRecyclerView(){
        adapter = MarketItemAdapter {item ->
            Toast.makeText(
                requireContext(),
                "Clicked: ${item.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeStatistics() {
        statisticsViewModel.statistics.observe(viewLifecycleOwner) { stats ->
            updateUI(stats.currentTargets, stats.outbidTargets, stats.allTimeTargets)
        }
    }

    private fun observeViewModel(){
        viewModel.uiState.observe(viewLifecycleOwner) {state ->
            handleUiState(state)
        }
    }

    private fun handleUiState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Idle -> {
                showContent()
            }

            is HomeUiState.Loading -> {
                showLoading()
            }

            is HomeUiState.Success -> {
                showContent()
                adapter.submitList(state.items)
            }

            is HomeUiState.Error -> {
                showError()
            }

            is HomeUiState.Empty -> {
                showEmpty()
            }
        }
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        errorText.visibility = View.GONE
        emptyText.visibility = View.GONE
        emptyImage.visibility = View.GONE
        errorImage.visibility = View.GONE
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        emptyText.visibility = View.GONE
        emptyImage.visibility = View.GONE
        errorImage.visibility = View.GONE
    }

    private fun showError() {
        progressBar.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        emptyImage.visibility = View.GONE
        errorImage.visibility = View.VISIBLE
    }

    private fun showEmpty() {
        progressBar.visibility = View.GONE
        errorText.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyImage.visibility = View.VISIBLE
        errorImage.visibility = View.GONE
    }

    private fun setupClickListeners() {
        addTarget.setOnClickListener {
            openDMarketTargets()
        }

        removeTarget.setOnClickListener {
            openDMarketTargets()
        }

        updatePage.setOnClickListener {
            refreshFragment()
        }

        checkAPI.setOnClickListener {
            openDMarketHome()
        }
    }

    private fun updateUI(current: Float, outbid: Float, allTime: Float) {
        currentTargetsText.text = current.toInt().toString()
        outbidTargetsText.text = outbid.toInt().toString()
        allTargetsText.text = allTime.toInt().toString()

        setupPieChart(current, outbid, allTime)
    }

    private fun setupPieChart(current: Float, outbid: Float, allTime: Float) {
        val entries = arrayListOf(
            PieEntry(current, "Current targets"),
            PieEntry(outbid, "Outbid targets"),
            PieEntry(allTime, "All time targets")
        )

        val dataSet = PieDataSet(entries, "Targets").apply {
            colors = listOf(
                Color.parseColor("#8DD294"),
                Color.parseColor("#D17575"),
                Color.parseColor("#6372CC")
            )
            valueTextSize = 10f
            valueTextColor = Color.WHITE
        }

        val pieData = PieData(dataSet)

        pieChart.apply {
            data = pieData
            description.text = "Targets detail statistic"
            centerText = "Targets statistic"
            animateY(2500)
            invalidate()
        }
    }

    private fun openDMarketTargets() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://dmarket.com/ingame-items/item-list/csgo-skins?exchangeTab=myTargets")
        )
        startActivity(intent)
    }

    private fun openDMarketHome() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://dmarket.com/ingame-items/item-list/csgo-skins?cheapestBySteamAnalyst=true")
        )
        startActivity(intent)
    }

    private fun refreshFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }
}