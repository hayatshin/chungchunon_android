package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.persistableBundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.comment_list.view.*
import kotlinx.coroutines.withContext

class CommentAdapter(var context: Context, var items: ArrayList<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val diaryDB = Firebase.firestore.collection("diary")
    var userId = Firebase.auth.currentUser?.uid

    private var originalDescription = ""
    private var commentId = ""
    private var diaryId = ""
    private var diaryPosition = 0

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var commentNameView: TextView = itemView.findViewById(R.id.commentName)
        var commentTimeStampView: TextView = itemView.findViewById(R.id.commentTimestamp)
        var commentDescriptionView: TextView = itemView.findViewById(R.id.commentDescription)
        var commentPartnerCheckImage: ImageView = itemView.findViewById(R.id.partnerCheckImg)
        var commentEditDeleteView = itemView.findViewById<LinearLayout>(R.id.editDeleteLayout)
        var commentAvatar: ImageView = itemView.findViewById(R.id.commentAvatar)

        fun bind(position: Int) {
            var commentUserType = items[position].commentUserType
            var commentUserId = items[position].commentUserId

            if (commentUserId != userId) commentEditDeleteView.visibility = View.GONE

            if (commentUserType == "파트너") {
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.comment_list, parent, false)
        return CommentViewHolder(v)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(position)

        holder.itemView.commentAvatar.setOnClickListener { view ->
            var goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", items[position].commentUserAvatar)
            context.startActivity(goEnlargeAvatar)
        }

        // 수정
        holder.itemView.editBtn.setOnClickListener { view ->
            var editIntent = Intent(context, CommentActivity::class.java)
            editIntent.setAction("EDIT_INTENT")
            editIntent.putExtra("editCommentId", items[position].commentId)
            editIntent.putExtra("editCommentPosition", position)
            editIntent.putExtra("originalDescription", originalDescription)
            LocalBroadcastManager.getInstance(context).sendBroadcast(editIntent);
        }

        // 삭제
        holder.itemView.deleteBtn.setOnClickListener { view ->

            var DiaryRef = diaryDB.document(items[position].diaryId)
//            DiaryRef.update("numComments", FieldValue.increment(-1))

            // comments DB 삭제
            DiaryRef
                .collection("comments")
                .document(items[position].commentId)
                .delete()
                .addOnSuccessListener {

                    // diary DB 내 numComments -1
                    DiaryRef.update("numComments", FieldValue.increment(-1))
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                var deleteIntent =
                                    Intent(context, CommentActivity::class.java)
                                deleteIntent.setAction("DELETE_INTENT")
                                deleteIntent.putExtra("deleteDiaryId", items[position].diaryId)
                                deleteIntent.putExtra(
                                    "deleteDiaryPosition",
                                    items[position].diaryPosition
                                )
                                deleteIntent.putExtra("deleteCommentPosition", position)
                                LocalBroadcastManager.getInstance(context)
                                    .sendBroadcast(deleteIntent);

                            }
                        }
                }


        }
    }

    override fun getItemCount(): Int = items.size
}