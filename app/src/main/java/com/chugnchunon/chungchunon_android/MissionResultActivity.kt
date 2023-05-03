package com.chugnchunon.chungchunon_android

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.chugnchunon.chungchunon_android.DataClass.RankingLine
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.MissionFragment
import com.chugnchunon.chungchunon_android.MissionDetailActivity.Companion.REFRESH_RESULT_MISSION_CODE
import com.chugnchunon.chungchunon_android.databinding.ActivityCommentBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityMissionDetailBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityMissionResultBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import me.moallemi.tools.daterange.localdate.rangeTo
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timer

class MissionResultActivity : Activity() {

    private val binding by lazy {
        ActivityMissionResultBinding.inflate(layoutInflater)
    }


    override fun onBackPressed() {
        finish()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val participateState = intent.getBooleanExtra("participateState", false)

        if (participateState) {
            binding.bigResultText.text = "이미 참여하셨습니다"
            binding.smallResultText.text = "열심히 점수 쌓고 계신가요? 화이팅!"
        } else {
            binding.bigResultText.text = "참여되었습니다"
            binding.smallResultText.text = "지금부터 열심히 점수를 쌓아보아요!"
        }

        Handler().postDelayed({
            finish()
        }, 2000)

        binding.missionResultLayout.setOnClickListener {
            finish()
        }
    }
}

