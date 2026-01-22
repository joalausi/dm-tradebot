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
import com.example.dmarketalert.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class HomeFragment : Fragment() {
    private lateinit var addTarget: ImageView
    private lateinit var removeTarget: ImageView
    private lateinit var updatePage: ImageView
    private lateinit var checkAPI: ImageView
    private lateinit var pieChart: PieChart

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
        pieChart = view.findViewById(R.id.piechart)

        addTarget.setOnClickListener {
            val intent1 = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/ingame-items/item-list/csgo-skins?exchangeTab=myTargets"))
            startActivity(intent1)
        }

        removeTarget.setOnClickListener {
            val intent2 = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/ingame-items/item-list/csgo-skins?exchangeTab=myTargets"))
            startActivity(intent2)
        }

        updatePage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        checkAPI.setOnClickListener {
            val intent3 = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://dmarket.com/ru/ingame-items/item-list/csgo-skins?utm_source=google&utm_medium=cpc&utm_campaign=dm_new_brand-ua_s&gclid=Cj0KCQiAubrJBhCbARIsAHIdxD9iQgymE9e0Xf1OyoNVeUuAmbHiM7ecPEBO_oniDF8EqwVkkQVi-QsaAq1yEALw_wcB&cheapestBySteamAnalyst=true"))
            startActivity(intent3)
        }

        val list: ArrayList<PieEntry> = ArrayList()

        list.add(PieEntry(127f, "20"))
        list.add(PieEntry(115f, "12"))
        list.add(PieEntry(102f, "5"))

        val setPieData = PieDataSet(list, "Targets")

        setPieData.setColors(Color.BLUE, Color.RED, Color.GREEN)
        setPieData.valueTextSize = 14f
        setPieData.valueTextColor = Color.WHITE

        val pieData = PieData(setPieData)
        pieChart.data = pieData

        pieChart.description.text = "Targets detail statistic"
        pieChart.centerText = "Targets statistic"
        pieChart.animateY(2500)

        return view
    }
}