package com.chugnchunon.chungchunon_android.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.DiaryActivity
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.chugnchunon.chungchunon_android.databinding.FragmentMyDiaryBinding


class MyDiaryFragment : Fragment() {

    private var _binding: FragmentMyDiaryBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.todayMood.adapter = activity?.applicationContext?.let {
            MoodArrayAdapter(
                it,
                listOf(
                    Mood(R.drawable.ic_emotion_1, "많이 슬퍼요"),
                    Mood(R.drawable.ic_emotion_2, "슬퍼요"),
                    Mood(R.drawable.ic_emotion_3, "평범해요"),
                    Mood(R.drawable.ic_emotion_4, "좋아요"),
                    Mood(R.drawable.ic_emotion_5, "많이 좋아요"),
                )
            )
        }

        binding.diaryBtn.setOnClickListener {
            Log.d("결과", binding.todayMood.selectedItem.toString())
        }


        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                val imm: InputMethodManager =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view!!.windowToken, 0)
                return true
            }
        })
        return view
    }
}