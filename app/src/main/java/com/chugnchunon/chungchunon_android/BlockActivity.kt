package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityBlockBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate


class BlockActivity : Activity() {

    private val binding by lazy {
        ActivityBlockBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var diaryId: String = ""
    private var diaryUserId: String = ""

    private var lastUserUpdate: Boolean = false
    lateinit var metrics: DisplayMetrics

    override fun onBackPressed() {
        resumePause = true

        val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
        binding.blockBox.startAnimation(downAnimation)

        downAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val diaryType = intent.getStringExtra("diaryType")

        binding.blockBackground.setOnClickListener {
            resumePause = true

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.blockBox.startAnimation(downAnimation)
           Handler().postDelayed({
                finish()
            }, 500)
        }

        val upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.blockBox.startAnimation(upAnimation)

        diaryId = intent.getStringExtra("diaryId").toString()
        diaryUserId = intent.getStringExtra("diaryUserId").toString()

        // 내 글인 경우 수정하기, 삭제하기 보이기
        if (diaryUserId == userId) {
            binding.editLayout.visibility = View.VISIBLE
            binding.deleteLayout.visibility = View.VISIBLE
            binding.blockDiaryLayout.visibility = View.GONE
            binding.blockUserLayout.visibility = View.GONE
            binding.deleteLayout.setBackgroundResource(R.drawable.transparent_fill_box)
        } else {
            binding.editLayout.visibility = View.GONE
            binding.deleteLayout.visibility = View.GONE
            binding.blockDiaryLayout.visibility = View.VISIBLE
            binding.blockUserLayout.visibility = View.VISIBLE
        }

        binding.editLayout.setOnClickListener {
            val goEditActivity = Intent(this, EditDiaryActivity::class.java)
            goEditActivity.putExtra("editDiaryId", diaryId)
            goEditActivity.putExtra("diaryType", diaryType)
            startActivity(goEditActivity)
        }

        binding.deleteLayout.setOnClickListener {

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.blockBox.startAnimation(downAnimation)
            Handler().postDelayed({
                binding.blockBox.visibility = View.GONE
            }, 500)

            binding.deleteConfirmLayout.visibility = View.VISIBLE
        }

        binding.deleteConfirmBox.setOnClickListener {
            resumePause = false

            // 좋아요 삭제
            diaryDB.document(diaryId).collection("likes").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val diaryUserId = document.data.getValue("id")
                        diaryDB.document(diaryId).collection("likes").document("$diaryUserId")
                            .delete()
                    }
                }

            // 답글 삭제
            diaryDB.document(diaryId).collection("comments").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val commentId = document.data.getValue("commentId")
                        diaryDB.document(diaryId).collection("comments").document("$commentId")
                            .collection("reComments").get()
                            .addOnSuccessListener { reCommentsDocs ->
                                for(reCommentDoc in reCommentsDocs) {
                                    val reCommentDocId = reCommentDoc.data.getValue("reCommentId")
                                    diaryDB.document(diaryId)
                                        .collection("comments").document("$commentId")
                                        .collection("reComments").document("$reCommentDocId")
                                        .delete()
                                }
                            }
                    }
                }

            // 댓글 삭제
            diaryDB.document(diaryId).collection("comments").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val commentId = document.data.getValue("commentId")
                        diaryDB.document(diaryId).collection("comments").document("$commentId")
                            .delete()
                    }
                }

            // 글 삭제
            diaryDB.document(diaryId).delete()

            val goMyFragment = Intent(this, DiaryTwoActivity::class.java)
            goMyFragment.putExtra("diaryType", diaryType)
            goMyFragment.putExtra("from", "delete")
            startActivity(goMyFragment)

            // SharedPreference 삭제
            val currentDateTime = LocalDate.now().toString()
            val userPref = getSharedPreferences("diary_${userId}_${currentDateTime}", Context.MODE_PRIVATE)
            val userPrefEdit = userPref.edit()
            userPrefEdit.clear().apply()
        }


        binding.blockGobackArrow.setOnClickListener {
            resumePause = true

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.blockBox.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        val layoutParams = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        layoutParams.setMargins(0, 40, 0, 40)

        val blockCheckIcon = ImageView(applicationContext)
        blockCheckIcon.layoutParams = layoutParams
        blockCheckIcon.setImageResource(R.drawable.ic_block_check)

        val resultView = TextView(applicationContext)

        resultView.layoutParams = layoutParams
        resultView.setTypeface(null, Typeface.BOLD)
        resultView.textSize = 20f
        resultView.setTextColor(Color.BLACK)

        // 신고하기
//        binding.reportLayout.setOnClickListener {
//            binding.blockBox.removeAllViews()
//            binding.blockBox.addView(blockCheckIcon)
//            resultView.text = "신고가 완료되었습니다."
//            binding.blockBox.addView(resultView)
//        }


        // 일기 차단하기
        binding.blockDiaryLayout.setOnClickListener {
            resumePause = false

            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "해당 일기를 차단했습니다."
            resultView.setTextSize(dpTextSize(10f))
            binding.blockBox.addView(resultView)

            diaryDB.document("$diaryId").update("blockedBy", (FieldValue.arrayUnion("$userId")))
                .addOnSuccessListener {
                    var blockDiaryIntent = Intent(this, AllDiaryFragmentTwo::class.java)
                    blockDiaryIntent.setAction("BLOCK_DIARY_INTENT")
                    LocalBroadcastManager.getInstance(this).sendBroadcast(blockDiaryIntent)
                    finish()
                }
        }

        // 유저 차단하기
        binding.blockUserLayout.setOnClickListener {
            resumePause = false

            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "이 사람의 일기를 모두 차단했습니다."
            resultView.setTextSize(dpTextSize(10f))
            binding.blockBox.addView(resultView)

            // userDB에 차단 목록
            userDB.document("${userId}")
                .update("blockUserList", (FieldValue.arrayUnion("$diaryUserId")))
                .addOnSuccessListener {
                    // 다이어리에 차단
                    diaryDB.whereEqualTo("userId", diaryUserId).get()
                        .addOnSuccessListener { documents ->
                            documents.forEachIndexed { index, queryDocumentSnapshot ->
                                var documentId = queryDocumentSnapshot.data.getValue("diaryId")
                                diaryDB.document("$documentId")
                                    .update("blockedBy", (FieldValue.arrayUnion("$userId")))
                                    .addOnSuccessListener {
                                        if (index == documents.size() - 1) {
                                            var blockUserDiary =
                                                Intent(this, AllDiaryFragmentTwo::class.java)
                                            blockUserDiary.setAction("BLOCK_USER_INTENT")
                                            LocalBroadcastManager.getInstance(this)
                                                .sendBroadcast(blockUserDiary)
                                            finish()
                                        }
                                    }
                            }
                        }
                }


        }

    }

    private fun dpTextSize(dp: Float): Float {
        metrics = applicationContext.resources.displayMetrics
        val fpixels = metrics.density * dp
        val pixels = fpixels * 0.5f
        return pixels
    }
}
