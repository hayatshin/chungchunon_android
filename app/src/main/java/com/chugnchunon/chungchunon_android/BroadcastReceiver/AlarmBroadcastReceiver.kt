package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.LockDiaryActivity
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.Service.MyService.Companion.ALARM_NOTIFICATION_NAME
import com.chugnchunon.chungchunon_android.Service.MyService.Companion.ALARM_REQ_CODE
import com.chugnchunon.chungchunon_android.Service.MyService.Companion.NOTIFICATION_ID
import com.chugnchunon.chungchunon_android.Service.MyService.Companion.alarmBroadcastReceiverCalled
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.util.*

lateinit var dateChangeBroadcastReceiver: DateChangeBroadcastReceiver
lateinit var deviceShutdownBroadcastReceiver: DeviceShutdownBroadcastReceiver
private val db = Firebase.firestore
private val userId = Firebase.auth.currentUser?.uid
private val userDB = Firebase.firestore.collection("users")


class AlarmBroadcastReceiver : BroadcastReceiver() {



    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("서비스", "알람: 브로드캐스트 리시버")

        alarmBroadcastReceiverCalled = true

        fun isServiceRunning(serviceClass: Class<*>): Boolean {
            val activityManager =
                context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

//        val alarmIntent = Intent(context, MyService::class.java)
//        alarmIntent.setAction("ALARM_BROADCAST_RECEIVER_RING")
//        alarmIntent.putExtra("alarm", true)
//        LocalBroadcastManager.getInstance(context!!).sendBroadcast(alarmIntent);

        val isServiceRunningBoolean = isServiceRunning(MyService::class.java)
        if (isServiceRunningBoolean) {
            Log.d("서비스", "running")
        } else {
            Log.d("서비스", "no running")

//            alarmIntent.setAction("ALARM_BROADCAST_RECEIVER_RING")
//            alarmIntent.putExtra("no_noti", true)
//            LocalBroadcastManager.getInstance(context!!).sendBroadcast(alarmIntent);

            userDB.document("$userId").get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        val document = task.result
                        if(document != null) {
                            if(document.exists()) {
                                try {
                                    val stepStatus = document.data?.getValue("step_status") as Boolean
                                    if(stepStatus) {
                                        // 허용
                                        try {
                                            val startService = Intent(context, MyService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                ContextCompat.startForegroundService(context!!, startService);
                                            } else {
                                                context!!.startService(startService);
                                            }
                                        } catch (e: Exception) {
                                           e.stackTrace
                                        }
                                    }
                                } catch (e: Exception) {
                                    try {
                                        val startService = Intent(context, MyService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            ContextCompat.startForegroundService(context!!, startService);
                                        } else {
                                            context!!.startService(startService);
                                        }
                                    } catch (e: Exception) {
                                       e.stackTrace
                                    }
                                }
                            }
                        }
                    }
                }

//            try {
//                val startService = Intent(context, MyService::class.java)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    ContextCompat.startForegroundService(context!!, startService);
//                } else {
//                    context!!.startService(startService);
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }


        }

    }
}


// 노티피케이션 서비스 호출

//        val mNotificationManager =
//            context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        val statusNotifications = mNotificationManager.activeNotifications
//        for (statusNotification in statusNotifications) {
//            if (statusNotification.id == MyService.NOTIFICATION_ID) {
//                // null
//            } else {
//                // 노티피케이션 없는 경우
//
//                alarmIntent.setAction("ALARM_BROADCAST_RECEIVER")
//                alarmIntent.putExtra("no_noti", true)
//                LocalBroadcastManager.getInstance(context!!).sendBroadcast(alarmIntent);
//
//                val startService = Intent(context, MyService::class.java)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    ContextCompat.startForegroundService(context, startService);
//                } else {
//                    context.startService(startService);
//                }
//            }
//        }