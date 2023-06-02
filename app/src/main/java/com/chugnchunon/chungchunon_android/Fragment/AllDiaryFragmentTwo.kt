package com.chugnchunon.chungchunon_android.Fragment

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DiaryTwoActivity.Companion.diaryType
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryTwoBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

        diaryType = arguments?.getString("diaryType").toString()

        if(diaryType == "all") {
            binding.regionViewPager.currentItem = 0
        } else if (diaryType == "friend") {
            binding.regionViewPager.currentItem = 1
        } else if (diaryType == "region") {
            binding.regionViewPager.currentItem = 2
        } else if (diaryType == "my") {
            binding.regionViewPager.currentItem = 3
        }

        val adapter = RegionDiaryAdapter(requireActivity())
        binding.regionViewPager.offscreenPageLimit = 4
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

        binding.regionViewPager.setCurrentItem(1, false)
        binding.regionTabLayout.selectTab(binding.regionTabLayout.getTabAt(1))

        return view
    }

    override fun onResume() {
        super.onResume()

        binding.regionViewPager.postDelayed(object : Runnable {
            override fun run() {
                if(diaryType == "all") {
                    binding.regionViewPager.setCurrentItem(0, false)
                    binding.regionTabLayout.selectTab(binding.regionTabLayout.getTabAt(0))
                } else if (diaryType == "friend") {
                    binding.regionViewPager.setCurrentItem(1, false)
                    binding.regionTabLayout.selectTab(binding.regionTabLayout.getTabAt(1))
                } else if (diaryType == "region") {
                    binding.regionViewPager.setCurrentItem(2, false)
                    binding.regionTabLayout.selectTab(binding.regionTabLayout.getTabAt(2))
                } else if (diaryType == "my") {
                    binding.regionViewPager.setCurrentItem(3, false)
                    binding.regionTabLayout.selectTab(binding.regionTabLayout.getTabAt(3))
                }
            }
        }, 0)
    }
}

@Keep
class LinearLayoutManagerWrapper : LinearLayoutManager {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}