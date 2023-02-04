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
import com.chugnchunon.chungchunon_android.BroadcastReceiver.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class MyService : Service(), SensorEventListener {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var todayStepCountFromDB = 0
    lateinit var dateChangeBroadcastReceiver: DateChangeBroadcastReceiver
    lateinit var deviceShutdownBroadcastReceiver: DeviceShutdownBroadcastReceiver

    companion object {
        lateinit var sensorManager: SensorManager
        lateinit var step_sensor: Sensor

        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"

        var todayTotalStepCount: Int? = 0
        const val STEP_THRESHOLD: Double = 6.5
    }

    private val initialCountKey = "InitialCountKey"
    lateinit var prefs: SharedPreferences

    private var startingStepCount : Int = 0
    private var stepCount : Int = 0

    override fun onCreate() {
        super.onCreate()

        // shared preference 설정
        prefs = getSharedPreferences(initialCountKey, Context.MODE_PRIVATE)
        var dummyData = prefs.getInt(userId, 0)
        Log.d("걸음수 체크체크 1", "$dummyData")


        // 매일 걸음수 0
        dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()

        val dateChangeIntent = IntentFilter()
        dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
        applicationContext?.registerReceiver(dateChangeBroadcastReceiver, dateChangeIntent)

        // 핸드폰 꺼질 때
        deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()

        val deviceShutdownIntent = IntentFilter()
        deviceShutdownIntent.addAction(Intent.ACTION_SHUTDOWN)
        applicationContext?.registerReceiver(deviceShutdownBroadcastReceiver, deviceShutdownIntent)


        // 기본
        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)


        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        );

        // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount = todayStepCountFromDB.toInt()
            StepCountNotification(this, todayTotalStepCount)
        }
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        startingStepCount = prefs.getInt(userId, -1)
        stepCount = sensorEvent!!.values[0].toInt()
        val editor = prefs.edit()

        Log.d("걸음수 체크체크 2", "$stepCount")

        if (!prefs.contains(userId)) {
            // 걸음수 pref 저장 안 된 상태

            // 1. 걸음수 초기화
            editor.putInt(userId, stepCount)
            editor.commit()

            StepCountNotification(this, stepCount)

            var intent = Intent(this, StepCountBroadcastReceiver::class.java)
            intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
            intent.putExtra("todayTotalStepCount", stepCount)
            sendBroadcast(intent)

        } else {

            // 걸음수 pref 저장 된 상태
            Log.d("걸음수 체크체크 3", "${prefs.contains(userId)}")

            var todayStepCount = stepCount - startingStepCount

            StepCountNotification(this, todayStepCount)

            var intent = Intent(this, StepCountBroadcastReceiver::class.java)
            intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
            intent.putExtra("todayTotalStepCount", todayStepCount)
            sendBroadcast(intent)
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("서비스", "accuracychanged")
    }

    var stepInitializeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            StepCountNotification(context!!, 0)
        }
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
                .setSmallIcon(R.drawable.ic_new_alarm_icon)
                .setContentTitle("$step 걸음")
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(true)
                .build()
            startForeground(1, notification)
        }
    }
}




