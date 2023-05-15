package com.chugnchunon.chungchunon_android.Service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.chugnchunon.chungchunon_android.BroadcastReceiver.*
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.MainActivity
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.Service.MyFirebaseMessagingService.Companion.CHANNEL_ID
import com.chugnchunon.chungchunon_android.StartActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MyService : Service(), SensorEventListener {

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid


    lateinit var dateChangeBroadcastReceiver: DateChangeBroadcastReceiver
    lateinit var deviceShutdownBroadcastReceiver: DeviceShutdownBroadcastReceiver

    lateinit var alarmManager: AlarmManager
    lateinit var alarmIntent: Intent
    lateinit var pendingIntent: PendingIntent
    private val alarmBroadcastReceiver: AlarmBroadcastReceiver = AlarmBroadcastReceiver()

    companion object {
        const val ACTION_STEP_COUNTER_NOTIFICATION =
            "com.chungchunon.chunchunon_android.STEP_COUNTER_NOTIFICATION"
        const val ALARM_NOTIFICATION_NAME = "android.intent.action.MAIN"

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "1000"

        lateinit var sensorManager: SensorManager
        lateinit var step_sensor: Sensor
        var todayTotalStepCount: Int? = 0
        const val ALARM_REQ_CODE = 200

        const val UNIQUE_WORK_NAME = "stepWork"

        var alarmBroadcastReceiverCalled: Boolean = false
    }

    private var stepCount: Int = 0

    override fun onCreate() {
        super.onCreate()

        Log.d("서비스", "onCreate")

    }


    @SuppressLint("SimpleDateFormat")
    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        stepCount = sensorEvent!!.values[0].toInt()

        Log.d("걸음", "$stepCount")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val yesterdayCal = Calendar.getInstance()
        yesterdayCal.add(Calendar.DATE, -1)
        val yesterday = dateFormat.format(yesterdayCal.time)
        val todayCal = Calendar.getInstance()
        val today = dateFormat.format(todayCal.time)

        if (stepCount == 0) {
            // 리부팅 된 경우

            db.collection("user_step_count").document("$userId")
                .get()
                .addOnSuccessListener { userSteps ->
                    if (userSteps.contains(today)) {
                        // 리부팅: 오늘 걸음수 있는 경우
                        val todayIngStep = (userSteps.data?.getValue(today) as Long).toInt()
                        if (todayIngStep != 0) {
                            // 리부팅: 누적 걸음수 0이 아닌 경우 -> *일반적 리부팅
                            val newTodayIngStepSet = hashMapOf(
                                "dummy" to -todayIngStep
                            )
                            db.collection("user_step_count").document("$userId")
                                .set(newTodayIngStepSet, SetOptions.merge())

                            StepCountNotification(this, todayIngStep)

                            val userStepCountSet = hashMapOf(
                                today to todayIngStep
                            )

                            val periodStepCountSet = hashMapOf(
                                "$userId" to todayIngStep
                            )

                            // user_step_count
                            db.collection("user_step_count")
                                .document("$userId")
                                .set(userStepCountSet, SetOptions.merge())

                            // period_step_count
                            db.collection("period_step_count")
                                .document(today)
                                .set(periodStepCountSet, SetOptions.merge())

                            val todayStepCountSet = hashMapOf(
                                "todayStepCount" to todayIngStep
                            )
                            userDB.document("$userId")
                                .set(todayStepCountSet, SetOptions.merge())

                            val intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                                putExtra(
                                    "todayTotalStepCount",
                                    todayIngStep
                                )
                            }
                            LocalBroadcastManager.getInstance(applicationContext!!)
                                .sendBroadcast(intentToMyDiary)
                        } else {
                            // 리부팅: 누적 걸음수 0인 경우
                            val newTodayIngStepSet = hashMapOf(
                                "dummy" to 0
                            )
                            db.collection("user_step_count").document("$userId")
                                .set(newTodayIngStepSet, SetOptions.merge())

                            StepCountNotification(this, 0)

                            val userStepCountSet = hashMapOf(
                                today to 0
                            )

                            val periodStepCountSet = hashMapOf(
                                "$userId" to 0
                            )

                            // user_step_count
                            db.collection("user_step_count")
                                .document("$userId")
                                .set(userStepCountSet, SetOptions.merge())

                            // period_step_count
                            db.collection("period_step_count")
                                .document(today)
                                .set(periodStepCountSet, SetOptions.merge())

                            val todayStepCountSet = hashMapOf(
                                "todayStepCount" to 0
                            )
                            userDB.document("$userId")
                                .set(todayStepCountSet, SetOptions.merge())

                            val intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                                putExtra(
                                    "todayTotalStepCount",
                                    0
                                )
                            }
                            LocalBroadcastManager.getInstance(applicationContext!!)
                                .sendBroadcast(intentToMyDiary)
                        }
                    } else {
                        // 리부팅: 오늘 걸음수 없는 경우 -> 새로운 날 (DateChange보다 먼저 작동)
                        val newTodayIngStepSet = hashMapOf(
                            "dummy" to 0
                        )
                        db.collection("user_step_count").document("$userId")
                            .set(newTodayIngStepSet, SetOptions.merge())

                        StepCountNotification(this, 0)

                        val userStepCountSet = hashMapOf(
                            today to 0
                        )

                        val periodStepCountSet = hashMapOf(
                            "$userId" to 0
                        )

                        // user_step_count
                        db.collection("user_step_count")
                            .document("$userId")
                            .set(userStepCountSet, SetOptions.merge())

                        // period_step_count
                        db.collection("period_step_count")
                            .document(today)
                            .set(periodStepCountSet, SetOptions.merge())

                        val todayStepCountSet = hashMapOf(
                            "todayStepCount" to 0
                        )
                        userDB.document("$userId")
                            .set(todayStepCountSet, SetOptions.merge())

                        val intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                            putExtra(
                                "todayTotalStepCount",
                                0
                            )
                        }
                        LocalBroadcastManager.getInstance(applicationContext!!)
                            .sendBroadcast(intentToMyDiary)
                    }
                }
        } else {
            // 리부팅 x
            db.collection("user_step_count").document("$userId").get()
                .addOnSuccessListener { userStepCount ->
                    if (userStepCount.contains("dummy")) {
                        // 리부팅 x: 더미 값이 있는 경우 -> 처음 시작 x
                        val dummyStepCount = (userStepCount.data?.getValue("dummy") as Long).toInt()

                        if (userStepCount.contains(today)) {
                            // 리부팅 x: 오늘 값이 있는 경우 -> *잘 작동 중
                            Log.d("걸음수체크", "리부팅 x: 오늘 값이 있는 경우 -> *잘 작동 중")

                            todayTotalStepCount = stepCount - dummyStepCount

                            StepCountNotification(this, todayTotalStepCount)

                            val userStepCountSet = hashMapOf(
                                today to todayTotalStepCount
                            )
                            val periodStepCountSet = hashMapOf(
                                "$userId" to todayTotalStepCount
                            )

                            // user_step_count
                            db.collection("user_step_count")
                                .document("$userId")
                                .set(userStepCountSet, SetOptions.merge())
                            // period_step_count
                            db.collection("period_step_count")
                                .document(today)
                                .set(periodStepCountSet, SetOptions.merge())


                            val todayStepCountSet = hashMapOf(
                                "todayStepCount" to todayTotalStepCount
                            )
                            userDB.document("$userId")
                                .set(todayStepCountSet, SetOptions.merge())

                            val intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                                putExtra(
                                    "todayTotalStepCount",
                                    todayTotalStepCount
                                )
                            }
                            LocalBroadcastManager.getInstance(applicationContext!!)
                                .sendBroadcast(intentToMyDiary)
                        } else {
                            // 리부팅 x: 오늘 값이 없는 경우 -> 잘 작동 x
                            Log.d("걸음수체크", "리부팅 x: 오늘 값이 없는 경우 -> DateChange보다 먼저 새로운 날")

                            val newDummySet = hashMapOf(
                                "dummy" to stepCount
                            )
                            db.collection("user_step_count").document("$userId")
                                .set(newDummySet, SetOptions.merge())

                            StepCountNotification(this, 0)

                            val userStepCountSet = hashMapOf(
                                today to 0
                            )

                            val periodStepCountSet = hashMapOf(
                                "$userId" to 0
                            )

                            // user_step_count
                            db.collection("user_step_count")
                                .document("$userId")
                                .set(userStepCountSet, SetOptions.merge())

                            // period_step_count
                            db.collection("period_step_count")
                                .document(today)
                                .set(periodStepCountSet, SetOptions.merge())

                            val todayStepCountSet = hashMapOf(
                                "todayStepCount" to 0
                            )
                            userDB.document("$userId")
                                .set(todayStepCountSet, SetOptions.merge())

                            val intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
                                putExtra(
                                    "todayTotalStepCount",
                                    0
                                )
                            }
                            LocalBroadcastManager.getInstance(applicationContext!!)
                                .sendBroadcast(intentToMyDiary)
                        }
                    } else {
                        // 리부팅 x: 더미 값이 없는 경우 -> 처음 시작
                        Log.d("걸음수체크", "리부팅 x: 더미 값이 없는 경우 -> 처음 시작")

                        val newDummySet = hashMapOf(
                            "dummy" to stepCount
                        )
                        db.collection("user_step_count").document("$userId")
                            .set(newDummySet, SetOptions.merge())

                        StepCountNotification(this, 0)

                        val userStepCountSet = hashMapOf(
                            today to 0
                        )

                        val periodStepCountSet = hashMapOf(
                            "$userId" to 0
                        )

                        // user_step_count
                        db.collection("user_step_count")
                            .document("$userId")
                            .set(userStepCountSet, SetOptions.merge())

                        // period_step_count
                        db.collection("period_step_count")
                            .document(today)
                            .set(periodStepCountSet, SetOptions.merge())

                        val todayStepCountSet = hashMapOf(
                            "todayStepCount" to 0
                        )
                        userDB.document("$userId")
                            .set(todayStepCountSet, SetOptions.merge())

                        val intentToMyDiary = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply {
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
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)

        Log.d("서비스", "onStart")

        // 워크매니저
        val workManager = WorkManager.getInstance(applicationContext)
        val periodicWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<RegisterAlarmWorker>(15, TimeUnit.MINUTES).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        val state = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME).get()


        // 알람 매니저
        alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        registerReceiver(alarmBroadcastReceiver, IntentFilter(ALARM_NOTIFICATION_NAME))

        alarmIntent = Intent(applicationContext, AlarmBroadcastReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            ALARM_REQ_CODE,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val newCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            newCalendar.timeInMillis,
            pendingIntent
        )

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

        // 1. 1분마다 체크 (날짜 바뀔 때)
        dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()
        val dateChangeIntent = IntentFilter()
        dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
        applicationContext?.registerReceiver(dateChangeBroadcastReceiver, dateChangeIntent)

        // 2. 날짜 바뀔 때
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        )

        // 3. 핸드폰 꺼질 때
        deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()
        val deviceShutdownIntent = IntentFilter()
        deviceShutdownIntent.addAction(Intent.ACTION_BOOT_COMPLETED)
        deviceShutdownIntent.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        applicationContext?.registerReceiver(deviceShutdownBroadcastReceiver, deviceShutdownIntent)

        // 4. 알람 주기적 브로드캐스터
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            BroadcastReregister,
            IntentFilter("ALARM_BROADCAST_RECEIVER_RING")
        );

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("서비스", "onStartCommand")

        // 워크매니저
        val workManager = WorkManager.getInstance(applicationContext)
        val periodicWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<RegisterAlarmWorker>(15, TimeUnit.MINUTES).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        val state = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME).get()


        // 알람 매니저
        alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        registerReceiver(alarmBroadcastReceiver, IntentFilter(ALARM_NOTIFICATION_NAME))

        alarmIntent = Intent(applicationContext, AlarmBroadcastReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            ALARM_REQ_CODE,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val newCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            newCalendar.timeInMillis,
            pendingIntent
        )

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

        // 1. 1분마다 체크 (날짜 바뀔 때)
        dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()
        val dateChangeIntent = IntentFilter()
        dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
        applicationContext?.registerReceiver(dateChangeBroadcastReceiver, dateChangeIntent)

        // 2. 날짜 바뀔 때
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        )

        // 3. 핸드폰 꺼질 때
        deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()
        val deviceShutdownIntent = IntentFilter()
        deviceShutdownIntent.addAction(Intent.ACTION_BOOT_COMPLETED)
        deviceShutdownIntent.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        applicationContext?.registerReceiver(deviceShutdownBroadcastReceiver, deviceShutdownIntent)

        // 4. 알람 주기적 브로드캐스터
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            BroadcastReregister,
            IntentFilter("ALARM_BROADCAST_RECEIVER_RING")
        )
        return START_STICKY
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onLowMemory() {
        super.onLowMemory()

        Log.d("서비스", "onLowMemory")
    }

    override fun onDestroy() {
        Log.d("서비스", "onDestroy")

        stopForeground(false)

        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(dateChangeBroadcastReceiver)
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(stepInitializeReceiver)
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(deviceShutdownBroadcastReceiver)
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(BroadcastReregister)

        // 1. 1분마다 체크 (날짜 바뀔 때)
        dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()
        val dateChangeIntent = IntentFilter()
        dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
        applicationContext?.registerReceiver(dateChangeBroadcastReceiver, dateChangeIntent)

        // 2. 날짜 바뀔 때
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        )

        // 3. 핸드폰 꺼질 때
        deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()
        val deviceShutdownIntent = IntentFilter()
        deviceShutdownIntent.addAction(Intent.ACTION_BOOT_COMPLETED)
        deviceShutdownIntent.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        applicationContext?.registerReceiver(deviceShutdownBroadcastReceiver, deviceShutdownIntent)

        // 4. 알람 주기적 브로드캐스터
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            BroadcastReregister,
            IntentFilter("ALARM_BROADCAST_RECEIVER_RING")
        )

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    var BroadcastReregister: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

//            val alarmManager =
//                applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            val alarmBroadcastReceiver = AlarmBroadcastReceiver()
//            registerReceiver(alarmBroadcastReceiver, IntentFilter(ALARM_NOTIFICATION_NAME))
//            val alarmIntent = Intent(applicationContext, AlarmBroadcastReceiver::class.java)
//            val pendingIntent = PendingIntent.getBroadcast(
//                applicationContext,
//                ALARM_REQ_CODE,
//                alarmIntent,
//                PendingIntent.FLAG_IMMUTABLE
//            )

            if (intent!!.action == "ALARM_BROADCAST_RECEIVER_RING") {

                if (alarmBroadcastReceiverCalled) {

                    val newCalendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                    }
                    newCalendar.add(Calendar.MINUTE, 30)

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        newCalendar.timeInMillis,
                        pendingIntent
                    )


                    // 테스트 코드
//                    if (intent?.hasExtra("alarm") == true) {
//                        val alarmValue = intent.getBooleanExtra("alarm", false)
//                        if (alarmValue == true) {
//
//                            val lastAlarmSet = hashMapOf(
//                                "last_alarm" to FieldValue.serverTimestamp()
//                            )
//
//                            db.collection("background_check")
//                                .document("$userId")
//                                .set(lastAlarmSet, SetOptions.merge())
//                        }
//                    } else if (intent?.hasExtra("no_noti") == true) {
//                        val noNotiValue = intent.getBooleanExtra("no_noti", false)
//                        if (noNotiValue == true) {
//                            val lastNoNotiSet = hashMapOf(
//                                "last_no_noti" to FieldValue.serverTimestamp()
//                            )
//
//                            db.collection("background_check")
//                                .document("$userId")
//                                .set(lastNoNotiSet, SetOptions.merge())
//                        }
//                    }

                    // 브로드 캐스트 다시 등록
                    LocalBroadcastManager.getInstance(applicationContext)
                        .unregisterReceiver(dateChangeBroadcastReceiver)
                    LocalBroadcastManager.getInstance(applicationContext)
                        .unregisterReceiver(deviceShutdownBroadcastReceiver)
                    LocalBroadcastManager.getInstance(applicationContext)
                        .unregisterReceiver(stepInitializeReceiver)

                    // 1. 1분마다 체크 (날짜 바뀔 때)
                    dateChangeBroadcastReceiver = DateChangeBroadcastReceiver()
                    val dateChangeIntent = IntentFilter()
                    dateChangeIntent.addAction(Intent.ACTION_TIME_TICK)
                    applicationContext?.registerReceiver(
                        dateChangeBroadcastReceiver,
                        dateChangeIntent
                    )

                    // 2. 날짜 바뀔 때
                    LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
                        stepInitializeReceiver,
                        IntentFilter("NEW_DATE_STEP_ZERO")
                    )

                    // 3. 핸드폰 꺼질 때
                    deviceShutdownBroadcastReceiver = DeviceShutdownBroadcastReceiver()
                    val deviceShutdownIntent = IntentFilter()
                    deviceShutdownIntent.addAction(Intent.ACTION_BOOT_COMPLETED)
                    deviceShutdownIntent.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                    applicationContext?.registerReceiver(
                        deviceShutdownBroadcastReceiver,
                        deviceShutdownIntent
                    )
                }
                alarmBroadcastReceiverCalled = false
            }
        }
    }

    var stepInitializeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            StepCountNotification(context!!, 0)
        }
    }

    private fun StepCountNotification(context: Context, stepCount: Int?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MyApp",
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
//            NotificationManagerCompat.from(context).createNotificationChannel(channel)

            val decimal = DecimalFormat("#,###")
            val step = decimal.format(stepCount)

            val notificationPendingIntent: PendingIntent =
                Intent(context, DiaryTwoActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(
                        context,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }

            val stepNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_new_alarm_icon)
                .setContentTitle("$step 걸음")
                .setContentIntent(notificationPendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_HIGH)
                .build()

            startForeground(NOTIFICATION_ID, stepNotification)
        }


//            (context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
//                channel
//            )
//
//            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_new_alarm_icon)
//                .setContentTitle("$step 걸음")
//                .setDefaults(Notification.DEFAULT_LIGHTS)
//                .setOngoing(true)
//                .setShowWhen(false)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(false)
//                .build()
//            startForeground(NOTIFICATION_ID, notification)

    }
}






