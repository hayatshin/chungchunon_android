package com.chugnchunon.chungchunon_android.DataClass

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import java.sql.Date
import java.sql.Timestamp

class DateFormat {

    fun converDate(timefromdb: com.google.firebase.Timestamp): String {
        val timestamp = timefromdb as com.google.firebase.Timestamp
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val netDate = Date(milliseconds)
        val date = sdf.format(netDate).toString()
        return date
    }


}