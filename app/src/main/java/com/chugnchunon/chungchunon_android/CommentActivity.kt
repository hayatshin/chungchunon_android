package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.app.Activity
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
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.CommentAdapter
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.AllRegionDataLoadingState
import com.chugnchunon.chungchunon_android.databinding.ActivityCommentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.*
import kotlinx.android.synthetic.main.fragment_region_data.view.*

class CommentActivity : FragmentActivity() {

    private val binding by lazy {
        ActivityCommentBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: CommentAdapter

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

    private var editBtn: Boolean = false
    private var editCommentId: String? = ""
    private var editCommentPosition: Int? = 0
    private var originalDescription: String? = ""

    private var diaryPosition: Int? = 0
    lateinit var commentDataLoading: CommentDataLoading

    companion object {
        var commentItems: ArrayList<Comment> = ArrayList()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        resumePause = true
    }

    override fun onResume() {
        super.onResume()
        commentItems.clear()
    }

    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.commentLayout.startAnimation(upAnimation)

        commentDataLoading = ViewModelProvider(this).get(CommentDataLoading::class.java)

        commentDataLoading.loadingCompleteData.observe(this, Observer { value ->

            if (!commentDataLoading.loadingCompleteData.value!!) {
                Log.d("댓글", "로딩 false")
                binding.commentRecyclerView.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                Log.d("댓글", "로딩 true")

                binding.commentRecyclerView.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })


        binding.commentBackground.setOnClickListener {
            resumePause = true

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.commentLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 500)
        }

        binding.commentLayout.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.commentWriteText.clearFocus()

                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

                true
            }
            false
        }


        binding.commentWriteBtn.isEnabled = false

        binding.commentGobackArrow.setOnClickListener {
            resumePause = true

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.commentLayout.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        diaryId = intent.getStringExtra("diaryId").toString()
        diaryPosition = intent.getIntExtra("diaryPosition", 0)

        adapter = CommentAdapter(this, commentItems)
        binding.commentRecyclerView.adapter = adapter

        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                commentUserId = document.data?.getValue("userId").toString()
                username = document.data?.getValue("name").toString()
                userType = document.data?.getValue("userType").toString()
                userAvatar = document.data?.getValue("avatar").toString()
            }

        diaryDB.document(diaryId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.size() != 0) {
                    for (document in documents) {
                        var timeMillis = (document.data?.getValue("timestamp") as Timestamp)
                        var commentId = document.data?.getValue("commentId").toString()
                        var commentUserId = document.data?.getValue("userId").toString()
                        var commentTimestamp = DateFormat().convertTimeStampToDateTime(timeMillis)
                        var commentDescription =
                            document.data?.getValue("description").toString()

                        userDB.document("$commentUserId")
                            .get()
                            .addOnSuccessListener { userDocument ->
                                var commentUserName = userDocument.data?.getValue("name").toString()
                                var commentUserAvatar =
                                    userDocument.data?.getValue("avatar").toString()
                                var commentUserType =
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

                                binding.commentRecyclerView.layoutManager = LinearLayoutManager(
                                    this,
                                    RecyclerView.VERTICAL,
                                    false
                                )

                                adapter.notifyDataSetChanged()

                                commentDataLoading.loadingCompleteData.value = true

                                binding.commentRecyclerView.visibility = View.VISIBLE
                                binding.noItemText.visibility = View.GONE
                            }
                    }
                } else {
                    // 0인 경우
                    commentDataLoading.loadingCompleteData.value = true

                    binding.commentRecyclerView.visibility = View.GONE
                    binding.noItemText.visibility = View.VISIBLE
                }


//                commentDataLoading.loadingCompleteData.value = true

            }


        LocalBroadcastManager.getInstance(this).registerReceiver(
            editDescriptionReceiver,
            IntentFilter("EDIT_INTENT")
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
            deleteCommentReceiver,
            IntentFilter("DELETE_INTENT")
        );

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

            if(commentItems.size == 0) {
                Log.d("댓글", "size 0")
                binding.commentRecyclerView.visibility = View.VISIBLE
                binding.noItemText.visibility = View.GONE
            }

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
                    "userId" to commentUserId,
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
                        commentUserId,
                        userAvatar,
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

                if(commentItems.size == 0) {
                    binding.commentRecyclerView.visibility = View.GONE
                    binding.noItemText.visibility = View.VISIBLE
                }
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


class CommentDataLoading : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
