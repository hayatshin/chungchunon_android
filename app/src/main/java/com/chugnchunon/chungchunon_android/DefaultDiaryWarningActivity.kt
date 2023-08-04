package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.backgroundColor
import androidx.core.text.bold
import androidx.core.text.color
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.databinding.ActivityDefaultDiaryWarningBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_region.*


class DefaultDiaryWarningActivity : FragmentActivity() {

    private val binding by lazy {
        ActivityDefaultDiaryWarningBinding.inflate(layoutInflater)
    }

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

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

        if (warningType == "editDiary") {
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

            binding.edGobackArrow.setOnClickListener {
                finish()
            }

            binding.edWarningText.text = "파트너 가입 서비스는 준비 중입니다."

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "kakaoLoginError") {
            // 카카오 로그인 실패

            binding.edGobackArrow.visibility = View.GONE

            binding.edWarningText.text = "카카오 로그인이 실패했습니다.\n일반 로그인으로 시도해주세요."

            binding.edConfirmBox.setOnClickListener {
                val goMain = Intent(this, MainActivity::class.java)
                startActivity(goMain)
            }
        } else if (warningType == "partnerDiary") {
            // 파트너 일기 작성

            binding.edGobackArrow.setOnClickListener {
                finish()
            }
            binding.edWarningText.text = "40세 미만은 일기 쓰기가 제한됩니다.\n댓글과 좋아요로 응원해주세요!"

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "partnerMission") {
            binding.edWarningText.text = "40세 미만은 행사 참여가 제한됩니다."

            binding.edGobackArrow.setOnClickListener {
                finish()
            }

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "appInvitation") {
            binding.edWarningText.text = "초대하기에 오류가 있습니다."

            binding.edGobackArrow.setOnClickListener {
                finish()
            }

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "allowStep") {
            binding.edWarningText.text = "걸음수 측정이 시작됩니다."

            binding.edGobackArrow.visibility = View.GONE

            binding.edConfirmBox.setOnClickListener {

                val stepStatusSet = hashMapOf(
                    "step_status" to true,
                )

                userDB.document("$userId")
                    .set(stepStatusSet, SetOptions.merge())
                    .addOnSuccessListener {

                        try {
                            val startService =
                                Intent(this, MyService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(
                                    this,
                                    startService
                                );
                            } else {
                                this.startService(startService);
                            }
                        } catch (e: Exception) {
                            val serviceError = hashMapOf(
                                "service_error" to e,
                                "service_step_status" to false,
                            )
                            userDB.document("$userId")
                                .set(serviceError, SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "측정이 불가한 기종입니다. 문의를 남겨주세요.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        finish()
                    }
            }
        } else if (warningType == "authStepNo") {

            binding.edGobackArrow.visibility = View.GONE

            val authStepSet = hashMapOf(
                "auth_step" to false,
            )
            userDB.document("$userId")
                .set(authStepSet, SetOptions.merge())

            val spanText = SpannableStringBuilder()
                .append("걸음수 측정 권한이 없습니다.\n\n")
                .append("휴대폰의 ")
                .bold { append("설정 > 애플리케이션 > 오늘도청춘 > 권한") }
                .append("에서\n")
                .backgroundColor(ContextCompat.getColor(this, R.color.yellow_highlight)) {append("신체 활동 권한")}
                .append("을 허용해주세요.")
            binding.edWarningText.setText(spanText)

            binding.edConfirmBox.setOnClickListener {
                finish()
            }
        } else if (warningType == "originLoginFail") {
            binding.edWarningText.text = "유저 정보가 없습니다.\n다시 회원가입을 진행해주세요."

            binding.edGobackArrow.setOnClickListener {
                finish()
            }

            binding.edConfirmBox.setOnClickListener {
                val goMain = Intent(this, MainActivity::class.java)
                startActivity(goMain)
            }
        }
    }
}

