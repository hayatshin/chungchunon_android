package com.chugnchunon.chungchunon_android.MoneyActivity

import android.content.Intent
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityMoneyDetailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MoneyDetailActivity : FragmentActivity() {

    private val binding by lazy {
        ActivityMoneyDetailBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.setStatusBarColor(Color.parseColor("#B3000000"));

        binding.buttonCoinImg.visibility = View.VISIBLE
        binding.buttonCoinText.visibility = View.VISIBLE
        binding.buttonCoinProgressBar.visibility = View.GONE

        val decimal = DecimalFormat("#,###")

        val missionGoalScore = intent.getIntExtra("missionGoalScore", 0)
        val formatMissionGoalScore = decimal.format(missionGoalScore)
        val missionPrizeWinners = intent.getIntExtra("missionPrizeWinners", 0)
        val missionSmallDescription = intent.getStringExtra("missionSmallDescription")

        binding.prizeWinnersExplanation.text = "(${formatMissionGoalScore} 적립을 달성한 선착순 ${missionPrizeWinners}명)"
        binding.goalScoreExplanation.text = "${formatMissionGoalScore} 적립을 달성하신 후 아래 '환전하기' 버튼을 클릭해주세요.\n전화번호를 입력하시면 해당 번호로 상품권을 보내드립니다."

        binding.goBackArrow.setOnClickListener {
            finish()
        }

        binding.moneyBackground.setOnClickListener {
            finish()
        }


        userDB.document("$userId").get().addOnSuccessListener { userData ->
            try {
                val userType = userData.data?.getValue("userType").toString().toString()
                if(userType == "파트너") {
                    binding.moneyConfirmBox.alpha = 0.4f

                    binding.moneyConfirmBox.setOnClickListener {
                        Toast.makeText(this, "파트너는 참여할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val userPoint = userData.data?.getValue("userPoint").toString().toInt()

                    if(userPoint < missionGoalScore) {
                        binding.moneyConfirmBox.alpha = 0.4f

                        binding.moneyConfirmBox.setOnClickListener {
                            Toast.makeText(this, "${formatMissionGoalScore} 적립을 달성 후 환전해주세요!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 목표 달성!
                        binding.moneyConfirmBox.alpha = 1f
                        binding.moneyConfirmBox.setOnClickListener {
                            binding.buttonCoinImg.visibility = View.GONE
                            binding.buttonCoinText.visibility = View.GONE
                            binding.buttonCoinProgressBar.visibility = View.VISIBLE

                            val manwonRef = FirebaseFirestore.getInstance().collection("manwon")
                            manwonRef.get().addOnCompleteListener { task ->
                                if(task.isSuccessful) {
                                    val documentCount = task.result?.size() ?: 0

                                    if(documentCount > missionPrizeWinners) {
                                        val goMoneyFullActivity = Intent(this, MoneyFullActivity::class.java)
                                        startActivity(goMoneyFullActivity)
                                    } else {
                                        val goMoneyAchieveActivity = Intent(this, MoneyAchieveActivity::class.java)
                                        startActivity(goMoneyAchieveActivity)
                                    }
                                } else {
                                    Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                binding.moneyConfirmBox.isEnabled = false
            }
        }

    }
}

