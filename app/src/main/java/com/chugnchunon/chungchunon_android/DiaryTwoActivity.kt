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
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.DataClass.AppUpdate
import com.chugnchunon.chungchunon_android.Fragment.*
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDiaryTwoBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
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
    val remoteLiveData: LiveData<AppUpdate> = _mutableLiveData

    private var diaryType = ""
    private var lastBackPressTime: Long = 0

    private val binding by lazy {
        ActivityDiaryTwoBinding.inflate(layoutInflater)
    }

    companion object {
        val STEP_REQ_CODE: Int = 100
        val CONTACT_REQ_CODE: Int = 200
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
                if (currentAppVersion < resultJson.app_version.toInt() && resultJson.force_update as Boolean) {
                    // 즉시 업데이트할 것.
                    var window = this.window
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.parseColor("#CC000000"));

                    binding.updateLayout.visibility = View.VISIBLE
                    var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
                    binding.updateCardLayout.startAnimation(upAnimation)

                } else {
                    // GONE
                    binding.updateLayout.visibility = View.GONE
                }

            } else {
            }
        }

        diaryType = intent.getStringExtra("diaryType").toString()

        binding.updateCancelBox.setOnClickListener {

            var window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE);

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.updateCardLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                binding.updateLayout.visibility = View.GONE
            }, 500)
        }

        binding.updateConfirmBox.setOnClickListener {
            val uri =
                "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android"
            val goUpdateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(goUpdateIntent)
        }

        // 댓글 푸시 구독
        val userIdFormat = userId!!.replace(":", "")
        val firebaseMessaging = FirebaseMessaging.getInstance()
        firebaseMessaging.subscribeToTopic("/topics/$userIdFormat")

        // FCM 토큰 저장
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                try {
                    val myFcmToken = document.data?.getValue("fcmToken").toString()

                } catch (e: Exception) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
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
        var readContactPermissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)

        if (readContactPermissionCheck == PackageManager.PERMISSION_DENIED) {

            // 걸음수 & 휴대폰 연동
            userDB.document("$userId").get()
                .addOnSuccessListener { document ->
                    var userType = document.data?.getValue("userType").toString()

                    if (userType != "파트너") {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACTIVITY_RECOGNITION,
                            ) == PackageManager.PERMISSION_DENIED
                        ) {
                            db.collection("user_step_count")
                                .document("$userId")
                                .get()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        var userSnap = task.result
                                        if (userSnap.exists()) {
                                            if (userSnap.getLong("dummy") != null) {

                                                var deleteDummy = hashMapOf<String, Any>(
                                                    "dummy" to FieldValue.delete()
                                                )

                                                db.collection("user_step_count")
                                                    .document("${userId}")
                                                    .update(deleteDummy)
                                                    .addOnSuccessListener {
                                                        requestPermissions(
                                                            arrayOf(
                                                                Manifest.permission.ACTIVITY_RECOGNITION,
                                                                Manifest.permission.READ_CONTACTS
                                                            ),
                                                            STEP_REQ_CODE
                                                        )
                                                    }
                                            } else {
                                                // 걸음수, 휴대폰 연동 권한
                                                requestPermissions(
                                                    arrayOf(
                                                        Manifest.permission.ACTIVITY_RECOGNITION,
                                                        Manifest.permission.READ_CONTACTS
                                                    ),
                                                    STEP_REQ_CODE
                                                )
                                            }
                                        }

                                    }
                                }
                        } else {
                            // 휴대폰 연동 권한
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_CONTACTS),
                                CONTACT_REQ_CODE
                            )
                        }
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            CONTACT_REQ_CODE
                        )
                    }
                }
        } else {

            // 휴대폰 연동 O , 걸음수 권한 x
            userDB.document("$userId").get()
                .addOnSuccessListener { document ->
                    var userType = document.data?.getValue("userType").toString()

                    if (userType != "파트너") {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACTIVITY_RECOGNITION,
                            ) == PackageManager.PERMISSION_DENIED
                        ) {
                            db.collection("user_step_count")
                                .document("$userId")
                                .get()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        var userSnap = task.result
                                        if (userSnap.exists()) {
                                            if (userSnap.getLong("dummy") != null) {
                                                // dummy 있음
                                                var deleteDummy = hashMapOf<String, Any>(
                                                    "dummy" to FieldValue.delete()
                                                )

                                                db.collection("user_step_count")
                                                    .document("${userId}")
                                                    .update(deleteDummy)
                                                    .addOnSuccessListener {
                                                        requestPermissions(
                                                            arrayOf(
                                                                Manifest.permission.ACTIVITY_RECOGNITION,
                                                            ),
                                                            STEP_REQ_CODE
                                                        )
                                                    }
                                            } else {
                                                // dummy 없음
                                                requestPermissions(
                                                    arrayOf(
                                                        Manifest.permission.ACTIVITY_RECOGNITION,
                                                    ),
                                                    STEP_REQ_CODE
                                                )
                                            }
                                        }

                                    }
                                }
                        } else {
                            // 휴대폰 o, 걸음수 o

                            var startService = Intent(this, MyService::class.java)
                            //오레오 이상부터 동작하는 코드
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(this, startService);
                            } else {
                                startService(startService);
                            }
                        }
                    }
                }
        }

        // 메뉴 이동
        from = intent.getStringExtra("from").toString()
        var notificationDiaryId = intent?.getStringExtra("notificationDiaryId")

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
                if (notificationDiaryId != null) {
                    changeFragment(AllDiaryFragmentTwo())
                    binding.bottomNav.selectedItemId = R.id.ourTodayMenu
                } else {
                    changeFragment(MyDiaryFragment())
                    binding.bottomNav.selectedItemId = R.id.myTodayMenu

                }
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

        // 배터리 사용 제한없음 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                this.startActivity(intent)
            }
        }

        when (requestCode) {
            STEP_REQ_CODE -> {
                if (grantResults.size > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        var startService = Intent(this, MyService::class.java)
                        //오레오 이상부터 동작하는 코드
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this, startService);
                        } else {
                            startService(startService);
                        }
                    }
                }
            }
            CONTACT_REQ_CODE -> {
                if (grantResults.size > 0) {
                    var startService = Intent(this, MyService::class.java)
                    //오레오 이상부터 동작하는 코드
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this, startService);
                    } else {
                        startService(startService);
                    }
                }
            }
        }

    }
}
