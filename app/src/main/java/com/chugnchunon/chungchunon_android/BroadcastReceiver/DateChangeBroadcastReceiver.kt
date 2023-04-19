package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.Service.MyService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

open class DateChangeBroadcastReceiver : BroadcastReceiver() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    @SuppressLint("SimpleDateFormat")
    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val yesterdayCal = Calendar.getInstance()
        yesterdayCal.add(Calendar.DATE, -1)
        val yesterday = dateFormat.format(yesterdayCal.time)
        val todayCal = Calendar.getInstance()
        val today = dateFormat.format(todayCal.time)

        when (intentAction) {
            Intent.ACTION_TIME_TICK -> {

                db.collection("user_step_count").document("$userId")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            if(document.contains(today)) {
                                // 기존 날
                            } else {
                                // 새로운 날
                               if(document.contains(yesterday)){
                                   // 어제 값 있음 -> dummy 값 추가
                                   val dummyStepCount = (document.data?.getValue("dummy") as Long).toInt()
                                   val yesterdayStepCount = (document.data?.getValue(yesterday) as Long).toInt()
                                   val newDummy = dummyStepCount + yesterdayStepCount

                                   val newDummySet = hashMapOf(
                                       "dummy" to newDummy
                                   )
                                   db.collection("user_step_count").document("$userId")
                                       .set(newDummySet, SetOptions.merge())

                                   val userStepCountSet = hashMapOf(
                                       today to 0
                                   )

                                   val periodStepCountSet = hashMapOf(
                                       "$userId" to 0
                                   )

                                   // user_step_count
                                   db.collection("user_step_count")
                                       .document("$userId")
                                       .set(userStepCountSet, SetOptions.merge())

                                   // period_step_count
                                   db.collection("period_step_count")
                                       .document(today)
                                       .set(periodStepCountSet, SetOptions.merge())

                                   // todayStepCount
                                   val todayStepCountSet = hashMapOf(
                                       "todayStepCount" to 0
                                   )
                                   userDB.document("$userId")
                                       .set(todayStepCountSet, SetOptions.merge())

                               } else {
                                   // 어제 값 없음 -> dummy 0 세팅

                                   val newDummySet = hashMapOf(
                                       "dummy" to 0
                                   )
                                   db.collection("user_step_count").document("$userId")
                                       .set(newDummySet, SetOptions.merge())

                                   val userStepCountSet = hashMapOf(
                                       today to 0
                                   )

                                   val periodStepCountSet = hashMapOf(
                                       "$userId" to 0
                                   )

                                   // user_step_count
                                   db.collection("user_step_count")
                                       .document("$userId")
                                       .set(userStepCountSet, SetOptions.merge())

                                   // period_step_count
                                   db.collection("period_step_count")
                                       .document(today)
                                       .set(periodStepCountSet, SetOptions.merge())

                                   // todayStepCount
                                   val todayStepCountSet = hashMapOf(
                                       "todayStepCount" to 0
                                   )
                                   userDB.document("$userId")
                                       .set(todayStepCountSet, SetOptions.merge())

                               }

                                // noti & UI 걸음수 0로 초기화
                                var goDiary =
                                    Intent(context, MyDiaryFragment::class.java)
                                goDiary.setAction("NEW_DATE_STEP_ZERO")
                                LocalBroadcastManager.getInstance(context!!)
                                    .sendBroadcast(goDiary);

                                var goService = Intent(context, MyService::class.java)
                                goService.setAction("NEW_DATE_STEP_ZERO")
                                LocalBroadcastManager.getInstance(context!!)
                                    .sendBroadcast(goService);

                            }
                        }
                    }
            }
        }
    }
}


