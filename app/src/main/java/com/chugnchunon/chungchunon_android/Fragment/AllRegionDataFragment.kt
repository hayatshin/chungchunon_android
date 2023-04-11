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
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.AllDiaryCardAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.tabChange
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryTwoBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.view.*
import kotlinx.android.synthetic.main.fragment_region_data.view.*
import kotlin.collections.ArrayList

class AllRegionDataFragment : Fragment() {

    private var _binding: FragmentRegionDataBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var diarySet: DiaryCard
    private var diaryItems: ArrayList<DiaryCard> = ArrayList()
    private lateinit var adapter: AllDiaryCardAdapter
    lateinit var mcontext: Context

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var allDataLoadingState: AllRegionDataLoadingState


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

        getData()

        binding.recyclerDiary.itemAnimator = null

        binding.swipeRecyclerDiary.visibility = View.VISIBLE
        binding.dataLoadingProgressBar.visibility = View.GONE
//        binding.authLayout.visibility = View.GONE

        allDataLoadingState =
            ViewModelProvider(requireActivity()).get(AllRegionDataLoadingState::class.java)

        allDataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->
            Log.d("지역보기 66", "${allDataLoadingState.loadingCompleteData.value}")

            if (!allDataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        allDataLoadingState.loadingCompleteData.value = false
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        adapter = AllDiaryCardAdapter(requireContext(), diaryItems)

        swipeRefreshLayout = binding.swipeRecyclerDiary

        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            var ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
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
            newNumChangeReceiver,
            IntentFilter("COMMENT_ACTION")
        );

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            newLikeToggleReceiver,
            IntentFilter("LIKE_TOGGLE_ACTION")
        );

        return binding
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(
            blockReloadFragment
        );

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            newNumChangeReceiver
        );

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            newLikeToggleReceiver
        );
    }

    private var newLikeToggleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var toggleDiaryId = intent?.getStringExtra("newDiaryId")
            var newLikeToggle = intent?.getBooleanExtra("newLikeToggle", false)
            var newNumLikes = intent?.getIntExtra("newNumLikes", 0)

            for (diaryItem in diaryItems) {
                if (diaryItem.diaryId == toggleDiaryId) {
                    diaryItem.numLikes = newNumLikes?.toLong()
                }
            }

            adapter.notifyDataSetChanged()
        }
    }

    private var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            diaryItems.clear()
            if (diaryItems.isEmpty()) {
                getData()
            }
        }
    }

    var newNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            var createDiaryId = intent?.getStringExtra("newDiaryId")
            var createNumComments = intent?.getIntExtra("newNumComments", 0)

            for (diaryItem in diaryItems) {
                if (diaryItem.diaryId == createDiaryId) {
                    diaryItem.numComments = createNumComments?.toLong()
                }
            }

            adapter.notifyDataSetChanged()
        }
    }

    private fun getData() {
        diaryDB
            .whereNotEqualTo("blockedBy", "$userId")
            .get()
            ?.addOnSuccessListener { documents ->
                for (document in documents) {
                    var blockedList =
                        document.data.getValue("blockedBy") as ArrayList<String>
                    if (!blockedList.contains(userId)) {
                        // 내가 차단하지 않은 글

                        var secretStatus = document.data.getValue("secret") as Boolean

                        if (secretStatus == false) {
                            var diaryUserId = document.data?.getValue("userId").toString()
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
                                                            DateFormat().convertMillis(timefromdb),
                                                            username,
                                                            stepValue,
                                                            diaryMood,
                                                            diaryDiary,
                                                            numLikes,
                                                            numComments,
                                                            diaryImages,
                                                            false,
                                                        )
                                                        diaryItems.add(diarySet)
                                                        diaryItems.sortWith(compareBy({ it.writeTime }))
                                                        diaryItems.reverse()
//                                                    adapter.notifyDataSetChanged()

                                                        binding.recyclerDiary.adapter = adapter
                                                        binding.recyclerDiary.layoutManager =
                                                            LinearLayoutManagerWrapper(
                                                                mcontext,
                                                                RecyclerView.VERTICAL,
                                                                false
                                                            )
                                                        if (diaryItems.size == 0) {
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
                                                            userId = diaryUserId,
                                                            username = username,
                                                            userAvatar = userAvatar,
                                                            diaryId = diaryId,
                                                            writeTime = DateFormat().convertMillis(
                                                                timefromdb
                                                            ),
                                                            name = username,
                                                            stepCount = stepValue,
                                                            mood = diaryMood,
                                                            diary = diaryDiary,
                                                            numLikes = numLikes,
                                                            numComments = numComments,
                                                            secret = false
                                                        )
                                                        diaryItems.add(diarySet)

                                                        diaryItems.sortWith(compareBy({ it.writeTime }))
                                                        diaryItems.reverse()
//                                                    adapter.notifyDataSetChanged()

                                                        binding.recyclerDiary.adapter = adapter
                                                        binding.recyclerDiary.layoutManager =
                                                            LinearLayoutManagerWrapper(
                                                                mcontext,
                                                                RecyclerView.VERTICAL,
                                                                false
                                                            )
                                                        if (diaryItems.size == 0) {
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
                            }
                        }

                        adapter.notifyDataSetChanged()
                        allDataLoadingState.loadingCompleteData.value =
                            true

//                        Handler().postDelayed({
//                            allDataLoadingState.loadingCompleteData.value =
//                                true
//                        }, 300)

                    }

                }
            }
    }
}


class AllRegionDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
