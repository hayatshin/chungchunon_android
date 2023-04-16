package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
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

        if (intentAction == Intent.ACTION_BOOT_COMPLETED) {

            // 부트값 저장
            val bootSet = hashMapOf(
                "last_boot" to FieldValue.serverTimestamp()
            )
            userDB.document("$userId").set(bootSet, SetOptions.merge())

            val startService = Intent(context, MyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context!!, startService);
            } else {
                context!!.startService(startService);
            }

        }
    }
}