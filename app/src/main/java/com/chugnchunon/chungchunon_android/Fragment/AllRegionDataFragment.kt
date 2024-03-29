package com.chugnchunon.chungchunon_android.Fragment

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.AllDiaryCardAdapter
import com.chugnchunon.chungchunon_android.Adapter.DisplayPhotosAdapter
import com.chugnchunon.chungchunon_android.CommentActivity
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.EnlargeImageActivity
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.diary_card_two.view.*
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

    private var notificationDiaryId: String = ""
    private var notificationCommentId: String = ""


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionDataBinding.inflate(inflater, container, false)
        val binding = binding.root

        binding.communitySelectRecycler.visibility = View.GONE

        // 코멘트 노티피케이션
        notificationDiaryId =
            activity?.intent?.getStringExtra("notificationDiaryId").toString()
        notificationCommentId =
            activity?.intent?.getStringExtra("notificationCommentId").toString()

        if (activity?.intent?.hasExtra("notificationDiaryId") == true) {
            val openComment = Intent(requireActivity(), CommentActivity::class.java)
            openComment.putExtra("notificationDiaryId", notificationDiaryId)
            openComment.putExtra("notificationCommentId", notificationCommentId)
            startActivity(openComment)
        }

        // 스와이프
        binding.recyclerDiary.itemAnimator = null
        swipeRefreshLayout = binding.swipeRecyclerDiary
        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            val ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
        }

        // 로딩 상태
        allDataLoadingState =
            ViewModelProvider(requireActivity()).get(AllRegionDataLoadingState::class.java)
        allDataLoadingState.loadingCompleteData.value = false
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        allDataLoadingState.loadingCompleteData.observe(
            requireActivity(),
            Observer { value ->
                if (!allDataLoadingState.loadingCompleteData.value!!) {
                    binding.swipeRecyclerDiary.visibility = View.GONE
                    binding.dataLoadingProgressBar.visibility = View.VISIBLE
                } else {
                    binding.swipeRecyclerDiary.visibility = View.VISIBLE
                    binding.dataLoadingProgressBar.visibility = View.GONE
                }
            })

        // 어댑터 연결
        adapter = AllDiaryCardAdapter(requireActivity(), diaryItems)
        binding.recyclerDiary.adapter = adapter
        binding.recyclerDiary.layoutManager = LinearLayoutManagerWrapper(
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

    override fun onResume() {
        super.onResume()
        resumePause = false
    }

    override fun onPause() {
        super.onPause()
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
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val toggleDiaryId = intent?.getStringExtra("newDiaryId")
            val newLikeToggle = intent?.getBooleanExtra("newLikeToggle", true) as Boolean
            val newNumLikes = intent?.getIntExtra("newNumLikes", 0)
            val DiaryRef = diaryDB.document("$toggleDiaryId")

//            val newLikeNum = hashMapOf(
//                "numLikes" to newNumLikes
//            )
//            DiaryRef.set(newLikeNum, SetOptions.merge())

            val likeUserSet = hashMapOf(
                "id" to userId,
                "timestamp" to FieldValue.serverTimestamp(),
                "diaryId" to toggleDiaryId,
            )

            if (newLikeToggle) {
                // 좋아요 누른 경우
                diaryDB.document("$toggleDiaryId")
                    .collection("likes").document("$userId")
                    .set(likeUserSet, SetOptions.merge())

            } else {
                // 좋아요 해제한 경우
                diaryDB.document("$toggleDiaryId").collection("likes")
                    .document("$userId")
                    .delete()
            }

            diaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == toggleDiaryId) {
                    diaryItem.numLikes = newNumLikes?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
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
            val createDiaryId = intent?.getStringExtra("newDiaryId")
            val createNumComments = intent?.getIntExtra("newNumComments", 0)

            diaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == createDiaryId) {
                    diaryItem.numComments = createNumComments?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    fun getData() {
        diaryDB.get().addOnSuccessListener { diaryTotalDocuments ->
            val lastIndex = diaryTotalDocuments.size() - 1

            // 다이어리 모두 가져오기
            for ((index, document) in diaryTotalDocuments.withIndex()) {
                val isLastItem = index == lastIndex

                val blockedList = document.data.getValue("blockedBy") as ArrayList<*>
                val diaryUserId = document.data?.getValue("userId").toString()

                if (!blockedList.contains((userId)) && !diaryUserId.startsWith("notice:")) {

                    // 내가 차단하지 않은 글
                    val secretStatus = document.data.getValue("secret") as Boolean
                    if (!secretStatus && !document.data.containsKey("forceSecret")) {
                        // 비밀이 아닌 글 -> 가져올 전체 일기
                        val diaryId = document.data?.getValue("diaryId").toString()
                        val numLikes = document.data?.getValue("numLikes") as Long
                        val numComments =
                            document.data?.getValue("numComments") as Long
                        val timefromdb =
                            document.data?.getValue("timestamp") as com.google.firebase.Timestamp
                        val timeDate = DateFormat().convertTimeStampToDate(timefromdb)
                        val diaryMood =
                            (document.data?.getValue("todayMood") as Map<*, *>)["position"] as Long
                        val diaryDiary = document.data?.getValue("todayDiary").toString()

                        var userFinalId = "default_user"
                        var userFinalName = "탈퇴자"
                        var userFinalAvatar =
                            "https://postfiles.pstatic.net/MjAyMzA1MTdfNTEg/MDAxNjg0MzAwMTE1NTg4.Ut_2NzdCmpjurruKjSwqWSH-c0_ONiJZM2Mn-ib-uSQg.qX8hjpYrVpE6Nlnnmcs1J780Ycwnl4WIuMLX-tpgVT8g.PNG.hayat_shin/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-05-17_%EC%98%A4%ED%9B%84_2.08.31.png?type=w773"
                        var userFinalStep = 0L

                        // 유저 정보
                        userDB.document(diaryUserId)
                            .get()
                            .addOnCompleteListener { userTask ->
                                val userData = userTask.result
                                if (userData != null && userData.data?.getValue("name") != null) {

                                    // 유저 정보 o
                                    userFinalId = diaryUserId
                                    userFinalName = userData.data?.getValue("name").toString()
                                    userFinalAvatar =
                                        userData.data?.getValue("avatar") as String

                                    db.collection("user_step_count")
                                        .document(diaryUserId)
                                        .get()
                                        .addOnCompleteListener { task ->
                                            var stepDocuments = task.result

                                            if (stepDocuments.contains(timeDate)) {
                                                // 해당 기간 걸음수 데이터 o
                                                val stepDoc = stepDocuments.getLong(timeDate)

                                                userFinalStep = stepDoc as Long

                                                // 이미지 여부
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
                                                        secret = false,
                                                        forceSecret = false,
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
                                                        secret = false,
                                                        forceSecret = false,
                                                    )
                                                }
                                                diaryItems.add(diarySet)
                                                diaryItems.sortWith(compareBy({ it.writeTime }))
                                                diaryItems.reverse()
                                                adapter.notifyDataSetChanged()

                                                binding.noItemText.visibility =
                                                    View.GONE
                                                binding.recyclerDiary.visibility = View.VISIBLE

                                                allDataLoadingState.loadingCompleteData.value =
                                                    true

                                                if (isLastItem) {
                                                    Handler().postDelayed({
                                                        if (diaryItems.size == 0) {
                                                            allDataLoadingState.loadingCompleteData.value =
                                                                true
                                                            binding.noItemText.visibility =
                                                                View.VISIBLE
                                                            binding.recyclerDiary.visibility =
                                                                View.GONE
                                                        }
                                                    }, 2000)
                                                }

                                                if (activity?.intent?.hasExtra("notificationDiaryId") == true) {
                                                    if (diaryItems.size != 0) {
                                                        diaryItems.forEachIndexed { index, diaryItem ->
                                                            if (diaryItem.diaryId == notificationDiaryId) {
                                                                binding.recyclerDiary.scrollToPosition(
                                                                    index
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // 해당 기간 걸음수 데이터 x
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
                                                        secret = false,
                                                        forceSecret = false,
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
                                                        secret = false,
                                                        forceSecret = false,
                                                    )
                                                }
                                                diaryItems.add(diarySet)
                                                diaryItems.sortWith(compareBy({ it.writeTime }))
                                                diaryItems.reverse()
                                                adapter.notifyDataSetChanged()

                                                allDataLoadingState.loadingCompleteData.value =
                                                    true
                                                binding.noItemText.visibility =
                                                    View.GONE
                                                binding.recyclerDiary.visibility = View.VISIBLE

                                                if (isLastItem) {
                                                    Handler().postDelayed({
                                                        if (diaryItems.size == 0) {
                                                            allDataLoadingState.loadingCompleteData.value =
                                                                true
                                                            binding.noItemText.visibility =
                                                                View.VISIBLE
                                                            binding.recyclerDiary.visibility =
                                                                View.GONE
                                                        }
                                                    }, 2000)
                                                }

                                                if (activity?.intent?.hasExtra("notificationDiaryId") == true) {
                                                    if (diaryItems.size != 0) {
                                                        diaryItems.forEachIndexed { index, diaryItem ->
                                                            if (diaryItem.diaryId == notificationDiaryId) {
                                                                binding.recyclerDiary.scrollToPosition(
                                                                    index
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                } else {
                                    // 유저 정보 x

                                    if (isLastItem) {
                                        Handler().postDelayed({
                                            if (diaryItems.size == 0) {
                                                allDataLoadingState.loadingCompleteData.value =
                                                    true
                                                binding.noItemText.visibility =
                                                    View.VISIBLE
                                                binding.recyclerDiary.visibility =
                                                    View.GONE
                                            }
                                        }, 2000)
                                    }

                                }
                            }
                    } else {
                        // 비밀 글
                        if (isLastItem) {
                            Handler().postDelayed({
                                if (diaryItems.size == 0) {
                                    allDataLoadingState.loadingCompleteData.value =
                                        true
                                    binding.noItemText.visibility =
                                        View.VISIBLE
                                    binding.recyclerDiary.visibility =
                                        View.GONE
                                }
                            }, 2000)
                        }
                    }
                } else {
                    // 내가 차단한 글

                    if (isLastItem) {
                        Handler().postDelayed({
                            if (diaryItems.size == 0) {
                                allDataLoadingState.loadingCompleteData.value =
                                    true
                                binding.noItemText.visibility =
                                    View.VISIBLE
                                binding.recyclerDiary.visibility =
                                    View.GONE
                            }
                        }, 2000)
                    }
                }
            }
        }
    }
}

@Keep
class AllRegionDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
