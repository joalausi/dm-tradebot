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
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.dmarketalert.R
import com.example.dmarketalert.viewModel.TargetStatisticsViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class HomeFragment : Fragment() {

    private lateinit var addTarget: ImageView
    private lateinit var removeTarget: ImageView
    private lateinit var updatePage: ImageView
    private lateinit var checkAPI: ImageView
    private lateinit var pieChart: PieChart
    private lateinit var currentTargetsText: TextView
    private lateinit var outbidTargetsText: TextView
    private lateinit var allTargetsText: TextView
    private val statisticsViewModel: TargetStatisticsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        observeStatistics()
        setupClickListeners()

        return view
    }

    private fun initViews(view: View) {
        addTarget = view.findViewById(R.id.imageView_add_target)
        removeTarget = view.findViewById(R.id.imageView_remove_target)
        updatePage = view.findViewById(R.id.imageView_update_page)
        checkAPI = view.findViewById(R.id.imageView_check_API)
        pieChart = view.findViewById(R.id.piechart)
        currentTargetsText = view.findViewById(R.id.textView_current_targets_inBox)
        outbidTargetsText = view.findViewById(R.id.textView_outbid_targets_inBox)
        allTargetsText = view.findViewById(R.id.textView_all_targets_inBox)
    }

    private fun observeStatistics() {
        statisticsViewModel.statistics.observe(viewLifecycleOwner) { stats ->
            updateUI(stats.currentTargets, stats.outbidTargets, stats.allTimeTargets)
        }
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