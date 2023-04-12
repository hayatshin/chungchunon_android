package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.Service.MyService.Companion.ACTION_STEP_COUNTER_NOTIFICATION
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.util.*

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

            db.collection("user_step_count").document("$userId")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        var snapShot = document.data

                        if (snapShot!!.containsKey(currentDate.toString())) {
                            // 오늘 날짜 있는 경우 - 기존 날

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

                        } else {
                            // 오늘 날짜 없는 경우 = 새로운 날

                            var todayStepCountSet = hashMapOf<String, Int?>(
                                "todayStepCount" to 0
                            )

                            userDB
                                .document("$userId")
                                .set(todayStepCountSet, SetOptions.merge())
                                .addOnSuccessListener {

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

                            var dateFormat = SimpleDateFormat("yyyy-MM-dd")
                            var cal = Calendar.getInstance()
                            cal.add(Calendar.DATE, -1)
                            var yesterday = dateFormat.format(cal.getTime())

                            if (snapShot!!.containsKey(yesterday)) {
                                // 어제 값 존재
                                var yesterdayStep = (snapShot[yesterday] as Long).toInt()
                                var dummyStep = (snapShot["dummy"] as Long).toInt()

                                var newStepSet = hashMapOf(
                                    "dummy" to (yesterdayStep + dummyStep)
                                )

                                db.collection("user_step_count").document("$userId")
                                    .set(newStepSet, SetOptions.merge())


                                var userStepCountSet = hashMapOf(
                                    "${currentDate}" to 0
                                )

                                var periodStepCountSet = hashMapOf(
                                    "${userId}" to 0
                                )

                                // period_step_count
                                db.collection("period_step_count")
                                    .document("$currentDate")
                                    .set(periodStepCountSet, SetOptions.merge())

                                // user_step_count
                                db.collection("user_step_count")
                                    .document("$userId")
                                    .set(userStepCountSet, SetOptions.merge())

                            } else {
                                // 어제 값 존재 x

                                var userStepCountSet = hashMapOf(
                                    "$currentDate" to 0
                                )

                                var periodStepCountSet = hashMapOf(
                                    "$userId" to 0
                                )

                                // period_step_count
                                db.collection("period_step_count")
                                    .document("$currentDate")
                                    .set(periodStepCountSet, SetOptions.merge())

                                // user_step_count
                                db.collection("user_step_count")
                                    .document("$userId")
                                    .set(userStepCountSet, SetOptions.merge())
                            }
                        }
                    }
                }
        }
    }
}



