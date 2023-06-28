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
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingMenuBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_ranking.view.*
import kotlinx.android.synthetic.main.fragment_ranking.view.rankingViewPager
import kotlinx.android.synthetic.main.fragment_ranking_menu.view.*
import java.util.*

class RankingMenuFragment: Fragment() {

    private var _binding: FragmentRankingMenuBinding? = null
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

        _binding = FragmentRankingMenuBinding.inflate(inflater, container, false)
        val binding = binding.root

        val adapter = RankingMenuAdapter(requireActivity())
        binding.rankingViewPager.offscreenPageLimit = 3
        binding.rankingViewPager.adapter = adapter
        binding.rankingViewPager.isUserInputEnabled = false

        binding.rankingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.rankingTabLayout.selectTab(binding.rankingTabLayout.getTabAt(position))
            }
        })

        binding.rankingTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.rankingViewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding.rankingViewPager.setCurrentItem(1, false)
        binding.rankingTabLayout.selectTab(binding.rankingTabLayout.getTabAt(1))

        return binding
    }
}