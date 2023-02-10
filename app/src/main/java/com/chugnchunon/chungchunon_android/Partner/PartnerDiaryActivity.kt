package com.chugnchunon.chungchunon_android.Partner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.PartnerTabPageAdapter
import com.chugnchunon.chungchunon_android.Adapter.TabPageAdapter
import com.chugnchunon.chungchunon_android.BroadcastReceiver.DiaryUpdateBroadcastReceiver
import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.chugnchunon.chungchunon_android.databinding.PartnerActivityDiaryBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.*
import kotlinx.android.synthetic.main.partner_activity_diary.*
import java.time.LocalDateTime

class PartnerDiaryActivity : AppCompatActivity() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)

    private val binding by lazy {
        PartnerActivityDiaryBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val yourTabLayoutView = binding.partnerTabLayout // If you aren't using data biniding, You can use findViewById to get the view
        var yourTabItemView = (yourTabLayoutView.getChildAt(0) as LinearLayout).getChildAt(1).layoutParams as LinearLayout.LayoutParams
        yourTabItemView.weight = 0.3f

        binding.partnerViewPager.isUserInputEnabled = false

        setUpTabBar()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpTabBar() {
        val adapter = PartnerTabPageAdapter(this, partnerTabLayout.tabCount)

        partnerViewPager.adapter = adapter

        partnerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                partnerTabLayout.selectTab(partnerTabLayout.getTabAt(position))
            }
        })

        partnerTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                partnerViewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

}