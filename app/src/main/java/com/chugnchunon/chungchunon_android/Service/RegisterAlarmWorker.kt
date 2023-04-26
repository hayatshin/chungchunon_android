package com.chugnchunon.chungchunon_android.Service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RegisterAlarmWorker(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {

        Log.d("워크", "리시버")

        // 브로드캐스트 주기적 등록
        val alarmIntent = Intent(context, MyService::class.java)
        alarmIntent.setAction("ALARM_BROADCAST_RECEIVER")
        alarmIntent.putExtra("alarm", true)
        LocalBroadcastManager.getInstance(context!!).sendBroadcast(alarmIntent);

        // 노티피케이션 서비스 호출
        val mNotificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        return Result.success()
    }
}