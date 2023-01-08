package com.chugnchunon.chungchunon_android.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.view.*
import org.apache.commons.lang3.mutable.MutableBoolean


class DiaryCardAdapter(val context: Context, items: ArrayList<DiaryCard>) :
    RecyclerView.Adapter<DiaryCardAdapter.CardViewHolder>() {

    var items = items
    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var diaryDB = Firebase.firestore.collection("diary")
    var likeToggleCheck: HashMap<Int, Boolean> = HashMap()
    var likeNumLikes: MutableMap<Int, Int> = mutableMapOf()

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var userWriteTime: TextView = itemView.findViewById(R.id.userWriteTime)
        var userNameView: TextView = itemView.findViewById<TextView>(R.id.userName)
        var userStepCountView: TextView = itemView.findViewById<TextView>(R.id.userStepCount)
        var userMoodView: ImageView = itemView.findViewById<ImageView>(R.id.userMood)
        var userDiaryView: TextView = itemView.findViewById<TextView>(R.id.userDiary)
        var userLikeView: TextView = itemView.findViewById<TextView>(R.id.likeText)
        var userCommentView: TextView = itemView.findViewById<TextView>(R.id.commentText)
        var likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        var commentIcon: ImageView = itemView.findViewById(R.id.commentIcon)

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {

            userWriteTime.text = items[position].writeTime
            userNameView.text = items[position].name
            userStepCountView.text = "${items[position].stepCount}보"
            userMoodView.setImageResource(items[position].mood!!.toInt())
            userDiaryView.text = items[position].diary
            userLikeView.text = "좋아요 ${items[position].numLikes}"
            userCommentView.text = "댓글 ${items[position].numComments}"

            likeNumLikes.put(position, items[position].numLikes!!.toInt())


            diaryDB.document(items[position].diaryId).collection("likes").document("$userId")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        var document = task.result
                        if (document != null) {
                            if (document.exists()) {
                                likeToggleCheck.put(position, true)
                                likeIcon.setImageResource(R.drawable.ic_filledheart)
                            } else {
                                likeToggleCheck.put(position, false)
                                likeIcon.setImageResource(R.drawable.ic_emptyheart)
                            }
                        }
                    }
                }
        } // bind
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.diary_card, parent, false)
        return CardViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(position)

        Log.d("태건폰11", "$likeNumLikes")

        holder.itemView.likeBox.setOnClickListener { view ->

            var likeUserSet = hashMapOf(
                "id" to userId
            )

            // 하트 이모티콘
            if (likeToggleCheck[position]!!) {

                likeNumLikes[position] = likeNumLikes[position]!!.toInt() - 1

                var newNumSet = hashMapOf(
                    "numLikes" to  likeNumLikes[position]
                )

                holder.itemView.likeIcon.setImageResource(R.drawable.ic_emptyheart)
                holder.itemView.likeText.text = "좋아요 ${likeNumLikes[position]}"
                diaryDB.document(items[position].diaryId).collection("likes").document("$userId")
                    .delete()
                diaryDB.document(items[position].diaryId).set(newNumSet, SetOptions.merge())

                likeToggleCheck[position] = false
            } else {

                likeNumLikes[position] = likeNumLikes[position]!!.toInt() + 1

                var newNumSet = hashMapOf(
                    "numLikes" to  likeNumLikes[position]
                )

                holder.likeIcon.setImageResource(R.drawable.ic_filledheart)
                holder.itemView.likeText.text = "좋아요 ${likeNumLikes[position]}"
                diaryDB.document(items[position].diaryId).collection("likes").document("$userId")
                    .set(likeUserSet, SetOptions.merge())
                diaryDB.document(items[position].diaryId).set(newNumSet, SetOptions.merge())

                likeToggleCheck[position] = true
            }
        }

        holder.itemView.commentBox.setOnClickListener { view ->
            var openComment = Intent(context, CommentActivity::class.java)
            openComment.putExtra("diaryId", items[position].diaryId)
            openComment.putExtra("diaryPosition", "${position}")
            context.startActivity(openComment)
        }
    }

    override fun getItemCount(): Int = items.size
}

operator fun <T> MutableLiveData<T>.plus(t: Int): MutableLiveData<T> = this + t
operator fun <T> MutableLiveData<T>.minus(t: Int): MutableLiveData<T> = this - t




