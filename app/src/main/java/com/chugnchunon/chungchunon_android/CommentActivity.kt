package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.CommentAdapter
import com.chugnchunon.chungchunon_android.Adapter.CommentReAdapter
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.ReComment
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.LinearLayoutManagerWrapper
import com.chugnchunon.chungchunon_android.databinding.ActivityCommentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_comment.view.*
import kotlinx.android.synthetic.main.comment_list.view.*

class CommentActivity : FragmentActivity() {

    private val binding by lazy {
        ActivityCommentBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: CommentAdapter

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var commentUserId = ""
    private var username = ""
    private var userType = ""
    private var userAvatar = ""

    private var diaryId: String = ""

    @SuppressLint("SimpleDateFormat")
    private val simpledateformat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    enum class BtnStatusEnum {
        WRITE_COMMENT,
        EDIT_COMMENT,
        RE_WRITE_COMMENT,
        RE_EDIT_COMMENT
    }

    private var BtnStatus: BtnStatusEnum = BtnStatusEnum.WRITE_COMMENT

    private var editCommentId: String? = ""
    private var editCommentPosition: Int? = 0
    private var originalDescription: String? = ""

    private var rewriteCommentId: String = ""

    private var reEditCommentId: String? = ""
    private var reEditCommentCommentId: String? = ""
    private var reOriginalDescription: String? = ""

    private var diaryPosition: Int? = 0
    lateinit var commentDataLoading: CommentDataLoading

    private val anonymousUserAvatar =
        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png"

    companion object {
        var commentItems: ArrayList<Comment> = ArrayList()
    }

    private var notificationDiaryId: String = ""
    private var notificationCommentId: String = ""

    override fun onBackPressed() {
        resumePause = true

        val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
        binding.commentLayout.startAnimation(downAnimation)

        downAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }


    override fun onResume() {
        super.onResume()
        commentItems.clear()
    }

    override fun finish() {
        super.finish()
        resumePause = false
    }


    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 가장 아래 글로 스크롤
        binding.commentRecyclerView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                p0: View?,
                p1: Int,
                p2: Int,
                p3: Int,
                p4: Int,
                p5: Int,
                p6: Int,
                p7: Int,
                p8: Int
            ) {
                binding.commentRecyclerView.scrollToPosition(commentItems.size - 1)
            }
        })

        // 코멘트창 애니메이션
        val upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.commentLayout.startAnimation(upAnimation)

        // 데이터 로딩
        commentDataLoading = ViewModelProvider(this).get(CommentDataLoading::class.java)
        commentDataLoading.loadingCompleteData.observe(this, Observer { value ->

            if (!commentDataLoading.loadingCompleteData.value!!) {
                binding.commentRecyclerView.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.commentRecyclerView.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        // 배경 뒤로가기
        binding.commentBackground.setOnClickListener {
            resumePause = true

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.commentLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 500)
        }

        // 화살표 뒤로가기
        binding.commentGobackArrow.setOnClickListener {
            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.commentLayout.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        // 리사이클러뷰 터치 키보드 다운
        binding.commentRecyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.commentWriteText.clearFocus()

                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)

                true
            }
            false
        }

        // 작성버튼 초기 셋업
        binding.commentWriteBtn.isEnabled = false

        // 작성시 버튼 초기 셋업
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

        // 코멘트 푸시 노티피케이션
        if (intent?.hasExtra("notificationDiaryId") == true) {
            diaryId = intent.getStringExtra("notificationDiaryId").toString()
        } else if (intent?.hasExtra("diaryId") == true) {
            diaryId = intent.getStringExtra("diaryId").toString()
        }

        // 다이어리 포지션
        diaryPosition = intent.getIntExtra("diaryPosition", 0)

        // 어댑터
        adapter = CommentAdapter(this, commentItems)
        binding.commentRecyclerView.adapter = adapter

        // 초기 유저 정보 셋업
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                commentUserId = document.data?.getValue("userId").toString()
                username = document.data?.getValue("name").toString()
                userType = document.data?.getValue("userType").toString()
                userAvatar = document.data?.getValue("avatar").toString()
            }

        // 로컬 브로드캐스트
        LocalBroadcastManager.getInstance(this).registerReceiver(
            editDescriptionReceiver,
            IntentFilter("EDIT_INTENT")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            deleteCommentReceiver,
            IntentFilter("DELETE_INTENT")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            reCommentWriteReceiver,
            IntentFilter("RE_COMMENT_WRITE_INTENT")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            reCommentEditReceiver,
            IntentFilter("RE_COMMENT_EDIT_INTENT")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            reCommentDeleteReceiver,
            IntentFilter("RE_COMMENT_DELETE_INTENT")
        )

        // 댓글 작성
        binding.commentWriteBtn.setOnClickListener {
            binding.noItemText.visibility = View.GONE
            val DiaryRef = diaryDB.document(diaryId)

            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

            if (BtnStatus == BtnStatusEnum.WRITE_COMMENT) {
                // diary +1
                DiaryRef.update("numComments", FieldValue.increment(1))
                    .addOnSuccessListener {
                        // 전체 다이어리 화면
                        DiaryRef
                            .get()
                            .addOnSuccessListener { document ->
                                val createNumComments =
                                    document.data?.getValue("numComments").toString().toInt()

                                val createIntent = Intent(this, AllDiaryFragmentTwo::class.java)
                                createIntent.setAction("COMMENT_ACTION")
                                createIntent.putExtra("newDiaryId", diaryId)
                                createIntent.putExtra("newNumComments", createNumComments)
                                LocalBroadcastManager.getInstance(this!!)
                                    .sendBroadcast(createIntent)
                            }
                    }


                // diary DB 내 comments 추가
                val timestamp = System.currentTimeMillis()
                val commentId = "${userId}_${timestamp}"
                val description = binding.commentWriteText.text
                val commentSet = hashMapOf(
                    "commentId" to commentId,
                    "userId" to commentUserId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "description" to description.toString(),
                    "diaryId" to diaryId,
                )
                DiaryRef.collection("comments").document(commentId)
                    .set(commentSet, SetOptions.merge())

                // commentItems 추가
                commentItems.add(
                    Comment(
                        diaryId,
                        diaryPosition!!,
                        commentId,
                        commentUserId,
                        userAvatar,
                        username,
                        userType,
                        simpledateformat.format(timestamp),
                        description.toString()
                    )
                )
                adapter.notifyDataSetChanged()

                binding.commentRecyclerView.layoutManager =
                    LinearLayoutManagerWrapper(
                        this,
                        RecyclerView.VERTICAL,
                        false
                    )

                binding.commentRecyclerView.scrollToPosition(commentItems.size - 1);
                binding.commentWriteText.text.clear()

            } else if (BtnStatus == BtnStatusEnum.EDIT_COMMENT) {
                // 댓글 수정 버튼
                val newDescription = binding.commentWriteText.text

                val newDescriptionSet = hashMapOf(
                    "description" to newDescription.toString()
                )

                diaryDB.document(diaryId).collection("comments").document("$editCommentId")
                    .set(newDescriptionSet, SetOptions.merge())
                    .addOnSuccessListener {
                        binding.commentWriteText.text.clear()
                        commentItems[editCommentPosition!!].commentDescription =
                            newDescription.toString()
                        adapter.notifyDataSetChanged()
                        BtnStatus = BtnStatusEnum.WRITE_COMMENT
                        binding.commentWriteBtn.text = "댓글 작성"
                    }
            } else if (BtnStatus == BtnStatusEnum.RE_WRITE_COMMENT) {
                // diary DB 내 comments 추가
                val reTimestamp = System.currentTimeMillis()
                val reCommentId = "${userId}_${reTimestamp}"
                val reDescription = binding.commentWriteText.text.toString()
                val reCommentSet = hashMapOf(
                    "commentId" to rewriteCommentId,
                    "reCommentId" to reCommentId,
                    "userId" to commentUserId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "description" to reDescription,
                )

                DiaryRef.collection("comments").document(rewriteCommentId)
                    .collection("reComments")
                    .document(reCommentId)
                    .set(reCommentSet, SetOptions.merge())
                    .addOnSuccessListener {
                        binding.commentWriteText.text.clear()
                        BtnStatus = BtnStatusEnum.WRITE_COMMENT
                        binding.commentWriteBtn.text = "댓글 작성"
                        adapter.notifyDataSetChanged()
                    }

            } else if (BtnStatus == BtnStatusEnum.RE_EDIT_COMMENT) {
                // 답글 수정
                val newDescription = binding.commentWriteText.text
                val newDescriptionSet = hashMapOf(
                    "description" to newDescription.toString()
                )

                diaryDB.document(diaryId).collection("comments").document("$reEditCommentId")
                    .collection("reComments").document("$reEditCommentCommentId")
                    .set(newDescriptionSet, SetOptions.merge())
                    .addOnSuccessListener {
                        binding.commentWriteText.text.clear()
                        BtnStatus = BtnStatusEnum.WRITE_COMMENT
                        binding.commentWriteBtn.text = "댓글 작성"
                        adapter.notifyDataSetChanged()
                    }
            }
        }

        getData()
        scrollToPosition()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            editDescriptionReceiver
        )
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            deleteCommentReceiver
        )
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            reCommentWriteReceiver
        )
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            reCommentEditReceiver
        )
    }

    private fun scrollToPosition() {
        if (intent.hasExtra("notificationCommentId")) {
            if (commentItems.size != 0) {
                commentItems.forEachIndexed { index, commentItem ->
                    if (commentItem.commentId == notificationCommentId) {
                        binding.commentRecyclerView.scrollToPosition(index)
                    }
                }
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getData(): ArrayList<Comment> {
        diaryDB.document(diaryId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.size() != 0) {
                    for (document in documents) {
                        val timeMillis = (document.data?.getValue("timestamp") as Timestamp)
                        val commentId = document.data?.getValue("commentId").toString()
                        val commentUserId = document.data?.getValue("userId").toString()
                        val commentTimestamp = DateFormat().convertTimeStampToDateTime(timeMillis)
                        val commentDescription =
                            document.data?.getValue("description").toString()

                        userDB.document(commentUserId)
                            .get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument != null) {
                                    // 유저가 있는 경우
                                    val commentUserName =
                                        userDocument.data?.getValue("name").toString()
                                    val commentUserAvatar =
                                        userDocument.data?.getValue("avatar").toString()
                                    val commentUserType =
                                        userDocument.data?.getValue("userType").toString()

                                    commentItems.add(
                                        Comment(
                                            diaryId,
                                            diaryPosition!!,
                                            commentId,
                                            commentUserId,
                                            commentUserAvatar,
                                            commentUserName,
                                            commentUserType,
                                            commentTimestamp,
                                            commentDescription
                                        )
                                    )

                                    commentItems.sortWith(compareBy({ it.commentTimestamp }))

                                    binding.commentRecyclerView.layoutManager = LinearLayoutManager(
                                        this,
                                        RecyclerView.VERTICAL,
                                        false
                                    )

                                    commentDataLoading.loadingCompleteData.value = true
                                    binding.noItemText.visibility = View.GONE

                                    adapter.notifyDataSetChanged()
                                } else {
                                    // 유저가 없는 경우
                                    val commentUserName = "탈퇴자"
                                    val commentUserAvatar = anonymousUserAvatar
                                    val commentUserType = "마스터"

                                    commentItems.add(
                                        Comment(
                                            diaryId,
                                            diaryPosition!!,
                                            commentId,
                                            commentUserId,
                                            commentUserAvatar,
                                            commentUserName,
                                            commentUserType,
                                            commentTimestamp,
                                            commentDescription
                                        )
                                    )

                                    commentItems.sortWith(compareBy({ it.commentTimestamp }))

                                    binding.commentRecyclerView.layoutManager = LinearLayoutManager(
                                        this,
                                        RecyclerView.VERTICAL,
                                        false
                                    )

                                    commentDataLoading.loadingCompleteData.value = true
                                    binding.noItemText.visibility = View.GONE

                                    adapter.notifyDataSetChanged()
                                }
                            }

                        // 답글
//
                    }
                } else {
                    // 0인 경우
                    commentDataLoading.loadingCompleteData.value = true
                    binding.noItemText.visibility = View.VISIBLE
                }
            }
        return commentItems

    }

    var editDescriptionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            BtnStatus = BtnStatusEnum.EDIT_COMMENT
            editCommentId = intent?.getStringExtra("editCommentId").toString()
            originalDescription = intent?.getStringExtra("originalDescription").toString()
            editCommentPosition = intent?.getIntExtra("editCommentPosition", 0)

            binding.commentWriteText.setText(originalDescription)
            binding.commentWriteText.setSelection(binding.commentWriteText.length())

            binding.commentWriteBtn.text = "댓글 수정"

            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    var deleteCommentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val deleteDiaryId = intent?.getStringExtra("deleteDiaryId")
            var deleteDiaryPosition = intent?.getIntExtra("deleteDiaryPosition", 0)
            val deleteCommentPosition = intent?.getIntExtra("deleteCommentPosition", 0)

            if (commentItems.size != 0) {
                commentItems.removeAt(deleteCommentPosition!!.toInt())
                adapter.notifyDataSetChanged()

                if (commentItems.size == 0) {
                    binding.noItemText.visibility = View.VISIBLE
                }
            }

            diaryDB.document(deleteDiaryId!!).get()
                .addOnSuccessListener { document ->
                    val newNumComments = document.data?.getValue("numComments").toString().toInt()

                    // 전체 일기 화면
                    val intent = Intent(context, AllDiaryFragmentTwo::class.java)
                    intent.setAction("COMMENT_ACTION")
                    intent.putExtra("newDiaryId", diaryId)
                    intent.putExtra("newNumComments", newNumComments)
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
                }
        }
    }

    var reCommentWriteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            BtnStatus = BtnStatusEnum.RE_WRITE_COMMENT
            binding.commentWriteText.requestFocus()
            rewriteCommentId = intent?.getStringExtra("rewriteCommentId").toString()
            binding.commentWriteBtn.text = "답글 작성"

            val inputMethodManager =
                context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    var reCommentEditReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            BtnStatus = BtnStatusEnum.RE_EDIT_COMMENT
            binding.commentWriteText.requestFocus()

            reEditCommentId = intent?.getStringExtra("commentId").toString()
            reEditCommentCommentId = intent?.getStringExtra("reCommentId").toString()
            reOriginalDescription = intent?.getStringExtra("reOriginalDescription").toString()

            binding.commentWriteText.setText(reOriginalDescription)
            binding.commentWriteText.setSelection(binding.commentWriteText.length())
            binding.commentWriteBtn.text = "답글 수정"

            val inputMethodManager =
                context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    var reCommentDeleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            adapter.notifyDataSetChanged()
            binding.commentWriteText.text.clear()
            BtnStatus = BtnStatusEnum.WRITE_COMMENT
            binding.commentWriteBtn.text = "댓글 작성"
        }
    }
}


class CommentDataLoading : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
