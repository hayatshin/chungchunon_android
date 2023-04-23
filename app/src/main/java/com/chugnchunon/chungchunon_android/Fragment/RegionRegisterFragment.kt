package com.chugnchunon.chungchunon_android.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.chugnchunon.chungchunon_android.Adapter.RegionAdapter
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionListBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.Collator
import java.util.*
import kotlin.collections.ArrayList

class RegionRegisterFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!

    private var regionDB = Firebase.firestore.collection("region")
    private lateinit var adapter: RegionAdapter

    private var regionItems = ArrayList<String>()

    companion object {
        var smallRegionCheck:Boolean = false
    }

    val koreanCollator = Collator.getInstance(Locale.KOREAN)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionListBinding.inflate(inflater, container, false)
        val view = binding.root

        adapter = RegionAdapter(requireActivity(), regionItems, false)
        binding.regionRecycler.adapter = adapter
        binding.regionRecycler.layoutManager = LinearLayoutManager(activity)

        regionDB.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    for ((k, v) in document.data) {
                        regionItems.add(v.toString())
                        regionItems.sortWith{ a, b -> koreanCollator.compare(a, b)}
                        adapter.notifyDataSetChanged()
                    }
                }
            }

        return view

    }

}