package com.chugnchunon.chungchunon_android.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.PeriodRankingAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.Layout.CustomMarkerView
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_ranking.view.*

class RankingFragment: Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        val binding = binding.root

        val adapter = PeriodRankingAdapter(requireActivity())
        binding.rankingViewPager.offscreenPageLimit = 2
        binding.rankingViewPager.adapter = adapter
        binding.rankingViewPager.isUserInputEnabled = false

        binding.rankingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.periodTabLayout.selectTab(binding.periodTabLayout.getTabAt(position))
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