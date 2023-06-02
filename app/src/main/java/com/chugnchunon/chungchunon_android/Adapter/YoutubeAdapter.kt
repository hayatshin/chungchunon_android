package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.AttractionDetailActivity
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.Attraction
import com.chugnchunon.chungchunon_android.DataClass.Youtube
import com.chugnchunon.chungchunon_android.EnlargeYoutubeActivity
import com.chugnchunon.chungchunon_android.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.attraction_card.view.*
import kotlinx.android.synthetic.main.item_youtube.view.*

class YoutubeAdapter(val context: Context, private val youtubeItems: ArrayList<Youtube>) :
    RecyclerView.Adapter<YoutubeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {

        val youtubeView: ImageView? = view?.findViewById<ImageView>(R.id.youtubeView)
        val youtubeTitle: TextView? = view?.findViewById<TextView>(R.id.youtubeTitle)


        fun bind(context: Context, position: Int) {
            youtubeTitle?.text = youtubeItems[position].title
//
//            Glide.with(context)
//                .load(attractionData[position].mainImage)
//                .into(attractionImage!!)

            Picasso.get()
                .load(youtubeItems[position].thumbnail)
                .into(youtubeView)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): YoutubeAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_youtube, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: YoutubeAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)

        holder.itemView.youtubeView.setOnClickListener {
            val goYoutubePlay = Intent(context, EnlargeYoutubeActivity::class.java)
            goYoutubePlay.putExtra("link", youtubeItems[position].link)
            goYoutubePlay.putExtra("videoId", youtubeItems[position].videoId)
            context.startActivity(goYoutubePlay)
        }
    }

    override fun getItemCount(): Int = youtubeItems.size

}