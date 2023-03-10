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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Adapter.RegionPagerAdapter
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.Fragment.SmallRegionRegisterFragment
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryTwoActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityRegionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.*


class RegionRegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegionBinding.inflate(layoutInflater)
    }

    companion object {
        var editRegionCheck: Boolean = false
    }

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
            if(!RegionRegisterFragment.smallRegionCheck)  {
                // ????????? ??????

                binding.regionResult.text = ""
                finish()
            } else {
                // ????????? ?????? -> ????????? ??????

                binding.backBtn.visibility = View.GONE

                Log.d("??????", "??????")
                binding.regionRegisterBtn.isEnabled = false
                RegionRegisterFragment.smallRegionCheck = false

                binding.regionDescription.text = "??????????????? ???????????? ???????????????."
                binding.smallRegionResult.text = ""

                setupViewPager()
            }
        }



        var userType = intent.getStringExtra("userType")
        var userAge = intent.getIntExtra("userAge", 0)


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
            var regionSet = hashMapOf(
                "userId" to userId,
                "region" to selectedRegion,
                "smallRegion" to selectedSmallRegion
            )
            userDB.document("$userId")
                .set(regionSet, SetOptions.merge())
                .addOnSuccessListener {

                    if(userType == "?????????" || (userAge >= 50 && userType == "?????????")) {
                        var goDiary =
                            Intent(applicationContext, DiaryTwoActivity::class.java)
                        startActivity(goDiary)
                    } else if (userType == "?????????"  || (userAge < 50 && userType == "?????????")){
                        var goDiary = Intent(
                            applicationContext,
                            PartnerDiaryTwoActivity::class.java
                        )
                        startActivity(goDiary)
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
            binding.regionDescription.text = "?????? ??????????????? ???????????? ???????????????."

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
            binding.regionDescription.text = "??? ???????????? ????????? ???????????????"
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
