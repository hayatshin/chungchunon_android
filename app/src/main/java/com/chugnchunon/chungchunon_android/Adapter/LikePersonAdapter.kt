package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.LikePerson
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.EnlargeImageActivity
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.EnlargeImageCardBinding
import kotlinx.android.synthetic.main.activity_image_enlarge.view.*
import kotlinx.android.synthetic.main.enlarge_image_card.view.*
import kotlinx.android.synthetic.main.like_person_line.view.*

class LikePersonAdapter(
    val context: Context,
    private val likePersonList: ArrayList<LikePerson>
) :
    RecyclerView.Adapter<LikePersonAdapter.ViewHolder>() {

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val likePersonAvatar = view?.findViewById<ImageView>(R.id.likePersonAvatar)
        val likePersonName = view?.findViewById<TextView>(R.id.likePersonName)
        val likePersonRegion = view?.findViewById<TextView>(R.id.likePersonRegion)

        fun bind(context: Context, position: Int) {
            Glide.with(context)
                .load(likePersonList[position].userAvatar)
                .into(likePersonAvatar!!)

            likePersonName!!.text = likePersonList[position].userName
            likePersonRegion!!.text = likePersonList[position].userRegion
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LikePersonAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.like_person_line, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: LikePersonAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)

        holder.itemView.likePersonAvatar.setOnClickListener { view ->
            var goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", likePersonList[position].userAvatar)
            context.startActivity(goEnlargeAvatar)
        }
    }

    override fun getItemCount(): Int = likePersonList.size

}