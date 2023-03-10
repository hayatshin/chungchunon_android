package com.chugnchunon.chungchunon_android

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.ActivityCompat
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
//    lateinit var diaryUpdateBroadcastReceiver: DiaryUpdateBroadcastReceiver
    lateinit var dateChangeBroadcastReceiver: DateChangeBroadcastReceiver
    lateinit var deviceShutdownBroadcastReceiver: DeviceShutdownBroadcastReceiver
    lateinit var stepCountBroadcastReceiver: StepCountBroadcastReceiver

    companion object {
        lateinit var sensorManager: SensorManager
        lateinit var step_sensor: Sensor

        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"

        var todayTotalStepCount: Int? = 0
        const val STEP_THRESHOLD: Double = 6.5

        const val stepCountSharedPref = "stepCountSharedPreference"
        const val dateChangeSharedPref = "dateChangeSharedPref"
    }

    lateinit var prefs: SharedPreferences

    private var startingStepCount : Int = 0
    private var stepCount : Int = 0

    override fun onCreate() {
        super.onCreate()

        StepCountNotification(this, todayTotalStepCount)



        // ??????
        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_FASTEST)

        // ?????? ????????? ?????????
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount") ?: 0
            todayTotalStepCount = todayStepCountFromDB.toInt()
            StepCountNotification(this, todayTotalStepCount)
        }

        // shared preference ??????
        prefs = getSharedPreferences(stepCountSharedPref, Context.MODE_PRIVATE)
        var dummyData = prefs.getInt(userId, 0)
        var datePrefs = getSharedPreferences(dateChangeSharedPref, Context.MODE_PRIVATE)


        // DateChangeBroadcastReceiver : ?????? ????????? 0
        dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()

        val dateChangeIntent = IntentFilter()
        dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
        applicationContext?.registerReceiver(dateChangeBroadcastReceiver, dateChangeIntent)

        // ???????????? ????????????
//        diaryUpdateBroadcastReceiver = DiaryUpdateBroadcastReceiver()
//
//        val diaryUpdateIntent = IntentFilter()
//        diaryUpdateIntent.addAction(Intent.ACTION_TIME_TICK)
//        applicationContext?.registerReceiver(diaryUpdateBroadcastReceiver, diaryUpdateIntent)


        // DeviceShutDownBroadcastReceiver : ????????? ?????? ???
        deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()

        val deviceShutdownIntent = IntentFilter()
        deviceShutdownIntent.addAction(Intent.ACTION_SHUTDOWN)
        applicationContext?.registerReceiver(deviceShutdownBroadcastReceiver, deviceShutdownIntent)

        // StepCountBroadcastReceiver : ?????? ??????

        stepCountBroadcastReceiver = StepCountBroadcastReceiver()


        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        );
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        startingStepCount = prefs.getInt(userId, 0)
        stepCount = sensorEvent!!.values[0].toInt()
        val editor = prefs.edit()

        Log.d("????????? ???????????? 2", "stepCount: ${stepCount} // startingStepCount: ${startingStepCount}")

        if (!prefs.contains(userId)) {
            // ????????? pref ?????? ??? ??? ??????

            // 1. ????????? ?????????
            editor.putInt(userId, stepCount)
            editor.commit()

            StepCountNotification(this, 0)

            var intentToFirebase = Intent(this, StepCountBroadcastReceiver::class.java)
            intentToFirebase.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
            intentToFirebase.putExtra("todayTotalStepCount", 0)
            sendBroadcast(intentToFirebase)

            var intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                putExtra(
                    "todayTotalStepCount",
                    0
                )
            }
            LocalBroadcastManager.getInstance(applicationContext!!).sendBroadcast(intentToMyDiary)

        } else {

            // ????????? pref ?????? ??? ??????
            todayTotalStepCount = stepCount - startingStepCount

            var intentToFirebase = Intent(this, StepCountBroadcastReceiver::class.java)
            intentToFirebase.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
            intentToFirebase.putExtra("todayTotalStepCount", todayTotalStepCount)
            sendBroadcast(intentToFirebase)

            StepCountNotification(this, todayTotalStepCount)


            var intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                putExtra(
                    "todayTotalStepCount",
                    todayTotalStepCount
                )
            }
            LocalBroadcastManager.getInstance(applicationContext!!).sendBroadcast(intentToMyDiary)
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("?????????", "accuracychanged")
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
                .setContentTitle("$step ??????")
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
            startForeground(1, notification)
        }
    }
}




