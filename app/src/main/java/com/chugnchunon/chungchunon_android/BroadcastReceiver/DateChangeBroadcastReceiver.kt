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
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

open class DateChangeBroadcastReceiver : BroadcastReceiver() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid
    private val initialCountKey = "InitialCountKey"
    lateinit var prefs: SharedPreferences

    @SuppressLint("SimpleDateFormat")
    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action
        prefs = context!!.getSharedPreferences(initialCountKey, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        var dateFormat = SimpleDateFormat("yyyy-MM-dd")
        var cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        var yesterday = dateFormat.format(cal.getTime())

        when (intentAction) {
            Intent.ACTION_TIME_TICK -> {
                var REFRESH_DAILY =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                var todayChecking = prefs.getBoolean(REFRESH_DAILY, false)

                if (!todayChecking) {
                    // 새로운 날
                    Log.d("새로운 날", "$REFRESH_DAILY")

                    // todayStepCount = 0
                    var todayStepCountSet = hashMapOf<String, Int?>(
                        "todayStepCount" to 0
                    )

                    userDB
                        .document("$userId")
                        .set(todayStepCountSet, SetOptions.merge())
                        .addOnSuccessListener {

                            // noti & UI 걸음수 0로 초기화
                            var goDiary = Intent(context, MyDiaryFragment::class.java)
                            goDiary.setAction("NEW_DATE_STEP_ZERO")
                            LocalBroadcastManager.getInstance(context!!).sendBroadcast(goDiary);

                            var goService = Intent(context, MyService::class.java)
                            goService.setAction("NEW_DATE_STEP_ZERO")
                            LocalBroadcastManager.getInstance(context!!).sendBroadcast(goService);
                        }

                    // sharedPref 어제 값 추가
                    db.collection("user_step_count").document("${userId}")
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                var snapShot = document.data
                                if (snapShot!!.containsKey(yesterday)) {

                                    // yesterday 값 있는 경우
                                    var yesterdayStep = (snapShot[yesterday] as Long).toInt()
                                    var dummyStep = prefs.getInt(userId, 0)
                                    editor.putInt(userId, dummyStep + yesterdayStep)
                                    editor.apply()

                                } else {
                                    // yesterday 값 없는 경우
                                }
                            }
                        }

                    // 새로운 날 값 sharedPref에 저장
                    editor.putBoolean(REFRESH_DAILY, true)
                    editor.apply()

                } else {
                    // 새롭지 않은 날
                }
            }
        }
    }
}


//            Intent.ACTION_DATE_CHANGED -> {
//
//                // todayStepCount = 0
//                var todayStepCountSet = hashMapOf<String, Int?>(
//                    "todayStepCount" to 0
//                )
//
//                userDB
//                    .document("$userId")
//                    .set(todayStepCountSet, SetOptions.merge())
//                    .addOnSuccessListener {
//
//                        Log.d("걸음수체크", "디비 저장 성공")
//                       var goDiary = Intent(context, MyDiaryFragment::class.java)
//                        goDiary.setAction("NEW_DATE_STEP_ZERO")
//                        LocalBroadcastManager.getInstance(context!!).sendBroadcast(goDiary);
//
//                        var goService = Intent(context, MyService::class.java)
//                        goService.setAction("NEW_DATE_STEP_ZERO")
//                        LocalBroadcastManager.getInstance(context!!).sendBroadcast(goService);
//                    }
//
//                // sharedPref 어제 값 추가
//
//                var dateFormat = SimpleDateFormat("yyyy-MM-dd")
//                var cal = Calendar.getInstance()
//                cal.add(Calendar.DATE, -1)
//                var yesterday = dateFormat.format(cal.getTime())
//
//                db.collection("user_step_count").document("${userId}")
//                    .get()
//                    .addOnSuccessListener { document ->
//                        var yesterdayStep = (document.data?.getValue("${yesterday}") as Long).toInt()
//                        var dummyStep = prefs.getInt(userId, 0)
//
//                        Log.d("걸음수마지막 222", "yesterday: ${yesterdayStep} // dummyStep: ${dummyStep} //yesterday+dummyStep: ${yesterdayStep+dummyStep}")
//
//                        editor.putInt(userId, dummyStep+yesterdayStep)
//                        editor.putString("check", "yesterday: ${yesterdayStep} // dummyStep: ${dummyStep} //yesterday+dummyStep: ${yesterdayStep+dummyStep}")
//                        editor.apply()
//                    }
//        }


