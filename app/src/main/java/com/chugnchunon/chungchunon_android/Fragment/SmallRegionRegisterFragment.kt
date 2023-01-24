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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.chugnchunon.chungchunon_android.Adapter.RegionAdapter
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding.inflate
import com.chugnchunon.chungchunon_android.databinding.ActivityRegionBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionListBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionRegisterBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SmallRegionRegisterFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!

    private var regionDB = Firebase.firestore.collection("region")
    private lateinit var adapter: RegionAdapter
    private var selectedRegion : String = ""
    var regionData: ArrayList<String> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionListBinding.inflate(inflater, container, false)
        val view = binding.root

        Log.d("결과", "스몰리지온프래그먼트")

        var regionModel = ViewModelProvider(requireActivity()).get(SmallRegionRegisterFragment.MyViewModel::class.java)

        regionModel.regionModelData.observe(viewLifecycleOwner) { value ->
            adapter = RegionAdapter(requireActivity(), value, true)
            binding.regionRecycler.adapter = adapter
            binding.regionRecycler.layoutManager = LinearLayoutManager(activity)
        }


        var pref = activity?.getSharedPreferences("REGION_PREF", MODE_PRIVATE)
        selectedRegion = pref?.getString("selectedRegion", "강원도").toString()

        var regionRegister: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                regionModel.clearRegion()

                var changeRegion = intent!!.getStringExtra("selectedRegion").toString()

                Log.d("지역확인", "${changeRegion}")

                regionDB
                    .document("4ggk4cR82mz46CjrLg60")
                    .collection("small_region")
                    .document(changeRegion)
                    .get()
                    .addOnSuccessListener { documents ->

                        documents.data?.forEach { (k, v) ->
                            regionModel.addRegion(v.toString())
                        }
                        Log.d("리지온", "${documents.data}", )
                        adapter.notifyDataSetChanged()
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
                        regionModel.addRegion(v.toString())
                    }
                Log.d("리지온", "${documents.data}", )
                adapter.notifyDataSetChanged()
            }



        return view

    }

    class MyViewModel : ViewModel() {
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

        fun clearRegion() {
            regionModelData.value?.toMutableList()?.clear()
            regionDataList?.toMutableList()?.clear()
            templateList.clear()
        }
    }

}