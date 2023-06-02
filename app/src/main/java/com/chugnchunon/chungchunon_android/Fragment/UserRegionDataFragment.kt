package com.chugnchunon.chungchunon_android.Fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.CommunityMenuAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.Community
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_region_data.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.collections.ArrayList

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

    lateinit var regionCommunitySet: Community
    private var regionCommunityItems: ArrayList<Community> = ArrayList()
    private lateinit var communityMenuAdapter: CommunityMenuAdapter

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)


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

        binding.communitySelectRecycler.visibility = View.VISIBLE

        binding.recyclerDiary.itemAnimator = null
        swipeRefreshLayout = binding.swipeRecyclerDiary
        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            val ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
        }

        userDataLoadingState =
            ViewModelProvider(requireActivity()).get(UserRegionDataLoadingState::class.java)
        userDataLoadingState.loadingCompleteData.value = false
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        userDataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->
            if (!userDataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        adapter = RegionDiaryCardAdapter(requireContext(), userRegionDiaryItems)
        binding.recyclerDiary.adapter = adapter
        binding.recyclerDiary.layoutManager = LinearLayoutManagerWrapper(
            mcontext,
            RecyclerView.VERTICAL,
            false
        )

        communityMenuAdapter = CommunityMenuAdapter(requireContext(), regionCommunityItems, 0)
        binding.communitySelectRecycler.adapter = communityMenuAdapter
        binding.communitySelectRecycler.layoutManager = LinearLayoutManagerWrapper(
            mcontext,
            RecyclerView.HORIZONTAL,
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
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            newLikeToggleReceiver,
            IntentFilter("LIKE_TOGGLE_ACTION")
        );

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            communityGroupSelectReceiver,
            IntentFilter("COMMUNITY_GROUP_SELECT")
        );

        userRegionDiaryItems.clear()
        getMenuData()
        getRegionData()

        Handler().postDelayed({
            userDataLoadingState.loadingCompleteData.value = true

            if (userRegionDiaryItems.size == 0) {
                binding.noItemText.visibility =
                    View.VISIBLE
            } else {
                binding.noItemText.visibility =
                    View.GONE
                binding.recyclerDiary.visibility =
                    View.VISIBLE
            }

        }, 1500)

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
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            communityGroupSelectReceiver
        );
    }

    private var newLikeToggleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val toggleDiaryId = intent?.getStringExtra("newDiaryId")
            val newLikeToggle = intent?.getBooleanExtra("newLikeToggle", true) as Boolean
            val newNumLikes = intent?.getIntExtra("newNumLikes", 0)
            val DiaryRef = diaryDB.document("$toggleDiaryId")

            val newLikeNum = hashMapOf(
                "numLikes" to newNumLikes
            )
            DiaryRef.set(newLikeNum, SetOptions.merge())

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

            userRegionDiaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == toggleDiaryId) {
                    diaryItem.numLikes = newNumLikes?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            userRegionDiaryItems.clear()
            if (userRegionDiaryItems.isEmpty()) {
                getRegionData()
            }
        }
    }

    var newNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val createDiaryId = intent?.getStringExtra("newDiaryId")
            val createNumComments = intent?.getIntExtra("newNumComments", 0)

            userRegionDiaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == createDiaryId) {
                    diaryItem.numComments = createNumComments?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    var communityGroupSelectReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val selectPosition = intent?.getIntExtra("selectPosition", 0)
            val selectCommunity = intent?.getStringExtra("selectCommunity").toString()
            userDataLoadingState.loadingCompleteData.value = false

            if (selectPosition == 0) {
                userRegionDiaryItems.clear()
                adapter.notifyDataSetChanged()
                getRegionData()
            } else {
                userRegionDiaryItems.clear()
                adapter.notifyDataSetChanged()
                getCommunityData(selectCommunity)
            }
        }
    }

    private fun getMenuData() {

        userDB.document("$userId").get()
            .addOnSuccessListener { originUserData ->
                val originUserRegion = originUserData.data?.getValue("region").toString()
                val originUserSmallRegion = originUserData.data?.getValue("smallRegion").toString()
                val userSmallRegionParts = originUserSmallRegion.split(" ")
                val userRegionLastPart = userSmallRegionParts.last()
                val originUserFullRegion = "${originUserRegion} ${originUserSmallRegion}"

                db.collection("contract_region")
                    .document(originUserFullRegion)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val regionImageDocument = task.result
                            if (regionImageDocument != null) {
                                if (regionImageDocument.exists()) {
                                    // 이미지 존재
                                    val regionImage =
                                        regionImageDocument.data?.getValue("regionImage").toString()
                                    regionCommunitySet = Community(
                                        userRegionLastPart,
                                        regionImage
                                    )
                                } else {
                                    regionCommunitySet = Community(
                                        userRegionLastPart,
                                        null
                                    )
                                }
                                regionCommunityItems.add(regionCommunitySet)
                                communityMenuAdapter.notifyDataSetChanged()
                            } else {
                                regionCommunitySet = Community(
                                    userRegionLastPart,
                                    null
                                )
                                regionCommunityItems.add(regionCommunitySet)
                                communityMenuAdapter.notifyDataSetChanged()
                            }

                            db.collection("community")
                                .get()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val communities = task.result
                                        if (communities != null) {
                                            if (!communities.isEmpty) {
                                                // 소속기관 있음
                                                for (communityData in communities) {
                                                    val communityTitle =
                                                        communityData.data.getValue("communityTitle")
                                                            .toString()
                                                    val communityImage =
                                                        communityData.data.getValue("communityImage")
                                                            .toString()
                                                    val communityUsers =
                                                        communityData.data.getValue("users") as ArrayList<String>

                                                    if (communityUsers.contains(userId)) {
                                                        regionCommunitySet = Community(
                                                            communityTitle,
                                                            communityImage
                                                        )
                                                        regionCommunityItems.add(regionCommunitySet)
                                                        communityMenuAdapter.notifyDataSetChanged()
                                                    }
                                                }
                                            } else {
                                                // 소속기관 없음
                                            }
                                        } else {
                                            // 소속기관 없음
                                        }
                                    }
                                }
                        }
                    }
            }
    }

    private fun getCommunityData(selectCommunity: String) {
        diaryDB.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val blockedList =
                        document.data.getValue("blockedBy") as ArrayList<*>

                    if (!blockedList.contains((userId))) {
                        // 내가 차단하지 않은 글
                        val secretStatus = document.data.getValue("secret") as Boolean
                        if (secretStatus == false) {
                            // 비밀이 아닌 글 -> 가져올 전체 일기
                            val diaryUserId =
                                document.data?.getValue("userId").toString()
                            val diaryId = document.data?.getValue("diaryId").toString()
                            val numLikes = document.data?.getValue("numLikes") as Long
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
                                "https://postfiles.pstatic.net/MjAyMzA1MTdfNTEg/MDAxNjg0MzAwMTE1NTg4.Ut_2NzdCmpjurruKjSwqWSH-c0_ONiJZM2Mn-ib-uSQg.qX8hjpYrVpE6Nlnnmcs1J780Ycwnl4WIuMLX-tpgVT8g.PNG.hayat_shin/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-05-17_%EC%98%A4%ED%9B%84_2.08.31.png?type=w773"
                            var userFinalStep = 0L

                            db.collection("community").document(selectCommunity)
                                .get()
                                .addOnSuccessListener { communityData ->
                                    val communityUsers =
                                        communityData.data?.getValue("users") as ArrayList<String>

                                    if (communityUsers.contains(diaryUserId)) {
                                        // 유저 정보
                                        userDB.document(diaryUserId)
                                            .get()
                                            .addOnCompleteListener { userTask ->
                                                val userData = userTask.result
                                                if (userData != null) {
                                                    val userCommunity =
                                                        userData.data?.getValue("community") as ArrayList<String>

                                                    // 유저 정보 o
                                                    userFinalId = diaryUserId
                                                    userFinalName =
                                                        userData.data?.getValue("name").toString()
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
                                                                    stepDocuments.getLong(timeDate)
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
                                                                userRegionDiaryItems.add(diarySet)
                                                                userRegionDiaryItems.sortWith(
                                                                    compareBy({ it.writeTime })
                                                                )
                                                                userRegionDiaryItems.reverse()
                                                                adapter.notifyDataSetChanged()

                                                                if (userRegionDiaryItems.size == 0) {
                                                                    binding.noItemText.visibility =
                                                                        View.VISIBLE
                                                                } else {
                                                                    binding.noItemText.visibility =
                                                                        View.GONE
                                                                    binding.recyclerDiary.visibility =
                                                                        View.VISIBLE
                                                                }
                                                                userDataLoadingState.loadingCompleteData.value =
                                                                    true
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
                                                                userRegionDiaryItems.add(diarySet)
                                                                userRegionDiaryItems.sortWith(
                                                                    compareBy({ it.writeTime })
                                                                )
                                                                userRegionDiaryItems.reverse()
                                                                adapter.notifyDataSetChanged()

                                                                if (userRegionDiaryItems.size == 0) {
                                                                    binding.noItemText.visibility =
                                                                        View.VISIBLE
                                                                } else {
                                                                    binding.noItemText.visibility =
                                                                        View.GONE
                                                                    binding.recyclerDiary.visibility =
                                                                        View.VISIBLE
                                                                }
                                                                userDataLoadingState.loadingCompleteData.value =
                                                                    true
                                                            }

                                                        }
                                                }
                                            }
                                    }
                                }

                        } else {
                            // 비밀 글
                        }
                    } else {
                        // 내가 차단한 글
                    }
                }

            }
    }

    private fun getRegionData() {

        userDB.document("$userId").get()
            .addOnSuccessListener { originUserData ->
                val originUserRegion = originUserData.data?.getValue("region").toString()
                val originUserSmallRegion = originUserData.data?.getValue("smallRegion").toString()
                val originUserFullRegion = "${originUserRegion} ${originUserSmallRegion}"

                diaryDB.get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val blockedList =
                                document.data.getValue("blockedBy") as ArrayList<*>

                            if (!blockedList.contains((userId))) {
                                // 내가 차단하지 않은 글
                                val secretStatus = document.data.getValue("secret") as Boolean
                                if (secretStatus == false) {
                                    // 비밀이 아닌 글 -> 가져올 전체 일기
                                    val diaryUserId =
                                        document.data?.getValue("userId").toString()
                                    val diaryId = document.data?.getValue("diaryId").toString()
                                    val numLikes = document.data?.getValue("numLikes") as Long
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
                                        "https://postfiles.pstatic.net/MjAyMzA1MTdfNTEg/MDAxNjg0MzAwMTE1NTg4.Ut_2NzdCmpjurruKjSwqWSH-c0_ONiJZM2Mn-ib-uSQg.qX8hjpYrVpE6Nlnnmcs1J780Ycwnl4WIuMLX-tpgVT8g.PNG.hayat_shin/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-05-17_%EC%98%A4%ED%9B%84_2.08.31.png?type=w773"
                                    var userFinalStep = 0L

                                    // 유저 정보
                                    userDB.document(diaryUserId)
                                        .get()
                                        .addOnCompleteListener { userTask ->
                                            val userData = userTask.result
                                            if (userData != null) {

                                                val diaryUserRegion =
                                                    userData.data?.getValue("region").toString()
                                                val diaryUserSmallRegion =
                                                    userData.data?.getValue("smallRegion")
                                                        .toString()
                                                val diaryUserFullRegion =
                                                    "${diaryUserRegion} ${diaryUserSmallRegion}"

                                                if ((diaryUserFullRegion == originUserFullRegion || diaryUserId == "kakao:2358828971")
                                                    && !diaryUserId.startsWith("notice:community")
                                                ) {

                                                    // 유저 정보 o
                                                    userFinalId = diaryUserId
                                                    userFinalName =
                                                        userData.data?.getValue("name").toString()
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
                                                                    stepDocuments.getLong(timeDate)
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
                                                                userRegionDiaryItems.add(diarySet)
                                                                userRegionDiaryItems.sortWith(
                                                                    compareBy({ it.writeTime })
                                                                )
                                                                userRegionDiaryItems.reverse()
                                                                adapter.notifyDataSetChanged()

                                                                if (userRegionDiaryItems.size == 0) {
                                                                    binding.noItemText.visibility =
                                                                        View.VISIBLE
                                                                } else {
                                                                    binding.noItemText.visibility =
                                                                        View.GONE
                                                                    binding.recyclerDiary.visibility =
                                                                        View.VISIBLE
                                                                }
                                                                userDataLoadingState.loadingCompleteData.value =
                                                                    true
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
                                                                userRegionDiaryItems.add(diarySet)
                                                                userRegionDiaryItems.sortWith(
                                                                    compareBy({ it.writeTime })
                                                                )
                                                                userRegionDiaryItems.reverse()
                                                                adapter.notifyDataSetChanged()

                                                                if (userRegionDiaryItems.size == 0) {
                                                                    binding.noItemText.visibility =
                                                                        View.VISIBLE
                                                                } else {
                                                                    binding.noItemText.visibility =
                                                                        View.GONE
                                                                    binding.recyclerDiary.visibility =
                                                                        View.VISIBLE
                                                                }
                                                                userDataLoadingState.loadingCompleteData.value =
                                                                    true
                                                            }

                                                        }
                                                }
                                            }
                                        }
                                } else {
                                    // 비밀 글
                                }
                            } else {
                                // 내가 차단한 글
                            }
                        }

                    }
            }
    }

}


@Keep
class UserRegionDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
