package com.chugnchunon.chungchunon_android.Adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.EnlargeImageActivity
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.display_image_card.view.*
import kotlinx.android.synthetic.main.image_card.view.*
import java.net.URI

class DisplayPhotosAdapter(val context: Context, private val imageData: ArrayList<String>) :
    RecyclerView.Adapter<DisplayPhotosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val displayImageCard = view?.findViewById<ImageView>(R.id.displayImageCard)
        val videoPlayBtn = view?.findViewById<RelativeLayout>(R.id.videoPlayBtn)

        fun bind(context: Context, eachImage: String) {

            if(eachImage.contains("video")) {
                videoPlayBtn!!.visibility = View.VISIBLE

                Glide.with(context)
                    .load(eachImage)
                    .thumbnail(0.1f) // Load a 10% scaled down version as a thumbnail
                    .diskCacheStrategy(DiskCacheStrategy.DATA) // Cache the data source
                    .into(displayImageCard!!)

            } else {
                videoPlayBtn!!.visibility = View.GONE

                Glide.with(context)
                    .load(eachImage)
                    .into(displayImageCard!!);
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DisplayPhotosAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.display_image_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: DisplayPhotosAdapter.ViewHolder, position: Int) {
        holder.bind(context, imageData[position])

        holder.itemView.displayImageCard.transitionName = "imageSharedItem"

        holder.itemView.displayImageCard.setOnClickListener {
            resumePause = true

            val goEnlargeImage = Intent(context, EnlargeImageActivity::class.java)
            goEnlargeImage.putExtra("imageArray", imageData)
            goEnlargeImage.putExtra("imagePosition", position)
            context.startActivity(goEnlargeImage)
        }
    }

    override fun getItemCount(): Int = imageData.size

}