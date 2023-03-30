package com.chugnchunon.chungchunon_android.DataClass

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import java.sql.Date
import java.sql.Timestamp
import java.util.*

class DateFormat {

    fun convertMillis(timefromdb: com.google.firebase.Timestamp): Long {
        val timestamp = timefromdb as com.google.firebase.Timestamp
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000

        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val netDate = Date(milliseconds)
        val date = sdf.format(netDate).toString()
        return milliseconds
    }

    fun convertTimeStampToDateTime(timefromdb: com.google.firebase.Timestamp): String {
        val timestamp = timefromdb as com.google.firebase.Timestamp
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000

        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val netDate = Date(milliseconds)
        val date = sdf.format(netDate).toString()
        return date
    }

    fun convertTimeStampToDate(timefromdb: com.google.firebase.Timestamp): String {
        val timestamp = timefromdb as com.google.firebase.Timestamp
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000

        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val netDate = Date(milliseconds)
        val date = sdf.format(netDate).toString()
        return date
    }

    fun convertMillisToDate(milliseconds: Long): String {
        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val netDate = Date(milliseconds)
        val date = sdf.format(netDate).toString()
        return date
    }

    fun doDayOfWeek(): String? {
        val cal: Calendar = Calendar.getInstance()
        var strWeek: String? = null
        val nWeek: Int = cal.get(Calendar.DAY_OF_WEEK)

        if (nWeek == 1) {
            strWeek = "일"
        } else if (nWeek == 2) {
            strWeek = "월"
        } else if (nWeek == 3) {
            strWeek = "화"
        } else if (nWeek == 4) {
            strWeek = "수"
        } else if (nWeek == 5) {
            strWeek = "목"
        } else if (nWeek == 6) {
            strWeek = "금"
        } else if (nWeek == 7) {
            strWeek = "토"
        }
        return strWeek
    }

}