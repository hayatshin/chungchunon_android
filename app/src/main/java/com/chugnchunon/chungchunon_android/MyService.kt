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
import com.google.firebase.firestore.SetOptions
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

    private var startingStepCount: Int = 0
    private var stepCount: Int = 0

    override fun onCreate() {
        super.onCreate()

        StepCountNotification(this, todayTotalStepCount)


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

        // shared preference 설정
        prefs = getSharedPreferences(stepCountSharedPref, Context.MODE_PRIVATE)
        var dummyData = prefs.getInt(userId, 0)
        var datePrefs = getSharedPreferences(dateChangeSharedPref, Context.MODE_PRIVATE)

        // DateChangeBroadcastReceiver : 매일 걸음수 0
        dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()
        val dateChangeIntent = IntentFilter()
        dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
        applicationContext?.registerReceiver(dateChangeBroadcastReceiver, dateChangeIntent)

        // 다이어리 업데이트
//        diaryUpdateBroadcastReceiver = DiaryUpdateBroadcastReceiver()
//
//        val diaryUpdateIntent = IntentFilter()
//        diaryUpdateIntent.addAction(Intent.ACTION_TIME_TICK)
//        applicationContext?.registerReceiver(diaryUpdateBroadcastReceiver, diaryUpdateIntent)


        // DeviceShutDownBroadcastReceiver : 핸드폰 꺼질 때
        deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()

        val deviceShutdownIntent = IntentFilter()
        deviceShutdownIntent.addAction(Intent.ACTION_SHUTDOWN)
        applicationContext?.registerReceiver(deviceShutdownBroadcastReceiver, deviceShutdownIntent)

        // StepCountBroadcastReceiver : 디비 저장

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

        db.collection("user_step_count").document("$userId")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    var snapShot = document.data
                    if (snapShot!!.containsKey("dummy")) {
                        // 더미값 존재
                        var dummyStepCount = (snapShot["dummy"] as Long).toInt()

                        todayTotalStepCount = stepCount - dummyStepCount

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
                        LocalBroadcastManager.getInstance(applicationContext!!)
                            .sendBroadcast(intentToMyDiary)


                        Log.d(
                            "걸음수 체크체크 2",
                            "stepCount: ${stepCount} // startingStepCount: ${dummyStepCount}"
                        )

                    } else {
                        // 더미값 존재 x

                        var newDummySet = hashMapOf(
                            "dummy" to stepCount
                        )
                        db.collection("user_step_count").document("$userId")
                            .set(newDummySet, SetOptions.merge())

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
                        LocalBroadcastManager.getInstance(applicationContext!!)
                            .sendBroadcast(intentToMyDiary)
                    }
                }
            }

//        if (!prefs.contains(userId)) {
//            // 걸음수 pref 저장 안 된 상태
//
//            // 1. 걸음수 초기화
//            editor.putInt(userId, stepCount)
//            editor.commit()
//
//            StepCountNotification(this, 0)
//
//            var intentToFirebase = Intent(this, StepCountBroadcastReceiver::class.java)
//            intentToFirebase.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
//            intentToFirebase.putExtra("todayTotalStepCount", 0)
//            sendBroadcast(intentToFirebase)
//
//            var intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
//                putExtra(
//                    "todayTotalStepCount",
//                    0
//                )
//            }
//            LocalBroadcastManager.getInstance(applicationContext!!)
//                .sendBroadcast(intentToMyDiary)
//
//        } else {
//
//            // 걸음수 pref 저장 된 상태
//            todayTotalStepCount = stepCount - startingStepCount
//
//            var intentToFirebase = Intent(this, StepCountBroadcastReceiver::class.java)
//            intentToFirebase.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
//            intentToFirebase.putExtra("todayTotalStepCount", todayTotalStepCount)
//            sendBroadcast(intentToFirebase)
//
//            StepCountNotification(this, todayTotalStepCount)
//
//
//            var intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
//                putExtra(
//                    "todayTotalStepCount",
//                    todayTotalStepCount
//                )
//            }
//            LocalBroadcastManager.getInstance(applicationContext!!)
//                .sendBroadcast(intentToMyDiary)
//        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("서비스", "accuracychanged")
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
                .setContentTitle("$step 걸음")
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
            startForeground(1, notification)
        }
    }
}




