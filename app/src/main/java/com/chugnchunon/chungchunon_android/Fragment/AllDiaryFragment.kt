package com.chugnchunon.chungchunon_android.Fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.DiaryCardAdapter
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentMyDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_diary.*
import java.time.LocalDate
import java.time.LocalDateTime

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
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            items.add(
                                DiaryCard(
                                    username,
                                    userStepCount,
                                    (document.data?.getValue("todayMood") as Map<*, *>)["image"] as Long,
                                    document.data?.getValue("todayDiary").toString(),
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
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

