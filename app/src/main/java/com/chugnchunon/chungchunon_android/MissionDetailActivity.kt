package com.chugnchunon.chungchunon_android

import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.chugnchunon.chungchunon_android.DataClass.RankingLine
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.databinding.ActivityCommentBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityMissionDetailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import me.moallemi.tools.daterange.localdate.rangeTo
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.timer

class MissionDetailActivity : Activity() {

    private val binding by lazy {
        ActivityMissionDetailBinding.inflate(layoutInflater)
    }
    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var userPoint: Int = 0

    private var mdTitle = ""
    private var mdDescription = ""
    private var mdImage = ""
    private var mdCommunity = ""
    private var mdStartPeriod = ""
    private var mdEndPeriod = ""
    private var mdPeriod = ""

    private var formatStartDate = ""
    private var formatEndDate = ""

    lateinit var mdDateFormatStartPeriod: Date
    lateinit var mdDateFormatEndPeriod: Date

    private val uiScope = CoroutineScope(Dispatchers.Main)

    var timer : Timer? = null
    var deltaTime = 0

    override fun onBackPressed() {

        var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
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

        var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.mdLayout.startAnimation(upAnimation)

        binding.mdGobackArrow.setOnClickListener {
            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.mdLayout.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        binding.mdBackground.setOnClickListener {
            AllDiaryFragmentTwo.resumePause = true

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.mdLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 500)
        }


        // 데이터 받기
        mdTitle = intent.getStringExtra("mdTitle").toString()
        mdDescription = intent.getStringExtra("mdDescription").toString()
        mdCommunity = intent.getStringExtra("mdCommunity").toString()
        mdStartPeriod = intent.getStringExtra("mdStartPeriod").toString()
        mdEndPeriod = intent.getStringExtra("mdEndPeriod").toString()
        mdPeriod = "${mdStartPeriod} ~ ${mdEndPeriod}"

        binding.mdTitle.text = mdTitle
        binding.mdDescription.text = mdDescription
        binding.mdCommunity.text = mdCommunity
        binding.mdPeriod.text = mdPeriod

        val pattern = "yyyy-MM-dd"
        formatStartDate = mdStartPeriod.replace(".", "-")
        formatEndDate = mdEndPeriod.replace(".", "-")

        mdDateFormatStartPeriod = SimpleDateFormat(pattern).parse(formatStartDate)
        mdDateFormatEndPeriod = SimpleDateFormat(pattern).parse(formatEndDate)

        // 점수 계산
        uiScope.launch(Dispatchers.IO) {
            listOf(
                launch { stepCountToArrayFun() },
                launch { diaryToArrayFun() },
                launch { commentToArrayFun() },
                launch { likeToArrayFun() }
            ).joinAll()
            withContext(Dispatchers.Main) {
                launch {

                    if(userPoint <= 1000) {
                        binding.mdPointText.text = "${userPoint} / 1000"
                        var targetProgress = (userPoint.toFloat() / 1000f * 100f).toInt()
                        val animator = ObjectAnimator.ofInt(binding.mdPointProgress, "progress", 0, targetProgress)
                        animator.duration = 2000
                        animator.start()

                        binding.mdPointProgress.progress = userPoint / 1000 * 100
                    } else {
                        binding.mdPointText.text = "${userPoint} / 1000"
                        binding.mdPointProgress.progress = 100

                        val animator = ObjectAnimator.ofInt(binding.mdPointProgress, "progress", 0, 100)
                        animator.duration = 2000
                        animator.start()
                    }
                }
            }
        }
    }

    suspend fun stepCountToArrayFun() {
        var userStepCount: Int = 0

        val startDate = LocalDate.of(
            formatStartDate.substring(0, 4).toInt(),
            formatStartDate.substring(5, 7).toInt(),
            formatStartDate.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatEndDate.substring(0, 4).toInt(),
            formatEndDate.substring(5, 7).toInt(),
            formatEndDate.substring(8, 10).toInt()
        )


        var dataSteps = db.collection("user_step_count")
            .document("$userId")
            .get()
            .await()

        dataSteps.data?.forEach { (period, dateStepCount) ->
            for (stepDate in startDate..endDate) {
                if (period == stepDate.toString()) {
                    userStepCount += (dateStepCount as Long).toInt()
                }
            }
        }

        userPoint += ((Math.floor(userStepCount / 1000.0)) * 10).toInt()
    }


    suspend fun diaryToArrayFun() {
        var diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", mdDateFormatStartPeriod)
            .whereLessThanOrEqualTo("timestamp", mdDateFormatEndPeriod)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            userPoint += 100
        }
    }

    suspend fun commentToArrayFun() {
        var commentDocuments = db.collectionGroup("comments")
            .whereGreaterThanOrEqualTo("timestamp", mdDateFormatStartPeriod)
            .whereLessThanOrEqualTo("timestamp", mdDateFormatEndPeriod)
            .get()
            .await()

        for (commentDocument in commentDocuments) {
            userPoint += 20
        }
    }

    suspend fun likeToArrayFun() {
        var likeDocuments = db.collectionGroup("likes")
            .whereGreaterThanOrEqualTo("timestamp", mdDateFormatStartPeriod)
            .whereLessThanOrEqualTo("timestamp", mdDateFormatEndPeriod)
            .get()
            .await()

        for (likeDocument in likeDocuments) {
                userPoint += 10
        }
    }
}

