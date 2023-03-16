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
            userDB
                .document("$userId")
                .get()
                .addOnSuccessListener { document ->
                    var previousStepCount =
                        (document.data?.getValue("todayStepCount") as Long).toInt()

                    // sharedPref 어제 값 추가
//                    editor.putInt(userId, -previousStepCount)
//                    editor.apply()

//                    db.collection("user_step_count").document("${userId}")
//                        .get()
//                        .addOnSuccessListener { document ->
//                            if (document.exists()) {
//                                var snapShot = document.data
//                                if (snapShot!!.containsKey("dummy")) {
//                                    var dummyStepCount = (snapShot["dummy"] as Long).toInt()
//
//                                }
//                            }
//                        }



                }

        }
    }
}