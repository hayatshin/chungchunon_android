package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.MutableLiveData
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

open class BroadcastReceiver : BroadcastReceiver() {

    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid


    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action
        var stepCount = intent?.getIntExtra("stepCount", 0)

        when (intentAction) {
            Intent.ACTION_DATE_CHANGED -> {
                var todayStepCountSet = hashMapOf<String, Int?>(
                    "todayStepCount" to 0
                )
                userDB
                    .document("$userId")
                    .set(todayStepCountSet, SetOptions.merge())
                    .addOnSuccessListener {
                        var goDiary = Intent(context, MyDiaryFragment::class.java)
                        goDiary.putExtra("stepCount", 0)
                        context!!.startActivity(goDiary)
                    }
            }
        }
    }
}


private fun StepCountNotification(context: Context, stepCount: Int?) {

    if (Build.VERSION.SDK_INT >= 26) {
        val CHANNEL_ID = "my_app"
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MyApp", NotificationManager.IMPORTANCE_DEFAULT
        )
        (context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("$stepCount 걸음")
            .setColor(ContextCompat.getColor(context, R.color.main_color))
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setVibrate(longArrayOf(0L))

            .build()
    }
}


private operator fun <T> MutableLiveData<T>.plus(t: T): MutableLiveData<T> = this + t