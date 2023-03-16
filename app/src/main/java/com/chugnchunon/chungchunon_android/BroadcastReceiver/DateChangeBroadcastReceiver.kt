package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.MyService.Companion.dateChangeSharedPref
import com.chugnchunon.chungchunon_android.MyService.Companion.stepCountSharedPref
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.HashMap

open class DateChangeBroadcastReceiver : BroadcastReceiver() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    lateinit var datePrefs: SharedPreferences

    @SuppressLint("SimpleDateFormat")
    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action
        datePrefs = context!!.getSharedPreferences(dateChangeSharedPref, Context.MODE_PRIVATE)
        val datePrefsEdit = datePrefs.edit()

        var dateFormat = SimpleDateFormat("yyyy-MM-dd")
        var cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        var yesterday = dateFormat.format(cal.getTime())

        when (intentAction) {
            Intent.ACTION_TIME_TICK -> {

                var REFRESH_DAILY =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                var todayChecking = datePrefs.getBoolean(REFRESH_DAILY, false)

                db.collection("user_step_count").document("$userId")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var snapShot = document.data

                            if (snapShot!!.containsKey("dummy")) {
                                // 더미 데이터 있는 경우

                                if (snapShot!!.containsKey(REFRESH_DAILY)) { // 기존 날
                                    var testSetTwo = hashMapOf(
                                        "더미데이터 있고 기존 날" to "작동"
                                    )

                                    db.collection("test").document(REFRESH_DAILY)
                                        .set(testSetTwo, SetOptions.merge())

                                } else {
                                    // 새로운 날

                                    var testSetFour = hashMapOf(
                                        "더미데이터 있고 새로운 날" to "작동",
                                    )

                                    db.collection("test").document(REFRESH_DAILY)
                                        .set(testSetFour, SetOptions.merge())


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

                                    if (snapShot!!.containsKey(yesterday)) {
                                        // 어제 값 존재
                                        var yesterdayStep = (snapShot[yesterday] as Long).toInt()
                                        var dummyStep = (snapShot["dummy"] as Long).toInt()

                                        var newStepSet = hashMapOf(
                                            "dummy" to (yesterdayStep + dummyStep)
                                        )

                                        db.collection("user_step_count").document("$userId")
                                            .set(newStepSet, SetOptions.merge())

                                        var testSetThree = hashMapOf(
                                            "더미데이터 있고 새로운 날, 어제 값 존재" to "작동",
                                            "yesterday" to yesterday,
                                            "dummyStep" to dummyStep
                                        )

                                        db.collection("test").document(REFRESH_DAILY)
                                            .set(testSetThree, SetOptions.merge())

                                    } else {

                                        var testSetFive = hashMapOf(
                                            "더미데이터 있고 새로운 날, 어제 값 존재 x" to "작동",
                                        )

                                        db.collection("test").document(REFRESH_DAILY)
                                            .set(testSetFive, SetOptions.merge())


                                    }


                                    // 날짜 저장
                                    var newDateHashmap = hashMapOf(
                                        REFRESH_DAILY to 0
                                    )
                                    db.collection("user_step_count").document("$userId")
                                        .set(newDateHashmap, SetOptions.merge())
                                }


                            } else {
                                // 더미 데이터 없는 경우

                                var testSetSeven = hashMapOf(
                                    "더미데이터 없음, 어제 값 존재 x" to "작동",
                                )

                                db.collection("test").document(REFRESH_DAILY)
                                    .set(testSetSeven, SetOptions.merge())

                            }
                        }
                    }

//                if (!todayChecking) {
//                    // 새로운 날
//
//                    // todayStepCount = 0
//                    var todayStepCountSet = hashMapOf<String, Int?>(
//                        "todayStepCount" to 0
//                    )
//
//                    userDB
//                        .document("$userId")
//                        .set(todayStepCountSet, SetOptions.merge())
//                        .addOnSuccessListener {
//
//                            // noti & UI 걸음수 0로 초기화
//                            var goDiary = Intent(context, MyDiaryFragment::class.java)
//                            goDiary.setAction("NEW_DATE_STEP_ZERO")
//                            LocalBroadcastManager.getInstance(context!!).sendBroadcast(goDiary);
//
//                            var goService = Intent(context, MyService::class.java)
//                            goService.setAction("NEW_DATE_STEP_ZERO")
//                            LocalBroadcastManager.getInstance(context!!).sendBroadcast(goService);
//                        }
//
//
//                    // dateChangedPref DB 첫번재 값인지 체크
//                    var allDatePref: Map<String, *> = datePrefs.all
//
//                    if (allDatePref.size != 0) {
//
//                        // sharedPref 어제 값 추가
//                        db.collection("user_step_count").document("${userId}")
//                            .get()
//                            .addOnSuccessListener { document ->
//                                if (document.exists()) {
//                                    var snapShot = document.data
//                                    if (snapShot!!.containsKey(yesterday)) {
//                                        var stepPrefs = context!!.getSharedPreferences(
//                                            stepCountSharedPref,
//                                            Context.MODE_PRIVATE
//                                        )
//                                        var stepEdit = stepPrefs.edit()
//
//                                        // yesterday 값 있는 경우
//                                        var yesterdayStep = (snapShot[yesterday] as Long).toInt()
//                                        var dummyStep = stepPrefs.getInt(userId, 0)
//                                        stepEdit.putInt(userId, dummyStep + yesterdayStep)
//                                        stepEdit.apply()
//
//                                    } else {
//                                        // yesterday 값 없는 경우
//                                    }
//                                }
//                            }
//                    }
//
//                    // 새로운 날 값 sharedPref에 저장
//                    datePrefsEdit.putBoolean(REFRESH_DAILY, true)
//                    datePrefsEdit.apply()
//
//                } else {
//                    // 새롭지 않은 날
//                }
            }
        }
    }
}


