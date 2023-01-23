package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.MyService.Companion.ACTION_STEP_COUNTER_NOTIFICATION
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

open class StepCountBroadcastReceiver : BroadcastReceiver() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    override fun onReceive(context: Context?, intent: Intent?) {
    if (intent!!.action == ACTION_STEP_COUNTER_NOTIFICATION) {

        var currentDate = LocalDate.now()
        var todayTotalStepCount = intent.getIntExtra("todayTotalStepCount", 0)

        // user 내 todayStepCount
        var todayStepCountSet = hashMapOf(
            "todayStepCount" to todayTotalStepCount
        )
        userDB.document("$userId").set(todayStepCountSet, SetOptions.merge())

        var userStepCountSet = hashMapOf(
            "$currentDate" to todayTotalStepCount
        )

        var periodStepCountSet = hashMapOf(
            "$userId" to todayTotalStepCount
        )

        // user_step_count
        db.collection("user_step_count")
            .document("$userId")
            .set(userStepCountSet, SetOptions.merge())

        // period_step_count
        db.collection("period_step_count")
            .document("$currentDate")
            .set(periodStepCountSet, SetOptions.merge())


        val intent = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
            putExtra(
                "todayTotalStepCount",
                todayTotalStepCount
            )
        }
        LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
    }
}
}



