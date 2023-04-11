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
import com.chugnchunon.chungchunon_android.DataClass.Attraction
import com.chugnchunon.chungchunon_android.DataClass.Book
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.Layout.CenterZoomLayoutManager
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_mission.view.*


class MissionFragment: Fragment() {

    private var _binding: FragmentMissionBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val missionDB = db.collection("mission")
    private val booksDB = db.collection("books")
    private val attractionDB = db.collection("attraction")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var missionSet: Mission
    lateinit var bookSet: Book
    lateinit var attractionSet: Attraction

    private var missionList: ArrayList<Mission> = ArrayList()
    private var attractionList: ArrayList<Attraction> = ArrayList()

    lateinit var missionAdapter: MissionCardAdapter
    lateinit var attractionAdapter: AttractionAdapter

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

        attractionAdapter = AttractionAdapter(requireContext(), attractionList)
        binding.attractionRecyclerView.adapter = attractionAdapter
        binding.attractionRecyclerView.layoutManager = CenterZoomLayoutManager(requireContext())

        binding.indicator.setViewPager(binding.missionRecyclerView)
        binding.indicator.createIndicators(missionList.size, 0)
        missionAdapter.registerAdapterDataObserver(binding.indicator.getAdapterDataObserver());

        var initialSpanText = SpannableStringBuilder()
            .color(Color.WHITE) { append("1") }
            .append(" / ${missionList.size}")
        binding.cardIndex.text = initialSpanText

        binding.missionRecyclerView.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                var spanText = SpannableStringBuilder()
                    .color(Color.WHITE) { append("${position+1}") }
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

                    missionAdapter.notifyDataSetChanged()

                }
            }


        // 명소
        userDB
            .document("$userId")
            .get()
            .addOnSuccessListener { userData ->
                var bigRegion = userData.data?.getValue("region")
                var smallRegion = userData.data?.getValue("smallRegion")
                var userRegion = "${bigRegion} ${smallRegion}"

                attractionDB
                    .get()
                    .addOnSuccessListener { attractions ->
                        for(attraction in attractions) {
                            var attractionRegion = attraction.data.getValue("region")
                            if(attractionRegion == "전체" || attractionRegion == userRegion) {
                                var attractionName = attraction.data.getValue("name").toString()
                                var attractionDescription = attraction.data.getValue("description").toString()
                                var attractionLocation = attraction.data.getValue("location").toString()
                                var attractionMainImage = attraction.data.getValue("mainImage").toString()
                                var attractionSubImage = attraction.data.getValue("subImage") as ArrayList<String>

                                binding.attractionTitle.text = "${userRegion} 명소"

                                attractionSet = Attraction(
                                    attractionName,
                                    attractionDescription,
                                    attractionLocation,
                                    attractionMainImage,
                                    attractionSubImage
                                )

                                attractionList.add(attractionSet)
                                attractionAdapter.notifyDataSetChanged()

                            }
                        }
                    }
            }

//        booksDB.get().addOnSuccessListener { documents ->
//            for (document in documents) {
//                var title = document.data.getValue("title").toString()
//                var cover = document.data.getValue("cover").toString()
//                var author = document.data.getValue("author").toString()
//                var description = document.data.getValue("description").toString()
//
//                bookSet = Book(
//                    title,
//                    cover,
//                    author,
//                    description
//                )
//
//                bookList.add(bookSet)
//                booksAdapter.notifyDataSetChanged()
//            }
//        }

        return binding
    }
}

