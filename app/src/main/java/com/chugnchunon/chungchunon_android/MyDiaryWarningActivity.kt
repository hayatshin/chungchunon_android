package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import androidx.core.content.ContextCompat
import androidx.core.text.backgroundColor
import androidx.core.text.color
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityMyDiaryWarningBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterWarningBinding

class MyDiaryWarningActivity: Activity() {

    private val binding by lazy {
        ActivityMyDiaryWarningBinding.inflate(layoutInflater)
    }

    private var unfilledArray = ArrayList<String>()
    private var unfilledText: String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val recognitionBoolean = intent.getBooleanExtra("recognition", true)
        val moodBoolean = intent.getBooleanExtra("mood", true)
        val diaryBoolean = intent.getBooleanExtra("diary", true)

        if(!recognitionBoolean) unfilledArray.add("인지")
        if(!moodBoolean) unfilledArray.add("마음")
        if(!diaryBoolean) unfilledArray.add("쓰기")

        unfilledText = unfilledArray.joinToString(", ")

        val spanText = SpannableStringBuilder()
            .backgroundColor(ContextCompat.getColor(this, R.color.yellow_highlight)) {append("$unfilledText")}
            .append("도\n" +
                    "작성해주세요!")

        binding.registerWarningText.text = spanText


        binding.confirmBox.setOnClickListener {
            finish()

            val intent = Intent(this, MyDiaryFragment::class.java)
            intent.setAction("MY_DIARY_WARNING_FEEDBACK")
            intent.putExtra("recognitionBoolean", recognitionBoolean)
            intent.putExtra("moodBoolean", moodBoolean)
            intent.putExtra("diaryBoolean", diaryBoolean)
            LocalBroadcastManager.getInstance(this!!).sendBroadcast(intent)
        }
    }
}