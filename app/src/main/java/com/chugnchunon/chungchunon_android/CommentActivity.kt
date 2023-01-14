package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.*
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
    private var userType = ""

    private var diaryId: String = ""
    var commentItems: ArrayList<Comment> = ArrayList()

    @SuppressLint("SimpleDateFormat")
    private val simpledateformat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    private var editBtn: Boolean = false
    private var editCommentId: String? = ""
    private var editCommentPosition: Int? = 0
    private var originalDescription: String? = ""

    private var diaryPosition: Int? = 0


    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.commentWriteBtn.isEnabled = false

        binding.commentGobackArrow.setOnClickListener {
            finish()
        }

        diaryId = intent.getStringExtra("diaryId").toString()
        diaryPosition = intent.getIntExtra("diaryPosition", 0)

        adapter = CommentAdapter(this, commentItems)

        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                username = document.data?.getValue("name").toString()
                userType = document.data?.getValue("userType").toString()
            }

        diaryDB.document(diaryId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    var timeMillis = (document.data?.getValue("timestamp") as Timestamp)

                    var commentId = document.data?.getValue("commentId").toString()
                    var commentUserName = document.data?.getValue("username").toString()
                    var commentUserType = document.data?.getValue("userType").toString()
                    var commentTimestamp = DateFormat().convertTimeStampToDateTime(timeMillis)
                    var commentDescription =
                        document.data?.getValue("description").toString()

                    commentItems.add(
                        Comment(
                            diaryId,
                            diaryPosition!!,
                            commentId,
                            commentUserName,
                            commentUserType,
                            commentTimestamp,
                            commentDescription
                        )
                    )
                    adapter.notifyDataSetChanged()

                }
                binding.commentRecyclerView.adapter = adapter
                binding.commentRecyclerView.layoutManager = LinearLayoutManager(
                    this,
                    RecyclerView.VERTICAL,
                    false
                )
            }


        LocalBroadcastManager.getInstance(this).registerReceiver(
            editDescriptionReceiver,
            IntentFilter("EDIT_INTENT")
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
            deleteCommentReceiver,
            IntentFilter("DELETE_INTENT")
        );

        binding.commentRecyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                true
            }
            false
        }

        binding.commentWriteText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // null
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.commentWriteBtn.isEnabled = char?.length != 0
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }

        })


        binding.commentWriteBtn.setOnClickListener {
            if (!editBtn) {

                // diary +1
                var DiaryRef = diaryDB.document(diaryId)
                DiaryRef.update("numComments", FieldValue.increment(1))
                    .addOnSuccessListener {
                        // 전체 다이어리 화면
                        DiaryRef
                            .get()
                            .addOnSuccessListener { document ->
                                var createNumComments =
                                    document.data?.getValue("numComments").toString().toInt()

                                var createIntent = Intent(this, AllDiaryFragment::class.java)
                                createIntent.setAction("CREATE_ACTION")
                                createIntent.putExtra("createDiaryPosition", diaryPosition)
                                createIntent.putExtra("createNumComments", createNumComments)
                                LocalBroadcastManager.getInstance(this!!)
                                    .sendBroadcast(createIntent)
                            }
                    }


                // diary DB 내 comments 추가
                var timestamp = System.currentTimeMillis()
                var commentId = "${userId}_${timestamp}"
                var description = binding.commentWriteText.text
                var commentSet = hashMapOf(
                    "commentId" to commentId,
                    "username" to username,
                    "userType" to userType,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "description" to description.toString()
                )
                DiaryRef.collection("comments").document(commentId)
                    .set(commentSet, SetOptions.merge())

                // commentItems 추가
                commentItems.add(
                    Comment(
                        diaryId,
                        diaryPosition!!,
                        commentId,
                        username,
                        userType,
                        simpledateformat.format(timestamp),
                        description.toString()
                    )
                )
                Log.d("commentItems", "생성: ${commentItems}")

                adapter.notifyDataSetChanged()
                binding.commentRecyclerView.scrollToPosition(commentItems.size - 1);

                binding.commentWriteText.text = null


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
                        commentItems[editCommentPosition!!].commentDescription =
                            newDescription.toString()
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
            var deleteDiaryId = intent?.getStringExtra("deleteDiaryId")
            var deleteDiaryPosition = intent?.getIntExtra("deleteDiaryPosition", 0)
            var deleteCommentPosition = intent?.getIntExtra("deleteCommentPosition", 0)

            if (commentItems.size != 0) {
                commentItems.removeAt(deleteCommentPosition!!.toInt())
                adapter.notifyDataSetChanged()
            }

            diaryDB.document(deleteDiaryId!!).get()
                .addOnSuccessListener { document ->
                    var newNumComments = document.data?.getValue("numComments").toString().toInt()

                    // 전체 일기 화면
                    var intent = Intent(context, AllDiaryFragment::class.java)
                    intent.setAction("DELETE_ACTION")
                    intent.putExtra("deleteDiaryPosition", deleteDiaryPosition)
                    intent.putExtra("deleteNumComments", newNumComments)
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
                }
        }
    }

}

