package com.chugnchunon.chungchunon_android.Adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chugnchunon.chungchunon_android.Fragment.*
import com.google.android.material.tabs.TabLayout

class RankingMenuAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    var fragmentList: MutableList<Fragment> = arrayListOf()
    var titleList: MutableList<String> = arrayListOf()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {

        when (position) {
            0 -> return PeriodAllRankingFragment()
            1 -> return PeriodRegionRankingFragment()
            2 -> return PeriodFriendRankingFragment()
//            3 -> return MyDataFragment()
        }
        return PeriodAllRankingFragment()
    }
}