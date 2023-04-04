package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.MyService.Companion.stepCountSharedPref
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DeviceShutdownBroadcastReceiver : BroadcastReceiver() {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid
    lateinit var prefs: SharedPreferences

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action
        prefs = context!!.getSharedPreferences(stepCountSharedPref, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (intentAction == Intent.ACTION_SHUTDOWN) {

            // dummy data - 0
            var dummyDataReset = hashMapOf(
                "dummy" to 0
            )
            db.collection("user_step_count")
                .document("${userId}")
                .set(dummyDataReset, SetOptions.merge())

        }
    }
}