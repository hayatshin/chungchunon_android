package com.chugnchunon.chungchunon_android.PhoneAuthLogin

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.chugnchunon.chungchunon_android.DefaultDiaryWarningActivity
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class PhoneAuthLoginCallback {



    fun phoneAuthLoginCallBack(activity: Activity, phoneNumber: String) {

        var result: Boolean =  false
        val firebaseAuth = FirebaseAuth.getInstance()
        val appVerifier = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {



            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(activity) { task ->
                        if(task.isSuccessful) {
                            val user = task.result?.user
                            Log.d("일반로그인 -> 성공", "${user?.uid}")
                            val goDiaryTwoActivity = Intent(activity, DiaryTwoActivity::class.java)
                            activity.startActivity(goDiaryTwoActivity)
                        } else {
                            Log.d("일반로그인 -> 실패 1", "${task.exception}")
                        }
                    }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                Log.d("일반로그인 -> 실패", "${exception}")

                val goWarning = Intent(activity, DefaultDiaryWarningActivity::class.java)
                goWarning.putExtra("warningType", "originLoginFail")
                activity.startActivity(goWarning)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("일반로그인 -> 코드: verificationId", "${verificationId}")
                Log.d("일반로그인 -> 코드: token", "${token}")

            }

        }

        val optionCompat = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(30L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(appVerifier)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(optionCompat)
        firebaseAuth.setLanguageCode("kr")

    }
}


