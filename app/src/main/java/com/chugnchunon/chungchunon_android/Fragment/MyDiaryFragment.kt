package com.chugnchunon.chungchunon_android.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.BroadCastReceiver
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentMyDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class MyDiaryFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentMyDiaryBinding? = null
    private val binding get() = _binding!!

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var running = false
    private lateinit var sensorManager: SensorManager
    private lateinit var step_sensor: Sensor

    lateinit var broadcastReceiver: BroadCastReceiver

    companion object {
        var todayTotalStepCount: Int = 0
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        // 걸음수 권한
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_DENIED
        ) {
            //ask for permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                100
            )
        }

        // 걸음수 셋업
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        broadcastReceiver = BroadCastReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_DATE_CHANGED)
        context?.registerReceiver(broadcastReceiver, filter)

        if (step_sensor != null) {
            sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_UI)
        } else {
        }


        // 기분 스니퍼
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

            val diarySet = hashMapOf(
                "writeTime" to FieldValue.serverTimestamp(),
                "todayMood" to (binding.todayMood.selectedItem.toString()),
                "todayDiary" to (binding.todayDiary.text.toString()),
            )

            diaryDB
                .document(userId.toString())
                .set(diarySet, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("내 일기", "작성 성공")
                }
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


    override fun onSensorChanged(stepEvent: SensorEvent?) {
        Log.d("걸음수", "sensorEvent ${stepEvent!!.values[0].toInt()}")
        todayTotalStepCount += stepEvent!!.values[0].toInt()
        binding.todayStepCount.text = todayTotalStepCount.toString()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("걸음수", "아직")
    }

//    private operator fun Int.plus(a: Int): Int = todayStepCount + a

}


//
//override fun onResume() {
//    super.onResume()
////        running = true
////        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
////
////        broadcastReceiver = BroadCastReceiver()
////        val filter = IntentFilter()
////        filter.addAction(Intent.ACTION_TIME_TICK)
////        context?.registerReceiver(broadcastReceiver, filter)
////
////        if (step_sensor != null) {
////            sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_UI)
////        } else {
////        }
//
//}
//
//override fun onPause() {
//    super.onPause()
////        running = false
////        sensorManager.unregisterListener(this)
//}