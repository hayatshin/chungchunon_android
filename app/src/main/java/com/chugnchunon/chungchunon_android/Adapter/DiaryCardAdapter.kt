package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.R



class DiaryCardAdapter(items: ArrayList<DiaryCard>) :
    RecyclerView.Adapter<DiaryCardAdapter.CardViewHolder>() {

    var items = items

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userNameView: TextView = itemView.findViewById<TextView>(R.id.userName)
        var userStepCountView: TextView = itemView.findViewById<TextView>(R.id.userStepCount)
        var userMoodView: ImageView = itemView.findViewById<ImageView>(R.id.userMood)
        var userDiaryView: TextView = itemView.findViewById<TextView>(R.id.userDiary)

        fun bind(position: Int) {
            userNameView.text = items[position].name
            userStepCountView.text = items[position].stepCount
            userMoodView.setImageResource(items[position].mood)
            userDiaryView.text = items[position].diary

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.diary_card, parent, false)
        return CardViewHolder(v)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size
}








