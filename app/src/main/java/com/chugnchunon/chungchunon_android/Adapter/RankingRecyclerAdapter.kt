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
import com.chugnchunon.chungchunon_android.DataClass.*
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
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


class RankingRecyclerAdapter(val context: Context, var items: ArrayList<RankingLine>) :
    RecyclerView.Adapter<RankingRecyclerAdapter.RankingViewHolder>() {

    inner class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var rankingIndex: TextView = itemView.findViewById(R.id.rankingIndex)
        var rankingName: TextView = itemView.findViewById(R.id.rankingName)
        var rankingAvatar: ImageView = itemView.findViewById(R.id.rankingAvatar)
        var rankingPoint: TextView = itemView.findViewById(R.id.rankingPoint)
        var rankingCrown: ImageView = itemView.findViewById(R.id.rankingCrown)

        fun bind(context: Context, rankingLine: RankingLine) {

            rankingCrown.bringToFront()
            rankingIndex.text = rankingLine.index.toString()

            if(rankingLine.index!!.toString() == "1") {
                rankingCrown.visibility = View.VISIBLE
                rankingCrown.setImageResource(R.drawable.ic_crown_filled)
            } else if (rankingLine.index!!.toString() == "2") {
                rankingCrown.visibility = View.VISIBLE
                rankingCrown.setImageResource(R.drawable.ic_crown_outlined)
            } else {
                rankingCrown.visibility = View.GONE
            }

            rankingName.text = rankingLine.username.toString()

            Glide.with(context)
                .load(rankingLine.userAvatar)
                .into(rankingAvatar)

            var decimal = DecimalFormat("#,###")
            var pointResult = decimal.format(rankingLine.point)

            rankingPoint.text = "${pointResult}점"

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RankingRecyclerAdapter.RankingViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.ranking_line, parent, false)
        return RankingViewHolder(v)
    }

    override fun onBindViewHolder(holder: RankingRecyclerAdapter.RankingViewHolder, position: Int) {
        holder.bind(context, items!![position])
    }

    override fun getItemCount(): Int = items.size


}
