package com.chugnchunon.chungchunon_android.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Adapter.PeriodRankingAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingBinding
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


        return binding
    }
}