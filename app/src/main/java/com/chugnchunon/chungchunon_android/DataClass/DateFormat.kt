package com.chugnchunon.chungchunon_android.DataClass

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.util.Log
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.util.*

class DateFormat {

    fun isDatePassed(certainBirthday: Int): Boolean {
        var passedOrNot : Boolean = false

        val currentMonth = LocalDate.now().monthValue.toString().toInt()
        val currentDate = LocalDate.now().dayOfMonth.toString().toInt()
        val certainMonth = certainBirthday.toString().substring(0, 1).toInt() // 0, 2
        val certainDate = certainBirthday.toString().substring(2, 3).toInt() // 2, 4

        passedOrNot = if(currentMonth == certainMonth) {
            currentDate > certainDate
        } else {
            currentMonth > certainMonth
        }
        return passedOrNot
    }

    fun calculateAge(birthYear: Int, birthDay: Int): Int {
        var returnAge: Int = 0
        val currentYear: Int = LocalDate.now().year
        val initialAge = currentYear - birthYear

        returnAge = if(isDatePassed(birthDay)) initialAge else initialAge -1
        return returnAge
    }

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