package com.chugnchunon.chungchunon_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.AppUpdate
import com.chugnchunon.chungchunon_android.Fragment.*
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryTwoBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.GsonBuilder
import java.time.LocalDateTime

class DiaryTwoActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")

    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)
    private var from = ""
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val _mutableLiveData = MutableLiveData<AppUpdate>()

    private var diaryType = ""
    private var lastBackPressTime: Long = 0

    private val binding by lazy {
        ActivityDiaryTwoBinding.inflate(layoutInflater)
    }

    companion object {
        const val STEP_CONTACT_REQ_CODE: Int = 100
        const val STEP_REQ_CODE: Int = 200
        const val CONTACT_REQ_CODE: Int = 300
        const val IGNORING_BATTERY_OPT_REQ_CODE: Int = 400
    }

    override fun onBackPressed() {

        val currentTime = System.currentTimeMillis()
        val interval = 2000

        if (currentTime - lastBackPressTime < interval) {
            super.onBackPressed()
            finishAffinity()
            finish()
        } else {
            lastBackPressTime = currentTime
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // diaryType 받아오기 - fragment 변동
        diaryType = intent.getStringExtra("diaryType").toString()

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

                // 현재 버전값 저장
                val currentVersionSet = hashMapOf(
                    "currentAppVersion" to currentAppVersion
                )
                userDB.document("$userId").set(currentVersionSet, SetOptions.merge())

                // 현재 버전이 remote config 버전보다 낮을 경우
                if (currentAppVersion < resultJson.app_version.toInt() && resultJson.force_update as Boolean) {
                    // 즉시 업데이트할 것
                    var window = this.window
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.parseColor("#B3000000"))

                    binding.updateLayout.visibility = View.VISIBLE
                    var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
                    binding.updateCardLayout.startAnimation(upAnimation)
                } else {
                    // 현재 버전이 remote config 버전보다 낮지 않을 경우
                    binding.updateLayout.visibility = View.GONE
                }
            } else {
                // remote config에서 버전을 가져오지 못한 경우
            }
        }


        // 인앱업데이트 취소 클릭
        binding.updateCancelBox.setOnClickListener {
            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.updateCardLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                binding.updateLayout.visibility = View.GONE

                var window = this.window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.WHITE);
            }, 500)
        }

        // 인앱업데이트 확인 클릭
        binding.updateConfirmBox.setOnClickListener {
            val uri =
                "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android"
            val goUpdateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(goUpdateIntent)
        }

        // 댓글 푸시 노티피케이션 구독
        val userIdFormat = userId!!.replace(":", "")
        val firebaseMessaging = FirebaseMessaging.getInstance()
        firebaseMessaging.subscribeToTopic("/topics/$userIdFormat")

        // FCM 토큰 저장
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                try {
                    // 발급 받은 토큰이 이미 있는 경우
                    val myFcmToken = document.data?.getValue("fcmToken").toString()

                } catch (e: Exception) {
                    // 발급 받은 토큰이 없는 경우

                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            return@OnCompleteListener
                        }

                        // 발급후 토큰 저장
                        val fcmToken = task.result
                        val fcmTokenSet = hashMapOf(
                            "fcmToken" to fcmToken
                        )
                        userDB.document("$userId")
                            .set(fcmTokenSet, SetOptions.merge())
                    })
                }
            }

        // 권한 체크
        val readContactPermissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        val stepPermissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)

        // 파트너 o -> 휴대폰 연동만 체크
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                var userType = document.data?.getValue("userType").toString()
                if (userType == "파트너") {
                    if (readContactPermissionCheck == PackageManager.PERMISSION_DENIED) {
                        // 휴대폰 연동 x
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            CONTACT_REQ_CODE
                        )
                    } else {
                        // 휴대폰 연동 o
                    }
                } else {
                    // 파트너 x -> (휴대폰 연동 -> 걸음수) -> 배터리 사용 제한없음

                    // 친구 연동 포함

                    if (readContactPermissionCheck == PackageManager.PERMISSION_DENIED && stepPermissionCheck == PackageManager.PERMISSION_DENIED) {
                        // 휴대폰 연동 x, 걸음수 x
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.ACTIVITY_RECOGNITION,
                                Manifest.permission.READ_CONTACTS
                            ),
                            STEP_CONTACT_REQ_CODE
                        )
                    } else if (readContactPermissionCheck == PackageManager.PERMISSION_DENIED && stepPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                        // 휴대폰 연동 x, 걸음수 o
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            CONTACT_REQ_CODE
                        )
                    } else if (readContactPermissionCheck == PackageManager.PERMISSION_GRANTED && stepPermissionCheck == PackageManager.PERMISSION_DENIED) {
                        // 휴대폰 연동 o, 걸음수 x
                        requestPermissions(
                            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                            STEP_REQ_CODE
                        )
                    } else if (readContactPermissionCheck == PackageManager.PERMISSION_GRANTED && stepPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                        // 휴대폰 연동 o, 걸음수 o

                        // db에 권한 저장
                        var authSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to true
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())

                        // 걸음수 호출
                        val startService = Intent(this, MyService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this, startService);
                        } else {
                            startService(startService);
                        }

                        val stepAuthIntent = Intent(this, MyDiaryFragment::class.java)
                        stepAuthIntent.setAction("STEP_AUTH_UPDATE")
                        stepAuthIntent.putExtra("StepAuth", true)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(stepAuthIntent)
                    }
                }
            }




        // 배터리 제한 없음 설정 안 한 경우
//        val packageName = packageName
//        val intent = Intent()
//        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
//
//        if(!pm.isIgnoringBatteryOptimizations(packageName)) {
//            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
//            intent.data = Uri.parse("packageName:$packageName")
//            startActivityForResult(intent, IGNORING_BATTERY_OPT_REQ_CODE)
//        } else {
//            val batteryAuthSet = hashMapOf(
//                "auth_ignoring_battery" to true,
//            )
//            userDB.document("$userId").set(batteryAuthSet, SetOptions.merge())
//        }

        // 메뉴 이동
        from = intent.getStringExtra("from").toString()

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
            if (from == "edit") {
                changeFragment(AllDiaryFragmentTwo())
                binding.bottomNav.selectedItemId = R.id.ourTodayMenu
            } else if (from == "delete") {
                changeFragment(AllDiaryFragmentTwo())
                binding.bottomNav.selectedItemId = R.id.ourTodayMenu
            } else {
                // 댓글 푸시 알림
                if (intent.hasExtra("notificationDiaryId")) {
                    // 들어오는 경우
                    changeFragment(AllDiaryFragmentTwo())
                    binding.bottomNav.selectedItemId = R.id.ourTodayMenu
                } else {
                    changeFragment(MyDiaryFragment())
                    binding.bottomNav.selectedItemId = R.id.myTodayMenu

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // onDestroy to Service - 로컬브로드캐스트 전달
        var sendCloseIntent = Intent(this, MyService::class.java)
        sendCloseIntent.setAction("CLOSE_APP")
        LocalBroadcastManager.getInstance(this!!)
            .sendBroadcast(sendCloseIntent);
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val packageName = packageName
        val intent = Intent()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        when (requestCode) {
            STEP_REQ_CODE -> {
                if (grantResults.size > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // 걸음수 권한 부여

                        // DB 저장
                        val authSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to true
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())

                        // 걸음수 서비스 호출
                        val startService = Intent(this, MyService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this, startService);
                        } else {
                            startService(startService);
                        }

                        val stepAuthIntent = Intent(this, MyDiaryFragment::class.java)
                        stepAuthIntent.setAction("STEP_AUTH_UPDATE")
                        stepAuthIntent.putExtra("StepAuth", true)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(stepAuthIntent)

                    } else {
                        // 걸음수 권한 부여 x
                        val authSet = hashMapOf(
                            "auth_step" to false,
                            "auth_contact" to true
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())
                    }
                }
            }
            STEP_CONTACT_REQ_CODE -> {
                // 0 걸음수, 1 휴대폰 연동
                if (grantResults.size > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        // 걸음수 권한 부여 o, 휴대폰 연동 권한 부여 o

                        val authSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to true
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())

                        val startService = Intent(this, MyService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this, startService);
                        } else {
                            startService(startService);
                        }

                        val stepAuthIntent = Intent(this, MyDiaryFragment::class.java)
                        stepAuthIntent.setAction("STEP_AUTH_UPDATE")
                        stepAuthIntent.putExtra("StepAuth", true)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(stepAuthIntent)

                    } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        // 걸음수 권한 부여 o, 휴대폰 연동 권한 부여 x

                        val stepAuthSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to false
                        )
                        userDB.document("$userId").set(stepAuthSet, SetOptions.merge())

                        val startService = Intent(this, MyService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this, startService);
                        } else {
                            startService(startService);
                        }

                        val stepAuthIntent = Intent(this, MyDiaryFragment::class.java)
                        stepAuthIntent.setAction("STEP_AUTH_UPDATE")
                        stepAuthIntent.putExtra("StepAuth", true)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(stepAuthIntent)

                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        // 걸음수 권한 부여 x, 휴대폰 연동 권한 부여 o
                        val stepAuthSet = hashMapOf(
                            "auth_step" to false,
                            "auth_contact" to true
                        )
                        userDB.document("$userId").set(stepAuthSet, SetOptions.merge())
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        // 걸음수 권한 부여 x, 휴대폰 연동 권한 부여 x
                        val stepAuthSet = hashMapOf(
                            "auth_step" to false,
                            "auth_contact" to false
                        )
                        userDB.document("$userId").set(stepAuthSet, SetOptions.merge())
                    }

                }
            }
            CONTACT_REQ_CODE -> {
                if (grantResults.size > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // 휴대폰 연동 권한 부여 o
                        val authSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to true
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())
                    } else {
                        // 휴대폰 연동 권한 부여 x
                        val authSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to false
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())
                    }

                    val startService = Intent(this, MyService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this, startService);
                    } else {
                        startService(startService)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == IGNORING_BATTERY_OPT_REQ_CODE) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if(pm.isIgnoringBatteryOptimizations(packageName)) {
                // 배터리 사용량 제한없음 권한 부여 o
                val batteryAuthSet = hashMapOf(
                    "auth_ignoring_battery" to true,
                )
                userDB.document("$userId").set(batteryAuthSet, SetOptions.merge())
            } else {
                // 권한 부여 x
                val batteryAuthSet = hashMapOf(
                    "auth_ignoring_battery" to false,
                )
                userDB.document("$userId").set(batteryAuthSet, SetOptions.merge())
            }
        }
    }
}