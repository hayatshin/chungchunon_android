package com.chugnchunon.chungchunon_android.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.Adapter.MissionCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.Layout.CenterZoomLayoutManager
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mig35.carousellayoutmanager.CarouselLayoutManager
import com.mig35.carousellayoutmanager.CarouselZoomPostLayoutListener
import com.mig35.carousellayoutmanager.CenterScrollListener
import kotlinx.android.synthetic.main.fragment_mission.view.*


class MissionFragment: Fragment() {

    private var _binding: FragmentMissionBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val missionDB = db.collection("mission")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var missionSet: Mission
    private var missionList: ArrayList<Mission> = ArrayList()
    private lateinit var adapter: MissionCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMissionBinding.inflate(inflater, container, false)
        val binding = binding.root


        adapter = MissionCardAdapter(requireContext(), missionList)
        binding.missionRecyclerView.adapter = adapter

        var centerZoomLayoutManager = CenterZoomLayoutManager(requireActivity())
        binding.missionRecyclerView.layoutManager = centerZoomLayoutManager

        missionDB.get()
            .addOnSuccessListener { documents ->
                for(document in documents) {
                    var community = document.data.getValue("community").toString()
                    var communityLogo = document.data.getValue("communityLogo").toString()
                    var missionImage = document.data.getValue("missionImage").toString()
                    var title = document.data.getValue("title").toString()
                    var startPeriod = document.data.getValue("startPeriod").toString()
                    var endPeriod = document.data.getValue("endPeriod").toString()
                    var description = document.data.getValue("description").toString()
                    var state = document.data.getValue("state").toString()

                    missionSet = Mission(
                        community,
                        communityLogo,
                        missionImage,
                        title,
                        startPeriod,
                        endPeriod,
                        description,
                        state
                    )

                    missionList.add(missionSet)
                    missionList.sortWith(compareBy({ it.state }))
                    missionList.reverse()

                    adapter.notifyDataSetChanged()

                }
            }

        return binding
    }
}

