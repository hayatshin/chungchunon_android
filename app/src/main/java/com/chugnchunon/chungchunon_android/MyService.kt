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

    // 걸음수
    private var MagnitudePrevious: Double = 0.0
    private var stepCount: Int = 0

    companion object {
        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"

        var todayTotalStepCount: Int? = 0

        const val STEP_THRESHOLD: Int = 6
    }

    override fun onCreate() {
        super.onCreate()

//        sensorManager =
//            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_GAME)

        sensorManager =
            applicationContext?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_NORMAL)


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
            sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_GAME)
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


    override fun onSensorChanged(sensorEvent: SensorEvent?) {

//        if(sensorEvent != null) {
//            var x_acceleration: Double = sensorEvent.values[0].toDouble()
//            var y_acceleration: Double = sensorEvent.values[1].toDouble()
//            var z_acceleration: Double = sensorEvent.values[2].toDouble()
//
//            var Magnitude: Double = Math.sqrt(x_acceleration * x_acceleration + y_acceleration * y_acceleration + z_acceleration * z_acceleration)
//            var MagnitudeDelta: Double = Magnitude - MagnitudePrevious
//            MagnitudePrevious = Magnitude
//
//            if(MagnitudeDelta > STEP_THRESHOLD){
//                 todayTotalStepCount = todayTotalStepCount?.plus(1)
//
//                var currentDate = LocalDate.now()
//                StepCountNotification(this, todayTotalStepCount)
//
//                // user 내 todayStepCount
//                var todayStepCountSet = hashMapOf(
//                    "todayStepCount" to todayTotalStepCount
//                )
//                userDB.document("$userId").set(todayStepCountSet, SetOptions.merge())
//
//                var userStepCountSet = hashMapOf(
//                    "$currentDate" to todayTotalStepCount
//                )
//
//                var periodStepCountSet = hashMapOf(
//                    "$userId" to todayTotalStepCount
//                )
//
//                // user_step_count
//                db.collection("user_step_count")
//                    .document("$userId")
//                    .set(userStepCountSet, SetOptions.merge())
//
//                // period_step_count
//                db.collection("period_step_count")
//                    .document("$currentDate")
//                    .set(periodStepCountSet, SetOptions.merge())
//
//                var intent = Intent(this, StepCountBroadcastReceiver::class.java)
//                intent.setAction(ACTION_STEP_COUNTER_NOTIFICATION)
//                intent.putExtra("todayTotalStepCount", todayTotalStepCount)
//                sendBroadcast(intent)
//            }
//        }

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
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_stepcount_noti)
                .setContentTitle("$todayTotalStepCount 걸음")
                .setColor(ContextCompat.getColor(context, R.color.main_color))
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .build()
            startForeground(1, notification)
        }
    }
}
operator fun <T> MutableLiveData<T>.plus(t: String): MutableLiveData<T> = this + t