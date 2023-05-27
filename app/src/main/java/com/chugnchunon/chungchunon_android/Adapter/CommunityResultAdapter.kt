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
import androidx.core.content.ContextCompat
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
import kotlinx.android.synthetic.main.item_community_result.view.*

class CommunityResultAdapter(private var context: Context, private var selectedCommunityItems: ArrayList<String>?)
    : RecyclerView.Adapter<CommunityResultAdapter.CommunityViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid

    inner class CommunityViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val communityResultView: TextView = itemView.findViewById(R.id.communityResult)

        fun bind (position: Int) {
        communityResultView.text = spanTextFn(selectedCommunityItems!!.get(position))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_result, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        holder.bind(position)
        var intent : Intent

        holder.itemView.communityRemoveButton.setOnClickListener {

            val regionIntent = Intent(context, CommunityRegisterActivity::class.java)
            regionIntent.setAction("COMMUNITY_DELETE")
            regionIntent.putExtra("deletePosition", position)
            LocalBroadcastManager.getInstance(context).sendBroadcast(regionIntent)
        }
    }

    override fun getItemCount(): Int {
        return selectedCommunityItems?.size ?: 0
    }

    private fun spanTextFn(text: String): Spannable {
        val spanText = Spannable.Factory.getInstance().newSpannable(text)
        val color = ContextCompat.getColor(context, R.color.light_main_color)
        spanText.setSpan(
            BackgroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spanText
    }
}