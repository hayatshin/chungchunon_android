package com.chugnchunon.chungchunon_android.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.MoreFragment
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.google.android.material.tabs.TabLayout

class TabPageAdapter(activity: FragmentActivity, private val tabCount: Int): FragmentStateAdapter(activity) {

    override fun getItemCount(): Int  = tabCount

    override fun createFragment(position: Int): Fragment {

        return when(position) {
            0 -> MyDiaryFragment()
            1 -> AllDiaryFragment()
            2 -> MoreFragment()
            else -> MyDiaryFragment()
        }
    }
}