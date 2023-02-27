package com.chugnchunon.chungchunon_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Fragment.*
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryTwoBinding
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime

class DiaryTwoActivity : AppCompatActivity() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")

    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)
    private var from = ""

    private val binding by lazy {
        ActivityDiaryTwoBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        finishAffinity()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 걸음수 권한

        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                var userType = document.data?.getValue("userType").toString()

                if(userType != "파트너") {
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
                }
            }

        from = intent.getStringExtra("from").toString()


        // 메뉴 이동
        binding.bottomNav.run {
            setOnItemSelectedListener {
                when(it.itemId) {
                    R.id.myTodayMenu -> {
                        changeFragment(MyDiaryFragment())
                    }
                    R.id.ourTodayMenu -> {
                        changeFragment(AllDiaryFragmentTwo())
                    }
                    R.id.missionMenu -> {
                        changeFragment(MissionFragment())
                    }
                    R.id.rankingMenu -> {
                        changeFragment(RankingFragment())
                    }
                    R.id.moreMenu -> {
                        changeFragment(MoreFragment())
                    }
                }
                true
            }

            // 초기값 세팅

            Log.d("이동", "${from}")

            if (from == "edit") {
                changeFragment(MyDiaryFragment())
                binding.bottomNav.selectedItemId = R.id.ourTodayMenu
            } else if (from == "delete") {
                changeFragment(AllDiaryFragment())
                binding.bottomNav.selectedItemId = R.id.ourTodayMenu
            } else {
                changeFragment(MyDiaryFragment())
                binding.bottomNav.selectedItemId = R.id.myTodayMenu

//                diaryDB
//                    .document("${userId}_${writeTime}")
//                    .get()
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            val document = task.result
//                            if (document != null) {
//                                if (document.exists()) {
//                                    changeFragment(AllDiaryFragmentTwo())
//                                    binding.bottomNav.selectedItemId = R.id.ourTodayMenu
//                                } else {
//                                    changeFragment(MyDiaryFragment())
//                                    binding.bottomNav.selectedItemId = R.id.myTodayMenu
//                                }
//                            }
//                        } else {
//                            changeFragment(MyDiaryFragment())
//                            binding.bottomNav.selectedItemId = R.id.myTodayMenu
//                        }
//                    }
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.enterFrameLayout, fragment)
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            var startService = Intent(this, MyService::class.java)
            startForegroundService(startService)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

