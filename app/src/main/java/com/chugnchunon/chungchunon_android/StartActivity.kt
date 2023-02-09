package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.icu.text.AlphabeticIndex
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.view.WindowManager
import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryActivity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StartActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val userDB = Firebase.firestore.collection("users")

    private var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE: Int = 200
    init {
        appUpdateManager = AppUpdateManagerFactory.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // 앱 업데이트
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Request the update.
                appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    AppUpdateType.IMMEDIATE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    MY_REQUEST_CODE
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        Handler(Looper.getMainLooper()).postDelayed({


            // 일반 자동 로그인
            val currentUser = auth.currentUser
            Log.d("로그인", "$currentUser")

            if (currentUser != null) {
                val userId = Firebase.auth.currentUser?.uid
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var userType = document.data?.getValue("userType")
                        Log.d("userType", "$userType")

                        if(userType == "치매예방자" || userType == "마스터") {
                            val goDiaryActivity = Intent(this, DiaryActivity::class.java)
                            startActivity(goDiaryActivity)
                        } else if (userType == "파트너") {
                            val goPartnerDiaryActivity = Intent(this, PartnerDiaryActivity::class.java)
                            startActivity(goPartnerDiaryActivity)
                        }
                    }
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

        }, 0)
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        MY_REQUEST_CODE
                    )
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // 일반 자동 로그인
            val currentUser = auth.currentUser
            Log.d("로그인", "$currentUser")

            if (currentUser != null) {
                val userId = Firebase.auth.currentUser?.uid
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var userType = document.data?.getValue("userType")
                        Log.d("userType", "$userType")

                        if(userType == "치매예방자" || userType == "마스터") {
                            val goDiaryActivity = Intent(this, DiaryActivity::class.java)
                            startActivity(goDiaryActivity)
                        } else if (userType == "파트너") {
                            val goPartnerDiaryActivity = Intent(this, PartnerDiaryActivity::class.java)
                            startActivity(goPartnerDiaryActivity)
                        }
                    }
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

        }, 0)
    }
}