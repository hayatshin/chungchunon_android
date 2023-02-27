package com.chugnchunon.chungchunon_android.Fragment

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.chugnchunon.chungchunon_android.Adapter.BooksAdapter
import com.chugnchunon.chungchunon_android.Adapter.DisplayPhotosAdapter
import com.chugnchunon.chungchunon_android.Adapter.MissionCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.Book
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.Layout.CenterZoomLayoutManager
import com.chugnchunon.chungchunon_android.Layout.LinePagerIndicatorDecoration
import com.chugnchunon.chungchunon_android.databinding.FragmentMissionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_mission.view.*


class MissionFragment: Fragment() {

    private var _binding: FragmentMissionBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val missionDB = db.collection("mission")
    private val booksDB = db.collection("books")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var missionSet: Mission
    lateinit var bookSet: Book

    private var missionList: ArrayList<Mission> = ArrayList()
    private var bookList: ArrayList<Book> = ArrayList()

    lateinit var missionAdapter: MissionCardAdapter
    lateinit var booksAdapter: BooksAdapter

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


        // 책
        booksAdapter = BooksAdapter(requireContext(), bookList)
        binding.bookRecyclerView.adapter = booksAdapter
        binding.bookRecyclerView.layoutManager = CenterZoomLayoutManager(requireContext())

        booksDB.get().addOnSuccessListener { documents ->
            for (document in documents) {
                var title = document.data.getValue("title").toString()
                var cover = document.data.getValue("cover").toString()
                var author = document.data.getValue("author").toString()
                var description = document.data.getValue("description").toString()

                bookSet = Book(
                    title,
                    cover,
                    author,
                    description
                )

                bookList.add(bookSet)
                booksAdapter.notifyDataSetChanged()
            }
        }

        return binding
    }
}

