package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

class DiaryUpdateBroadcastReceiver : BroadcastReceiver() {

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid
    private val currentDate = LocalDate.now()

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action
        if (intentAction == Intent.ACTION_TIME_TICK) {

            diaryDB.document("${userId}_${currentDate}")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        var document = task.result
                        if (document != null) {
                            if (document.exists()) {
                                var diaryUserId = document.data?.getValue("userId").toString()
                                userDB.document("$diaryUserId").get()
                                    .addOnSuccessListener { document ->
                                        var recentStepCount =
                                            document.data?.getValue("todayStepCount")
                                        var recentStepCountSet = hashMapOf(
                                            "stepCount" to recentStepCount
                                        )
                                        diaryDB.document("${diaryUserId}_${currentDate}")
                                            .set(recentStepCountSet, SetOptions.merge())
                                    }
                            }


                        }
                    }
                }
        }


    }
}