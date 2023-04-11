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
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.attraction_card.view.*

class AttractionAdapter(val context: Context, private val attractionData: ArrayList<Attraction>) :
    RecyclerView.Adapter<AttractionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val attractionTitle = view?.findViewById<TextView>(R.id.attractionTitle)
        val attractionImage = view?.findViewById<ImageView>(R.id.attractionImage)

        fun bind(context: Context, position: Int) {
            attractionTitle?.text = attractionData[position].name

            Glide.with(context)
                .load(attractionData[position].mainImage)
                .into(attractionImage!!)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AttractionAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.attraction_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttractionAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)

        holder.itemView.attractionImage.setOnClickListener {
            var goAttractionDetail = Intent(context, AttractionDetailActivity::class.java)
            goAttractionDetail.putExtra("adName", attractionData[position].name)
            goAttractionDetail.putExtra("adDescription", attractionData[position].description)
            goAttractionDetail.putExtra("adLocation", attractionData[position].location)
            goAttractionDetail.putExtra("adSubImage", attractionData[position].subImage)
            context.startActivity(goAttractionDetail)
        }

    }

    override fun getItemCount(): Int = attractionData.size

}