package com.chugnchunon.chungchunon_android.DataClass

import android.graphics.drawable.Drawable
import com.chugnchunon.chungchunon_android.R

class EmoticonInteger {
    fun EmoticonToInt(emoticon: Int): Int {
        var emoticonInt: Int = 0
        when(emoticon) {
            R.drawable.ic_joy -> emoticonInt = 0
            R.drawable.ic_shalom -> emoticonInt = 1
            R.drawable.ic_throb -> emoticonInt = 2
            R.drawable.ic_soso -> emoticonInt = 3
            R.drawable.ic_anxious -> emoticonInt = 4
            R.drawable.ic_sad -> emoticonInt = 5
            R.drawable.ic_gloomy -> emoticonInt = 6
            R.drawable.ic_angry -> emoticonInt = 7
        }
        return emoticonInt
    }


    fun IntToEmoticon(integar: Int): Int {
        var drawable: Int = 0
        when(integar) {
            0 -> drawable = R.drawable.ic_joy
            1 -> drawable = R.drawable.ic_shalom
            2 -> drawable = R.drawable.ic_throb
            3 -> drawable = R.drawable.ic_soso
            4 -> drawable = R.drawable.ic_anxious
            5 -> drawable = R.drawable.ic_sad
            6 -> drawable = R.drawable.ic_gloomy
            7 -> drawable = R.drawable.ic_angry
        }
        return drawable
    }
}