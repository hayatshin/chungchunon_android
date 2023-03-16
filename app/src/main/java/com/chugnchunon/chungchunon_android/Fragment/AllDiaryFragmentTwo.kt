package com.chugnchunon.chungchunon_android.Fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryTwoBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.*
import kotlinx.android.synthetic.main.activity_diary.viewPager
import kotlinx.android.synthetic.main.fragment_all_diary.*
import kotlinx.android.synthetic.main.fragment_all_diary_two.*
import kotlinx.android.synthetic.main.fragment_my_diary.*
import java.util.*

class AllDiaryFragmentTwo : Fragment() {

    private lateinit var adapter: RegionDiaryAdapter

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")

    private val userId = Firebase.auth.currentUser?.uid

    private lateinit var username: String
    private lateinit var userStepCount: String

    lateinit var diarySet: DiaryCard

    private var _binding: FragmentAllDiaryTwoBinding? = null
    private val binding get() = _binding!!

    private var diaryItems: ArrayList<DiaryCard> = ArrayList()
    private var sortItems: ArrayList<DiaryCard> = ArrayList()


    lateinit var dataGroupSelection: DataGroupSelection
    var order = 0
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    lateinit var mcontext: Context

    companion object {
        var resumePause : Boolean = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAllDiaryTwoBinding.inflate(inflater, container, false)
        val view = binding.root

        var diaryType = arguments?.getString("diaryType")

        Log.d("수정수정", "$diaryType")

        if(diaryType == "all") {
            binding.regionViewPager.currentItem = 0
        } else if (diaryType == "region") {
            binding.regionViewPager.currentItem = 1
        } else if (diaryType == "my") {
            binding.regionViewPager.currentItem = 2
        }

        val adapter = RegionDiaryAdapter(requireActivity())
        binding.regionViewPager.offscreenPageLimit = 3
        binding.regionViewPager.adapter = adapter
        binding.regionViewPager.isUserInputEnabled = false

        binding.regionViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.regionTabLayout.selectTab(binding.regionTabLayout.getTabAt(position))
            }
        })

        binding.regionTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.regionViewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        return view
    }

}

