package com.chugnchunon.chungchunon_android.Partner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.chugnchunon.chungchunon_android.databinding.PartnerActivityDiaryBinding
import com.google.android.material.tabs.TabLayout
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

        // 걸음수 권한
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_DENIED
        ) {
            //ask for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                100
            )
        }

        var startService = Intent(this, MyService::class.java)
        startForegroundService(startService)

        val yourTabLayoutView = binding.partnerTabLayout // If you aren't using data biniding, You can use findViewById to get the view
        var yourTabItemView = (yourTabLayoutView.getChildAt(0) as LinearLayout).getChildAt(1).layoutParams as LinearLayout.LayoutParams
        yourTabItemView.weight = 0.3f

//        binding.viewPager.isUserInputEnabled = false
//
//        binding.viewPager.setPageTransformer(object: ViewPager2.PageTransformer {
//            override fun transformPage(page: View, position: Float) {
//                page.alpha = 0f
//                page.visibility = View.VISIBLE
//
//                page.animate()
//                    .withLayer()
//                    .alpha(1f)
//                    .setDuration(0)
//            }
//        })

        setUpTabBar()

    }


    @SuppressLint("NotifyDataSetChanged")
    private fun setUpTabBar() {
        val adapter = PartnerTabPageAdapter(this, partnerTabLayout.tabCount)

//        diaryDB
//            .document("${userId}_${writeTime}")
//            .get()
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val document = task.result
//                    if(document != null) {
//                        if (document.exists()) {
//                            viewPager.currentItem = 1
//                        } else {
//                            viewPager.currentItem = 0
//                        }
//                    }
//                } else {
//                    viewPager.currentItem = 0
//                }
//            }

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