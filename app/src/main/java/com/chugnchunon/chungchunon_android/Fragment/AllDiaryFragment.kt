package com.chugnchunon.chungchunon_android.Fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.MutableData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_diary.*
import kotlinx.android.synthetic.main.fragment_my_diary.*
import java.lang.Exception
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AllDiaryFragment : Fragment() {

    private lateinit var adapter: DiaryCardAdapter

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")

    private val userId = Firebase.auth.currentUser?.uid

    private lateinit var username: String
    private lateinit var userStepCount: String

    lateinit var diarySet: DiaryCard

    private var _binding: FragmentAllDiaryBinding? = null
    private val binding get() = _binding!!

    private var diaryItems: ArrayList<DiaryCard> = ArrayList()
    private var sortItems: ArrayList<DiaryCard> = ArrayList()


    lateinit var dataGroupSelection: DataGroupSelection
    var order = 0
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    lateinit var dataLoadingState: DataLoadingState


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAllDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        dataLoadingState =
            ViewModelProvider(requireActivity()).get(DataLoadingState::class.java)

        dataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->
            if (!value) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE

            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })


//        var startService = Intent(activity, MyService::class.java)
//        activity?.let { ContextCompat.startForegroundService(it, startService) }

        swipeRefreshLayout = binding.swipeRecyclerDiary

        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            var ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
        }

        var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                diaryItems.clear()
                if (diaryItems.isEmpty()) {

                    Handler().postDelayed({
                        getData()
                    }, 500)

//                    getData()
                    Log.d("차단", "다이어리: ${diaryItems}")
                }
            }
        }


        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            blockReloadFragment,
            IntentFilter("BLOCK_DIARY_INTENT")
        );

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            blockReloadFragment,
            IntentFilter("BLOCK_USER_INTENT")
        );

        // 토글버튼
//        var contexThemeWrappter: Context = ContextThemeWrapper(activity, R.style.MaterialAppTheme)
//        var localinflater = inflater.cloneInContext(contexThemeWrappter)
//        var rootview = localinflater.inflate(R.layout.fragment_all_diary,container, false)

        binding.toggleBtnGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            diaryItems.clear()
            dataLoadingState.loadingCompleteData.value = false

            if (isChecked) {
                when (checkedId) {
                    R.id.allData -> {
                        dataGroupSelection.regionCheck.value = false
                        binding.regionInfo.visibility = View.GONE
                    }
                    R.id.regionData -> {
                        dataGroupSelection.regionCheck.value = true
                        binding.regionInfo.visibility = View.VISIBLE
                    }
                }
            }
        }

        dataGroupSelection =
            ViewModelProvider(requireActivity()).get(DataGroupSelection::class.java)

        dataGroupSelection.regionCheck.observe(requireActivity(), Observer { value ->
            binding.noItemText.visibility = View.GONE

            Handler().postDelayed({
                getData()
            }, 500)
        })


        adapter = DiaryCardAdapter(requireContext(), diaryItems)


        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            deleteNumChangeReceiver,
            IntentFilter("DELETE_ACTION")
        );

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            createNumChangeReceiver,
            IntentFilter("CREATE_ACTION")
        );

        return view
    }


    var deleteNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var deleteNumComments = intent?.getIntExtra("deleteNumComments", 0)
            var deleteDiaryPosition = intent?.getIntExtra("deleteDiaryPosition", 0)

            diaryItems[deleteDiaryPosition!!].numComments = deleteNumComments?.toLong()
            adapter.notifyDataSetChanged()
        }
    }

    var createNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var createNumComments = intent?.getIntExtra("createNumComments", 0)
            var createDiaryPosition = intent?.getIntExtra("createDiaryPosition", 0)

            diaryItems[createDiaryPosition!!].numComments = createNumComments?.toLong()
            adapter.notifyDataSetChanged()
        }
    }


    private fun getData() {

        if (!dataGroupSelection.regionCheck.value!!) {
            // 전체 보기
            var storeBuilder: Query?

            diaryDB
                .whereNotEqualTo("blockedBy", "$userId")
                .get()
                ?.addOnSuccessListener { documents ->
                    dataLoadingState.loadingCompleteData.value = true
                    for (document in documents) {
                        var blockedList =
                            document.data.getValue("blockedBy") as ArrayList<String>
                        if (!blockedList.contains(userId)) {
                            try {
                                var userId = document.data?.getValue("userId").toString()
                                var username =
                                    document.data?.getValue("username").toString()
                                var stepCount = document.data?.getValue("stepCount") as Long
                                var diaryId = document.data?.getValue("diaryId").toString()
                                var numLikes = document.data?.getValue("numLikes") as Long
                                var numComments =
                                    document.data?.getValue("numComments") as Long
                                var timefromdb =
                                    document.data?.getValue("timestamp") as com.google.firebase.Timestamp
                                var imageList =
                                    document.data?.getValue("images") as ArrayList<String>

                                // items 추가
                                diarySet = DiaryCard(
                                    userId,
                                    username,
                                    diaryId,
                                    DateFormat().convertMillis(timefromdb),
                                    username,
                                    stepCount,
                                    (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long,
                                    document.data?.getValue("todayDiary").toString(),
                                    numLikes,
                                    numComments,
                                    imageList
                                )
                            } catch (e: Exception) {
                                var userId = document.data?.getValue("userId").toString()
                                var username =
                                    document.data?.getValue("username").toString()
                                var stepCount = document.data?.getValue("stepCount") as Long
                                var diaryId = document.data?.getValue("diaryId").toString()
                                var numLikes = document.data?.getValue("numLikes") as Long
                                var numComments =
                                    document.data?.getValue("numComments") as Long
                                var timefromdb =
                                    document.data?.getValue("timestamp") as com.google.firebase.Timestamp

                                // items 추가
                                diarySet = DiaryCard(
                                    userId,
                                    username,
                                    diaryId,
                                    DateFormat().convertMillis(timefromdb),
                                    username,
                                    stepCount,
                                    (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long,
                                    document.data?.getValue("todayDiary").toString(),
                                    numLikes,
                                    numComments,
                                )
                            }
                            diaryItems.add(diarySet)
                            diaryItems.sortWith(compareBy({ it.writeTime }))
                            diaryItems.reverse()
                            adapter.notifyDataSetChanged()
                        }

                        binding.recyclerDiary.adapter = adapter
                        binding.recyclerDiary.layoutManager = LinearLayoutManagerWrapper(
                            requireContext(),
                            RecyclerView.VERTICAL,
                            false
                        )
                        if (diaryItems.size == 0) {
                            Log.d("지역보기", "0 임")
                            binding.noItemText.visibility = View.VISIBLE
                        } else {
                            Log.d("지역보기", "0 이 아님")
                            binding.noItemText.visibility = View.GONE
                        }
                    }

                }
        } else {
            // 지역 보기
            userDB.document("$userId")
                .get()
                .addOnSuccessListener { document ->

                    var userRegion = document.data?.getValue("region")
                    var userSmallRegion = document.data?.getValue("smallRegion")
                    var userRegionGroup = "${userRegion} ${userSmallRegion}"
                    binding.regionInfo.text = "${userRegion} ${userSmallRegion}"

                    diaryDB
                        .whereEqualTo("regionGroup", userRegionGroup)
                        .get()
                        .addOnSuccessListener { documents ->
                            dataLoadingState.loadingCompleteData.value = true

                            for (document in documents) {
                                var blockedList =
                                    document.data?.getValue("blockedBy") as ArrayList<String>
                                if (!blockedList.contains(userId)) {
                                    try {
                                        var userId =
                                            document.data?.getValue("userId").toString()
                                        var username =
                                            document.data?.getValue("username").toString()
                                        var stepCount =
                                            document.data?.getValue("stepCount") as Long
                                        var diaryId =
                                            document.data?.getValue("diaryId").toString()
                                        var numLikes =
                                            document.data?.getValue("numLikes") as Long
                                        var numComments =
                                            document.data?.getValue("numComments") as Long
                                        var timefromdb =
                                            document.data?.getValue("timestamp") as com.google.firebase.Timestamp
                                        var imageList =
                                            document.data?.getValue("images") as ArrayList<String>

                                        // items 추가
                                        diarySet = DiaryCard(
                                            userId,
                                            username,
                                            diaryId,
                                            DateFormat().convertMillis(timefromdb),
                                            username,
                                            stepCount,
                                            (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long,
                                            document.data?.getValue("todayDiary").toString(),
                                            numLikes,
                                            numComments,
                                            imageList
                                        )
                                    } catch (e: Exception) {
                                        var userId =
                                            document.data?.getValue("userId").toString()
                                        var username =
                                            document.data?.getValue("username").toString()
                                        var stepCount =
                                            document.data?.getValue("stepCount") as Long
                                        var diaryId =
                                            document.data?.getValue("diaryId").toString()
                                        var numLikes =
                                            document.data?.getValue("numLikes") as Long
                                        var numComments =
                                            document.data?.getValue("numComments") as Long
                                        var timefromdb =
                                            document.data?.getValue("timestamp") as com.google.firebase.Timestamp

                                        // items 추가
                                        diarySet = DiaryCard(
                                            userId,
                                            username,
                                            diaryId,
                                            DateFormat().convertMillis(timefromdb),
                                            username,
                                            stepCount,
                                            (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long,
                                            document.data?.getValue("todayDiary").toString(),
                                            numLikes,
                                            numComments,
                                        )
                                    }

                                    diaryItems.add(diarySet)
                                    diaryItems.sortWith(compareBy({ it.writeTime }))
                                    diaryItems.reverse()
                                    adapter.notifyDataSetChanged()
                                }

                                Log.d("지역보기1", "${diaryItems.size}")
                            }
                            binding.recyclerDiary.adapter = adapter
                            binding.recyclerDiary.layoutManager = LinearLayoutManagerWrapper(
                                requireContext(),
                                RecyclerView.VERTICAL,
                                false
                            )
                            Log.d("지역보기2", "${diaryItems.size}")
                            if (diaryItems.size == 0) {
                                Log.d("지역보기", "0 임")
                                binding.noItemText.visibility = View.VISIBLE
                            } else {
                                Log.d("지역보기", "0 이 아님")
                                binding.noItemText.visibility = View.GONE
                            }
                        }
                }
        }
    }
}

class DataGroupSelection : ViewModel() {
    val regionCheck by lazy { MutableLiveData<Boolean>(false) }
}

class DataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}

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
