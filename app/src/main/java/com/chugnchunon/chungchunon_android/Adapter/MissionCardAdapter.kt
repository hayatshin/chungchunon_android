package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.MissionDetail.MissionDetailActivity
import com.chugnchunon.chungchunon_android.MissionDetail.MissionDetailActivityAutoProgress
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.mission_card_two.view.*

class MissionCardAdapter(val context: Context, private val missionList: ArrayList<Mission>) :

    RecyclerView.Adapter<MissionCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {

        val missionImage = view?.findViewById<ImageView>(R.id.missionImage)
        val finishMissionLayout = view?.findViewById<LinearLayout>(R.id.finishMissionLayout)

        fun bind(context: Context, position: Int) {

            Glide.with(context)
                .load(missionList[position].missionImage)
                .into(missionImage!!)

            if (missionList[position].state == "진행") {
                finishMissionLayout?.visibility = View.GONE
            } else {
                finishMissionLayout?.visibility = View.VISIBLE
            }

            var spanText = SpannableStringBuilder()
                .color(Color.WHITE) { append("${position+1}") }
                .append(" / ${missionList.size}")


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MissionCardAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.mission_card_two, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: MissionCardAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)

        if(missionList[position].state == "진행") {
            holder.itemView.missionImage.setOnClickListener {
                if (missionList[position].autoProgress) {
                    // 자동 진행 o
                    val goMissionDetail = Intent(context, MissionDetailActivityAutoProgress::class.java)
                    goMissionDetail.putExtra("mdDocID", missionList[position].documentId)
                    goMissionDetail.putExtra("mdTitle", missionList[position].title)
                    goMissionDetail.putExtra("mdDescription", missionList[position].description)
                    goMissionDetail.putExtra("mdImage", missionList[position].missionImage)
                    goMissionDetail.putExtra("mdCommunity", missionList[position].contractName)
                    goMissionDetail.putExtra("mdStartPeriod", missionList[position].startPeriod)
                    goMissionDetail.putExtra("mdEndPeriod", missionList[position].endPeriod)
                    context.startActivity(goMissionDetail)
                } else {
                    // 자동 진행 x
                    val goMissionDetail = Intent(context, MissionDetailActivity::class.java)
                    goMissionDetail.putExtra("mdDocID", missionList[position].documentId)
                    goMissionDetail.putExtra("mdTitle", missionList[position].title)
                    goMissionDetail.putExtra("mdDescription", missionList[position].description)
                    goMissionDetail.putExtra("mdImage", missionList[position].missionImage)
                    goMissionDetail.putExtra("mdCommunity", missionList[position].contractName)
                    goMissionDetail.putExtra("mdStartPeriod", missionList[position].startPeriod)
                    goMissionDetail.putExtra("mdEndPeriod", missionList[position].endPeriod)
                    goMissionDetail.putExtra("mdGoalScore", missionList[position].goalScore)
                    goMissionDetail.putExtra("mdPrizeWinners", missionList[position].prizeWinners)
                    context.startActivity(goMissionDetail)
                }
            }
        }

    }

    override fun getItemCount(): Int = missionList.size

}