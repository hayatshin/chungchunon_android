package com.chugnchunon.chungchunon_android.BroadcastReceiver

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

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action

        when (intentAction) {
            Intent.ACTION_DATE_CHANGED -> {

                // todayStepCount = 0
                var todayStepCountSet = hashMapOf<String, Int?>(
                    "todayStepCount" to 0
                )

                userDB
                    .document("$userId")
                    .set(todayStepCountSet, SetOptions.merge())
                    .addOnSuccessListener {

                        Log.d("걸음수체크", "디비 저장 성공")
                       var goDiary = Intent(context, MyDiaryFragment::class.java)
                        goDiary.setAction("NEW_DATE_STEP_ZERO")
                        LocalBroadcastManager.getInstance(context!!).sendBroadcast(goDiary);

                        var goService = Intent(context, MyService::class.java)
                        goService.setAction("NEW_DATE_STEP_ZERO")
                        LocalBroadcastManager.getInstance(context!!).sendBroadcast(goService);
                    }

                // sharedPref 어제 값 추가
                prefs = context!!.getSharedPreferences(initialCountKey, Context.MODE_PRIVATE)

                var dateFormat = SimpleDateFormat("yyyy-MM-dd")
                var cal = Calendar.getInstance()
                cal.add(Calendar.DATE, -1)
                var yesterday = dateFormat.format(cal.getTime())

                db.collection("user_step_count").document("${userId}")
                    .get()
                    .addOnSuccessListener { document ->
                        var yesterdayStep = (document.data?.getValue("${yesterday}") as Long).toInt()
                        var dummyStep = prefs.getInt(userId, 0)

                        Log.d("걸음수마지막 222", "yesterday: ${yesterdayStep} // dummyStep: ${dummyStep} //yesterday+dummyStep: ${yesterdayStep+dummyStep}")

                        val editor = prefs.edit()
                        editor.putInt(userId, dummyStep+yesterdayStep)
                        editor.putString("check", "yesterday: ${yesterdayStep} // dummyStep: ${dummyStep} //yesterday+dummyStep: ${yesterdayStep+dummyStep}")
                        editor.apply()
                    }
            }
        }
    }
}


