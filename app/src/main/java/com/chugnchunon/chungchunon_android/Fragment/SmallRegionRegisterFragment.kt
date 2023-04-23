package com.chugnchunon.chungchunon_android.Fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.chugnchunon.chungchunon_android.Adapter.RegionAdapter
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionListBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.Collator
import java.util.*
import kotlin.collections.ArrayList

class SmallRegionRegisterFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!

    private var regionDB = Firebase.firestore.collection("region")
    private lateinit var adapter: RegionAdapter
    private var selectedRegion: String = ""
    var regionData: ArrayList<String> = ArrayList()

    val koreanCollator = Collator.getInstance(Locale.KOREAN)
    val smallRegionItems = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionListBinding.inflate(inflater, container, false)
        val view = binding.root

        adapter = RegionAdapter(requireActivity(), smallRegionItems, true)
        binding.regionRecycler.adapter = adapter
        binding.regionRecycler.layoutManager = LinearLayoutManager(activity)
        adapter.notifyDataSetChanged()

        val pref = activity?.getSharedPreferences("REGION_PREF", MODE_PRIVATE)
        selectedRegion = pref?.getString("selectedRegion", "강원도").toString()

        val regionRegister: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                smallRegionItems.clear()

                val changeRegion = intent!!.getStringExtra("selectedRegion").toString()

                regionDB
                    .document("4ggk4cR82mz46CjrLg60")
                    .collection("small_region")
                    .document(changeRegion)
                    .get()
                    .addOnSuccessListener { documents ->

                        documents.data?.forEach { (k, v) ->
                            smallRegionItems.add(v.toString())
                            smallRegionItems.sortWith { a, b -> koreanCollator.compare(a, b) }
                            adapter.notifyDataSetChanged()
                        }
                    }

            }
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            regionRegister,
            IntentFilter("REGION_REGISTER")
        )

        regionDB
            .document("4ggk4cR82mz46CjrLg60")
            .collection("small_region")
            .document(selectedRegion)
            .get()
            .addOnSuccessListener { documents ->
                documents.data?.forEach { (k, v) ->
                    smallRegionItems.add(v.toString())
                    smallRegionItems.sortWith { a, b -> koreanCollator.compare(a, b) }
                    adapter.notifyDataSetChanged()
                }
            }

        return view

    }
}