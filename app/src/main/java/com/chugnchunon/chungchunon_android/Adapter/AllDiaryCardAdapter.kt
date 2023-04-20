package com.chugnchunon.chungchunon_android.Adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DataClass.EmoticonInteger
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.view.*
import kotlinx.android.synthetic.main.diary_card.view.commentBox
import kotlinx.android.synthetic.main.diary_card.view.likeIcon
import kotlinx.android.synthetic.main.diary_card.view.likeText
import kotlinx.android.synthetic.main.diary_card.view.moreIcon
import kotlinx.android.synthetic.main.diary_card_two.view.*

class AllDiaryCardAdapter(val context: Context, var items: ArrayList<DiaryCard>) :
    RecyclerView.Adapter<AllDiaryCardAdapter.CardViewHolder>() {

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
        var moreDiaryView: TextView = itemView.findViewById(R.id.moreDiary)

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {

            val decimal = DecimalFormat("#,###")
            val step = decimal.format(items[position].stepCount)

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
            userNameView.text = items[position].username
            userStepCountView.text = "${step}보"
            userMoodView.setImageResource(EmoticonInteger().IntToEmoticon(items[position].mood!!.toInt()))

            if(items[position].diary.toString().length > 150) {
                userDiaryView.text = "${items[position].diary.toString().substring(0, 150)}..."
                moreDiaryView.visibility = View.VISIBLE
            } else {
                userDiaryView.text = items[position].diary
                moreDiaryView.visibility = View.GONE
            }

            moreDiaryView.setOnClickListener {
                moreDiaryView.visibility = View.GONE
                userDiaryView.text = items[position].diary
            }

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

            // 좋아요 토글 작업
            likeNumLikes.put(position, items[position].numLikes!!.toInt())

            diaryDB.document(items[position].diaryId).collection("likes").document("$userId")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.diary_card_two, parent, false)
        return CardViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(position)

        holder.itemView.userAvatar.setOnClickListener { view ->
            val goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", items[position].userAvatar)
            context. startActivity(goEnlargeAvatar)
        }

        holder.itemView.likeIcon.setOnClickListener { view ->

            val DiaryRef = diaryDB.document(items[position].diaryId)

            val likeUserSet = hashMapOf(
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

            val intent = Intent(context, AllDiaryFragmentTwo::class.java)
            intent.setAction("LIKE_TOGGLE_ACTION")
            intent.putExtra("newDiaryId", items[position].diaryId)
            intent.putExtra("newLikeToggle", likeToggleCheck[position]!!)
            intent.putExtra("newNumLikes", likeNumLikes[position]!!.toInt())
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)

        }

        // 댓글버튼 클릭
        holder.itemView.commentBox.setOnClickListener { view ->
            resumePause = true

            val openComment = Intent(context, CommentActivity::class.java)
            openComment.putExtra("diaryId", items[position].diaryId)
            openComment.putExtra("diaryPosition", position)
            context.startActivity(openComment)
        }

        // 차단 아이콘 클릭
        holder.itemView.moreIcon.setOnClickListener { view ->
            resumePause = true

            val openBlockActivity = Intent(context, BlockActivity::class.java)
            openBlockActivity.putExtra("diaryId", items[position].diaryId)
            openBlockActivity.putExtra("diaryUserId", items[position].userId)
            openBlockActivity.putExtra("diaryType", "all")
            context.startActivity(openBlockActivity)
        }

        holder.itemView.likeText.setOnClickListener{

            if(likeNumLikes[position] != 0) {
                resumePause = true

                val openLikePersonActivity = Intent(context, LikePersonActivity::class.java)
                openLikePersonActivity.putExtra("diaryId", items[position].diaryId)
                openLikePersonActivity.putExtra("diaryUserId", items[position].userId)
                context.startActivity(openLikePersonActivity)
            }

        }
    }

    override fun getItemCount(): Int = items.size
}
