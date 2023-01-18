package com.chugnchunon.chungchunon_android.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.chugnchunon.chungchunon_android.Adapter.RegionAdapter
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding.inflate
import com.chugnchunon.chungchunon_android.databinding.ActivityRegionBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionListBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionRegisterBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegionRegisterFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!

    private var regionDB = Firebase.firestore.collection("region")
    private lateinit var adapter: RegionAdapter

    lateinit var regionModel: MyViewModel
    companion object {
        var smallRegionCheck:Boolean = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionListBinding.inflate(inflater, container, false)
        val view = binding.root

        Log.d("결과", "리지온프래그먼트")

        regionModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)

        regionDB.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    for ((k, v) in document.data) {
                        regionModel.addRegion(v.toString())
                    }
                }
                adapter.notifyDataSetChanged()
            }

        regionModel.regionModelData.observe(viewLifecycleOwner) { value ->
            adapter = RegionAdapter(requireActivity(), value, false)
            binding.regionRecycler.adapter = adapter
            binding.regionRecycler.layoutManager = LinearLayoutManager(activity)
        }

        return view

    }

    class MyViewModel : ViewModel() {
//        var selectedRegion = MutableLiveData<String?>()
        var regionModelData = MutableLiveData<List<String>>()
        var regionDataList = regionModelData.value
        var templateList = mutableListOf<String>()

        fun addRegion(text: String) {
            regionDataList?.forEach { data ->
                templateList.add(data.toString())
            }
            templateList.add(text)
            regionModelData.value = templateList
        }
    }
}