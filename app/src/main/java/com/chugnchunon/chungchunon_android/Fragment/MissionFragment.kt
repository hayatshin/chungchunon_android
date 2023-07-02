package com.chugnchunon.chungchunon_android.Fragment

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.chugnchunon.chungchunon_android.Adapter.AttractionAdapter
import com.chugnchunon.chungchunon_android.Adapter.MissionCardAdapter
import com.chugnchunon.chungchunon_android.Adapter.YoutubeAdapter
import com.chugnchunon.chungchunon_android.DataClass.Attraction
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.DataClass.Youtube
import com.chugnchunon.chungchunon_android.Layout.CenterZoomLayoutManager
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_mission.view.*


class MissionFragment : Fragment() {

    private var _binding: FragmentMissionBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userId = Firebase.auth.currentUser?.uid

    lateinit var missionSet: Mission
    lateinit var youtubeSet: Youtube

    private var missionList: ArrayList<Mission> = ArrayList()
    private var youtubeItems: ArrayList<Youtube> = ArrayList()

    lateinit var missionAdapter: MissionCardAdapter
    lateinit var youtubeAdapter: YoutubeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMissionBinding.inflate(inflater, container, false)
        val binding = binding.root

        missionAdapter = MissionCardAdapter(requireContext(), missionList)
        binding.missionRecyclerView.adapter = missionAdapter
        binding.missionRecyclerView.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        youtubeAdapter = YoutubeAdapter(requireContext(), youtubeItems)
        binding.youtubeRecyclerView.adapter = youtubeAdapter
        binding.youtubeRecyclerView.layoutManager = CenterZoomLayoutManager(requireContext())

        binding.indicator.setViewPager(binding.missionRecyclerView)
        binding.indicator.createIndicators(missionList.size, 0)
        missionAdapter.registerAdapterDataObserver(binding.indicator.getAdapterDataObserver());

        val initialSpanText = SpannableStringBuilder()
            .color(Color.WHITE) { append("1") }
            .append(" / ${missionList.size}")
        binding.cardIndex.text = initialSpanText

        binding.missionRecyclerView.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                var spanText = SpannableStringBuilder()
                    .color(Color.WHITE) { append("${position + 1}") }
                    .append(" / ${missionList.size}")
                binding.cardIndex.text = spanText
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
//                var spanText = SpannableStringBuilder()
//                    .color(Color.WHITE) { append("${position+1}") }
//                    .append(" / ${missionList.size}")
//                binding.cardIndex.text = spanText
            }
        })

        // 행사
        db.collection("mission")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val allUser = document.data.getValue("allUser") as Boolean
                    val autoProgress = document.data.getValue("autoProgress") as Boolean
                    val missionDocId = document.data.getValue("documentId").toString()
                    val community = document.data.getValue("community").toString()
                    val communityLogo = document.data.getValue("communityLogo").toString()
                    val missionImage = document.data.getValue("missionImage").toString()
                    val title = document.data.getValue("title").toString()
                    val startPeriod = document.data.getValue("startPeriod").toString()
                    val endPeriod = document.data.getValue("endPeriod").toString()
                    val description = document.data.getValue("description").toString()
                    val state = document.data.getValue("state").toString()
                    val goalScore = (document.data.getValue("goalScore") as Long).toInt()
                    val prizeWinners = (document.data.getValue("prizeWinners") as Long).toInt()

                    if (allUser) {
                        missionSet = Mission(
                            missionDocId,
                            community,
                            communityLogo,
                            missionImage,
                            title,
                            startPeriod,
                            endPeriod,
                            description,
                            state,
                            goalScore,
                            autoProgress,
                            prizeWinners
                        )

                        missionList.add(missionSet)
                        missionList.sortWith(compareBy({ it.state }))
//                        missionList.reverse()

                        missionAdapter.notifyDataSetChanged()
                    } else {

                        if (document.contains("fullRegion")) {
                            // 지역 보기
                            val missionFullRegion = document.data?.getValue("fullRegion").toString()

                            db.collection("users")
                                .document("$userId")
                                .get()
                                .addOnSuccessListener { userData ->
                                    val userRegion = userData.data?.getValue("region").toString()
                                    val userSmallRegion =
                                        userData.data?.getValue("smallRegion").toString()
                                    val userFullRegion = "${userRegion} ${userSmallRegion}"

                                    if (userFullRegion == missionFullRegion) {

                                        missionSet = Mission(
                                            missionDocId,
                                            community,
                                            communityLogo,
                                            missionImage,
                                            title,
                                            startPeriod,
                                            endPeriod,
                                            description,
                                            state,
                                            goalScore,
                                            autoProgress,
                                            prizeWinners
                                        )

                                        missionList.add(missionSet)
                                        missionList.sortWith(compareBy({ it.state }))
//                                        missionList.reverse()

                                        missionAdapter.notifyDataSetChanged()
                                    }

                                }

                        }

                        if (document.contains("community")) {
                            // 커뮤니티 보기
                            val missionCommunity = document.data?.getValue("community").toString()

                            db.collection("users")
                                .document("$userId")
                                .get()
                                .addOnSuccessListener { userData ->
                                    val userCommunity =
                                        userData.data?.getValue("community") as ArrayList<String>

                                    if (userCommunity.contains(missionCommunity)) {
                                        missionSet = Mission(
                                            missionDocId,
                                            community,
                                            communityLogo,
                                            missionImage,
                                            title,
                                            startPeriod,
                                            endPeriod,
                                            description,
                                            state,
                                            goalScore,
                                            autoProgress,
                                            prizeWinners
                                        )

                                        missionList.add(missionSet)
                                        missionList.sortWith(compareBy({ it.state }))
//                                        missionList.reverse()

                                        missionAdapter.notifyDataSetChanged()
                                    }
                                }
                        }


                    }

                }
            }

        // 청춘테레비
        db.collection("youtube")
            .get()
            .addOnSuccessListener { youtubeDatas ->
                for (youtubeData in youtubeDatas) {
                    val allUser = youtubeData.data?.getValue("allUser") as Boolean
                    val title = youtubeData.data?.getValue("title").toString()
                    val link = youtubeData.data?.getValue("link").toString()
                    val thumbnail = youtubeData.data?.getValue("thumbnail").toString()
                    val videoId = youtubeData.data?.getValue("videoId").toString()

                    if (allUser) {
                        val title = youtubeData.data?.getValue("title").toString()
                        val link = youtubeData.data?.getValue("link").toString()
                        val thumbnail = youtubeData.data?.getValue("thumbnail").toString()
                        val videoId = youtubeData.data?.getValue("videoId").toString()

                        youtubeSet = Youtube(
                            title,
                            link,
                            thumbnail,
                            videoId
                        )

                        youtubeItems.add(youtubeSet)
                        youtubeAdapter.notifyDataSetChanged()
                    } else {
                        // 전체 공개 아닌 경우

                        if (youtubeData.contains("fullRegion")) {
                            // 지역 보기
                            val tvFullRegion = youtubeData.data?.getValue("fullRegion").toString()
                            db.collection("users")
                                .document("$userId")
                                .get()
                                .addOnSuccessListener { userData ->
                                    val userRegion = userData.data?.getValue("region").toString()
                                    val userSmallRegion =
                                        userData.data?.getValue("smallRegion").toString()
                                    val userFullRegion = "${userRegion} ${userSmallRegion}"

                                    if (userFullRegion == tvFullRegion) {
                                        youtubeSet = Youtube(
                                            title,
                                            link,
                                            thumbnail,
                                            videoId
                                        )

                                        youtubeItems.add(youtubeSet)
                                        youtubeAdapter.notifyDataSetChanged()
                                    }

                                }

                        }

                        if (youtubeData.contains("community")) {
                            // 커뮤니티 보기
                            val tvCommunity = youtubeData.data?.getValue("community").toString()

                            db.collection("users")
                                .document("$userId")
                                .get()
                                .addOnSuccessListener { userData ->
                                    val userCommunity =
                                        userData.data?.getValue("community") as ArrayList<String>

                                    if (userCommunity.contains(tvCommunity)) {
                                        youtubeSet = Youtube(
                                            title,
                                            link,
                                            thumbnail,
                                            videoId
                                        )

                                        youtubeItems.add(youtubeSet)
                                        youtubeAdapter.notifyDataSetChanged()
                                    }
                                }
                        }

                    }
                }
            }

//        userDB
//            .document("$userId")
//            .get()
//            .addOnSuccessListener { userData ->
//                var bigRegion = userData.data?.getValue("region")
//                var smallRegion = userData.data?.getValue("smallRegion")
//                var userRegion = "${bigRegion} ${smallRegion}"
//
//                attractionDB
//                    .get()
//                    .addOnSuccessListener { attractions ->
//                        for(attraction in attractions) {
//                            var attractionRegion = attraction.data.getValue("region")
//                            if(attractionRegion == "전체" || attractionRegion == userRegion) {
//                                val attractionName = attraction.data.getValue("name").toString()
//                                val attractionDescription = attraction.data.getValue("description").toString()
//                                val attractionLocation = attraction.data.getValue("location").toString()
//                                val attractionMainImage = attraction.data.getValue("mainImage").toString()
//                                val attractionSubImage = attraction.data.getValue("subImage") as ArrayList<String>
//
//                                binding.attractionTitle.text = "${userRegion} 명소"
//
//                                attractionSet = Attraction(
//                                    attractionName,
//                                    attractionDescription,
//                                    attractionLocation,
//                                    attractionMainImage,
//                                    attractionSubImage
//                                )
//
//                                attractionList.add(attractionSet)
//                                attractionAdapter.notifyDataSetChanged()
//
//                            }
//                        }
//                    }
//            }
        return binding
    }
}

