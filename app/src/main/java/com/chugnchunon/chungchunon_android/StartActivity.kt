package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.auth.KakaoSDK
import com.kakao.sdk.common.KakaoSdk
import io.grpc.internal.ConscryptLoader
import java.security.Security
import java.util.*

class StartActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private var lastBackPressTime: Long = 0L

    companion object {
    }

    override fun onBackPressed() {
        super.onBackPressed()

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
        setContentView(R.layout.activity_start)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun onResume() {
        super.onResume()


        // 상태바 화이트
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        val currentUser = auth.currentUser
        val userId = Firebase.auth.currentUser?.uid

        val timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        val currentYear = Calendar.getInstance(timeZone).get(Calendar.YEAR)

        if (userId != null) {
            userDB.document("$userId").get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userData = task.result
                        if (userData != null) {
                            val dbUserAge =
                                (userData.data?.getValue("userAge")!! as Long).toInt()
                            val dbUserType =
                                userData.data?.getValue("userType")
                                    .toString()

                            val userBirthYear =
                                userData.data?.getValue("birthYear")!!
                                    .toString().toInt()
                            val userBirthDay =
                                userData.data?.getValue("birthDay")!!
                                    .toString().toInt()
                            val newUserAge = DateFormat().calculateAge(userBirthYear, userBirthDay)

                            if (newUserAge != dbUserAge) {
                                var newUserType = ""

                                if (dbUserType == "마스터") {
                                    newUserType = "마스터"
                                } else {
                                    if (newUserAge >= 40) {
                                        newUserType = "사용자"
                                    } else {
                                        newUserType = "파트너"
                                    }
                                }

                                val newUserAgeSet = hashMapOf(
                                    "userAge" to newUserAge,
                                    "userType" to newUserType
                                )

                                userDB.document(userId)
                                    .set(
                                        newUserAgeSet,
                                        SetOptions.merge()
                                    )
                                    .addOnSuccessListener {
                                        val goDiaryTwoActivity =
                                            Intent(
                                                this,
                                                DiaryTwoActivity::class.java
                                            )
                                        startActivity(
                                            goDiaryTwoActivity
                                        )
                                    }
                            } else {
                                val goDiaryTwoActivity =
                                    Intent(
                                        this,
                                        DiaryTwoActivity::class.java
                                    )
                                startActivity(
                                    goDiaryTwoActivity
                                )
                            }
                        }
                    } else {
                        val goMainActivity =
                            Intent(
                                this,
                                MainActivity::class.java
                            )
                        startActivity(
                            goMainActivity
                        )
                    }
                }
        } else {
            val goMainActivity =
                Intent(
                    this,
                    MainActivity::class.java
                )
            startActivity(
                goMainActivity
            )
        }
    }
}
