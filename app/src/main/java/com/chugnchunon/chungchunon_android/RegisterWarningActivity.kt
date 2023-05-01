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
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterWarningBinding

class RegisterWarningActivity: Activity() {

    private val binding by lazy {
        ActivityRegisterWarningBinding.inflate(layoutInflater)
    }

    private var unfilledArray = ArrayList<String>()
    private var unfilledText: String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        val avatarBoolean = intent.getBooleanExtra("avatar", true)
        val nameBoolean = intent.getBooleanExtra("name", true)
        val birthBoolean = intent.getBooleanExtra("birth", true)
        val phoneBoolean = intent.getBooleanExtra("phone", true)

        if(!avatarBoolean) unfilledArray.add("사진")
        if(!nameBoolean) unfilledArray.add("이름")
        if(!birthBoolean) unfilledArray.add("생년월일")
        if(!phoneBoolean) unfilledArray.add("휴대폰 번호")

        unfilledText = unfilledArray.joinToString(", ")

        val spanText = SpannableStringBuilder()
            .backgroundColor(ContextCompat.getColor(this, R.color.yellow_highlight)) {append("$unfilledText")}
            .append("도\n" +
                    "작성해주세요!")

        binding.registerWarningText.text = spanText


        binding.confirmBox.setOnClickListener {
            finish()

            val intent = Intent(this, RegisterActivity::class.java)
            intent.setAction("REGISTER_WARNING_CONFIRM")
            intent.putExtra("avatarBoolean", avatarBoolean)
            intent.putExtra("nameBoolean", nameBoolean)
            intent.putExtra("birthBoolean", birthBoolean)
            intent.putExtra("phoneBoolean", phoneBoolean)
            LocalBroadcastManager.getInstance(this!!).sendBroadcast(intent)
        }
    }
}