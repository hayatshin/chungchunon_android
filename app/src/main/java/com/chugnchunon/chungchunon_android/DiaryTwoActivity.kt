package com.chugnchunon.chungchunon_android

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
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
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.NumberFormatException
import java.time.LocalDateTime

class DiaryTwoActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")

    private val userId = Firebase.auth.currentUser?.uid
    val writeTime = LocalDateTime.now().toString().substring(0, 10)
    private var from = ""
    private lateinit var remoteConfig: FirebaseRemoteConfig

    private var lastBackPressTime: Long = 0L

    private val binding by lazy {
        ActivityDiaryTwoBinding.inflate(layoutInflater)
    }

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        var diaryType: String? = null

        private var stepAllowBanCheck = true
        private var permissionCheck = false
        private var permissionCase: Int = 1

        const val ALL_ABOVE_13_CODE: Int = 300
        const val ALL_BELOW_13_CODE: Int = 400
        const val BAN_STEP_ABOVE_13_CODE: Int = 300
        const val BAN_STEP_BELOW_13_CODE: Int = 400

        const val ALL_REQ_CODE: Int = 500
        const val PARTNER_REQ_CODE: Int = 600
    }

    override fun onBackPressed() {

        val currentTime = System.currentTimeMillis()
        val interval = 2000L

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

        val currentAppVersion = packageManager.getPackageInfo(packageName, 0).versionCode

        // 거주 지역 설정 안되어있으면 설정하도록 넘기기
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { regionSnapShot ->
                if (regionSnapShot.contains("region") && regionSnapShot.contains("smallRegion")) {
                    // 모두 존재
                } else {
                    val goWarning = Intent(this, DefaultDiaryWarningActivity::class.java)
                    goWarning.putExtra("warningType", "emptyRegion")
                    startActivity(goWarning)
                }
            }

        // 현재 버전값 저장
        val currentVersionSet = hashMapOf(
            "currentAppVersion" to currentAppVersion
        )
        userDB.document("$userId").set(currentVersionSet, SetOptions.merge())

        // 인앱업데이트
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val gson = GsonBuilder().create()
                val result = remoteConfig.getString("app_update")

                val resultJson = gson.fromJson(result, AppUpdate::class.java)

                if (resultJson != null) {
                    try {
                        val remoteAppVersion = resultJson.app_version?.toInt()
                        val remoteForceUpdate = resultJson.force_update

//                        Toast.makeText(this, "$remoteAppVersion", Toast.LENGTH_LONG).show()

                        // 현재 버전이 remote config 버전보다 낮을 경우
                        if (currentAppVersion < remoteAppVersion!! && remoteForceUpdate!!) {

                            // 즉시 업데이트할 것
                            val window = this.window
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            window.setStatusBarColor(Color.parseColor("#B3000000"))

                            binding.updateLayout.visibility = View.VISIBLE
                            val upAnimation =
                                AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
                            binding.updateCardLayout.startAnimation(upAnimation)
                        } else {
                            // 현재 버전이 remote config 버전보다 낮지 않을 경우
                            binding.updateLayout.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                // remote config에서 버전을 가져오지 못한 경우
            }
        }

        // 버전 체크
        val android_versionCode = Build.VERSION.SDK_INT
        val android_versionName = Build.VERSION.RELEASE
        val versionSet = hashMapOf(
            "android_api_level" to android_versionCode,
            "android_release_version" to android_versionName,
        )
        userDB.document("$userId")
            .set(versionSet, SetOptions.merge())

        // diaryType 받아오기 - fragment 변동
        diaryType = intent.getStringExtra("diaryType").toString()

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
        firebaseMessaging.subscribeToTopic("$userIdFormat")

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

        uiScope.launch(Dispatchers.IO) {
            launch { permissionAssign() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    permissionNotification()
                }
            }
        }


        // 파트너 구분 x
        binding.authConfirmBtn.setOnClickListener {
            binding.authNotificationLayout.visibility = View.GONE

            if (permissionCase == 1 || permissionCase == 2) {
                // 걸음수 사용
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ),
                        ALL_ABOVE_13_CODE
                    )
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                        ),
                        ALL_BELOW_13_CODE
                    )
                }
            } else {
                // 걸음수 사용 x
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ),
                        BAN_STEP_ABOVE_13_CODE
                    )
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                        ),
                        BAN_STEP_BELOW_13_CODE
                    )
                }
            }

        }

        // 메뉴 이동
        from = intent.getStringExtra("from").toString()

        binding.bottomNav.run {
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.myTodayMenu -> {
                        diaryType = null
                        changeFragment(MyDiaryFragment())
                    }
                    R.id.ourTodayMenu -> {
                        changeFragment(AllDiaryFragmentTwo())
                    }
                    R.id.missionMenu -> {
                        diaryType = null
                        changeFragment(MissionFragment())
                    }
                    R.id.rankingMenu -> {
                        diaryType = null
                        changeFragment(RankingFragment())
                    }
                    R.id.moreMenu -> {
                        diaryType = null
                        changeFragment(MoreFragment())
                    }
                }
                true
            }

            // 초기값 세팅
            if (from == "write") {
                changeFragment(AllDiaryFragmentTwo())
                binding.bottomNav.selectedItemId = R.id.ourTodayMenu
            } else if (from == "edit") {
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

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // onDestroy to Service - 로컬브로드캐스트 전달
        var sendCloseIntent = Intent(this, MyService::class.java)
        sendCloseIntent.setAction("CLOSE_APP")
        LocalBroadcastManager.getInstance(this!!)
            .sendBroadcast(sendCloseIntent);
    }

    suspend fun permissionAssign() {
        val readContactPermissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        val stepPermissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
        val readGalleryPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val readMediaImagesPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        val postNotificationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        val userData = userDB.document("$userId").get().await()

        try {
            val stepStatus = userData.data?.getValue("step_status") as Boolean

            if (stepStatus) {
                // 걸음수 허용
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionCase = 1
                    permissionCheck =
                        readContactPermissionCheck == PackageManager.PERMISSION_GRANTED
                                && stepPermissionCheck == PackageManager.PERMISSION_GRANTED
                                && readMediaImagesPermission == PackageManager.PERMISSION_GRANTED
                                && postNotificationPermission == PackageManager.PERMISSION_GRANTED
                } else {
                    permissionCase = 2
                    permissionCheck =
                        readContactPermissionCheck == PackageManager.PERMISSION_GRANTED
                                && stepPermissionCheck == PackageManager.PERMISSION_GRANTED
                                && readGalleryPermission == PackageManager.PERMISSION_GRANTED
                }
            } else {
                // 걸음수 불허
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionCase = 3
                    permissionCheck =
                        readContactPermissionCheck == PackageManager.PERMISSION_GRANTED
                                && readMediaImagesPermission == PackageManager.PERMISSION_GRANTED
                                && postNotificationPermission == PackageManager.PERMISSION_GRANTED
                } else {
                    permissionCase = 4
                    permissionCheck =
                        readContactPermissionCheck == PackageManager.PERMISSION_GRANTED
                                && readGalleryPermission == PackageManager.PERMISSION_GRANTED
                }
            }

        } catch (e: Exception) {
            // step_status 없는 경우
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionCase = 1
                permissionCheck =
                    readContactPermissionCheck == PackageManager.PERMISSION_GRANTED
                            && stepPermissionCheck == PackageManager.PERMISSION_GRANTED
                            && readMediaImagesPermission == PackageManager.PERMISSION_GRANTED
                            && postNotificationPermission == PackageManager.PERMISSION_GRANTED
            } else {
                permissionCase = 2
                permissionCheck =
                    readContactPermissionCheck == PackageManager.PERMISSION_GRANTED
                            && stepPermissionCheck == PackageManager.PERMISSION_GRANTED
                            && readGalleryPermission == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    suspend fun permissionNotification() {
        if (!permissionCheck) {
            binding.authNotificationLayout.visibility = View.VISIBLE
        } else {
            // 모두 허용된 경우
            binding.authNotificationLayout.visibility = View.GONE

            // 배터리 제한 없음 설정 안 한 경우

            if (permissionCase == 1 || permissionCase == 2) {
                // 걸음수 사용


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    val powerManager =
                        this.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:$packageName")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        this.startActivity(intent)
                    } else {
                        // 배터리 사용량 제한없음 권한 부여 o
                        val batteryAuthSet = hashMapOf(
                            "auth_ignoring_battery" to true,
                        )
                        userDB.document("$userId").set(batteryAuthSet, SetOptions.merge())
                    }
                }

                // db에 권한 저장
                val authSet = hashMapOf(
                    "auth_step" to true,
                    "auth_contact" to true,
                    "auth_gallery" to true,
                    "auth_notification" to true,
                )
                userDB.document("$userId").set(authSet, SetOptions.merge())

                // 걸음수 호출
                try {
                    val startService = Intent(this, MyService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this, startService);
                    } else {
                        startService(startService);
                    }
                } catch (e: Exception) {
                    val serviceError = hashMapOf(
                        "service_error" to e,
                        "service_step_status" to false,
                    )
                    userDB.document("$userId")
                        .set(serviceError, SetOptions.merge())
                }

                val stepAuthIntent = Intent(this, MyDiaryFragment::class.java)
                stepAuthIntent.setAction("STEP_AUTH_UPDATE")
                stepAuthIntent.putExtra("StepAuth", true)
                LocalBroadcastManager.getInstance(this).sendBroadcast(stepAuthIntent)

            } else {
                // 걸음수 사용 x

                // db에 권한 저장
                val authSet = hashMapOf(
                    "auth_step" to false,
                    "auth_contact" to true,
                    "auth_gallery" to true,
                    "auth_notification" to true,
                    "step_status" to false,
                )
                userDB.document("$userId").set(authSet, SetOptions.merge())
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val packageName = packageName
        val intent = Intent()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        when (requestCode) {
            PARTNER_REQ_CODE -> {
                if (grantResults.size > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    ) {
                        val authSet = hashMapOf(
                            "auth_contact" to true,
                            "auth_gallery" to true,
                            "auth_notification" to true,
                            "step_status" to true,
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())
                    } else {
                        // 권한 부여 x
                        val authSet = hashMapOf(
                            "auth_step" to false,
                            "step_status" to false,
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())
                    }
                }
            }
            ALL_ABOVE_13_CODE, ALL_BELOW_13_CODE -> {
                if (grantResults.size > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        // 배터리 제한 없음 설정 안 한 경우
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val powerManager =
                                this.getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = Uri.parse("package:$packageName")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                this.startActivity(intent)
                            } else {
                                // 배터리 사용량 제한없음 권한 부여 o
                                val batteryAuthSet = hashMapOf(
                                    "auth_ignoring_battery" to true,
                                )
                                userDB.document("$userId").set(batteryAuthSet, SetOptions.merge())
                            }
                        }

                        val authSet = hashMapOf(
                            "auth_step" to true,
                            "auth_contact" to true,
                            "auth_gallery" to true,
                            "auth_notification" to true,
                            "step_status" to true,
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())

                        try {
                            val startService = Intent(this, MyService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(this, startService);
                            } else {
                                startService(startService);
                            }
                        } catch (e: Exception) {
                            val serviceError = hashMapOf(
                                "service_error" to e,
                                "service_step_status" to false,
                            )
                            userDB.document("$userId")
                                .set(serviceError, SetOptions.merge())
                        }

                        val stepAuthIntent = Intent(this, MyDiaryFragment::class.java)
                        stepAuthIntent.setAction("STEP_AUTH_UPDATE")
                        stepAuthIntent.putExtra("StepAuth", true)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(stepAuthIntent)
                    } else {
                        // 걸음수 권한 x
                        val authSet = hashMapOf(
                            "auth_step" to false,
                            "step_status" to false,
                        )
                        userDB.document("$userId").set(authSet, SetOptions.merge())
                    }

                }
            }
            BAN_STEP_ABOVE_13_CODE, BAN_STEP_BELOW_13_CODE -> {
                val authSet = hashMapOf(
                    "auth_step" to false,
                    "step_status" to false,
                )
                userDB.document("$userId").set(authSet, SetOptions.merge())

            }
        }
    }
}