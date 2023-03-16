package com.chugnchunon.chungchunon_android.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.BlockActivity
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DataClass.EmoticonInteger
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.LikePersonActivity
import com.chugnchunon.chungchunon_android.R
import com.firebase.ui.auth.AuthUI.getApplicationContext
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.view.*
import org.apache.commons.lang3.mutable.MutableBoolean
import java.time.LocalDate


class RegionDiaryCardAdapter(val context: Context, var items: ArrayList<DiaryCard>) :
    RecyclerView.Adapter<RegionDiaryCardAdapter.CardViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var diaryDB = Firebase.firestore.collection("diary")

    var likeToggleCheck: MutableMap<Int, Boolean> = mutableMapOf()
    var likeNumLikes: MutableMap<Int, Int> = mutableMapOf()



    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var userWriteTime: TextView = itemView.findViewById(R.id.userWriteTime)
        var userNameView: TextView = itemView.findViewById<TextView>(R.id.userName)
        var userAvatarView: ImageView = itemView.findViewById<ImageView>(R.id.userAvatar)
        var userStepCountView: TextView = itemView.findViewById<TextView>(R.id.userStepCount)
        var userMoodView: ImageView = itemView.findViewById<ImageView>(R.id.userMood)
        var userDiaryView: TextView = itemView.findViewById<TextView>(R.id.userDiary)
        var userLikeView: TextView = itemView.findViewById<TextView>(R.id.likeText)
        var userCommentView: TextView = itemView.findViewById<TextView>(R.id.commentText)
        var likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        var commentIcon: ImageView = itemView.findViewById(R.id.commentIcon)
        var imageDisplayRecyclerView: RecyclerView = itemView.findViewById(R.id.imageDisplayRecyclerView)
        var secretStatusView: ImageView = itemView.findViewById(R.id.secretStatusView)

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {

            var decimal = DecimalFormat("#,###")
            var step = decimal.format(items[position].stepCount)

            if (items[position].userAvatar != null) {
                Glide.with(context)
                    .load(items[position].userAvatar)
                    .into(userAvatarView)
            } else {
                // nothing
            }

            if(items[position].secret) {
                secretStatusView.visibility = View.VISIBLE
            } else {
                secretStatusView.visibility = View.GONE
            }

            userWriteTime.text = DateFormat().convertMillisToDate(items[position].writeTime)
            userNameView.text = items[position].name
            userStepCountView.text = "${step}보"
            userMoodView.setImageResource(EmoticonInteger().IntToEmoticon(items[position].mood!!.toInt()))
            userDiaryView.text = items[position].diary
            userLikeView.text = "좋아요 ${items[position].numLikes}"
            userCommentView.text = "댓글 ${items[position].numComments}"

            when(items[position].mood!!.toInt()) {
                0 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.main_color))
                1 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.shalom_color))
                2 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.throb_color))
                3 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.soso_color))
                4 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.anxious_color))
                5 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.sad_color))
                6 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.gloomy_color))
                7 -> userMoodView.setColorFilter(ContextCompat.getColor(context, R.color.angry_color))
            }

            // 이미지 작업

            if(items[position].images == null || items[position].images?.size == 0) {
                imageDisplayRecyclerView.visibility = View.GONE
            } else {
                imageDisplayRecyclerView.visibility = View.VISIBLE
                imageDisplayRecyclerView.adapter = DisplayPhotosAdapter(context, items[position].images!!)
            }

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
        val v = LayoutInflater.from(parent.context).inflate(R.layout.diary_card_two, parent, false)
        return CardViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(position)

        holder.itemView.likeIcon.setOnClickListener { view ->

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
            resumePause = true

            var openComment = Intent(context, CommentActivity::class.java)
            openComment.putExtra("diaryId", items[position].diaryId)
            openComment.putExtra("diaryPosition", position)
            context.startActivity(openComment)
        }

        // 차단 아이콘 클릭
        holder.itemView.moreIcon.setOnClickListener { view ->
            resumePause = true

            var openBlockActivity = Intent(context, BlockActivity::class.java)
            openBlockActivity.putExtra("diaryId", items[position].diaryId)
            openBlockActivity.putExtra("diaryUserId", items[position].userId)
            openBlockActivity.putExtra("diaryType", "region")
            context.startActivity(openBlockActivity)
        }

        holder.itemView.likeText.setOnClickListener{

            if(likeNumLikes[position] != 0) {
                resumePause = true

                var openLikePersonActivity = Intent(context, LikePersonActivity::class.java)
                openLikePersonActivity.putExtra("diaryId", items[position].diaryId)
                openLikePersonActivity.putExtra("diaryUserId", items[position].userId)
                context.startActivity(openLikePersonActivity)
            }

        }
    }

    override fun getItemCount(): Int = items.size
}

