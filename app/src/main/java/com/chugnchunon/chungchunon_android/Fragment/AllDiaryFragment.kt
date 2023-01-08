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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

    private var items: ArrayList<DiaryCard> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAllDiaryBinding.inflate(inflater, container, false)
        val view = binding.root
        adapter = DiaryCardAdapter(requireContext(), items)


        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            commentNumChangeReceiver,
            IntentFilter("DELETE_ACTION")
        );

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            commentNumChangeReceiver,
            IntentFilter("CREATE_ACTION")
        );

        diaryDB
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    var userId = document.data?.getValue("userId")
                    var diaryId = document.data?.getValue("diaryId").toString()
                    var numLikes = document.data?.getValue("numLikes") as Long
                    var numComments = document.data?.getValue("numComments") as Long
                    var timefromdb = document.data?.getValue("timestamp") as com.google.firebase.Timestamp

                    userDB
                        .document("$userId")
                        .get()
                        .addOnSuccessListener { userInfo ->
                            var username = userInfo.data?.getValue("name").toString()
                            var todayStepCount = userInfo.data?.getValue("todayStepCount").toString()

                            // items 추가
                            items.add(
                                DiaryCard(
                                    diaryId,
                                    DateFormat().converDate(timefromdb),
                                    username,
                                    todayStepCount,
                                    (document.data?.getValue("todayMood") as Map<*, *>)["image"] as Long,
                                    document.data?.getValue("todayDiary").toString(),
                                    numLikes,
                                    numComments,
                                )
                            )

                            adapter.notifyDataSetChanged()
                        }
                }
            }


        binding.recyclerDiary.adapter = adapter
        binding.recyclerDiary.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false
        )
        return view
    }

    var commentNumChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var diaryPosition = intent?.getIntExtra("diaryPosition", 0)
            var newNumComments = intent?.getIntExtra("newNumComments", 0)

            items[diaryPosition!!].numComments = newNumComments!!.toLong()
            adapter.notifyDataSetChanged()
        }
    }
}


