package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import com.chugnchunon.chungchunon_android.Fragment.UserRegionDataFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_region.view.*
import kotlinx.android.synthetic.main.item_community_result.view.*

class CommunityMenuAdapter(private var context: Context, private var regionCommunityItems: ArrayList<Community>?, private var selectPosition: Int)
    : RecyclerView.Adapter<CommunityMenuAdapter.CommunityViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var clickPosition: Int = selectPosition

    inner class CommunityViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val communityBackBoxView: ConstraintLayout = itemView.findViewById(R.id.communityBackBox)
        val communityTitleView: TextView = itemView.findViewById(R.id.communityTitle)
        val communityImageView: ImageView = itemView.findViewById(R.id.communityImage)

        fun bind (position: Int) {

            if(position == clickPosition) {
                val drawableBox =
                    ResourcesCompat.getDrawable(context.resources, R.drawable.mindbox_radius_main_20, null)
                communityBackBoxView.background = drawableBox
                communityTitleView.setTextColor(ContextCompat.getColor(context, R.color.main_color))
            } else {
                val drawableBox =
                    ResourcesCompat.getDrawable(context.resources, R.drawable.mindbox_radius_border_20, null)
                communityBackBoxView.background = drawableBox
                communityTitleView.setTextColor(ContextCompat.getColor(context, R.color.custom_gray))
            }

            communityTitleView.text = regionCommunityItems!!.get(position).communityTitle

            if(regionCommunityItems!!.get(position).communityImage != null) {
                Glide.with(context)
                    .load(regionCommunityItems!!.get(position).communityImage)
                    .into(communityImageView)
            } else {
                communityImageView.setImageResource(R.drawable.mindbox_gray)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_menu, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        holder.bind(position)
        var intent : Intent

        holder.itemView.setOnClickListener {
            val regionIntent = Intent(context, UserRegionDataFragment::class.java)
            regionIntent.setAction("COMMUNITY_GROUP_SELECT")
            regionIntent.putExtra("selectPosition", position)
            regionIntent.putExtra("selectCommunity", regionCommunityItems!!.get(position).communityTitle)
            LocalBroadcastManager.getInstance(context).sendBroadcast(regionIntent)

            clickPosition = holder.adapterPosition
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return regionCommunityItems?.size ?: 0
    }

}