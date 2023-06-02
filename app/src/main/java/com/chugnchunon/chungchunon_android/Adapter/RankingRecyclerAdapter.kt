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
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.R
import com.firebase.ui.auth.AuthUI.getApplicationContext
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card.view.*
import kotlinx.android.synthetic.main.ranking_line.view.*
import org.apache.commons.lang3.mutable.MutableBoolean
import java.time.LocalDate


class RankingRecyclerAdapter(val context: Context, var items: ArrayList<RankingLine>) :
    RecyclerView.Adapter<RankingRecyclerAdapter.RankingViewHolder>() {

    var db = Firebase.firestore
    var userId = Firebase.auth.currentUser?.uid

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
                rankingCrown.setImageResource(R.drawable.ic_crown_1st)
            } else if (rankingLine.index!!.toString() == "2") {
                rankingCrown.visibility = View.VISIBLE
                rankingCrown.setImageResource(R.drawable.ic_crown_2nd)
            } else if (rankingLine.index!!.toString() == "3") {
                rankingCrown.visibility = View.VISIBLE
                rankingCrown.setImageResource(R.drawable.ic_crown_3rd)
            } else {
                rankingCrown.visibility = View.GONE
            }

            if(rankingLine.username.length > 10) {
                rankingName.text = "${rankingLine.username.substring(0, 10)}.."
            } else {
                rankingName.text = rankingLine.username
            }

            Glide.with(context)
                .load(rankingLine.userAvatar)
                .into(rankingAvatar)

            val decimal = DecimalFormat("#,###")
            val pointResult = decimal.format(rankingLine.point)

            // 점 or 원

            db.collection("users")
                .document("$userId")
                .get()
                .addOnSuccessListener { userData ->
                    val userRegion = userData.data?.getValue("region").toString()
                    val userSmallRegion = userData.data?.getValue("smallRegion").toString()
                    val userFullRegion = "${userRegion} ${userSmallRegion}"

                    db.collection("contract_region")
                        .document(userFullRegion)
                        .get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val cdDocument = task.result
                                if (cdDocument != null) {
                                    if (cdDocument.exists()) {
                                        val regionUnit =
                                            cdDocument.data?.getValue("unit").toString()

                                        if(regionUnit == "원") {
                                            rankingPoint.text = "${pointResult}원"
                                        } else {
                                            rankingPoint.text = "${pointResult}점"
                                        }

                                    } else {
                                        // 지역 존재하지 않을 때

                                        db.collection("community")
                                            .whereArrayContains("users", "$userId").get()
                                            .addOnSuccessListener { communityDocuments ->
                                                if(communityDocuments.size() > 0) {
                                                    // 소속기관 있음
                                                    rankingPoint.text = "${pointResult}원"
                                                } else {
                                                    rankingPoint.text = "${pointResult}점"
                                                }
                                            }
                                    }
                                } else {
                                    db.collection("community")
                                        .whereArrayContains("users", "$userId").get()
                                        .addOnSuccessListener { communityDocuments ->
                                            if(communityDocuments.size() > 0) {
                                                // 소속기관 있음
                                                rankingPoint.text = "${pointResult}원"
                                            } else {
                                                rankingPoint.text = "${pointResult}점"
                                            }
                                        }
                                }
                            }
                        }
                }
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

        holder.itemView.rankingAvatar.setOnClickListener { view ->
            var goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", items[position].userAvatar)
            context.startActivity(goEnlargeAvatar)
        }
    }

    override fun getItemCount(): Int = items.size


}
