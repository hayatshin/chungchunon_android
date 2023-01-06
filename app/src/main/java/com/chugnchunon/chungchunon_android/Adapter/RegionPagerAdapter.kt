package com.chugnchunon.chungchunon_android.Adapter

import android.util.Log
import androidx.fragment.app.*
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.Fragment.SmallRegionRegisterFragment

class RegionPagerAdapter(activity: FragmentActivity, smallRegionCheck: Boolean): FragmentStateAdapter(activity) {

    private val COUNT = 2

    override fun createFragment(position: Int): Fragment {
        Log.d("리지온0", "$smallRegionCheck")

        return when(smallRegionCheck) {
            false -> RegionRegisterFragment()
            true -> SmallRegionRegisterFragment()
            else -> RegionRegisterFragment()
        }
    }

    override fun getItemCount(): Int = COUNT
}