package com.chugnchunon.chungchunon_android

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.chugnchunon.chungchunon_android.databinding.ActivityMissionResultBinding

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

        val participateState = intent.getStringExtra("participateState").toString()


        if (participateState == "Already") {
            binding.bigResultText.text = "이미 참여하셨습니다"
            binding.smallResultText.text = "열심히 점수 쌓고 계신가요?"
        } else if (participateState == "Excess") {
            binding.bigResultText.text = "100명이 이미 참여했습니다"
            binding.smallResultText.text = "아쉽지만 다음에 도전해보아요!"
        } else if (participateState == "Possible") {
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

