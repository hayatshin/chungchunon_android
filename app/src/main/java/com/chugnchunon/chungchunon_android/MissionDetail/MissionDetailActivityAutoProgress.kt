package com.chugnchunon.chungchunon_android.MissionDetail

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DefaultDiaryWarningActivity
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.MissionResultActivity
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.ActivityMissionDetailBinding
import com.google.firebase.Timestamp
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

class MissionDetailActivityAutoProgress : Activity() {

    private val binding by lazy {
        ActivityMissionDetailBinding.inflate(layoutInflater)
    }
    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var userPoint: Int = 0

    private var mdDocId = ""
    private var mdTitle = ""
    private var mdDescription = ""
    private var mdImage = ""
    private var mdCommunity = ""
    private var mdStartPeriod = ""
    private var mdEndPeriod = ""
    private var mdPeriod = ""

    lateinit var mdParticipateDate: Date

    private var formatStartDate = ""
    private var formatEndDate = ""

    lateinit var mdDateFormatStartPeriod: Date
    lateinit var mdDateFormatEndPeriod: Date

    private val uiScope = CoroutineScope(Dispatchers.Main)

    var timer: Timer? = null
    var deltaTime = 0

    private var participateState: Boolean = false

    companion object {
        private var partnerOrNotForMission: Boolean = false
        const val REFRESH_RESULT_MISSION_CODE = 100
    }


    override fun onBackPressed() {

        val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
        binding.mdLayout.startAnimation(downAnimation)

        downAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    override fun finish() {
        super.finish()
        userPoint = 0
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.mdParticipationBtn.text = "자동으로 참여 중"
        binding.mdParticipationBtn.isEnabled = false

        // 데이터 받기
        mdDocId = intent.getStringExtra("mdDocID").toString()
        mdTitle = intent.getStringExtra("mdTitle").toString()
        mdDescription = intent.getStringExtra("mdDescription").toString()
        mdCommunity = intent.getStringExtra("mdCommunity").toString()
        mdStartPeriod = intent.getStringExtra("mdStartPeriod").toString()
        mdEndPeriod = intent.getStringExtra("mdEndPeriod").toString()
        mdPeriod = "${mdStartPeriod} ~ ${mdEndPeriod}"

        binding.mdTitle.text = mdTitle
        binding.mdDescription.text = mdDescription.replace("\\n", "\n")
        binding.mdCommunity.text = mdCommunity
        binding.mdPeriod.text = mdPeriod

        val upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.mdLayout.startAnimation(upAnimation)

        binding.mdGobackArrow.setOnClickListener {
            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.mdLayout.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        binding.mdBackground.setOnClickListener {
            AllDiaryFragmentTwo.resumePause = true

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.mdLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 500)
        }

        binding.mdPointProgress.visibility = View.GONE
    }

    private fun drawProgress() {
        // 점수 계산
        uiScope.launch(Dispatchers.IO)
        {
            launch { eventParticiapteDate() }.join()
            launch { diaryToArrayFun() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    if (userPoint <= 10000) {
                        val decimal = DecimalFormat("#,###")
                        binding.mdPointText.text = "${decimal.format(userPoint)}원 / 만원"
                        val targetProgress = (userPoint.toFloat() / 10000f * 100f).toInt()
                        val animator = ObjectAnimator.ofInt(
                            binding.mdPointProgress,
                            "progress",
                            0,
                            targetProgress
                        )
                        animator.duration = 2000
                        animator.start()

                        binding.mdPointProgress.progress = userPoint / 10000 * 100
                    } else {
                        binding.mdPointText.text = "만원 달성!"
                        binding.mdPointProgress.progress = 100

                        val animator =
                            ObjectAnimator.ofInt(
                                binding.mdPointProgress,
                                "progress",
                                0,
                                100
                            )
                        animator.duration = 2000
                        animator.start()
                    }
                }
            }
        }
    }

    suspend fun eventParticiapteDate() {
        val eventDocument = db.collection("mission")
            .document(mdDocId)
            .collection("participants")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for(document in eventDocument) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val getTimestamp = document.data.getValue("timestamp") as Timestamp
            val getString = DateFormat().convertTimeStampToDate(getTimestamp)
            mdParticipateDate = dateFormat.parse(getString)
        }
    }

    suspend fun diaryToArrayFun() {
        val diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", mdParticipateDate)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            userPoint * 100
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REFRESH_RESULT_MISSION_CODE) {
            db.collection("mission")
                .document(mdDocId)
                .collection("participants")
                .document("$userId")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userCheck = task.result
                        if (userCheck != null) {
                            if (userCheck.exists()) {
                                // 이미 참여
                                participateState = true
                                binding.mdParticipationBtn.alpha = 0.4f
                                binding.mProgressTextBox.visibility = View.VISIBLE

                                drawProgress()
                            } else {
                                participateState = false
                                binding.mdParticipationBtn.alpha = 1f
                                binding.mProgressTextBox.visibility = View.GONE
                            }
                        } else {
                            participateState = false
                            binding.mdParticipationBtn.alpha = 1f
                            binding.mProgressTextBox.visibility = View.GONE
                        }
                    }
                }
        }
    }
}

