package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityBlockBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


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

    override fun onBackPressed() {
        super.onBackPressed()
        resumePause = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.blockBackground.setOnClickListener {
            resumePause = true

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.blockBox.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.blockBox.startAnimation(upAnimation)

        diaryId = intent.getStringExtra("diaryId").toString()
        diaryUserId = intent.getStringExtra("diaryUserId").toString()

        // ??? ?????? ?????? ????????????, ???????????? ?????????
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
            var goEditActivity = Intent(this, EditDiaryActivity::class.java)
            goEditActivity.putExtra("editDiaryId", diaryId)
            startActivity(goEditActivity)
        }

        binding.deleteLayout.setOnClickListener {

            // ????????? ??????
            diaryDB.document("$diaryId").collection("likes").get().addOnSuccessListener { documents ->
                for (document in documents) {
                    var diaryUserId = document.data.getValue("id")
                    diaryDB.document("$diaryId").collection("likes").document("$diaryUserId").delete()
                }
            }

            // ?????? ??????
            diaryDB.document("$diaryId").collection("comments").get().addOnSuccessListener { documents ->
                for (document in documents) {
                    var commentId = document.data.getValue("commentId")
                    diaryDB.document("$diaryId").collection("comments").document("$commentId").delete()
                }
            }

            // ??? ??????
            diaryDB.document("$diaryId").delete()


            var goMyFragment = Intent(this, DiaryTwoActivity::class.java)
            goMyFragment.putExtra("from", "delete")
            startActivity(goMyFragment)
        }


        binding.blockGobackArrow.setOnClickListener {
            resumePause = true

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
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

        var blockCheckIcon = ImageView(applicationContext)
        blockCheckIcon.layoutParams = layoutParams
        blockCheckIcon.setImageResource(R.drawable.ic_block_check)

        var resultView = TextView(applicationContext)

        resultView.layoutParams = layoutParams
        resultView.setTypeface(null, Typeface.BOLD)
        resultView.textSize = 20f
        resultView.setTextColor(Color.BLACK)

        // ????????????
//        binding.reportLayout.setOnClickListener {
//            binding.blockBox.removeAllViews()
//            binding.blockBox.addView(blockCheckIcon)
//            resultView.text = "????????? ?????????????????????."
//            binding.blockBox.addView(resultView)
//        }


        // ?????? ????????????
        binding.blockDiaryLayout.setOnClickListener {
            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "????????? ??????????????????."
            binding.blockBox.addView(resultView)



            diaryDB.document("$diaryId").update("blockedBy", (FieldValue.arrayUnion("$userId")))
                .addOnSuccessListener {
                    var blockDiaryIntent = Intent(this, AllDiaryFragment::class.java)
                    blockDiaryIntent.setAction("BLOCK_DIARY_INTENT")
                    LocalBroadcastManager.getInstance(this).sendBroadcast(blockDiaryIntent)
                    finish()
                }
        }

        // ?????? ????????????
        binding.blockUserLayout.setOnClickListener {
            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "????????? ??????????????????."
            binding.blockBox.addView(resultView)


            diaryDB.whereEqualTo("userId", diaryUserId).get()
                .addOnSuccessListener { documents ->
                    documents.forEachIndexed { index, queryDocumentSnapshot ->
                        var documentId = queryDocumentSnapshot.data.getValue("diaryId")
                        diaryDB.document("$documentId")
                            .update("blockedBy", (FieldValue.arrayUnion("$userId")))
                            .addOnSuccessListener {
                                if (index == documents.size() - 1) {
                                    var blockUserDiary = Intent(this, AllDiaryFragment::class.java)
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