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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chugnchunon.chungchunon_android.Adapter.FriendDiaryCardAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionDiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DataClass.Region
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.tabChange
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentRegionDataBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.view.*
import kotlinx.android.synthetic.main.fragment_region_data.view.*
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

        friendDataLoadingState =
            ViewModelProvider(requireActivity()).get(FriendDataLoadingState::class.java)

        friendDataLoadingState.loadingCompleteData.observe(requireActivity(), Observer { value ->

            if (!friendDataLoadingState.loadingCompleteData.value!!) {
                binding.swipeRecyclerDiary.visibility = View.GONE
                binding.dataLoadingProgressBar.visibility = View.VISIBLE
            } else {
                binding.swipeRecyclerDiary.visibility = View.VISIBLE
                binding.dataLoadingProgressBar.visibility = View.GONE
            }
        })

        friendDataLoadingState.loadingCompleteData.value = false
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        // 휴대폰 연동 권한 체크
        var readContactPermissionCheck =
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS)
        if (readContactPermissionCheck == PackageManager.PERMISSION_DENIED) {
            // 권한 없음
            binding.authLayout.visibility = View.VISIBLE
        } else {
            // 권한 있음
            binding.authLayout.visibility = View.GONE

            if (resumePause == false ) {
                friendDataLoadingState.loadingCompleteData.value = false
                friendDiaryItems.clear()
                getData()
            }
        }

        binding.authCancelBox.setOnClickListener {
            binding.authLayout.visibility = View.GONE
            binding.noContactReadAuthText.visibility = View.VISIBLE
        }

        binding.authConfirmBox.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 100)
        }


        // 일반


        adapter = FriendDiaryCardAdapter(requireContext(), friendDiaryItems)

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

//        getData()

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

            for (diaryItem in friendDiaryItems) {
                if (diaryItem.diaryId == toggleDiaryId) {
                    diaryItem.numLikes = newNumLikes?.toLong()
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private var blockReloadFragment: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            friendDiaryItems.clear()
            if (friendDiaryItems.isEmpty()) {
                getData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // 권한 부여
            getData()

        } else {
            binding.authLayout.visibility = View.VISIBLE
        }
    }


    var newNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            var createDiaryId = intent?.getStringExtra("newDiaryId")
            var createNumComments = intent?.getIntExtra("newNumComments", 0)

            for(diaryItem in friendDiaryItems) {
                if(diaryItem.diaryId == createDiaryId) {
                    diaryItem.numComments = createNumComments?.toLong()
                }
            }

            adapter.notifyDataSetChanged()
        }
    }

    private fun getRefinedAllContactNumbers(): ArrayList<String> {
        val numbersList = getAllContactNumbers(requireActivity())
        var newNumberList = ArrayList<String>()
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

    private fun getData() {

        var myContactList = getRefinedAllContactNumbers()

        diaryDB.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    var diaryUserId = document.data.getValue("userId").toString()

                    userDB.document("$diaryUserId")
                        .get()
                        .addOnSuccessListener { userData ->
                            var userPhone = userData.data?.getValue("phone")
                            myContactList.forEach { contact ->

                                if (contact == userPhone) {
                                    var blockedList =
                                        document.data?.getValue("blockedBy") as java.util.ArrayList<String>
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
                                                    document.data?.getValue("images") as java.util.ArrayList<String>

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
                                                                        friendDiaryItems.add(
                                                                            diarySet
                                                                        )

                                                                        friendDiaryItems.sortWith(
                                                                            compareBy({ it.writeTime })
                                                                        )
                                                                        friendDiaryItems.reverse()
                                                                        adapter.notifyDataSetChanged()

                                                                        binding.recyclerDiary.adapter =
                                                                            adapter
                                                                        binding.recyclerDiary.layoutManager =
                                                                            LinearLayoutManagerWrapper(
                                                                                mcontext,
                                                                                RecyclerView.VERTICAL,
                                                                                false
                                                                            )
                                                                        if (friendDiaryItems.size == 0) {
                                                                            binding.noItemText.visibility =
                                                                                View.VISIBLE
                                                                            binding.noItemText.text = getString(R.string.no_friend_contact)
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
                                                                        friendDiaryItems.add(
                                                                            diarySet
                                                                        )

                                                                        friendDiaryItems.sortWith(
                                                                            compareBy({ it.writeTime })
                                                                        )
                                                                        friendDiaryItems.reverse()
                                                                        adapter.notifyDataSetChanged()

                                                                        binding.recyclerDiary.adapter =
                                                                            adapter
                                                                        binding.recyclerDiary.layoutManager =
                                                                            LinearLayoutManagerWrapper(
                                                                                mcontext,
                                                                                RecyclerView.VERTICAL,
                                                                                false
                                                                            )

                                                                        if (friendDiaryItems.size == 0) {
                                                                            binding.noItemText.visibility =
                                                                                View.VISIBLE
                                                                            binding.noItemText.text = getString(R.string.no_friend_contact)
                                                                        } else {
                                                                            binding.noItemText.visibility =
                                                                                View.GONE
                                                                        }
                                                                    }
                                                            }
                                                        }
                                                    }
                                            }
                                            friendDataLoadingState.loadingCompleteData.value = true
                                        }

                                    }

                                }


                            }
                        }

                }



            }

    }
}


class FriendDataLoadingState : ViewModel() {
    val loadingCompleteData by lazy { MutableLiveData<Boolean>(false) }
}
