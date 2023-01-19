package com.chugnchunon.chungchunon_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DecimalFormat
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.chugnchunon.chungchunon_android.BroadcastReceiver.StepCountBroadcastReceiver
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

    // 걸음수
    private var MagnitudePrevious: Double = 0.0
    private var stepCount: Int = 0
    private val gravity = FloatArray(3)
    private var smoothed = FloatArray(3)
    private val bearing = 0.0
    private val toggle = false
    private var prevY = 0.0
    private val ignore = false
    private val countdown = 0

    companion object {
        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"

        var todayTotalStepCount: Int? = 0
        const val STEP_THRESHOLD: Double = 6.0
    }

    override fun onCreate() {
        super.onCreate()


        // 가속도 센서
        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_NORMAL)

        // 기본
//        sensorManager =
//            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
//        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)


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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("결과gm", "onStartcommand");
        // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount = todayStepCountFromDB.toInt()

            StepCountNotification(this, todayTotalStepCount)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }

    private fun lowPassFilter(input: FloatArray, output: FloatArray): FloatArray {
        if (output == null) return input
        for (i in input.indices) {
            output[i] = output[i] + 1.0f * (input[i] - output[i])
        }
        return output
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        if (sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            smoothed = lowPassFilter(sensorEvent.values, gravity)
            gravity[0] = smoothed[0];
            gravity[1] = smoothed[1];
            gravity[2] = smoothed[2];

            if (Math.abs(prevY - gravity[1]) > STEP_THRESHOLD) {
                todayTotalStepCount = todayTotalStepCount?.plus(1)

                var currentDate = LocalDate.now()
                StepCountNotification(this, todayTotalStepCount)

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


                var intent = Intent(this, StepCountBroadcastReceiver::class.java)
                intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
                intent.putExtra("todayTotalStepCount", todayTotalStepCount)
                sendBroadcast(intent)

            }
            prevY = gravity[1].toDouble();
        }

//        todayTotalStepCount = todayTotalStepCount?.plus(1)

        var currentDate = LocalDate.now()
        StepCountNotification(this, todayTotalStepCount)

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

        var intent = Intent(this, StepCountBroadcastReceiver::class.java)
        intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
        intent.putExtra("todayTotalStepCount", todayTotalStepCount)
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
            var step = decimal.format(todayTotalStepCount)

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

operator fun <T> MutableLiveData<T>.plus(t: String): MutableLiveData<T> = this + t
