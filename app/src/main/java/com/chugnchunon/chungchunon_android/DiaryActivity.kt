package com.chugnchunon.chungchunon_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.PartnerTabPageAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.*
import java.time.LocalDateTime

class DiaryActivity : AppCompatActivity() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")

    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)
    private var from = ""

    private val binding by lazy {
        ActivityDiaryBinding.inflate(layoutInflater)
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

        from = intent.getStringExtra("from").toString()

        val yourTabLayoutView =
            binding.tabLayout // If you aren't using data biniding, You can use findViewById to get the view
        var yourTabItemView =
            (yourTabLayoutView.getChildAt(0) as LinearLayout).getChildAt(2).layoutParams as LinearLayout.LayoutParams
        yourTabItemView.weight = 0.3f

        binding.viewPager.isUserInputEnabled = false

        setUpTabBar()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Log.d("걸음수", "요청")
            var startService = Intent(this, MyService::class.java)
            startForegroundService(startService)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun setUpTabBar() {
        Log.d("결과결과", "$from")

        val adapter = PartnerTabPageAdapter(this, tabLayout.tabCount)
        viewPager.adapter = adapter


        if (from == "edit") {
            viewPager.setCurrentItem(0, false)
            binding.tabLayout.getTabAt(0)?.select()
        } else if (from == "delete") {
            viewPager.setCurrentItem(1, false)
            binding.tabLayout.getTabAt(1)?.select()
        } else {
            diaryDB
                .document("${userId}_${writeTime}")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document != null) {
                            if (document.exists()) {
                                viewPager.setCurrentItem(1, false)
                                binding.tabLayout.getTabAt(1)?.select()
                            } else {
                                viewPager.setCurrentItem(0, false)
                                binding.tabLayout.getTabAt(0)?.select()
                            }
                        }
                    } else {
                        viewPager.setCurrentItem(0, false)
                        binding.tabLayout.getTabAt(0)?.select()
                    }
                }
        }



        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

}