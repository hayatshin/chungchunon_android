package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.R

class MissionCardAdapter(val context: Context, private val missionList: ArrayList<Mission>) :

    RecyclerView.Adapter<MissionCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {

        val missionImage = view?.findViewById<ImageView>(R.id.missionImage)
        val finishMissionLayout = view?.findViewById<LinearLayout>(R.id.finishMissionLayout)
//        val cardIndex = view?.findViewById<TextView>(R.id.cardIndex)

//        val noMissionCard = view?.findViewById<LinearLayout>(R.id.noMissionCard)
//        val communityLogo = view?.findViewById<ImageView>(R.id.communityLogo)
//        val community = view?.findViewById<TextView>(R.id.community)
//        val missionPeriod = view?.findViewById<TextView>(R.id.missionPeriod)
//        val missionButton = view?.findViewById<AppCompatButton>(R.id.missionButton)

        fun bind(context: Context, position: Int) {

            Glide.with(context)
                .load(missionList[position].missionImage)
                .into(missionImage!!)

//            if(missionList[position].missionImage == "event_festival") {
//                missionImage?.setImageResource(R.drawable.event_festival)
//            } else missionImage?.setImageResource(R.drawable.event_money)

            if (missionList[position].state == "진행") {
                finishMissionLayout?.visibility = View.GONE
            } else {
                finishMissionLayout?.visibility = View.VISIBLE
            }

            var spanText = SpannableStringBuilder()
                .color(Color.WHITE) { append("${position+1}") }
                .append(" / ${missionList.size}")

//            cardIndex?.text = spanText

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

    }

    override fun getItemCount(): Int = missionList.size

}