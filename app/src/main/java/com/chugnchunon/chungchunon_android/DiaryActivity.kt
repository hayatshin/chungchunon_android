package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.TabPageAdapter
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.*
import java.time.LocalDateTime

class DiaryActivity : AppCompatActivity() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)

    private val binding by lazy {
        ActivityDiaryBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val yourTabLayoutView = binding.tabLayout // If you aren't using data biniding, You can use findViewById to get the view
        var yourTabItemView = (yourTabLayoutView.getChildAt(0) as LinearLayout).getChildAt(2).layoutParams as LinearLayout.LayoutParams
        yourTabItemView.weight = 0.3f

        binding.viewPager.isUserInputEnabled = false
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
        val adapter = TabPageAdapter(this, tabLayout.tabCount)

        diaryDB
            .document("${userId}_${writeTime}")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if(document != null) {
                        if (document.exists()) {
                            viewPager.currentItem = 1
                        } else {
                            viewPager.currentItem = 0
                        }
                    }
                } else {
                    viewPager.currentItem = 0
                }
            }

        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}