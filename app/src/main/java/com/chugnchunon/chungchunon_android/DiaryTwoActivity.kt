package com.chugnchunon.chungchunon_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
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

        // ??????????????????
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
                // ?????? ???????????? - ?????????????????? - resultJson.app_version.toInt()
                if(currentAppVersion < resultJson.app_version.toInt() && resultJson.force_update as Boolean) {
                    // ?????? ??????????????? ???.
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

        // ????????? ??????

        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                var userType = document.data?.getValue("userType").toString()

                if (userType != "?????????") {
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


        // ?????? ??????
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

            // ????????? ??????

            Log.d("??????", "${from}")

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
