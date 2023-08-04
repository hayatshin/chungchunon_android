package com.chugnchunon.chungchunon_android.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.FriendDiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_region_data.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import okhttp3.internal.toImmutableList
import kotlin.collections.ArrayList
import kotlin.text.StringBuilder

class FriendDataFragment : Fragment() {

    private var _binding: FragmentRegionDataBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val diaryDB = db.collection("diary")

    private val userId = Firebase.auth.currentUser?.uid

    lateinit var diarySet: DiaryCard
    private var friendDiaryItems: ArrayList<DiaryCard> = ArrayList()
    private lateinit var adapter: FriendDiaryCardAdapter
    lateinit var mcontext: Context

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var friendDataLoadingState: FriendDataLoadingState

    private val mutex = Mutex()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    companion object {
        private val CONTACT_REQ: Int = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegionDataBinding.inflate(inflater, container, false)
        val binding = binding.root

        binding.communitySelectRecycler.visibility = View.GONE


        binding.recyclerDiary.itemAnimator = null
        swipeRefreshLayout = binding.swipeRecyclerDiary
        binding.swipeRecyclerDiary.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            var ft = parentFragmentManager.beginTransaction()
            ft.detach(this).attach(this).commitAllowingStateLoss()
        }

        friendDataLoadingState =
            ViewModelProvider(requireActivity()).get(FriendDataLoadingState::class.java)
        friendDataLoadingState.loadingCompleteData.value = false
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        friendDataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->

            if (!friendDataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        // 휴대폰 연동 권한 체크
        val readContactPermissionCheck =
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS)
        if (readContactPermissionCheck == PackageManager.PERMISSION_DENIED) {
            // 권한 없음
            binding.authLayout.visibility = View.VISIBLE
            binding.dataLoadingProgressBar.visibility = View.GONE
        } else {
            // 권한 있음
            binding.authLayout.visibility = View.GONE
            if (resumePause == false) {
                friendDataLoadingState.loadingCompleteData.value = false
                friendDiaryItems.clear()

                uiScope.launch {
                    getData()

                    Handler().postDelayed({
                        friendDataLoadingState.loadingCompleteData.value =
                            true

                        if (friendDiaryItems.size == 0) {
                            binding.friendNoItemText.visibility =
                                View.VISIBLE
                        } else {
                            binding.friendNoItemText.visibility =
                                View.GONE
                        }
                    }, 3000)
                }

            }
        }

        // 일반
        adapter = FriendDiaryCardAdapter(requireContext(), friendDiaryItems)
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

            friendDiaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == toggleDiaryId) {
                    diaryItem.numLikes = newNumLikes?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            friendDiaryItems.clear()
            if (friendDiaryItems.isEmpty()) {
                uiScope.launch {
                    getData()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CONTACT_REQ && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 휴대폰 연동 권한 부여 o
            uiScope.launch {
                getData()
            }

        } else {
            // 휴대폰 연동 권한 부여 x
            binding.authLayout.visibility = View.VISIBLE
        }
    }


    var newNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val createDiaryId = intent?.getStringExtra("newDiaryId")
            val createNumComments = intent?.getIntExtra("newNumComments", 0)

            friendDiaryItems.forEachIndexed { index, diaryItem ->
                if (diaryItem.diaryId == createDiaryId) {
                    diaryItem.numComments = createNumComments?.toLong()
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    suspend fun getRefinedAllContactNumbers(): List<String> {
        val numbersList = getAllContactNumbers(requireActivity())
        var newNumberList = myContactNumber()

        for (number in numbersList) {
            val numberBuilder = StringBuilder(number)
            if (number.contains("-")) {
                // 있는 경우
                newNumberList.add(number.toString())
            } else {
                // 없는 경우
                when (number.length) {
                    13 -> {
                        newNumberList.add(number)
                    }
                    11 -> {
                        numberBuilder.insert(3, "-")
                        numberBuilder.insert(8, "-")
                        newNumberList.add(numberBuilder.toString())
                    }
                    10 -> {
                        numberBuilder.insert(3, "-")
                        numberBuilder.insert(7, "-")
                        newNumberList.add(numberBuilder.toString())
                    }
                }
            }
        }
        return newNumberList.distinct().toImmutableList()
    }

    suspend fun myContactNumber(): ArrayList<String> {
        var newNumberList = ArrayList<String>()

        val userData = userDB.document("$userId").get().await()
        val userPhone = userData.data?.getValue("phone").toString()
        newNumberList.add(userPhone)
        return newNumberList
    }

    @SuppressLint("Range")
    private fun getAllContactNumbers(context: Context): List<String> {
        val numbersList = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        while (cursor?.moveToNext() == true) {
            if (cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) != -1) {
                val number =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                numbersList.add(number)
            }
        }
        cursor?.close()
        return numbersList
    }

    private suspend fun getData() {

        val myContactList = getRefinedAllContactNumbers()

        diaryDB.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val diaryUserId = document.data.getValue("userId").toString()

                    userDB.document(diaryUserId)
                        .get()
                        .addOnSuccessListener { userData ->
                            try {
                                val userPhone = userData.data?.getValue("phone")
                                myContactList.forEach { contact ->

                                    if (userPhone == contact) {
                                        // 핸드폰에 있는 사람들 -> 전체 일기
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
                                                    "https://postfiles.pstatic.net/MjAyMzA1MTdfNTEg/MDAxNjg0MzAwMTE1NTg4.Ut_2NzdCmpjurruKjSwqWSH-c0_ONiJZM2Mn-ib-uSQg.qX8hjpYrVpE6Nlnnmcs1J780Ycwnl4WIuMLX-tpgVT8g.PNG.hayat_shin/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-05-17_%EC%98%A4%ED%9B%84_2.08.31.png?type=w773"
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
                                                                        friendDiaryItems.add(diarySet)
                                                                        friendDiaryItems.sortWith(
                                                                            compareBy({ it.writeTime })
                                                                        )
                                                                        friendDiaryItems.reverse()
                                                                        adapter.notifyDataSetChanged()

//                                                                        if (friendDiaryItems.size == 0) {
////                                                                            binding.friendNoItemText.visibility =
////                                                                                View.VISIBLE
//                                                                        } else {
//                                                                            binding.friendNoItemText.visibility =
//                                                                                View.GONE
//                                                                            binding.recyclerDiary.visibility =
//                                                                                View.VISIBLE
//                                                                        }
                                                                        friendDataLoadingState.loadingCompleteData.value =
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
                                                                        friendDiaryItems.add(diarySet)
                                                                        friendDiaryItems.sortWith(
                                                                            compareBy({ it.writeTime })
                                                                        )
                                                                        friendDiaryItems.reverse()
                                                                        adapter.notifyDataSetChanged()

//                                                                        if (friendDiaryItems.size == 0) {
////                                                                            binding.friendNoItemText.visibility =
////                                                                                View.VISIBLE
//                                                                        } else {
////                                                                            binding.friendNoItemText.visibility =
////                                                                                View.GONE
//                                                                            binding.recyclerDiary.visibility =
//                                                                                View.VISIBLE
//                                                                        }
                                                                        friendDataLoadingState.loadingCompleteData.value =
                                                                            true
                                                                    }
                                                                }
                                                        } else {
                                                            // 유저 정보 x
//                                                            if (document.data.contains("images")) {
//                                                                // 이미지 o
//                                                                val diaryImages =
//                                                                    document.data?.getValue("images") as ArrayList<String>
//
//                                                                diarySet = DiaryCard(
//                                                                    userId = userFinalId,
//                                                                    username = userFinalName,
//                                                                    userAvatar = userFinalAvatar,
//                                                                    diaryId = diaryId,
//                                                                    writeTime = DateFormat().convertMillis(
//                                                                        timefromdb
//                                                                    ),
//                                                                    stepCount = userFinalStep,
//                                                                    mood = diaryMood,
//                                                                    diary = diaryDiary,
//                                                                    numLikes = numLikes,
//                                                                    numComments = numComments,
//                                                                    images = diaryImages,
//                                                                    secret = false
//                                                                )
//
//                                                            } else {
//                                                                // 이미지 x
//                                                                diarySet = DiaryCard(
//                                                                    userId = userFinalId,
//                                                                    username = userFinalName,
//                                                                    userAvatar = userFinalAvatar,
//                                                                    diaryId = diaryId,
//                                                                    writeTime = DateFormat().convertMillis(
//                                                                        timefromdb
//                                                                    ),
//                                                                    stepCount = userFinalStep,
//                                                                    mood = diaryMood,
//                                                                    diary = diaryDiary,
//                                                                    numLikes = numLikes,
//                                                                    numComments = numComments,
//                                                                    secret = false
//                                                                )
//                                                            }
//                                                            friendDiaryItems.add(diarySet)
//                                                            friendDiaryItems.sortWith(compareBy({ it.writeTime }))
//                                                            friendDiaryItems.reverse()
//                                                            adapter.notifyDataSetChanged()
//
//                                                            if (friendDiaryItems.size == 0) {
////                                                                binding.friendNoItemText.visibility =
////                                                                    View.VISIBLE
//                                                            } else {
//                                                                binding.friendNoItemText.visibility =
//                                                                    View.GONE
//                                                                binding.recyclerDiary.visibility =
//                                                                    View.VISIBLE
//                                                            }
//                                                            friendDataLoadingState.loadingCompleteData.value =
//                                                                true
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
                            } catch (e: Exception) {
                                Log.d("친구", "$e")
                            }
                        }
                }
            }
    }
}

@Keep
class FriendDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
