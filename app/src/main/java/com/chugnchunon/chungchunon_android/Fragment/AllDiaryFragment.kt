package com.chugnchunon.chungchunon_android.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AllDiaryFragment : Fragment() {

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


        var adapter = DiaryCardAdapter(requireContext(), items)

        userDB
            .document("$userId")
            .get()
            .addOnSuccessListener { document ->
                username = document.data?.getValue("name").toString()
                userStepCount = document.data?.getValue("todayStepCount").toString()

                diaryDB
                    .orderBy("writeTime", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {

                            var userId = document.data?.getValue("userId")

                            userDB
                                .document("$userId")
                                .get()
                                .addOnSuccessListener { userinfo ->
                                    var username = userinfo.data?.getValue("name").toString()
                                    var todayStepCount =
                                        userinfo.data?.getValue("todayStepCount").toString()
                                    items.add(
                                        DiaryCard(
                                            document.data?.getValue("writeTime").toString(),
                                            username,
                                            todayStepCount,
                                            (document.data?.getValue("todayMood") as Map<*, *>)["image"] as Long,
                                            document.data?.getValue("todayDiary").toString(),
                                        )
                                    )
                                    adapter.notifyDataSetChanged()
                                }
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
}

