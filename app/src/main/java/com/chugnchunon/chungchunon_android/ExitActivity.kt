package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.chugnchunon.chungchunon_android.KakaoLogin.SessionCallback
import com.chugnchunon.chungchunon_android.databinding.ActivityExitBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ExitActivity : Activity() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userAuth = FirebaseAuth.getInstance().currentUser
    private val userId = Firebase.auth.currentUser?.uid

    private val binding by lazy {
        ActivityExitBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.exitGobackArrow.setOnClickListener {
            finish()
        }

        binding.exitCancelBox.setOnClickListener {
            finish()
        }

        binding.exitConfirmBox.setOnClickListener {
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
    }

}



