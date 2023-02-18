package com.chugnchunon.chungchunon_android.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Adapter.RankingRecyclerAdapter
import com.chugnchunon.chungchunon_android.DataClass.RankingLine
import com.chugnchunon.chungchunon_android.Layout.BarChartCustomRenderer
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingWeekBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class PeriodThisWeekRankingFragment: Fragment() {

    private var _binding: FragmentRankingWeekBinding? = null
    private val binding get() = _binding!!

    lateinit var rankingAdapter: RankingRecyclerAdapter
    private var rankingItems = ArrayList<RankingLine>()

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRankingWeekBinding.inflate(inflater, container, false)
        val view = binding.root

        var myColors = ArrayList<Int>()
        myColors.add(Color.BLACK);
        myColors.add(Color.YELLOW);
        myColors.add(Color.BLUE);
        myColors.add(Color.DKGRAY);
        myColors.add(Color.GREEN);
        myColors.add(Color.GRAY);

        var myText = arrayListOf<String>("1위", "2위", "3위")

        binding.rankingBarChart.setDrawBarShadow(false)
        binding.rankingBarChart.description.isEnabled = false
        binding.rankingBarChart.setDrawGridBackground(false)

        var xaxis = binding.rankingBarChart.xAxis
        xaxis.setDrawGridLines(false)
        xaxis.position = XAxis.XAxisPosition.BOTTOM
        xaxis.setDrawLabels(true)
        xaxis.setDrawAxisLine(false)

        var yAxisLeft = binding.rankingBarChart.axisLeft
        yAxisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        yAxisLeft.setDrawGridLines(false)
        yAxisLeft.setDrawAxisLine(false)
        yAxisLeft.isEnabled = false

        binding.rankingBarChart.axisRight.isEnabled = false
        binding.rankingBarChart.renderer = BarChartCustomRenderer(
            binding.rankingBarChart,
            binding.rankingBarChart.animator,
            binding.rankingBarChart.viewPortHandler,
            myColors
        )
        binding.rankingBarChart.setDrawValueAboveBar(true)

        var legend = binding.rankingBarChart.legend
        legend.isEnabled = false

        val valueSet1 = ArrayList<BarEntry>()

        for (i in 0..5) {
            val entry = BarEntry(i.toFloat(), ((i + 1) * 10).toFloat())
            valueSet1.add(entry)
        }

        val dataSets: MutableList<IBarDataSet> = ArrayList()
        val barDataSet = BarDataSet(valueSet1, " ")
//        barDataSet.valueFormatter = Val
        barDataSet.color = Color.CYAN
        dataSets.add(barDataSet)

        val data = BarData(dataSets)
        binding.rankingBarChart.setData(data)

        return view
    }




}