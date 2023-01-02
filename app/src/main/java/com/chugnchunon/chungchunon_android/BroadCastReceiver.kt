package com.chugnchunon.chungchunon_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_TIME_CHANGED
import android.util.Log
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BroadCastReceiver: BroadcastReceiver() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    override fun onReceive(context: Context?, intent: Intent?) {
        if(Intent.ACTION_DATE_CHANGED == intent!!.action) {

            var diarySet = hashMapOf<String, Int>(
                "stepCount" to MyDiaryFragment.todayTotalStepCount
            )

            diaryDB
                .document(userId.toString())
                .set(diarySet, SetOptions.merge())
                .addOnSuccessListener {
                    MyDiaryFragment.todayTotalStepCount = 0
                }

        } else {
            Log.d("브로드캐스트", "실패")
        }
    }
}