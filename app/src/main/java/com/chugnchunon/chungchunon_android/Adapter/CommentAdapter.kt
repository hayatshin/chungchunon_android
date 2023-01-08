package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.persistableBundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.comment_list.view.*
import kotlinx.coroutines.withContext

class CommentAdapter(var context: Context, var items: ArrayList<Comment>):
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private var originalDescription = ""
    private var commentId = ""

    inner class CommentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var commentNameView: TextView = itemView.findViewById(R.id.commentName)
        var commentTimeStampView: TextView = itemView.findViewById(R.id.commentTimestamp)
        var commentDescriptionView: TextView = itemView.findViewById(R.id.commentDescription)

        fun bind(position: Int) {
            commentId = items[position].commentId
            commentNameView.text = items[position].commentName
            commentTimeStampView.text = items[position].commentTimestamp
            commentDescriptionView.text = items[position].commentDescription
            originalDescription = items[position].commentDescription
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.comment_list, parent, false)
        return CommentViewHolder(v)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(position)

        // 수정
        holder.itemView.editBtn.setOnClickListener { view ->
           var editIntent = Intent(context, CommentActivity::class.java)
            editIntent.setAction("EDIT_INTENT")
            editIntent.putExtra("editCommentId", commentId)
            editIntent.putExtra("editCommentPosition", position)
            editIntent.putExtra("originalDescription", originalDescription)
            LocalBroadcastManager.getInstance(context).sendBroadcast(editIntent);
        }

        // 삭제
        holder.itemView.deleteBtn.setOnClickListener { view ->
            var deleteIntent = Intent(context, CommentActivity::class.java)
            deleteIntent.setAction("DELETE_INTENT")
            deleteIntent.putExtra("deleteCommentId", commentId)
            deleteIntent.putExtra("deleteCommentPosition", position)
            LocalBroadcastManager.getInstance(context).sendBroadcast(deleteIntent);
        }

    }

    override fun getItemCount(): Int = items.size
}