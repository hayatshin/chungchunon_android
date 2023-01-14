package com.chugnchunon.chungchunon_android.Fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.Comment
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.MutableData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_my_diary.*
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

    private var _binding: FragmentAllDiaryBinding? = null
    private val binding get() = _binding!!

    private var diaryItems: ArrayList<DiaryCard> = ArrayList()
    private var sortItems: ArrayList<DiaryCard> = ArrayList()

    var order = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAllDiaryBinding.inflate(inflater, container, false)
        val view = binding.root


        adapter = DiaryCardAdapter(requireContext(), diaryItems)


        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            deleteNumChangeReceiver,
            IntentFilter("DELETE_ACTION")
        );

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            createNumChangeReceiver,
            IntentFilter("CREATE_ACTION")
        );

        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                var userRegion = document.data?.getValue("region")
                var userSmallRegion = document.data?.getValue("smallRegion")
                var userRegionGroup = "${userRegion} ${userSmallRegion}"
                Log.d("1/13", "userRegionGroup: ${userRegionGroup}")

                diaryDB
                    .whereEqualTo("regionGroup", userRegionGroup)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            var userId = document.data?.getValue("userId").toString()
                            var username = document.data?.getValue("username").toString()
                            var stepCount = document.data?.getValue("stepCount") as Long
                            var diaryId = document.data?.getValue("diaryId").toString()
                            var numLikes = document.data?.getValue("numLikes") as Long
                            var numComments = document.data?.getValue("numComments") as Long
                            var timefromdb =
                                document.data?.getValue("timestamp") as com.google.firebase.Timestamp

                            // items 추가
                            var diarySet = DiaryCard(
                                userId,
                                username,
                                diaryId,
                                DateFormat().convertMillis(timefromdb),
                                username,
                                stepCount,
                                (document.data?.getValue("todayMood") as Map<*, *>)["image"] as Long,
                                document.data?.getValue("todayDiary").toString(),
                                numLikes,
                                numComments,
                            )

                            diaryItems.add(diarySet)
                            diaryItems.sortWith(compareBy({ it.writeTime }))
                            diaryItems.reverse()
                            adapter.notifyDataSetChanged()
                        }
                    }
                binding.recyclerDiary.adapter = adapter
                binding.recyclerDiary.layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.VERTICAL,
                    false)

            }



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

}

