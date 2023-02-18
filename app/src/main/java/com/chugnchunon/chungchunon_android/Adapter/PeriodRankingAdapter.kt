package com.chugnchunon.chungchunon_android.Adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chugnchunon.chungchunon_android.Fragment.*
import com.google.android.material.tabs.TabLayout

class PeriodRankingAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    var fragmentList: MutableList<Fragment> = arrayListOf()
    var titleList: MutableList<String> = arrayListOf()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {

        Log.d("내글", "$position")
        when (position) {
            0 -> return PeriodThisWeekRankingFragment()
            1 -> return PeriodThisWeekRankingFragment()
        }
        return PeriodThisWeekRankingFragment()
    }
}