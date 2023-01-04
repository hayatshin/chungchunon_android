package com.chugnchunon.chungchunon_android.Fragment

import com.chugnchunon.chungchunon_android.MyService
import com.chugnchunon.chungchunon_android.MyService.Companion.todayTotalStepCount
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.BroadcastReceiver.BroadcastReceiver
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.ViewModel.BaseViewModel
import com.chugnchunon.chungchunon_android.databinding.FragmentMyDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_diary.*
import kotlinx.android.synthetic.main.fragment_my_diary.*
import java.time.LocalDateTime
import java.util.*
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.BroadcastReceiver.StepCountBroadcastReceiver
import com.chugnchunon.chungchunon_android.DiaryActivity
import com.google.firebase.firestore.FieldValue

class MyDiaryFragment : Fragment() {

    private var _binding: FragmentMyDiaryBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

//    private lateinit var sensorManager: SensorManager
//    private lateinit var step_sensor: Sensor

    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var stepCountBroadcastReceiver: StepCountBroadcastReceiver

    private var currentMonth: String = ""
    private val model: BaseViewModel by viewModels()

    private var todayTotalStepCount: Int = 0


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        var startService = Intent(activity, MyService::class.java)
        activity?.let { ContextCompat.startForegroundService(it, startService) }

//        todayTotalStepCount?.observe(viewLifecycleOwner) { value ->
//            binding.todayStepCount.text = value.toString()
//        }


         // 오늘 걸음수 초기화
        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = document.getLong("todayStepCount")
            todayTotalStepCount = todayStepCountFromDB?.toInt() ?: 0
            binding.todayStepCount.text = "$todayStepCountFromDB 보"
        }

        currentMonth = LocalDateTime.now().toString().substring(0, 7)


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
        broadcastReceiver = BroadcastReceiver()
        stepCountBroadcastReceiver = StepCountBroadcastReceiver()

        val intent = IntentFilter()
        intent.addAction(Intent.ACTION_DATE_CHANGED)
        activity?.registerReceiver(broadcastReceiver, intent)


        registerBroadCastReceiver()


//        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        step_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
//
//        if (step_sensor != null) {
//            sensorManager.registerListener(this, step_sensor, SensorManager.SENSOR_DELAY_UI)
//        } else {
//        }


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

        // 음성녹음

        model.initial(textToSpeechEngine, startForResult)

        binding.recordBtn.setOnClickListener {
            model.displaySpeechRecognizer()
            val text = todayDiary.text?.trim().toString()
            model.speak(if (text.isNotEmpty()) text else "Text tidak boleh kosong")

        }

        // 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {

            val writeTime = LocalDateTime.now().toString().substring(0, 10)

            val diarySet = hashMapOf(
                "userId" to userId.toString(),
                "writeTime" to FieldValue.serverTimestamp(),
                "todayMood" to binding.todayMood.selectedItem,
                "todayDiary" to (binding.todayDiary.text.toString()),
            )

            diaryDB
                .document("${userId}_${writeTime.toString().substring(0, 10)}")
                .set(diarySet, SetOptions.merge())
                .addOnSuccessListener {
                    var intent = Intent(activity, DiaryActivity::class.java)
                    startActivity(intent)
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

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    .let { text -> text?.get(0) }
            binding.todayDiary.setText(spokenText)
        }
    }

    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) textToSpeechEngine.language = Locale("in_ID")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun registerBroadCastReceiver() {
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            receiver,
            IntentFilter(MyService.ACTION_STEP_COUNTER_NOTIFICATION)
        )
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                val todayTotalStepCount = it.getIntExtra("todayTotalStepCount", 0)
                binding.todayStepCount.text = "$todayTotalStepCount 보"
            }
        }
    }
}





//
//    override fun onSensorChanged(stepEvent: SensorEvent?) {
//        Log.d("결과아", "sensorEvent ${stepEvent!!.values[0].toInt()}")
//
//        var currentDate = LocalDate.now()
//
//        todayTotalStepCount = todayTotalStepCount?.plus(stepEvent!!.values[0].toInt())
//        binding.todayStepCount.text = todayTotalStepCount.toString()
//
//        // user 내 todayStepCount
//        var todayStepCountSet = hashMapOf(
//            "todayStepCount" to todayTotalStepCount
//        )
//
//        userDB.document("$userId").set(todayStepCountSet, SetOptions.merge())
//
//        var userStepCountSet = hashMapOf(
//            "$currentDate" to todayTotalStepCount
//        )
//
//        var periodStepCountSet = hashMapOf(
//            "$userId" to todayTotalStepCount
//        )
//
//        // user_step_count
//        db.collection("user_step_count")
//            .document("$userId")
//            .set(userStepCountSet, SetOptions.merge())
//
//        // period_step_count
//        db.collection("period_step_count")
//            .document("$currentDate")
//            .set(periodStepCountSet, SetOptions.merge())
//
//    }
//
//    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
//        Log.d("걸음수", "아직")
//    }


