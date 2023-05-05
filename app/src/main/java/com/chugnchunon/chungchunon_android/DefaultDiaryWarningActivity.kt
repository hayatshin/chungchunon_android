package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.databinding.ActivityDefaultDiaryWarningBinding
import kotlinx.android.synthetic.main.activity_region.*


class DefaultDiaryWarningActivity : FragmentActivity() {

    private val binding by lazy {
        ActivityDefaultDiaryWarningBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.setStatusBarColor(Color.parseColor("#B3000000"));

        val warningType = intent.getStringExtra("warningType")

        // --> editDiary / emptyRegion / partnerRegister

        if(warningType == "editDiary") {
            // 일기 수정
            binding.edWarningText.text = "수정하신 내용이 없습니다.\n수정 후 확인을 눌러주세요!"

            binding.edGobackArrow.setOnClickListener {
                finish()
            }

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "emptyRegion") {
            // 거주지역 설정 x
            binding.edWarningText.text = "거주 지역 설정이 안 되었습니다.\n설정 후 다시 돌아와주세요!"

            binding.edGobackArrow.visibility = View.GONE

            binding.edConfirmBox.setOnClickListener {
                val goRegion = Intent(this, RegionRegisterActivity::class.java)
                smallRegionCheck = false
                startActivity(goRegion)
            }
        } else if (warningType == "partnerRegister") {
            // 파트너 가입
            binding.edWarningText.text = "파트너 가입 서비스는 준비 중입니다."

            binding.edConfirmBox.setOnClickListener {
               finish()
            }
        } else if (warningType == "kakaoLoginError") {
            // 카카오 로그인 실패
            binding.edWarningText.text = "카카오 로그인이 실패했습니다.\n일반 로그인으로 시도해주세요."

            binding.edConfirmBox.setOnClickListener {
                val goMain = Intent(this, MainActivity::class.java)
                startActivity(goMain)
            }
        } else if (warningType == "partnerDiary") {
            // 파트너 일기 작성
            binding.edWarningText.text = "50세 미만은 일기 쓰기가 제한됩니다."

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "partnerMission") {
            binding.edWarningText.text = "50세 미만은 행사 참여가 제한됩니다."

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        }
    }
}

