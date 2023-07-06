package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.KakaoLogin.SessionCallback
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDefaultCancelWarningBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class DefaultCancelWarningActivity : Activity() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userAuth = FirebaseAuth.getInstance().currentUser
    private val userId = Firebase.auth.currentUser?.uid

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    private val binding by lazy {
        ActivityDefaultCancelWarningBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val warningType = intent?.getStringExtra("warningType")

        binding.gobackArrow.setOnClickListener {
            finish()
        }

        binding.cancelBox.setOnClickListener {
            finish()
        }

        if (warningType == "exit") {
            // 탈퇴
            binding.warningText.text = "정말로 탈퇴하시겠습니까?"
            binding.confirmBox.setOnClickListener {

                Toast.makeText(this, "잠시만 기다려주세요..", Toast.LENGTH_LONG).show()

                binding.confirmBox.isFocusable = false
                binding.confirmBox.isClickable = false
                binding.confirmBox.isFocusableInTouchMode = false
                binding.cancelBox.isFocusable = false
                binding.cancelBox.isClickable = false
                binding.cancelBox.isFocusableInTouchMode = false

                uiScope.launch(Dispatchers.IO) {
                    launch { recordExitCollection() }.join()
                    launch { recordUserCollection() }.join()
                    withContext(Dispatchers.Main) {
                        launch {
                            val exitIntent = Intent(applicationContext, MyService::class.java)
                            exitIntent.setAction("EXIT_APP")
                            LocalBroadcastManager.getInstance(applicationContext)
                                .sendBroadcast(exitIntent)

                            val goMain = Intent(applicationContext, MainActivity::class.java)
                            goMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(goMain)
                        }
                    }
                }

            }
        } else if (warningType == "banStep") {
            binding.warningText.text = "걸음수 측정을 끄면\n걸음수 점수가 더해지지 않습니다.\n\n정말로 걸음수 측정을 끄시겠습니까?"

            binding.confirmBox.setOnClickListener {
                val stepStatusSet = hashMapOf(
                    "step_status" to false,
                )

                userDB.document("$userId")
                    .set(stepStatusSet, SetOptions.merge())
                    .addOnSuccessListener {
                        val exitIntent = Intent(this, MyService::class.java)
                        exitIntent.setAction("EXIT_APP")
                        LocalBroadcastManager.getInstance(this)
                            .sendBroadcast(exitIntent)

                        finish()
                    }
            }

        }
    }

    suspend fun recordExitCollection() {

        val userDataReference =
            FirebaseFirestore.getInstance().collection("users").document("$userId")
        val userDataReferenceResult = userDataReference.get().await()

        if (userDataReferenceResult != null && userDataReferenceResult.exists()) {
            val exitTimestamp = hashMapOf(
                "exitTimestamp" to FieldValue.serverTimestamp()
            )

            db.collection("exit").document("$userId")
                .set(exitTimestamp, SetOptions.merge()).addOnSuccessListener {
                    val userDataSet = userDataReferenceResult.data
                    for ((userKey, userValue) in userDataSet!!.entries) {
                        val userDataSet = hashMapOf(
                            userKey to userValue
                        )
                        db.collection("exit").document("$userId")
                            .set(userDataSet, SetOptions.merge())
                    }
                }
        }
    }

    suspend fun recordUserCollection() {

        val exitUserAvatar =
            "https://postfiles.pstatic.net/MjAyMzA1MTdfNTEg/MDAxNjg0MzAwMTE1NTg4.Ut_2NzdCmpjurruKjSwqWSH-c0_ONiJZM2Mn-ib-uSQg.qX8hjpYrVpE6Nlnnmcs1J780Ycwnl4WIuMLX-tpgVT8g.PNG.hayat_shin/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-05-17_%EC%98%A4%ED%9B%84_2.08.31.png?type=w773"

        val exitUserSet = hashMapOf(
            "name" to "탈퇴자",
            "avatar" to exitUserAvatar,
            "phone" to "010-0000-0000",
            "todayStepCount" to 0,
            "blockUserList" to ArrayList<String>(),
        )
        userDB.document("$userId").set(exitUserSet, SetOptions.merge()).await()
    }
}





