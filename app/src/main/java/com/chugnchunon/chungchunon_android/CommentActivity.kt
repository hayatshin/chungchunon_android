package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.CommentAdapter
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityCommentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.*
import java.time.LocalDateTime

class CommentActivity : Activity() {

    private val binding by lazy {
        ActivityCommentBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: CommentAdapter

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var username = ""
    private var diaryId: String = ""
    private var diaryPosition: Int? = 0
    private var items: ArrayList<Comment> = ArrayList()
    @SuppressLint("SimpleDateFormat")
    private val simpledateformat = SimpleDateFormat("yyyy-MM-dd HH:mm")
//    private var numComments: Int = 0

    private var editBtn: Boolean = false
    private var editCommentId: String? = ""
    private var editCommentPosition: Int? = 0
    private var originalDescription: String? = ""

    private var deleteCommentId: String? = ""
    private var deleteCommentPosition: Int? = 0

    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Log.d("태건폰", "$userId")

        binding.commentGobackArrow.setOnClickListener {
            finish()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            editDescriptionReceiver,
            IntentFilter("EDIT_INTENT")
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
            deleteCommentReceiver,
            IntentFilter("DELETE_INTENT")
        );

        diaryId = intent.getStringExtra("diaryId").toString()
        Log.d("태건폰23", "$diaryId")
        diaryPosition = intent.getIntExtra("diaryPosition", 0)

        adapter = CommentAdapter(this, items)

        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                username = document.data?.getValue("name").toString()
            }

        diaryDB.document(diaryId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    var timeMillis = (document.data?.getValue("timestamp") as Timestamp)

                    var commentId = document.data?.getValue("commentId").toString()
                    var commentUserName = document.data?.getValue("username").toString()
                    var commentTimestamp = DateFormat().converDate(timeMillis)
                    var commentDescription =
                        document.data?.getValue("description").toString()

                    items.add(Comment(commentId, commentUserName, commentTimestamp, commentDescription))

                    adapter.notifyDataSetChanged()
                }
                binding.commentRecyclerView.adapter = adapter
                binding.commentRecyclerView.layoutManager = LinearLayoutManager(
                    this,
                    RecyclerView.VERTICAL,
                    false
                )
            }

        binding.commentRecyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                true
            }
            false
        }


        binding.commentWriteBtn.setOnClickListener {
            if (!editBtn) {

                // 댓글 작성 버튼
                diaryDB.document("$diaryId")
                    .get()
                    .addOnSuccessListener { document ->

                        // diary 디비 numComments 1 추가
                        var prevNumComments = document.data?.getValue("numComments").toString().toInt()

                        var commentNumSet = hashMapOf(
                            "numComments" to prevNumComments + 1
                        )
                        diaryDB.document("$diaryId")
                            .set(commentNumSet, SetOptions.merge())

                        // diary 내 서브클래스 comments 추가
                        var timestamp = System.currentTimeMillis()
                        var commentId = "${userId}_${timestamp}"
                        var description = binding.commentWriteText.text
                        var commentSet = hashMapOf(
                            "commentId" to commentId,
                            "username" to username,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "description" to description.toString()
                        )

                        diaryDB.document("$diaryId").collection("comments").document("${userId}_${timestamp}")
                            .set(commentSet, SetOptions.merge())
                            .addOnSuccessListener {
                                binding.commentWriteText.text = null
                                items.add(
                                    Comment(
                                        commentId,
                                        username,
                                        simpledateformat.format(timestamp),
                                        description.toString()
                                    )
                                )
                                adapter.notifyDataSetChanged()
                                binding.commentRecyclerView.scrollToPosition(items.size - 1);
                            }

                        // 전체 다이어리 화면

                        var intent = Intent(this, AllDiaryFragment::class.java)
                        intent.setAction("CREATE_ACTION")
                        intent.putExtra("diaryPosition", diaryPosition)
                        intent.putExtra("newNumComments", (prevNumComments+1))
                        LocalBroadcastManager.getInstance(this!!).sendBroadcast(intent)
                    }
            } else {
                // 댓글 수정 버튼
                var newDescription = binding.commentWriteText.text

                var newDescriptionSet = hashMapOf(
                    "description" to newDescription.toString()
                )
                diaryDB.document(diaryId).collection("comments").document("$editCommentId")
                    .set(newDescriptionSet, SetOptions.merge())
                    .addOnSuccessListener {
                        binding.commentWriteText.text = null
                        items[editCommentPosition!!].commentDescription = newDescription.toString()
                        adapter.notifyDataSetChanged()
                        editBtn = false
                        binding.commentWriteBtn.text = "댓글 작성"
                    }
            }
        }
    }

    var editDescriptionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            editBtn = true
            editCommentId = intent?.getStringExtra("editCommentId").toString()
            originalDescription = intent?.getStringExtra("originalDescription").toString()
            editCommentPosition = intent?.getIntExtra("editCommentPosition", 0)

            binding.commentWriteText.setText(originalDescription)
            binding.commentWriteBtn.text = "댓글 수정"
        }
    }


    var deleteCommentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            deleteCommentId = intent?.getStringExtra("deleteCommentId").toString()
            deleteCommentPosition = intent?.getIntExtra("deleteCommentPosition", 0)

            diaryDB.document("$diaryId").get()
                .addOnSuccessListener { document ->
                    var prevNumComments = document.data?.getValue("numComments").toString().toInt()

                    // numComments 1 감소
                    var newNumCommentSet = hashMapOf(
                        "numComments" to (prevNumComments-1)
                    )

                    diaryDB.document("$diaryId").set(newNumCommentSet, SetOptions.merge())

                    // 전체 일기 화면
                    var intent = Intent(context, AllDiaryFragment::class.java)
                    intent.setAction("DELETE_ACTION")
                    intent.putExtra("diaryPosition", diaryPosition)
                    intent.putExtra("newNumComments", (prevNumComments-1))
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
                }

            // comments 삭제
            diaryDB.document("$diaryId").collection("comments").document("$deleteCommentId").delete()

            // 코멘트 화면
            items.removeAt(deleteCommentPosition!!.toInt())
            adapter.notifyDataSetChanged()


        }
    }
}
