package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.image_card.view.*
import java.net.URI

class UploadPhotosAdapter(val context: Context, private val imageData: ArrayList<Uri>) :
    RecyclerView.Adapter<UploadPhotosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val imageView = view?.findViewById<ImageView>(R.id.imageCard)
        val removeButton = view?.findViewById<ImageButton>(R.id.removeButton)

        fun bind(context: Context, imageBitmap: Uri) {
            imageView?.setImageURI(imageBitmap)
            imageView?.setBackgroundResource(R.drawable.agreement_box)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UploadPhotosAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: UploadPhotosAdapter.ViewHolder, position: Int) {
        holder.bind(context, imageData[position])

        holder.itemView.imageLayout.setOnClickListener { view ->
            var deleteImageIntent = Intent(context, MyDiaryFragment::class.java)
            deleteImageIntent.setAction("DELETE_IMAGE")
            deleteImageIntent.putExtra("deleteImagePosition", position)
            LocalBroadcastManager.getInstance(context).sendBroadcast(deleteImageIntent);
        }
    }

    override fun getItemCount(): Int = imageData.size

}