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
                            if (userData.exists()) {
                                val exitReference =
                                    FirebaseFirestore.getInstance().collection("exit")
                                        .document("$userId")
                                        .get()
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                if (task.result != null) {
                                                    // 탈퇴 기록 있음
                                                    if (task.result.exists()) {
                                                        // 탈퇴 기록 있음
                                                        Log.d("탈퇴", "기록 있음")
                                                        val dbUserName =
                                                            userData.data?.getValue("name")
                                                                .toString()
                                                        if (dbUserName == "탈퇴자") {
                                                            // 진짜 탈퇴
                                                            val intent = Intent(
                                                                this,
                                                                MainActivity::class.java
                                                            )
                                                            startActivity(intent)
                                                        } else {
                                                            // 재가입
                                                            val dbUserAge =
                                                                (userData.data?.getValue("userAge")!! as Long).toInt()
                                                            val dbUserType =
                                                                userData.data?.getValue("userType")
                                                                    .toString()

                                                            val userBirthYear =
                                                                userData.data?.getValue("birthYear")!!
                                                                    .toString().toInt()
                                                            val newUserAge =
                                                                currentYear - userBirthYear + 1

                                                            if (newUserAge != dbUserAge) {
                                                                var newUserType = ""

                                                                if (dbUserType == "마스터") {
                                                                    newUserType = "마스터"
                                                                } else {
                                                                    if (newUserAge >= 50) {
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
                                                        // 탈퇴 기록 없음
                                                        Log.d("탈퇴", "기록 없음1")

                                                        val dbUserAge =
                                                            (userData.data?.getValue("userAge")!! as Long).toInt()
                                                        val dbUserType =
                                                            userData.data?.getValue("userType")
                                                                .toString()

                                                        val userBirthYear =
                                                            userData.data?.getValue("birthYear")!!
                                                                .toString().toInt()
                                                        val newUserAge =
                                                            currentYear - userBirthYear + 1

                                                        if (newUserAge != dbUserAge) {
                                                            var newUserType = ""

                                                            if (dbUserType == "마스터") {
                                                                newUserType = "마스터"
                                                            } else {
                                                                if (newUserAge >= 50) {
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
                                                    // 탈퇴 기록 없음
                                                    Log.d("탈퇴", "기록 없음2")
                                                    val dbUserAge =
                                                        (userData.data?.getValue("userAge")!! as Long).toInt()
                                                    val dbUserType =
                                                        userData.data?.getValue("userType")
                                                            .toString()

                                                    val userBirthYear =
                                                        userData.data?.getValue("birthYear")!!
                                                            .toString().toInt()
                                                    val newUserAge =
                                                        currentYear - userBirthYear + 1

                                                    if (newUserAge != dbUserAge) {
                                                        var newUserType = ""

                                                        if (dbUserType == "마스터") {
                                                            newUserType = "마스터"
                                                        } else {
                                                            if (newUserAge >= 50) {
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
                                                // 값을 가져오는데 에러 -> 탈퇴 기록 없음
                                                val dbUserAge =
                                                    (userData.data?.getValue("userAge")!! as Long).toInt()
                                                val dbUserType =
                                                    userData.data?.getValue("userType")
                                                        .toString()

                                                val userBirthYear =
                                                    userData.data?.getValue("birthYear")!!
                                                        .toString().toInt()
                                                val newUserAge =
                                                    currentYear - userBirthYear + 1

                                                if (newUserAge != dbUserAge) {
                                                    var newUserType = ""

                                                    if (dbUserType == "마스터") {
                                                        newUserType = "마스터"
                                                    } else {
                                                        if (newUserAge >= 50) {
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
                                        }
                            } else {
                                Log.d("접속", "3")
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                            }
                        } else {
                            // userDB에 없음
                            Log.d("접속", "4")
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
        } else {
            Log.d("접속", "5")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}

// userDB에 있음
//                            if (document.data!!.containsKey("smallRegion")) {
//                                // 정상 기록
//                                val goDiaryTwoActivity = Intent(this, DiaryTwoActivity::class.java)
//                                startActivity(goDiaryTwoActivity)
//                            } else {
//                                // 정상기록 x
//                                FirebaseAuth.getInstance().signOut()
//
//                                userDB.document("$userId")
//                                    .delete().addOnSuccessListener {
//                                        // 정상 기록 x
//                                        val intent = Intent(this, MainActivity::class.java)
//                                        startActivity(intent)
//                                    }
//                            }