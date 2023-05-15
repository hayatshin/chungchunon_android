package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.KakaoLogin.SessionCallback
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDefaultCancelWarningBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DefaultCancelWarningActivity : Activity() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userAuth = FirebaseAuth.getInstance().currentUser
    private val userId = Firebase.auth.currentUser?.uid

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

        if(warningType == "exit") {
            // 탈퇴
            binding.warningText.text = "정말로 탈퇴하시겠습니까?"

            binding.confirmBox.setOnClickListener {
                val exitSet = hashMapOf(
                    "userId" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("exit").add(exitSet)
                    .addOnSuccessListener {
                        // userDB에서 수정
                        userDB.document("$userId").delete()
                            .addOnSuccessListener {
                                val goMain = Intent(this, MainActivity::class.java)
                                goMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(goMain)
                            }
                    }
            }
        } else if (warningType == "banStep") {
            binding.warningText.text = "걸음수 측정을 끈 뒤로는\n걸음수 점수가 더해지지 않습니다.\n정말로 걸음수 측정을 끄시겠습니까?"

            binding.confirmBox.setOnClickListener {
                val editIntent = Intent(this, MyService::class.java)
                editIntent.setAction("BAN_STEP")
                LocalBroadcastManager.getInstance(this).sendBroadcast(editIntent)

                finish()
            }

        }
    }

}



