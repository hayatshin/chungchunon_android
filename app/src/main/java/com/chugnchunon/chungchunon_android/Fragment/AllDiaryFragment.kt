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
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentAllDiaryBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentMyDiaryBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_diary.*

class AllDiaryFragment : Fragment() {

    private val db = Firebase.firestore
    private val dbUsers = db.collection("users")

    private var _binding: FragmentAllDiaryBinding? = null
    private val binding get() = _binding!!

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


        dbUsers.get()
            .addOnSuccessListener { results ->
                for (result in results) {
                    Log.d("결과3", result.data.getValue("name").toString())
                    items.add(
                        DiaryCard(
                            result.data.getValue("name").toString(),
                            result.data.getValue("stepCount").toString()  ,
                            result.data.getValue("mood").toString().toInt(),
                            result.data.getValue("diary").toString()
                        )
                    )
                }
            }


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

        var adapter = DiaryCardAdapter(items)
        recyclerDiary.adapter = adapter
        recyclerDiary.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false
        )
    }
}