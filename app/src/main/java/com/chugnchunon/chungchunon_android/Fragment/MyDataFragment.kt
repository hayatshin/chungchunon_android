package com.chugnchunon.chungchunon_android.Fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import com.chugnchunon.chungchunon_android.Adapter.MyDiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_region_data.view.*
import java.util.ArrayList

class MyDataFragment : Fragment() {

    private var _binding: FragmentRegionDataBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var diarySet: DiaryCard
    private var myDiaryItems: ArrayList<DiaryCard> = ArrayList()
    private lateinit var adapter: MyDiaryCardAdapter
    lateinit var mcontext: Context

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var myDataLoadingState: MyDataLoadingState


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

        binding.recyclerDiary.itemAnimator = null
        swipeRefreshLayout = binding.swipeRecyclerDiary
        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            var ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
        }

        myDataLoadingState =
            ViewModelProvider(requireActivity()).get(MyDataLoadingState::class.java)
        myDataLoadingState.loadingCompleteData.value = false

        myDataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->
            if (!myDataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        adapter = MyDiaryCardAdapter(requireContext(), myDiaryItems)
        binding.recyclerDiary.adapter =
            adapter
        binding.recyclerDiary.layoutManager =
            LinearLayoutManagerWrapper(
                mcontext,
                RecyclerView.VERTICAL,
                false
            )


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

        getData()

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
            val toggleDiaryId = intent?.getStringExtra("newDiaryId")
            val newNumLikes = intent?.getIntExtra("newNumLikes", 0)

            myDiaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == toggleDiaryId) {
                    diaryItem.numLikes = newNumLikes?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }


    private var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            myDiaryItems.clear()
            if (myDiaryItems.isEmpty()) {
                getData()
            }
        }
    }

    var newNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val createDiaryId = intent?.getStringExtra("newDiaryId")
            val createNumComments = intent?.getIntExtra("newNumComments", 0)

            myDiaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == createDiaryId) {
                    diaryItem.numComments = createNumComments?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun getData() {
        diaryDB
            .whereEqualTo("userId", "$userId")
            .get()
            .addOnCompleteListener { task ->
                val documents = task.result

                if (!documents.isEmpty) {
                    // 내 데이터 있는 경우
                    for (document in documents) {
                        val blockedList =
                            document.data.getValue("blockedBy") as ArrayList<*>
                        if (!blockedList.contains((userId))) {
                            // 내가 차단하지 않은 글
                            val secretStatus =
                                document.data.getValue("secret") as Boolean
                            if (secretStatus == false) {
                                // 비밀이 아닌 글 -> 가져올 전체 일기
                                val diaryUserId =
                                    document.data?.getValue("userId").toString()
                                val diaryId =
                                    document.data?.getValue("diaryId").toString()
                                val numLikes =
                                    document.data?.getValue("numLikes") as Long
                                val numComments =
                                    document.data?.getValue("numComments") as Long
                                val timefromdb =
                                    document.data?.getValue("timestamp") as com.google.firebase.Timestamp
                                val timeDate =
                                    DateFormat().convertTimeStampToDate(timefromdb)
                                val diaryMood =
                                    (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long
                                val diaryDiary =
                                    document.data?.getValue("todayDiary").toString()

                                var userFinalId = "default_user"
                                var userFinalName = "탈퇴자"
                                var userFinalAvatar =
                                    "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png"
                                var userFinalStep = 0L

                                // 유저 정보
                                userDB.document(diaryUserId)
                                    .get()
                                    .addOnCompleteListener { userTask ->
                                        val userData = userTask.result
                                        if (userData != null) {
                                            // 유저 정보 o
                                            userFinalId = diaryUserId
                                            userFinalName =
                                                userData.data?.getValue("name")
                                                    .toString()
                                            userFinalAvatar =
                                                userData.data?.getValue("avatar") as String

                                            db.collection("user_step_count")
                                                .document(diaryUserId)
                                                .get()
                                                .addOnCompleteListener { task ->
                                                    var stepDocuments = task.result

                                                    if (stepDocuments.contains(timeDate)) {
                                                        // 해당 기간 걸음수 데이터 o
                                                        val stepDoc =
                                                            stepDocuments.getLong(
                                                                timeDate
                                                            )

                                                        userFinalStep = stepDoc as Long

                                                        // 이미지 여부
                                                        if (document.data.contains("images")) {
                                                            // 이미지 o
                                                            val diaryImages =
                                                                document.data?.getValue(
                                                                    "images"
                                                                ) as ArrayList<String>

                                                            diarySet = DiaryCard(
                                                                userId = userFinalId,
                                                                username = userFinalName,
                                                                userAvatar = userFinalAvatar,
                                                                diaryId = diaryId,
                                                                writeTime = DateFormat().convertMillis(
                                                                    timefromdb
                                                                ),
                                                                stepCount = userFinalStep,
                                                                mood = diaryMood,
                                                                diary = diaryDiary,
                                                                numLikes = numLikes,
                                                                numComments = numComments,
                                                                images = diaryImages,
                                                                secret = false
                                                            )

                                                        } else {
                                                            // 이미지 x
                                                            diarySet = DiaryCard(
                                                                userId = userFinalId,
                                                                username = userFinalName,
                                                                userAvatar = userFinalAvatar,
                                                                diaryId = diaryId,
                                                                writeTime = DateFormat().convertMillis(
                                                                    timefromdb
                                                                ),
                                                                stepCount = userFinalStep,
                                                                mood = diaryMood,
                                                                diary = diaryDiary,
                                                                numLikes = numLikes,
                                                                numComments = numComments,
                                                                secret = false
                                                            )
                                                        }
                                                        myDiaryItems.add(diarySet)
                                                        myDiaryItems.sortWith(compareBy({ it.writeTime }))
                                                        myDiaryItems.reverse()
                                                        adapter.notifyDataSetChanged()

                                                        if (myDiaryItems.size == 0) {
                                                            binding.noItemText.visibility =
                                                                View.VISIBLE
                                                        } else {
                                                            binding.noItemText.visibility =
                                                                View.GONE
                                                            binding.recyclerDiary.visibility =
                                                                View.VISIBLE
                                                        }
                                                        myDataLoadingState.loadingCompleteData.value =
                                                            true
                                                    } else {
                                                        // 해당 기간 걸음수 데이터 x
                                                        if (document.data.contains("images")) {
                                                            // 이미지 o
                                                            val diaryImages =
                                                                document.data?.getValue(
                                                                    "images"
                                                                ) as ArrayList<String>

                                                            diarySet = DiaryCard(
                                                                userId = userFinalId,
                                                                username = userFinalName,
                                                                userAvatar = userFinalAvatar,
                                                                diaryId = diaryId,
                                                                writeTime = DateFormat().convertMillis(
                                                                    timefromdb
                                                                ),
                                                                stepCount = userFinalStep,
                                                                mood = diaryMood,
                                                                diary = diaryDiary,
                                                                numLikes = numLikes,
                                                                numComments = numComments,
                                                                images = diaryImages,
                                                                secret = false
                                                            )

                                                        } else {
                                                            // 이미지 x
                                                            diarySet = DiaryCard(
                                                                userId = userFinalId,
                                                                username = userFinalName,
                                                                userAvatar = userFinalAvatar,
                                                                diaryId = diaryId,
                                                                writeTime = DateFormat().convertMillis(
                                                                    timefromdb
                                                                ),
                                                                stepCount = userFinalStep,
                                                                mood = diaryMood,
                                                                diary = diaryDiary,
                                                                numLikes = numLikes,
                                                                numComments = numComments,
                                                                secret = false
                                                            )
                                                        }
                                                        myDiaryItems.add(diarySet)
                                                        myDiaryItems.sortWith(
                                                            compareBy({ it.writeTime })
                                                        )
                                                        myDiaryItems.reverse()
                                                        adapter.notifyDataSetChanged()

                                                        if (myDiaryItems.size == 0) {
                                                            binding.noItemText.visibility =
                                                                View.VISIBLE
                                                        } else {
                                                            binding.noItemText.visibility =
                                                                View.GONE
                                                            binding.recyclerDiary.visibility =
                                                                View.VISIBLE
                                                        }
                                                        myDataLoadingState.loadingCompleteData.value =
                                                            true
                                                    }
                                                }
                                        } else {
                                            // 유저 정보 x
                                            if (document.data.contains("images")) {
                                                // 이미지 o
                                                val diaryImages =
                                                    document.data?.getValue("images") as ArrayList<String>

                                                diarySet = DiaryCard(
                                                    userId = userFinalId,
                                                    username = userFinalName,
                                                    userAvatar = userFinalAvatar,
                                                    diaryId = diaryId,
                                                    writeTime = DateFormat().convertMillis(
                                                        timefromdb
                                                    ),
                                                    stepCount = userFinalStep,
                                                    mood = diaryMood,
                                                    diary = diaryDiary,
                                                    numLikes = numLikes,
                                                    numComments = numComments,
                                                    images = diaryImages,
                                                    secret = false
                                                )

                                            } else {
                                                // 이미지 x
                                                diarySet = DiaryCard(
                                                    userId = userFinalId,
                                                    username = userFinalName,
                                                    userAvatar = userFinalAvatar,
                                                    diaryId = diaryId,
                                                    writeTime = DateFormat().convertMillis(
                                                        timefromdb
                                                    ),
                                                    stepCount = userFinalStep,
                                                    mood = diaryMood,
                                                    diary = diaryDiary,
                                                    numLikes = numLikes,
                                                    numComments = numComments,
                                                    secret = false
                                                )
                                            }
                                            myDiaryItems.add(diarySet)
                                            myDiaryItems.sortWith(compareBy({ it.writeTime }))
                                            myDiaryItems.reverse()
                                            adapter.notifyDataSetChanged()

                                            if (myDiaryItems.size == 0) {
                                                binding.noItemText.visibility =
                                                    View.VISIBLE
                                            } else {
                                                binding.noItemText.visibility =
                                                    View.GONE
                                                binding.recyclerDiary.visibility =
                                                    View.VISIBLE
                                            }
                                            myDataLoadingState.loadingCompleteData.value =
                                                true
                                        }
                                    }
                            } else {
                                // 비밀 글
                            }
                        } else {
                            // 내가 차단한 글
                        }
                    }
                } else {
                    // 내 데이터 없는 경우
                    binding.swipeRecyclerDiary.visibility = View.GONE
                    binding.noItemText.visibility = View.VISIBLE
                    myDataLoadingState.loadingCompleteData.value = true
                }
            }
    }
}


class MyDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
