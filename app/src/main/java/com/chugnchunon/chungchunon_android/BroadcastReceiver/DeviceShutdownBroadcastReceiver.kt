package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Service.MyService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DeviceShutdownBroadcastReceiver : BroadcastReceiver() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action

        if (intentAction == Intent.ACTION_BOOT_COMPLETED || intentAction == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            // 부트값 저장
            val bootSet = hashMapOf(
                "last_boot" to FieldValue.serverTimestamp()
            )
            userDB.document("$userId").set(bootSet, SetOptions.merge())

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
                                            val serviceError = hashMapOf(
                                                "service_error" to e,
                                                "service_step_status" to false,
                                            )
                                            userDB.document("$userId")
                                                .set(serviceError, SetOptions.merge())
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
                                        val serviceError = hashMapOf(
                                            "service_error" to e,
                                            "service_step_status" to false,
                                        )
                                        userDB.document("$userId")
                                            .set(serviceError, SetOptions.merge())
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}
