package com.chugnchunon.chungchunon_android.DataClass

import com.chugnchunon.chungchunon_android.R

class EmoticonInteger {
    fun EmoticonToInt(emoticon: Int): Int {
        var emoticonInt: Int = 0
        when(emoticon) {
//            R.drawable.ic_joy -> emoticonInt = 0
//            R.drawable.ic_shalom -> emoticonInt = 1
//            R.drawable.ic_throb -> emoticonInt = 2
//            R.drawable.ic_soso -> emoticonInt = 3
//            R.drawable.ic_anxious -> emoticonInt = 4
//            R.drawable.ic_sad -> emoticonInt = 5
//            R.drawable.ic_gloomy -> emoticonInt = 6
//            R.drawable.ic_angry -> emoticonInt = 7
            R.drawable.ic_joy -> emoticonInt = 0
            R.drawable.ic_throb -> emoticonInt = 1
            R.drawable.ic_thanksful -> emoticonInt = 2
            R.drawable.ic_shalom -> emoticonInt = 3
            R.drawable.ic_soso -> emoticonInt = 4
            R.drawable.ic_lonely -> emoticonInt = 5
            R.drawable.ic_anxious -> emoticonInt = 6
            R.drawable.ic_gloomy -> emoticonInt = 7
            R.drawable.ic_sad -> emoticonInt = 8
            R.drawable.ic_angry -> emoticonInt = 9
        }
        return emoticonInt
    }


    fun IntToEmoticon(integar: Int): Int {
        var drawable: Int = 0
        when(integar) {
            0 -> drawable = R.drawable.ic_joy
            1 -> drawable = R.drawable.ic_throb
            2 -> drawable = R.drawable.ic_thanksful
            3 -> drawable = R.drawable.ic_shalom
            4 -> drawable = R.drawable.ic_soso
            5 -> drawable = R.drawable.ic_lonely
            6 -> drawable = R.drawable.ic_anxious
            7 -> drawable = R.drawable.ic_gloomy
            8 -> drawable = R.drawable.ic_sad
            9 -> drawable = R.drawable.ic_angry
        }
        return drawable
    }
}