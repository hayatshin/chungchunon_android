package com.chugnchunon.chungchunon_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate


class MyService : Service(), SensorEventListener {

    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler

    private lateinit var sensorManager: SensorManager
    private lateinit var step_sensor: Sensor

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    companion object {
        var todayTotalStepCount: MutableLiveData<Int>? = MutableLiveData()
    }

    override fun onCreate() {
        super.onCreate()

    Log.d("결과gm", "onStart")
        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_UI)


        // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount?.value = todayStepCountFromDB.toInt()
        }

        secondNoti()
//
//        todayTotalStepCount?.value?.let { createNotification("hi", it) };

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId)

        Log.d("결과gm", "onStartcommand");

        secondNoti()
//        todayTotalStepCount?.value?.let { createNotification("hi", it) };
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }


    override fun onSensorChanged(stepEvent: SensorEvent?) {
        Log.d("결과", "$stepEvent")

        var currentDate = LocalDate.now()

        todayTotalStepCount?.postValue(todayTotalStepCount?.value?.plus(1))

//        todayTotalStepCount?.value = todayTotalStepCount?.value?.plus(1)
//        binding.todayStepCount.text = MyDiaryFragment.todayTotalStepCount.toString()

        // user 내 todayStepCount
        var todayStepCountSet = hashMapOf(
            "todayStepCount" to (todayTotalStepCount?.value?.plus(1))
        )

        userDB.document("$userId").set(todayStepCountSet, SetOptions.merge())

        var userStepCountSet = hashMapOf(
            "$currentDate" to (todayTotalStepCount?.value?.plus(1))
        )

        var periodStepCountSet = hashMapOf(
            "$userId" to (todayTotalStepCount?.value?.plus(1))
        )

        // user_step_count
        db.collection("user_step_count")
            .document("$userId")
            .set(userStepCountSet, SetOptions.merge())

        // period_step_count
        db.collection("period_step_count")
            .document("$currentDate")
            .set(periodStepCountSet, SetOptions.merge())
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("서비스", "accuracychanged")
    }

//    private var mThread: Thread? = object : Thread("My Thread") {
//        override fun run() {
//            super.run()
//
//            sensorManager = applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//            step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
//
//            if (step_sensor != null) {
//               sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_UI)
//            }
//
//
//
//        }
//    }


//
//    private fun createNotification(channelId: String, todayTotalStepCount: Int) {
//        val builder = NotificationCompat.Builder(this, "default").setOngoing(true)
//        builder.setSmallIcon(R.mipmap.ic_launcher)
//        builder.setContentTitle("$todayTotalStepCount")
//        builder.setContentText("$todayTotalStepCount")
//        builder.color = Color.RED
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//        val pendingIntent =
//            PendingIntent.getActivity(this, 20, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
//        builder.setContentIntent(pendingIntent) // 알림 클릭시 이동
//
//        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notificationManager.createNotificationChannel(
//                NotificationChannel(
//                    "default",
//                    "$channelId",
//                    NotificationManager.IMPORTANCE_DEFAULT
//                )
//            )
//        }
//        notificationManager.notify(3, builder.build())
//        val notification = builder.build()
//        startForeground(3, notification)
//    }

    private fun secondNoti() {
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_app"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MyApp", NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        }
    }

}


private operator fun <T> MutableLiveData<T>.plus(t: T): MutableLiveData<T> = this + t


