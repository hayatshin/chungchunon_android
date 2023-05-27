package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.DataClass.Community
import com.chugnchunon.chungchunon_android.EditRegionRegisterActivity.Companion.editRegionCheck
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.Fragment.SmallRegionRegisterFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_region.view.*

class CommunityAdapter(private var context: Context, private var communityItems: List<Community>?)
    : RecyclerView.Adapter<CommunityAdapter.CommunityViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid

    inner class CommunityViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val communityTitleView: TextView = itemView.findViewById(R.id.communityTitle)
        val communityImageView: ImageView = itemView.findViewById(R.id.communityImage)

        fun bind (position: Int) {
            communityTitleView.text = communityItems!!.get(position).communityTitle

            Glide.with(context)
                .load(communityItems!!.get(position).communityImage)
                .into(communityImageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        holder.bind(position)
        var intent : Intent

        holder.itemView.setOnClickListener {
            val regionIntent = Intent(context, CommunityRegisterActivity::class.java)
            regionIntent.setAction("COMMUNITY_SELECT")
            regionIntent.putExtra("communityTitle", communityItems!!.get(position).communityTitle)
            LocalBroadcastManager.getInstance(context).sendBroadcast(regionIntent)
        }

    }

    override fun getItemCount(): Int {
        return communityItems?.size ?: 0
    }


}