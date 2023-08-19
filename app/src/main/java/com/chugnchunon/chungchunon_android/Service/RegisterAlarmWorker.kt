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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


private val db = Firebase.firestore
private val userDB = Firebase.firestore.collection("users")
private val userId = Firebase.auth.currentUser?.uid

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
                                                val startService = Intent(applicationContext, MyService::class.java)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    ContextCompat.startForegroundService(applicationContext!!, startService);
                                                } else {
                                                    applicationContext!!.startService(startService);
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            val startService = Intent(applicationContext, MyService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                ContextCompat.startForegroundService(applicationContext!!, startService);
                                            } else {
                                                applicationContext!!.startService(startService);
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }
                    }

//                try {
//                    val startService = Intent(applicationContext, MyService::class.java)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        ContextCompat.startForegroundService(applicationContext, startService);
//                    } else {
//                        applicationContext.startService(startService);
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }


            }

        } catch (e: Exception) {
            Log.d("서비스 - 워크", "$e")
            return Result.retry()
        }
        return Result.success()
    }
}