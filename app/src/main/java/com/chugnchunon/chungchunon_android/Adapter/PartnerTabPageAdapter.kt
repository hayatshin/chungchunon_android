package com.chugnchunon.chungchunon_android.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.MoreFragment

class PartnerTabPageAdapter(activity: FragmentActivity, private val tabCount: Int): FragmentStateAdapter(activity) {

    override fun getItemCount(): Int  = tabCount

    override fun createFragment(position: Int): Fragment {

        return when(position) {
            0 -> AllDiaryFragmentTwo()
            1 -> MoreFragment()
            else -> AllDiaryFragmentTwo()
        }
    }
}