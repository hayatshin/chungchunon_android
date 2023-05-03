package com.chugnchunon.chungchunon_android.Service

import android.app.ActivityManager
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters

class RegisterAlarmWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {
    override fun doWork(): Result {

        Log.d("서비스 - 워크", "리시버")

        fun isServiceRunning(serviceClass: Class<*>): Boolean {
            val activityManager =
                applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        try {
            val isServiceRunningBoolean = isServiceRunning(MyService::class.java)
            if (isServiceRunningBoolean) {
                Log.d("서비스 - 워크", "running")
            } else {
                Log.d("서비스 - 워크", "no running")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val startService = Intent(applicationContext, MyService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(applicationContext, startService);
                        } else {
                            applicationContext.startService(startService);
                        }
                    } catch (e: ForegroundServiceStartNotAllowedException) {
                        Log.d("서비스 - 워크: 포그라운드 오류", "$e")
                    }
                } else {
                    try {
                        val startService = Intent(applicationContext, MyService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(applicationContext, startService);
                        } else {
                            applicationContext.startService(startService);
                        }
                    } catch (e: Exception) {
                        Log.d("서비스 - 워크: 일반 오류", "$e")
                    }
                }

            }

        } catch (e: Exception) {
            Log.d("서비스 - 워크", "$e")
            return Result.retry()
        }
        return Result.success()
    }
}