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
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.Book
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.image_card.view.*
import java.net.URI

class BooksAdapter(val context: Context, private val bookData: ArrayList<Book>) :
    RecyclerView.Adapter<BooksAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val bookImage = view?.findViewById<ImageView>(R.id.bookImage)
        val bookTitle = view?.findViewById<TextView>(R.id.bookTitle)

        fun bind(context: Context, position: Int) {
           Glide.with(context)
               .load(bookData[position].cover)
               .into(bookImage!!)

            bookTitle?.text = bookData[position].title
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BooksAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.book_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BooksAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)

    }

    override fun getItemCount(): Int = bookData.size

}