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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.AllDiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_diary.*
import kotlinx.android.synthetic.main.fragment_my_diary.*
import java.util.*

class AllDiaryFragment : Fragment() {

    private lateinit var adapter: AllDiaryCardAdapter

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
    lateinit var mcontext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAllDiaryBinding.inflate(inflater, container, false)
        val view = binding.root


        dataLoadingState =
            ViewModelProvider(requireActivity()).get(DataLoadingState::class.java)

        dataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->
            Log.d("지역보기 66", "${dataLoadingState.loadingCompleteData.value}")

            if (!dataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        dataGroupSelection =
            ViewModelProvider(requireActivity()).get(DataGroupSelection::class.java)

        dataGroupSelection.regionCheck.observe(requireActivity(), Observer { value ->
            binding.noItemText.visibility = View.GONE
            getData()
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
                    getData()
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

        adapter = AllDiaryCardAdapter(requireContext(), diaryItems)


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
                    for (document in documents) {
                        var blockedList =
                            document.data.getValue("blockedBy") as ArrayList<String>
                        if (!blockedList.contains(userId)) {
                            var diaryUserId = document.data?.getValue("userId").toString()
                            var username =
                                document.data?.getValue("username").toString()
                            var diaryId = document.data?.getValue("diaryId").toString()
                            var numLikes = document.data?.getValue("numLikes") as Long
                            var numComments =
                                document.data?.getValue("numComments") as Long
                            var timefromdb =
                                document.data?.getValue("timestamp") as com.google.firebase.Timestamp
                            var timeDate = DateFormat().convertTimeStampToDate(timefromdb)
                            var diaryMood =
                                (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long
                            var diaryDiary = document.data?.getValue("todayDiary").toString()

                            db.collection("user_step_count")
                                .document("$diaryUserId")
                                .get()
                                .addOnCompleteListener { task ->
                                    var stepDocuments = task.result
                                    stepDocuments.data?.forEach { stepDoc ->
                                        if (stepDoc.key == timeDate) {
                                            var stepValue = stepDoc.value as Long

                                            // items 추가
                                            diarySet = DiaryCard(
                                                diaryUserId,
                                                username,
                                                null,
                                                diaryId,
                                                DateFormat().convertMillis(timefromdb),
                                                username,
                                                stepValue,
                                                diaryMood,
                                                diaryDiary,
                                                numLikes,
                                                numComments,
                                                null,
                                                false
                                            )
                                            diaryItems.add(diarySet)
                                        }
                                    }
                                    diaryItems.sortWith(compareBy({ it.writeTime }))
                                    diaryItems.reverse()
                                    adapter.notifyDataSetChanged()

                                    binding.recyclerDiary.adapter = adapter
                                    binding.recyclerDiary.layoutManager =
                                        LinearLayoutManagerWrapper(
                                            mcontext,
                                            RecyclerView.VERTICAL,
                                            false
                                        )
                                    if (diaryItems.size == 0) {
                                        binding.noItemText.visibility = View.VISIBLE
                                    } else {
                                        binding.noItemText.visibility = View.GONE
                                    }
                                    dataLoadingState.loadingCompleteData.value = true
                                }
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
                        .addOnCompleteListener { task ->
                            var regionDocuments = task.result
                            if (!regionDocuments.isEmpty) {
                                for (document in regionDocuments) {
                                    var blockedList =
                                        document.data?.getValue("blockedBy") as ArrayList<String>
                                    if (!blockedList.contains(userId)) {
                                        var diaryUserId =
                                            document.data?.getValue("userId").toString()
                                        var username =
                                            document.data?.getValue("username").toString()
                                        var diaryId = document.data?.getValue("diaryId").toString()
                                        var numLikes = document.data?.getValue("numLikes") as Long
                                        var numComments =
                                            document.data?.getValue("numComments") as Long
                                        var timefromdb =
                                            document.data?.getValue("timestamp") as com.google.firebase.Timestamp
                                        var timeDate =
                                            DateFormat().convertTimeStampToDate(timefromdb)
                                        var diaryMood =
                                            (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long
                                        var diaryDiary =
                                            document.data?.getValue("todayDiary").toString()

                                        db.collection("user_step_count")
                                            .document("$diaryUserId")
                                            .get()
                                            .addOnCompleteListener { task ->
                                                var stepDocuments = task.result
                                                stepDocuments.data?.forEach { stepDoc ->
                                                    if (stepDoc.key == timeDate) {
                                                        var stepValue = stepDoc.value as Long

                                                        // items 추가
                                                        diarySet = DiaryCard(
                                                            diaryUserId,
                                                            username,
                                                            null,
                                                            diaryId,
                                                            DateFormat().convertMillis(timefromdb),
                                                            username,
                                                            stepValue,
                                                            diaryMood,
                                                            diaryDiary,
                                                            numLikes,
                                                            numComments,
                                                            null,
                                                            false
                                                        )
                                                        diaryItems.add(diarySet)
                                                    }
                                                }

                                                diaryItems.sortWith(compareBy({ it.writeTime }))
                                                diaryItems.reverse()
                                                adapter.notifyDataSetChanged()

                                                binding.recyclerDiary.adapter = adapter
                                                binding.recyclerDiary.layoutManager =
                                                    LinearLayoutManagerWrapper(
                                                       mcontext,
                                                        RecyclerView.VERTICAL,
                                                        false
                                                    )
                                                binding.noItemText.visibility = View.GONE
                                                dataLoadingState.loadingCompleteData.value = true
                                            }
                                    }
                                }
                            } else {
                                // 다이어리가 비었을 때
                                adapter.notifyDataSetChanged()
                                binding.noItemText.visibility = View.VISIBLE
                                dataLoadingState.loadingCompleteData.value = true
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
