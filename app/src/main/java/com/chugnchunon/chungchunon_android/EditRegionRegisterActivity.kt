package com.chugnchunon.chungchunon_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Adapter.RegionPagerAdapter
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityRegionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class EditRegionRegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegionBinding.inflate(layoutInflater)
    }

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var selectedRegion: String = ""
    var selectedSmallRegion: String = ""

    companion object {
        var editRegionCheck: Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.regionRegisterBtn.isEnabled = false

        binding.regionViewPager.isUserInputEnabled = false


        binding.backBtn.setOnClickListener {
            if(!RegionRegisterFragment.smallRegionCheck)  {
                // 첫번째 화면

                binding.regionResult.text = ""
                finish()
            } else {
                // 두번째 화면 -> 첫번째 화면
                Log.d("결과", "클릭")
                binding.regionRegisterBtn.isEnabled = false
                RegionRegisterFragment.smallRegionCheck = false

                binding.regionDescription.text = "거주지역을 목록에서 선택하세요."
                binding.smallRegionResult.text = ""

                setupViewPager()
            }
        }


        binding.regionRegisterBtn.text = "지역 수정하기"

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

            var intent = Intent(this, EditProfileActivity::class.java)
            intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            intent.putExtra("region", selectedRegion)
            intent.putExtra("smallRegion", selectedSmallRegion)
            setResult(RESULT_OK, intent)
            finish()


//            var regionIntent = Intent(this, EditProfileActivity::class.java)
//            regionIntent.setAction("EDIT_REGION")
//            regionIntent.putExtra("region", selectedRegion)
//            regionIntent.putExtra("smallRegion", selectedSmallRegion)
//            startActivity(regionIntent)
        }
    }


    private fun setupViewPager() {
        val adapter = RegionPagerAdapter(this)
        val viewPager = binding.regionViewPager
        viewPager.adapter = adapter
    }


    var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?){
            binding.regionRegisterBtn.isEnabled = false

            var smallRegionCheck = intent?.getBooleanExtra("smallRegionCheck", true)!!
            selectedRegion = intent?.getStringExtra("selectedRegion").toString()
            Log.d("지역수정5", "$selectedRegion")

            binding.regionResult.text = spanTextFn(selectedRegion)
            binding.regionDescription.text="세부 거주지역을 목록에서 선택하세요."

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
            Log.d("지역수정6", "$selectedSmallRegion")

            binding.smallRegionResult.text = spanTextFn(selectedSmallRegion)
            binding.regionDescription.text="지역 수정하기 버튼을 눌러주세요"
        }
    }
    private fun spanTextFn(text: String): Spannable {
        var spanText = Spannable.Factory.getInstance().newSpannable(text)
        var color = ContextCompat.getColor(this, R.color.light_main_color)
        spanText.setSpan(BackgroundColorSpan(color), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spanText
    }


}

