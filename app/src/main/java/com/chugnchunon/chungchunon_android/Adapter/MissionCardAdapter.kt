package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.R

class MissionCardAdapter(val context: Context, private val missionList: ArrayList<Mission>) :

    RecyclerView.Adapter<MissionCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {


        val missionImage = view?.findViewById<ImageView>(R.id.missionImage)
        val communityLogo = view?.findViewById<ImageView>(R.id.communityLogo)
        val community = view?.findViewById<TextView>(R.id.community)
        val missionState = view?.findViewById<TextView>(R.id.state)
        val missionPeriod = view?.findViewById<TextView>(R.id.missionPeriod)
        val missionButton = view?.findViewById<AppCompatButton>(R.id.missionButton)

        fun bind(context: Context, mission: Mission) {
            Log.d("미션 바인드", "뷰홀더")

            Glide.with(context)
                .load(mission.missionImage)
                .into(missionImage!!)

            Glide.with(context)
                .load(mission.communityLogo)
                .into(communityLogo!!)

            missionState?.text = mission.state

            if(mission.state == "진행") {
                missionButton?.isEnabled = true
                var drawableBack = ContextCompat.getDrawable(context, R.drawable.mission_participation)
                missionState?.setBackgroundResource(R.drawable.mission_participation)
                missionState?.setTextColor(ContextCompat.getColor(context, R.color.main_color))
            } else {
                missionButton?.isEnabled = false
                missionState?.setBackgroundResource(R.drawable.mission_no_participation)
                missionState?.setTextColor(Color.BLACK)
            }


            var totalPeriod = "${mission.startPeriod.replace("-", ".")} ~ ${mission.endPeriod.replace("-", ".")}"
            missionPeriod?.text = totalPeriod


            community?.text = mission.community


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MissionCardAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.mission_card, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: MissionCardAdapter.ViewHolder, position: Int) {
        holder.bind(context, missionList!![position])

    }

    override fun getItemCount(): Int  = missionList.size

}