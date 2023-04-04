package com.chugnchunon.chungchunon_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chugnchunon.chungchunon_android.DataClass.AppUpdate
import com.chugnchunon.chungchunon_android.Fragment.*
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryTwoBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.GsonBuilder
import java.time.LocalDateTime

class DiaryTwoActivity : AppCompatActivity() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")

    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)
    private var from = ""
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val _mutableLiveData = MutableLiveData<AppUpdate>()
    val remoteLiveData: LiveData<AppUpdate> = _mutableLiveData

    private var diaryType = ""

    private val binding by lazy {
        ActivityDiaryTwoBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        finishAffinity()
        finish()

//        finish()
//        System.exit(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 인앱업데이트
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                var gson = GsonBuilder().create()
                var result = remoteConfig.getString("app_update")
                var resultJson = gson.fromJson(result, AppUpdate::class.java)

                val currentAppVersion = packageManager.getPackageInfo(packageName, 0).versionCode
                // 인앱 업데이트 - 즉시업데이트 - resultJson.app_version.toInt()
                if(currentAppVersion < resultJson.app_version.toInt() && resultJson.force_update as Boolean) {
                    // 즉시 업데이트할 것.
                var window = this.window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.parseColor("#CC000000"));

                    binding.updateLayout.visibility = View.VISIBLE
                } else {
                    binding.updateLayout.visibility = View.GONE
                }

            } else {
            }
        }

         diaryType = intent.getStringExtra("diaryType").toString()

        binding.updateCancelBox.setOnClickListener {
            binding.updateLayout.visibility = View.GONE

            var window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE);
        }

        binding.updateConfirmBox.setOnClickListener {
            val uri =
                "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android"
            val goUpdateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(goUpdateIntent)
        }

        // 걸음수 권한
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                var userType = document.data?.getValue("userType").toString()

                if (userType != "파트너") {
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

//                    var startService = Intent(this, MyService::class.java)
//
//                    //오레오 이상부터 동작하는 코드
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        startForegroundService(startService);
//                    } else {
//                        startService(startService);
//                    }

                }
            }

        from = intent.getStringExtra("from").toString()


        // 메뉴 이동
        binding.bottomNav.run {
            setOnItemSelectedListener {
                when (it.itemId) {
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
                changeFragment(AllDiaryFragmentTwo())
                binding.bottomNav.selectedItemId = R.id.ourTodayMenu
            } else if (from == "delete") {
                changeFragment(AllDiaryFragmentTwo())
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
        val data = Bundle()
        data.putString("diaryType", diaryType)
        fragment.arguments = data

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

            //오레오 이상부터 동작하는 코드
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startService);
            } else {
                startService(startService);
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}
