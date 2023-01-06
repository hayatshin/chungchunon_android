package com.chugnchunon.chungchunon_android

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.BroadcastReceiver.StepCountBroadcastReceiver
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
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
        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"

        //        var todayTotalStepCount: MutableLiveData<Int>? = MutableLiveData()
        var todayTotalStepCount: Int? = 0
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_UI)


        // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount = todayStepCountFromDB.toInt()

            StepCountNotification(this, todayTotalStepCount)
        }


        //  todayTotalStepCount?.value?.let { createNotification("hi", it) };

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId)

        Log.d("결과gm", "onStartcommand");

        // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount = todayStepCountFromDB.toInt()

            StepCountNotification(this, todayTotalStepCount)
        }

        //        todayTotalStepCount?.value?.let { createNotification("hi", it) };
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }


    override fun onSensorChanged(stepEvent: SensorEvent?) {
        Log.d("결과", "$stepEvent")

        var currentDate = LocalDate.now()

        todayTotalStepCount = todayTotalStepCount?.plus(1)
        StepCountNotification(this, todayTotalStepCount)


        var intent = Intent(this, StepCountBroadcastReceiver::class.java)
        intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
        intent.putExtra("todayTotalStepCount", todayTotalStepCount)
        sendBroadcast(intent)


//        Intent().also { intent ->
//            Log.d("마이다이어리1.2", "$todayTotalStepTest")
//            intent.setAction = ACTION_STEP_COUNTER_NOTIFICATION
//            intent.putExtra("stepCount", "$todayTotalStepTest")
//            broadcaster.sendBroadcast(intent)
//        }
//        todayTotalStepCount?.value = todayTotalStepCount?.value?.com.chugnchunon.chungchunon_android.plus(1)
//        binding.todayStepCount.text = MyDiaryFragment.todayTotalStepCount.toString()

        // user 내 todayStepCount
        var todayStepCountSet = hashMapOf(
            "todayStepCount" to todayTotalStepCount
        )

        userDB.document("$userId").set(todayStepCountSet, SetOptions.merge())

        var userStepCountSet = hashMapOf(
            "$currentDate" to todayTotalStepCount
        )

        var periodStepCountSet = hashMapOf(
            "$userId" to todayTotalStepCount
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


    private fun StepCountNotification(context: Context, stepCount: Int?) {

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_app"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MyApp", NotificationManager.IMPORTANCE_LOW
            )
            (context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("$todayTotalStepCount 걸음")
                .setColor(ContextCompat.getColor(context, R.color.main_color))
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .build()

            startForeground(1, notification)
        }
    }

}


operator fun <T> MutableLiveData<T>.plus(t: String): MutableLiveData<T> = this + t