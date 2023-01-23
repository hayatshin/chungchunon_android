package com.chugnchunon.chungchunon_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.BroadcastReceiver.StepCountBroadcastReceiver
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class MyService : Service(), SensorEventListener {
    lateinit var sensorManager: SensorManager
    lateinit var step_sensor: Sensor
    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var todayStepCountFromDB = 0

    companion object {
        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"

        var todayTotalStepCount: Int? = 0
        const val STEP_THRESHOLD: Double = 6.5
    }

    private val initialCountKey = "InitialCountKey"
    lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences(initialCountKey, Context.MODE_PRIVATE)

        var stepInitializeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                StepCountNotification(context!!, 0)
            }

        }

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        );



        // 오늘 걸음수 초기화
//        userDB.document("$userId").get().addOnSuccessListener { document ->
//            todayStepCountFromDB =
//                ((document.data?.getValue("todayStepCount") ?: 0) as Long).toInt()
//            todayTotalStepCount = todayStepCountFromDB.toInt()
//
//            StepCountNotification(this, todayTotalStepCount)
//        }

        // 가속도 센서
//        sensorManager =
//            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_NORMAL)

        // 기본
        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)

        // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount = todayStepCountFromDB.toInt()
            StepCountNotification(this, todayTotalStepCount)
        }
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)

        if (step_sensor != null) {
            sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {



        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        var stepCount = sensorEvent!!.values[0].toInt()

        if (!prefs.contains(userId)) {
            val editor = prefs.edit()
            editor.putInt(userId, stepCount)
            editor.commit()
        }

        var startingStepCount = prefs.getInt(userId, -1)
        var todayStepCount = stepCount - startingStepCount

        Log.d("걸음수마지막 111", "stepCount: ${stepCount} // prefStepCount: ${startingStepCount} // stepCount-prefStepCount: ${todayStepCount}")

        StepCountNotification(this, todayStepCount)

        var intent = Intent(this, StepCountBroadcastReceiver::class.java)
        intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
        intent.putExtra("todayTotalStepCount", todayStepCount)
        sendBroadcast(intent)
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("서비스", "accuracychanged")
    }

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
            var decimal = DecimalFormat("#,###")
            var step = decimal.format(stepCount)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_stepcount_noti)
                .setContentTitle("$step 걸음")
                .setColor(ContextCompat.getColor(context, R.color.main_color))
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .build()
            startForeground(1, notification)
        }
    }
}



