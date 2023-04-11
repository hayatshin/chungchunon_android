package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.AttractionDetailActivity
import com.chugnchunon.chungchunon_android.DataClass.Attraction
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.attraction_card.view.*

class AttractionDetailAdapter(val context: Context, private val attractionImageList: ArrayList<String>) :
    RecyclerView.Adapter<AttractionDetailAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val attractionDetailImage = view?.findViewById<ImageView>(R.id.attractionImage)

        fun bind(context: Context, position: Int) {
            Glide.with(context)
                .load(attractionImageList[position])
                .into(attractionDetailImage!!)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AttractionDetailAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.attraction_image_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttractionDetailAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)
    }

    override fun getItemCount(): Int = attractionImageList.size

}