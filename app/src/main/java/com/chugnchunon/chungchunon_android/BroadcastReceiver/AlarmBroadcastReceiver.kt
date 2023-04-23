package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.LockDiaryActivity
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.Service.MyService.Companion.NOTIFICATION_ID
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime

lateinit var dateChangeBroadcastReceiver: DateChangeBroadcastReceiver
lateinit var deviceShutdownBroadcastReceiver: DeviceShutdownBroadcastReceiver
private val db = Firebase.firestore
private val userId = Firebase.auth.currentUser?.uid


class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val now = LocalDateTime.now()

        Log.d("알람매니저: 리시브", "${now}")

        // 브로드캐스트 주기적 등록
        val alarmIntent = Intent(context, MyService::class.java)
        alarmIntent.setAction("ALARM_BROADCAST_RECEIVER")
        alarmIntent.putExtra("alarm", true)
        LocalBroadcastManager.getInstance(context!!).sendBroadcast(alarmIntent);

        // 노티피케이션 서비스 호출
        val mNotificationManager =
            context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val statusNotifications = mNotificationManager.activeNotifications
        for (statusNotification in statusNotifications) {
            if (statusNotification.id == MyService.NOTIFICATION_ID) {
                // null
            } else {
                // 노티피케이션 없는 경우

                alarmIntent.setAction("ALARM_BROADCAST_RECEIVER")
                alarmIntent.putExtra("no_noti", true)
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(alarmIntent);

                val startService = Intent(context, MyService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, startService);
                } else {
                    context.startService(startService);
                }
            }
        }
    }
}
