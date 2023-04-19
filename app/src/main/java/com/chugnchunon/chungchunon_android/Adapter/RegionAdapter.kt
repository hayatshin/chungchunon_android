package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.EditRegionRegisterActivity.Companion.editRegionCheck
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.Fragment.SmallRegionRegisterFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_region.view.*

class RegionAdapter(private var context: Context, private var regionData: List<String>?, smallRegionCheck: Boolean)
    : RecyclerView.Adapter<RegionAdapter.RegionViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid

    var selectedRegion = ""
    var selectedSmallRegion = ""

    companion object {
        val REGION_BROADCAST = "RegionBroadCast"
    }

    inner class RegionViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val regionView: TextView = itemView.findViewById(R.id.regionSelectText)

        fun bind (position: Int) {
            regionView.text = regionData?.get(position).toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_region, parent, false)
        return RegionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        holder.bind(position)
        var intent : Intent

        if(editRegionCheck) {
             intent = Intent(context, EditRegionRegisterActivity::class.java)
        } else {
            intent = Intent(context, RegionRegisterActivity::class.java)
        }
        intent.setAction(REGION_BROADCAST)

        holder.itemView.setOnClickListener { view ->

            if(!smallRegionCheck) {
                // users의 region 값 저장
                selectedRegion = regionData?.get(position).toString()
                smallRegionCheck = true

                Log.d("리지온", "첫번째 $selectedRegion")

                intent.setAction("REGION_BROADCAST")
                intent.putExtra("selectedRegion", selectedRegion)
                intent.putExtra("smallRegionCheck", smallRegionCheck)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                val regionIntent = Intent(context, SmallRegionRegisterFragment::class.java)
                regionIntent.setAction("REGION_REGISTER")
                regionIntent.putExtra("selectedRegion", selectedRegion)
                LocalBroadcastManager.getInstance(context).sendBroadcast(regionIntent)

            }  else {
                // users의 smallRegion 값 저장
                selectedSmallRegion = regionData?.get(position).toString()

                Log.d("리지온", "두번째 $selectedSmallRegion")

                intent.setAction("SMALL_REGION_BROADCAST")
                intent.putExtra("selectedSmallRegion", selectedSmallRegion)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }

        }
    }

    override fun getItemCount(): Int {
        return regionData?.size ?: 0
    }


}