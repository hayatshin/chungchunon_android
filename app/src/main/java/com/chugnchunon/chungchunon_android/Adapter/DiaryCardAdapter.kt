package com.chugnchunon.chungchunon_android.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.view.*
import org.apache.commons.lang3.mutable.MutableBoolean
import java.time.LocalDate


class DiaryCardAdapter(val context: Context, var items: ArrayList<DiaryCard>) :
    RecyclerView.Adapter<DiaryCardAdapter.CardViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var diaryDB = Firebase.firestore.collection("diary")

    var likeToggleCheck: MutableMap<Int, Boolean> = mutableMapOf()
    var likeNumLikes: MutableMap<Int, Int> = mutableMapOf()

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var diaryCardView: LinearLayout = itemView.findViewById(R.id.diaryCard)
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

            var decimal = DecimalFormat("#,###")
            var step = decimal.format(items[position].stepCount)

            userWriteTime.text = DateFormat().convertMillisToDate(items[position].writeTime)
            userNameView.text = items[position].name
            userStepCountView.text = "${step}보"
            userMoodView.setImageResource(items[position].mood!!.toInt())
            userDiaryView.text = items[position].diary
            userLikeView.text = "좋아요 ${items[position].numLikes}"
            userCommentView.text = "댓글 ${items[position].numComments}"

            // 내가 쓴 글 백그라운드 처리
//            if(items[position].userId == userId) {
//                var backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.my_diary_background)
//                diaryCardView.setBackgroundDrawable(backgroundDrawable)
//            }

            // 좋아요 토글 작업
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
                                likeIcon.setImageResource(R.drawable.ic_emptyheart)
                                likeToggleCheck.put(position, false)
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

        holder.itemView.likeBox.setOnClickListener { view ->

            var DiaryRef = diaryDB.document(items[position].diaryId)

            var likeUserSet = hashMapOf(
                "id" to userId,
                "timestamp" to FieldValue.serverTimestamp(),
            )

            if (likeToggleCheck[position]!!) {

                DiaryRef.update("numLikes", FieldValue.increment(-1))


                likeNumLikes[position] = likeNumLikes[position]!!.toInt() - 1
                holder.itemView.likeText.text = "좋아요 ${likeNumLikes[position]}"
                holder.itemView.likeIcon.setImageResource(R.drawable.ic_emptyheart)
                diaryDB.document(items[position].diaryId).collection("likes").document("$userId")
                    .delete()

                likeToggleCheck[position] = false

            } else {

                DiaryRef.update("numLikes", FieldValue.increment(1))


                likeNumLikes[position] = likeNumLikes[position]!!.toInt() + 1
                holder.itemView.likeText.text = "좋아요 ${likeNumLikes[position]}"
                holder.itemView.likeIcon.setImageResource(R.drawable.ic_filledheart)
                diaryDB.document(items[position].diaryId).collection("likes").document("$userId")
                    .set(likeUserSet, SetOptions.merge())

                likeToggleCheck[position] = true

            }

        }

        // 댓글버튼 클릭
        holder.itemView.commentBox.setOnClickListener { view ->
            var openComment = Intent(context, CommentActivity::class.java)
            openComment.putExtra("diaryId", items[position].diaryId)
            openComment.putExtra("diaryPosition", position)
            context.startActivity(openComment)
        }
    }

    override fun getItemCount(): Int = items.size
}

operator fun <T> MutableLiveData<T>.plus(t: Int): MutableLiveData<T> = this + t
operator fun <T> MutableLiveData<T>.minus(t: Int): MutableLiveData<T> = this - t




