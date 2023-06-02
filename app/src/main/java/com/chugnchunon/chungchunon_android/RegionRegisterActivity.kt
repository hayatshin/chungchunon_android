package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Adapter.RegionPagerAdapter
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.databinding.ActivityRegionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class RegionRegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegionBinding.inflate(layoutInflater)
    }

    var db = Firebase.firestore
    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var selectedRegion: String = ""
    var selectedSmallRegion: String = ""

    lateinit var adapter: RegionPagerAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.regionViewPager.isUserInputEnabled = false

        binding.regionRegisterBtn.isEnabled = false
        binding.backBtn.visibility = View.GONE

        binding.backBtn.setOnClickListener {
            if (!smallRegionCheck) {
                // 첫번째 화면
                binding.regionResult.setText(null)
                finish()
            } else {
                // 두번째 화면 -> 첫번째 화면
                binding.backBtn.visibility = View.GONE

                binding.regionRegisterBtn.isEnabled = false
                smallRegionCheck = false

                binding.regionDescription.text = "거주지역을 목록에서 선택하세요."
                binding.regionResult.setText(null)
                binding.smallRegionResult.setText(null)

                setupViewPager()
            }
        }

//        var userType = intent.getStringExtra("userType")
//        var userAge = intent.getIntExtra("userAge", 0)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter("REGION_BROADCAST")
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
            smMessageReceiver,
            IntentFilter("SMALL_REGION_BROADCAST")
        );

        setupViewPager()

        binding.regionRegisterBtn.setOnClickListener {
            val regionSet = hashMapOf(
                "userId" to userId,
                "region" to selectedRegion,
                "smallRegion" to selectedSmallRegion
            )
            // 다음 페이지
            userDB.document("$userId")
                .set(regionSet, SetOptions.merge())
                .addOnSuccessListener {

                    userDB.document("$userId")
                        .get()
                        .addOnSuccessListener { userData ->
                            if(!userData.contains("community")) {
                                db.collection("community")
                                    .get()
                                    .addOnCompleteListener { task ->
                                        if(task.isSuccessful) {
                                            val document = task.result
                                            if(document != null) {
                                                if(!document.isEmpty) {
                                                    // 소속기관 있음
                                                    val goCommunity = Intent(applicationContext, CommunityRegisterActivity::class.java)
                                                    startActivity(goCommunity)
                                                } else {
                                                    // 소속기관 없음
                                                    val goDiary = Intent(applicationContext, DiaryTwoActivity::class.java)
                                                    startActivity(goDiary)
                                                }
                                            } else {
                                                // 소속기관 없음
                                                val goDiary = Intent(applicationContext, DiaryTwoActivity::class.java)
                                                startActivity(goDiary)
                                            }
                                        }
                                    }
                            } else {
                                val goDiary = Intent(applicationContext, DiaryTwoActivity::class.java)
                                startActivity(goDiary)
                            }
                        }

                }
        }
    }

    private fun setupViewPager() {
        adapter = RegionPagerAdapter(this)
        val viewPager = binding.regionViewPager
        viewPager.adapter = adapter
    }


    var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.regionRegisterBtn.isEnabled = false
            binding.backBtn.visibility = View.VISIBLE


            smallRegionCheck = intent?.getBooleanExtra("smallRegionCheck", true)!!
            selectedRegion = intent?.getStringExtra("selectedRegion").toString()
            binding.regionResult.text = spanTextFn(selectedRegion)
            binding.regionDescription.text = "세부 거주지역을 목록에서 선택하세요."

            var pref = getSharedPreferences("REGION_PREF", Context.MODE_PRIVATE).edit()
            pref.putString("selectedRegion", selectedRegion)
            pref.apply()

            setupViewPager()
        }
    }


    var smMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.regionRegisterBtn.isEnabled = true

            selectedSmallRegion = intent?.getStringExtra("selectedSmallRegion").toString()
            binding.smallRegionResult.text = spanTextFn(selectedSmallRegion)
            binding.regionDescription.text = "다음 버튼을 눌러주세요"
        }
    }

    private fun spanTextFn(text: String): Spannable {
        var spanText = Spannable.Factory.getInstance().newSpannable(text)
        var color = ContextCompat.getColor(this, R.color.light_main_color)
        spanText.setSpan(
            BackgroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spanText
    }
}
