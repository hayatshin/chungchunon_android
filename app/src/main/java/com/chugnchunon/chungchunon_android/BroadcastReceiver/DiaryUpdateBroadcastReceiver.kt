package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DiaryUpdateBroadcastReceiver : BroadcastReceiver() {

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val currentDate = LocalDate.now().toString()

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent!!.action
        if (intentAction == Intent.ACTION_TIME_TICK) {

            diaryDB
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents){
                        // 오늘 쓴 글
                        var timeStamp = document.data.getValue("timestamp") as com.google.firebase.Timestamp
                        var date = DateFormat().convertTimeStampToDate(timeStamp)

                        if(date == currentDate) {
                            var diaryId = document.data.getValue("diaryId")
                            var diaryUserId = document.data.getValue("userId")

                            Log.d("걸음수 다이어리 55", "${diaryId} // ${diaryUserId}")

                            userDB.document("$diaryUserId").get()
                                .addOnSuccessListener { document ->
                                    var userStepCount = document.data?.getValue("todayStepCount")
                                    var newStepCountSet = hashMapOf(
                                        "stepCount" to userStepCount
                                    )

                                    diaryDB.document("$diaryId")
                                        .set(newStepCountSet, SetOptions.merge())

                                    Log.d("걸음수 다이어리 66", "$userStepCount")

                                }

                        } else null
                    }
                }


//            diaryDB.document("${userId}_${currentDate}")
//                .get()
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        var document = task.result
//                        if (document != null) {
//                            if (document.exists()) {
//                                var diaryUserId = document.data?.getValue("userId").toString()
//                                Log.d("걸음수 다이어리 diaryUserId", "$diaryUserId")
//
//                                userDB.document("$diaryUserId").get()
//                                    .addOnSuccessListener { document ->
//                                        var recentStepCount =
//                                            document.data?.getValue("todayStepCount")
//                                        Log.d("걸음수 다이어리 recent", "$recentStepCount")
//                                        var recentStepCountSet = hashMapOf(
//                                            "stepCount" to recentStepCount
//                                        )
//                                        diaryDB.document("${diaryUserId}_${currentDate}")
//                                            .set(recentStepCountSet, SetOptions.merge())
//                                    }
//                            }
//
//
//                        }
//                    }
//                }
        }


    }
}