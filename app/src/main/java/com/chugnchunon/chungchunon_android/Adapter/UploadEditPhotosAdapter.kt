package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.EditDiaryActivity
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.image_card.view.*
import kotlin.reflect.typeOf

class UploadEditPhotosAdapter(val context: Context, private val imageData: List<Any>?) :
    RecyclerView.Adapter<UploadEditPhotosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val imageView = view?.findViewById<ImageView>(R.id.imageCard)

        fun bind(context: Context, imageBitmap: Any) {

            if(imageBitmap.toString().startsWith("https://")) {
                Glide.with(context)
                    .load(imageBitmap)
                    .into(imageView!!)
            } else if (imageBitmap is Uri) {
                imageView?.setImageURI(imageBitmap)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UploadEditPhotosAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: UploadEditPhotosAdapter.ViewHolder, position: Int) {
        holder.bind(context, imageData!![position])

        holder.itemView.imageLayout.setOnClickListener { view ->
            Log.d("삭제: UploadEditPhotosAdapter", "$position")
            var deleteImageMyFragment = Intent(context, EditDiaryActivity::class.java)
            deleteImageMyFragment.setAction("DELETE_IMAGE_EDIT")
            deleteImageMyFragment.putExtra("deleteImagePosition", position)
            LocalBroadcastManager.getInstance(context).sendBroadcast(deleteImageMyFragment);
        }
    }

    override fun getItemCount(): Int {
        if(imageData != null) return imageData.size else return 0
    }
}