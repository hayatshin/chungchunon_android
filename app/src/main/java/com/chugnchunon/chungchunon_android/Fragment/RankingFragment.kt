package com.chugnchunon.chungchunon_android.Fragment

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.PeriodRankingAdapter
import com.chugnchunon.chungchunon_android.Adapter.RankingMenuAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.Layout.CustomMarkerView
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_ranking.view.*
import java.util.*

class RankingFragment: Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private var formatThisWeekStart: String = ""
    private var formatThisWeekEnd: String = ""
    private var formatLastWeekStart: String = ""
    private var formatLastWeekEnd: String = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        val binding = binding.root

        // elevation
        binding.appBarLayout.elevation = 0f

        // 지난주
        val dateFormat = SimpleDateFormat("MM/dd")
        val today = Calendar.getInstance()
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val startOfWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }.time

        val endOfWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.time

        val startOfLastWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
//            set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR))
//            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.DAY_OF_MONTH, -7)
        }.time

        val endOfLastWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
//            set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR))
//            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.DAY_OF_MONTH, -7)
        }.time

        formatThisWeekStart = dateFormat.format(startOfWeek)
        formatThisWeekEnd = dateFormat.format(endOfWeek)
        formatLastWeekStart = dateFormat.format(startOfLastWeek)
        formatLastWeekEnd = dateFormat.format(endOfLastWeek)

        binding.periodText.text = "$formatThisWeekStart ~ $formatThisWeekEnd"


        val adapter = RankingMenuAdapter(requireActivity())
        binding.rankingViewPager.offscreenPageLimit = 3
        binding.rankingViewPager.adapter = adapter
        binding.rankingViewPager.isUserInputEnabled = false

        binding.rankingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.periodTabLayout.selectTab(binding.periodTabLayout.getTabAt(position))
                if(position == 0) {
                    binding.periodText.text = "$formatThisWeekStart ~ $formatThisWeekEnd"
                } else if (position == 1) {
                    binding.periodText.text = "$formatLastWeekStart ~ $formatLastWeekEnd"
                }
            }
        })

        binding.periodTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.rankingViewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })



        return binding
    }
}