package com.chugnchunon.chungchunon_android.Service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chugnchunon.chungchunon_android.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    companion object {
        val CHANNEL_ID : String = "COMMENT_PUSH"
        val CHANNEL_NAME : String = "CommentPush"

    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)

        var tokenSet = hashMapOf(
            "fcmToken" to newToken
        )
        userDB.document("$userId")
            .set(tokenSet, SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // 수신한 메세지를 처리

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        var builder : NotificationCompat.Builder

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        } else {
            builder = NotificationCompat.Builder(applicationContext)
        }

        val title = message.notification?.title
        val body = message.notification?.body

        builder.setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_new_alarm_icon)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setAutoCancel(true)

        val random = Random
        val m = random.nextInt(9999-1000) + 1000

        val notification = builder.build()
        notificationManager.notify(m, notification)
    }

}
