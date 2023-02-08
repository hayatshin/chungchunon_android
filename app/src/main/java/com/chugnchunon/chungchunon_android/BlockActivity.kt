package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.blockBackground.setOnClickListener {
            finish()
        }

        var animation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.blockBox.startAnimation(animation)

        diaryId = intent.getStringExtra("diaryId").toString()
        diaryUserId = intent.getStringExtra("diaryUserId").toString()

        binding.blockGobackArrow.setOnClickListener {
            finish()
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

        // 신고하기
        binding.reportLayout.setOnClickListener {
            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "신고가 완료되었습니다."
            binding.blockBox.addView(resultView)
        }


        // 일기 차단하기
        binding.blockDiaryLayout.setOnClickListener {
            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "일기를 차단했습니다."
            binding.blockBox.addView(resultView)



            diaryDB.document("$diaryId").update("blockedBy", (FieldValue.arrayUnion("$userId")))
                .addOnSuccessListener {
                    var blockDiaryIntent = Intent(this, AllDiaryFragment::class.java)
                    blockDiaryIntent.setAction("BLOCK_DIARY_INTENT")
                    LocalBroadcastManager.getInstance(this).sendBroadcast(blockDiaryIntent)
                    finish()
                }
        }

        // 유저 차단하기
        binding.blockUserLayout.setOnClickListener {
            binding.blockBox.removeAllViews()
            binding.blockBox.addView(blockCheckIcon)
            resultView.text = "유저를 차단했습니다."
            binding.blockBox.addView(resultView)


            diaryDB.whereEqualTo("userId", diaryUserId).get()
                .addOnSuccessListener { documents ->
                    documents.forEachIndexed { index, queryDocumentSnapshot ->
                        var documentId = queryDocumentSnapshot.data.getValue("diaryId")
                        diaryDB.document("$documentId")
                            .update("blockedBy", (FieldValue.arrayUnion("$userId")))
                            .addOnSuccessListener {
                                if(index == documents.size()-1){
                                    var blockUserDiary = Intent(this, AllDiaryFragment::class.java)
                                    blockUserDiary.setAction("BLOCK_USER_INTENT")
                                    LocalBroadcastManager.getInstance(this).sendBroadcast(blockUserDiary)
                                    finish()
                                }
                            }
                    }

                }
        }

    }
}