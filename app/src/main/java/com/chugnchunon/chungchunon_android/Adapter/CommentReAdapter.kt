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
import com.chugnchunon.chungchunon_android.DataClass.ReComment
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.comment_list.view.*
import kotlinx.android.synthetic.main.comment_list_re.view.*
import kotlinx.coroutines.withContext

class CommentReAdapter(var context: Context, var items: ArrayList<ReComment>) :
    RecyclerView.Adapter<CommentReAdapter.CommentViewHolder>() {

    private val diaryDB = Firebase.firestore.collection("diary")
    var userId = Firebase.auth.currentUser?.uid

    private var originalDescription = ""
    private var commentId = ""
    private var diaryId = ""
    private var diaryPosition = 0

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var commentNameView: TextView = itemView.findViewById(R.id.reCommentName)
        var commentTimeStampView: TextView = itemView.findViewById(R.id.reCommentTimestamp)
        var commentDescriptionView: TextView = itemView.findViewById(R.id.reCommentDescription)
        var commentPartnerCheckImage: ImageView = itemView.findViewById(R.id.rePartnerCheckImg)
        var commentEditDeleteView = itemView.findViewById<LinearLayout>(R.id.reEditDeleteLayout)
        var commentAvatar: ImageView = itemView.findViewById(R.id.reCommentAvatar)

        fun bind(position: Int) {
            val commentUserType = items[position].reCommentUserType
            val commentUserId = items[position].reCommentUserId

            if (commentUserId != userId) {
                commentEditDeleteView.visibility = View.GONE
            } else {
                commentEditDeleteView.visibility = View.VISIBLE
            }

            if (commentUserType == "파트너") {
                commentPartnerCheckImage.visibility = View.VISIBLE
            } else {
                commentPartnerCheckImage.visibility = View.GONE
            }

            commentNameView.text = items[position].reCommentUserName
            commentTimeStampView.text = items[position].reCommentTimestamp
            commentDescriptionView.text = items[position].reCommentDescription
            originalDescription = items[position].reCommentDescription

            Glide.with(context)
                .load(items[position].reCommentUserAvatar)
                .into(commentAvatar)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.comment_list_re, parent, false)
        return CommentViewHolder(v)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(position)

        holder.itemView.reCommentAvatar.setOnClickListener { view ->
            var goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", items[position].reCommentUserAvatar)
            context.startActivity(goEnlargeAvatar)
        }

        // 수정
        holder.itemView.reEditBtn.setOnClickListener { view ->
            val editIntent = Intent(context, CommentActivity::class.java)
            editIntent.setAction("RE_COMMENT_EDIT_INTENT")
            editIntent.putExtra("commentId", items[position].commentId)
            editIntent.putExtra("reCommentId", items[position].reCommentId)
            editIntent.putExtra("reOriginalDescription", originalDescription)
            LocalBroadcastManager.getInstance(context).sendBroadcast(editIntent);
        }

        // 삭제
        holder.itemView.reDeleteBtn.setOnClickListener { view ->

            val DiaryRef = diaryDB.document(items[position].diaryId)

            // recomments DB 삭제
            DiaryRef.collection("comments")
                .document(items[position].commentId)
                .collection("reComments")
                .document(items[position].reCommentId)
                .get()
                .addOnSuccessListener { reCommentData ->
                    if (reCommentData.exists()) {
                        DiaryRef
                            .collection("comments")
                            .document(items[position].commentId)
                            .collection("reComments")
                            .document(items[position].reCommentId)
                            .delete()
                            .addOnSuccessListener {

                                val deleteIntent =
                                    Intent(context, CommentActivity::class.java)
                                deleteIntent.setAction("RE_COMMENT_DELETE_INTENT")
                                deleteIntent.putExtra("commentId", items[position].commentId)
                                deleteIntent.putExtra("deleteRecommentPosition", position)
                                LocalBroadcastManager.getInstance(context)
                                    .sendBroadcast(deleteIntent);
                            }
                    }
                }
        }
    }

    override fun getItemCount(): Int = items.size
}