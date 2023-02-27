package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.EnlargeImageActivity
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.EnlargeImageCardBinding
import kotlinx.android.synthetic.main.activity_image_enlarge.view.*
import kotlinx.android.synthetic.main.enlarge_image_card.view.*

class EnlargeImageAdapter(
    val context: Context,
    private val imageList: ArrayList<String>
) :
    RecyclerView.Adapter<EnlargeImageAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val enlargeImageView = view?.findViewById<ImageView>(R.id.enlargeImageView)

        fun bind(context: Context, position: Int) {
            Glide.with(context)
                .load(imageList[position])
                .into(enlargeImageView!!)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EnlargeImageAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.enlarge_image_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnlargeImageAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)


    }

    override fun getItemCount(): Int = imageList.size

}