package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.chugnchunon.chungchunon_android.databinding.ActivityAvatarEnlargeBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityExitBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ExitActivity: Activity() {

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private val userAuth = Firebase.auth.currentUser!!

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
            userAuth.delete()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        var goMain = Intent(this, MainActivity::class.java)
                        goMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(goMain)
                    }
                }
        }
    }
}