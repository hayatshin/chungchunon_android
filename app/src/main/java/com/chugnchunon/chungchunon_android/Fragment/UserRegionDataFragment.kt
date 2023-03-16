package com.chugnchunon.chungchunon_android.Fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DataClass.Region
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.view.*
import kotlinx.android.synthetic.main.fragment_region_data.view.*
import java.util.ArrayList

class UserRegionDataFragment : Fragment() {

    private var _binding: FragmentRegionDataBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var diarySet: DiaryCard
    private var userRegionDiaryItems: ArrayList<DiaryCard> = ArrayList()
    private lateinit var adapter: RegionDiaryCardAdapter
    lateinit var mcontext: Context

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var userDataLoadingState: UserRegionDataLoadingState

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    override fun onResume() {
        super.onResume()
        resumePause = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionDataBinding.inflate(inflater, container, false)
        val binding = binding.root

        if (resumePause == false) {
            userRegionDiaryItems.clear()
            getData()
        }

        binding.recyclerDiary.itemAnimator = null

        userDataLoadingState =
            ViewModelProvider(requireActivity()).get(UserRegionDataLoadingState::class.java)

        userDataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->
            Log.d("지역보기 66", "${userDataLoadingState.loadingCompleteData.value}")

            if (!userDataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        userDataLoadingState.loadingCompleteData.value = false
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        adapter = RegionDiaryCardAdapter(requireContext(), userRegionDiaryItems)

        swipeRefreshLayout = binding.swipeRecyclerDiary

        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            var ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
        }

        var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                userRegionDiaryItems.clear()
                if (userRegionDiaryItems.isEmpty()) {
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


        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            deleteNumChangeReceiver,
            IntentFilter("DELETE_ACTION")
        );

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            createNumChangeReceiver,
            IntentFilter("CREATE_ACTION")
        );

//        getData()

        return binding
    }


    var deleteNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var deleteNumComments = intent?.getIntExtra("deleteNumComments", 0)
            var deleteDiaryPosition = intent?.getIntExtra("deleteDiaryPosition", 0)

            userRegionDiaryItems[deleteDiaryPosition!!].numComments = deleteNumComments?.toLong()
            adapter.notifyDataSetChanged()
        }
    }

    var createNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var createNumComments = intent?.getIntExtra("createNumComments", 0)
            var createDiaryPosition = intent?.getIntExtra("createDiaryPosition", 0)

            userRegionDiaryItems[createDiaryPosition!!].numComments = createNumComments?.toLong()
            adapter.notifyDataSetChanged()
        }
    }

    private fun getData() {
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->

                var userRegion = document.data?.getValue("region")
                var userSmallRegion = document.data?.getValue("smallRegion")
                var userRegionGroup = "${userRegion} ${userSmallRegion}"

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

                                    var secretStatus = document.data.getValue("secret") as Boolean

                                    if (secretStatus == false) {


                                        var diaryUserId =
                                            document.data?.getValue("userId").toString()
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

                                        if (document.data.contains("images")) {
                                            // 이미지 포함
                                            var diaryImages =
                                                document.data?.getValue("images") as ArrayList<String>

                                            db.collection("user_step_count")
                                                .document("$diaryUserId")
                                                .get()
                                                .addOnCompleteListener { task ->
                                                    var stepDocuments = task.result
                                                    stepDocuments.data?.forEach { stepDoc ->
                                                        if (stepDoc.key == timeDate) {
                                                            var stepValue = stepDoc.value as Long

                                                            userDB.document("$diaryUserId")
                                                                .get()
                                                                .addOnCompleteListener { userTask ->
                                                                    var userData = userTask.result
                                                                    var username =
                                                                        userData.data?.getValue("name")
                                                                            .toString()
                                                                    var userAvatar =
                                                                        userData.data?.getValue("avatar") as String

                                                                    // items 추가
                                                                    diarySet = DiaryCard(
                                                                        diaryUserId,
                                                                        username,
                                                                        userAvatar,
                                                                        diaryId,
                                                                        DateFormat().convertMillis(
                                                                            timefromdb
                                                                        ),
                                                                        username,
                                                                        stepValue,
                                                                        diaryMood,
                                                                        diaryDiary,
                                                                        numLikes,
                                                                        numComments,
                                                                        diaryImages,
                                                                        false
                                                                    )
                                                                    userRegionDiaryItems.add(
                                                                        diarySet
                                                                    )

                                                                    userRegionDiaryItems.sortWith(
                                                                        compareBy({ it.writeTime })
                                                                    )
                                                                    userRegionDiaryItems.reverse()
                                                                    adapter.notifyDataSetChanged()

                                                                    binding.recyclerDiary.adapter =
                                                                        adapter
                                                                    binding.recyclerDiary.layoutManager =
                                                                        LinearLayoutManagerWrapper(
                                                                            mcontext,
                                                                            RecyclerView.VERTICAL,
                                                                            false
                                                                        )
                                                                    if (userRegionDiaryItems.size == 0) {
                                                                        binding.noItemText.visibility =
                                                                            View.VISIBLE
                                                                    } else {
                                                                        binding.noItemText.visibility =
                                                                            View.GONE
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }

                                        } else {
                                            // 이미지 미포함
                                            db.collection("user_step_count")
                                                .document("$diaryUserId")
                                                .get()
                                                .addOnCompleteListener { task ->
                                                    var stepDocuments = task.result
                                                    stepDocuments.data?.forEach { stepDoc ->
                                                        if (stepDoc.key == timeDate) {
                                                            var stepValue = stepDoc.value as Long

                                                            userDB.document("$diaryUserId")
                                                                .get()
                                                                .addOnCompleteListener { userTask ->
                                                                    var userData = userTask.result
                                                                    var username =
                                                                        userData.data?.getValue("name")
                                                                            .toString()
                                                                    var userAvatar =
                                                                        userData.data?.getValue("avatar") as String

                                                                    // items 추가
                                                                    diarySet = DiaryCard(
                                                                        diaryUserId,
                                                                        username,
                                                                        userAvatar,
                                                                        diaryId,
                                                                        DateFormat().convertMillis(
                                                                            timefromdb
                                                                        ),
                                                                        username,
                                                                        stepValue,
                                                                        diaryMood,
                                                                        diaryDiary,
                                                                        numLikes,
                                                                        numComments,
                                                                        null,
                                                                        false
                                                                    )
                                                                    userRegionDiaryItems.add(
                                                                        diarySet
                                                                    )

                                                                    userRegionDiaryItems.sortWith(
                                                                        compareBy({ it.writeTime })
                                                                    )
                                                                    userRegionDiaryItems.reverse()
                                                                    adapter.notifyDataSetChanged()

                                                                    binding.recyclerDiary.adapter =
                                                                        adapter
                                                                    binding.recyclerDiary.layoutManager =
                                                                        LinearLayoutManagerWrapper(
                                                                            mcontext,
                                                                            RecyclerView.VERTICAL,
                                                                            false
                                                                        )
                                                                    binding.noItemText.visibility =
                                                                        View.GONE
                                                                }
                                                        }
                                                    }
                                                }
                                        }
                                    }

                                }
                            }

                        } else {
                            // 다이어리가 비었을 때

                            adapter.notifyDataSetChanged()
                            binding.swipeRecyclerDiary.visibility = View.GONE
                            binding.noItemText.visibility = View.VISIBLE
                        }

                        userDataLoadingState.loadingCompleteData.value =
                            true
                    }
            }
    }
}


class UserRegionDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
