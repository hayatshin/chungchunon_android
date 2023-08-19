package com.chugnchunon.chungchunon_android.Adapter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.ReComment
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_comment.view.*
import kotlinx.android.synthetic.main.comment_list.view.*
import kotlinx.android.synthetic.main.comment_list_re.view.*

class CommentAdapter(var context: Context, var items: ArrayList<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    var userId = Firebase.auth.currentUser?.uid

    private var originalDescription = ""
    private var commentId = ""
    private var diaryId = ""
    private var diaryPosition = 0

    private val anonymousUserAvatar =
        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png"

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var commentNameView: TextView = itemView.findViewById(R.id.commentName)
        var commentTimeStampView: TextView = itemView.findViewById(R.id.commentTimestamp)
        var commentDescriptionView: TextView = itemView.findViewById(R.id.commentDescription)
        var commentPartnerCheckImage: ImageView = itemView.findViewById(R.id.partnerCheckImg)
        var commentEditDeleteView = itemView.findViewById<LinearLayout>(R.id.editDeleteLayout)
        var commentAvatar: ImageView = itemView.findViewById(R.id.commentAvatar)
        var reCommentRecyclerView: RecyclerView = itemView.findViewById(R.id.reCommentRecyclerView)


        fun bind(position: Int) {

            val commentUserType = items[position].commentUserType
            val commentUserId = items[position].commentUserId

            if (commentUserId != userId) {
                commentEditDeleteView.visibility = View.GONE
            } else {
                commentEditDeleteView.visibility = View.VISIBLE
            }

            if (commentUserType != "사용자") {
                commentPartnerCheckImage.visibility = View.VISIBLE
            } else {
                commentPartnerCheckImage.visibility = View.GONE
            }

            commentNameView.text = items[position].commentUserName
            commentTimeStampView.text = items[position].commentTimestamp
            commentDescriptionView.text = items[position].commentDescription
            originalDescription = items[position].commentDescription

            Glide.with(context)
                .load(items[position].commentUserAvatar)
                .into(commentAvatar)

            // 답글
            var reCommentItems: ArrayList<ReComment> = ArrayList<ReComment>()
            val reAdapter: CommentReAdapter

            reAdapter = CommentReAdapter(context, reCommentItems)
            reCommentRecyclerView.adapter = reAdapter
            reCommentRecyclerView.layoutManager =
                LinearLayoutManager(
                    context,
                    RecyclerView.VERTICAL,
                    false
                )

            diaryDB.document(items[position].diaryId).collection("comments")
                .document(items[position].commentId).collection("reComments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { reComments ->
                    if (reComments.size() != 0) {
                        for (reComment in reComments) {
                            val reTimeMillis =
                                (reComment.data?.getValue("timestamp") as Timestamp)
                            val reCommentId =
                                reComment.data?.getValue("reCommentId").toString()
                            val reCommentUserId =
                                reComment.data?.getValue("userId").toString()
                            val reCommentTimestamp =
                                DateFormat().convertTimeStampToDateTime(reTimeMillis)
                            val reCommentDescription =
                                reComment.data?.getValue("description").toString()

                            userDB.document(reCommentUserId)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    if (userDocument != null) {
                                        // 유저가 있는 경우
                                        val reCommentUserName =
                                            userDocument.data?.getValue("name").toString()
                                        val reCommentUserAvatar =
                                            userDocument.data?.getValue("avatar").toString()
                                        val reCommentUserType =
                                            userDocument.data?.getValue("userType")
                                                .toString()

                                        reCommentItems.add(
                                            ReComment(
                                                items[position].diaryId,
                                                items[position].commentId,
                                                reCommentId,
                                                reCommentUserId,
                                                reCommentUserAvatar,
                                                reCommentUserName,
                                                reCommentUserType,
                                                reCommentTimestamp,
                                                reCommentDescription
                                            )
                                        )
                                        reCommentItems.sortWith(compareBy { it.reCommentTimestamp })
                                        reAdapter.notifyDataSetChanged()
                                    } else {
                                        // 유저가 없는 경우
                                        val reCommentUserName = "탈퇴자"
                                        val reCommentUserAvatar = anonymousUserAvatar
                                        val reCommentUserType = "마스터"

                                        reCommentItems.add(
                                            ReComment(
                                                items[position].diaryId,
                                                items[position].commentId,
                                                reCommentId,
                                                reCommentUserId,
                                                reCommentUserAvatar,
                                                reCommentUserName,
                                                reCommentUserType,
                                                reCommentTimestamp,
                                                reCommentDescription
                                            )
                                        )
                                        reCommentItems.sortWith(compareBy { it.reCommentTimestamp })
                                        reAdapter.notifyDataSetChanged()
                                    }
                                }
                        }
                    }
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.comment_list, parent, false)

        return CommentViewHolder(v)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(position)

        // 터치 키보드 다운
        holder.itemView.reCommentRecyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                val imm: InputMethodManager =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

                true
            }
            false
        }

        // 가장 아래 글로 스크롤
        holder.itemView.reCommentRecyclerView.addOnLayoutChangeListener(object :
            View.OnLayoutChangeListener {
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
                val childCount = holder.reCommentRecyclerView.adapter?.itemCount
                holder.itemView.reCommentRecyclerView.scrollToPosition(childCount!! - 1)
            }
        })

        // 아바타 클릭
        holder.itemView.commentAvatar.setOnClickListener { view ->

            Log.d("코멘트", "$items")

            val goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", items[position].commentUserAvatar)
            context.startActivity(goEnlargeAvatar)
        }

        // 수정
        holder.itemView.editBtn.setOnClickListener { view ->
            val editIntent = Intent(context, CommentActivity::class.java)
            editIntent.setAction("EDIT_COMMENT_INTENT")
            editIntent.putExtra("editCommentId", items[position].commentId)
            editIntent.putExtra("editCommentPosition", position)
            editIntent.putExtra("originalDescription", items[position].commentDescription)
            LocalBroadcastManager.getInstance(context).sendBroadcast(editIntent);
        }

        // 삭제
        holder.itemView.deleteBtn.setOnClickListener { view ->

            val DiaryRef = diaryDB.document(items[position].diaryId)

            // comments DB 삭제
            DiaryRef.collection("comments")
                .document(items[position].commentId)
                .get()
                .addOnSuccessListener { commentData ->
                    if (commentData.exists()) {

                        DiaryRef
                            .collection("comments")
                            .document(items[position].commentId)
                            .delete()
                            .addOnSuccessListener {
                                // diary DB 내 numComments -1

                                val deleteIntent =
                                    Intent(context, CommentActivity::class.java)
                                deleteIntent.setAction("DELETE_COMMENT_INTENT")
                                deleteIntent.putExtra(
                                    "deleteDiaryId",
                                    items[position].diaryId
                                )
                                deleteIntent.putExtra(
                                    "deleteDiaryPosition",
                                    items[position].diaryPosition
                                )

                                deleteIntent.putExtra("deleteCommentPosition", position)
                                LocalBroadcastManager.getInstance(context)
                                    .sendBroadcast(deleteIntent);

//                                DiaryRef.update("numComments", FieldValue.increment(-1))
//                                    .addOnCompleteListener { task ->
//                                        if (task.isSuccessful) {
//
//                                            val deleteIntent =
//                                                Intent(context, CommentActivity::class.java)
//                                            deleteIntent.setAction("DELETE_COMMENT_INTENT")
//                                            deleteIntent.putExtra(
//                                                "deleteDiaryId",
//                                                items[position].diaryId
//                                            )
//                                            deleteIntent.putExtra(
//                                                "deleteDiaryPosition",
//                                                items[position].diaryPosition
//                                            )
//                                            deleteIntent.putExtra("deleteCommentPosition", position)
//                                            LocalBroadcastManager.getInstance(context)
//                                                .sendBroadcast(deleteIntent);
//                                        }
//                                    }
                            }
                    }
                }


        }

        // 답글 달기
        holder.itemView.reCommentBtn.setOnClickListener { view ->
            val reCommentIntent =
                Intent(context, CommentActivity::class.java)
            reCommentIntent.setAction("RE_COMMENT_WRITE_INTENT")
            reCommentIntent.putExtra("rewriteCommentId", items[position].commentId)
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(reCommentIntent)
        }

    }

    override fun getItemCount(): Int = items.size
}