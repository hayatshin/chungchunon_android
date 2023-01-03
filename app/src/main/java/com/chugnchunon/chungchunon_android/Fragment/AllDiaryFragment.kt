package com.chugnchunon.chungchunon_android.Fragment

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_diary.*
import java.time.LocalDate
import java.time.LocalDateTime

class AllDiaryFragment : Fragment() {

    private val db = Firebase.firestore
    private val diaryDB = db.collection("diary")
    private val userDB = db.collection("users")

    private val userId = Firebase.auth.currentUser?.uid

    private var _binding: FragmentAllDiaryBinding? = null
    private val binding get() = _binding!!

    private var username: String = "안녕"
    private var userStepCount: String = "342"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAllDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var items: ArrayList<DiaryCard> = ArrayList()
        var currentTime = LocalDate.now()


        userDB
            .document("$userId")
            .get()
            .addOnSuccessListener { document ->
                Log.d("전체다이어리", "${document.data?.getValue("name").toString()}")
//                username = document.data?.getValue("name").toString()
//                userStepCount = document.data?.getValue("todayStepCount").toString()
            }

        diaryDB
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    items.add(
                        DiaryCard(
                            username,
                            userStepCount,
                            (document.data?.getValue("todayMood") as Mood).image,
                            document.data?.getValue("todayDiary").toString(),
                        )
                    )
                }

            }
    }
}
//                    items.add(
//                        DiaryCard(
//                            result.data.getValue("name").toString(),
//                            result.data.getValue("stepCount").toString()  ,
//                            result.data.getValue("mood").toString().toInt(),
//                            result.data.getValue("diary").toString()
//                        )
//                    )



//        for (i in 1..10) {
//            items.add(
//                DiaryCard(
//                    "신순국",
//                    "1,357",
//                    2131230871,
//                    "보고싶다, 건강하거라",
//                )
//            )
//        }


