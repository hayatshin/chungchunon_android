package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.MyService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BroadcastReceiver : BroadcastReceiver() {

    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    override fun onReceive(context: Context?, intent: Intent?) {

        val intentAction = intent!!.action

        when (intentAction) {
            Intent.ACTION_DATE_CHANGED -> {
//                MyService.todayTotalStepCount = 0

                var todayStepCountSet = hashMapOf<String, Int?>(
                    "todayStepCount" to 0
                )

                userDB
                    .document("$userId")
                    .set(todayStepCountSet, SetOptions.merge())
            }
        }

    }
}