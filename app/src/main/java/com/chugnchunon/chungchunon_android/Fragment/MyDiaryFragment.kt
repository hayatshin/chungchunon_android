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
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.*
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.core.widget.addTextChangedListener
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.BroadcastReceiver.StepCountBroadcastReceiver
import com.chugnchunon.chungchunon_android.DataClass.MonthDate
import com.chugnchunon.chungchunon_android.DiaryActivity
import com.chugnchunon.chungchunon_android.FillCheckClass
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import kotlinx.android.synthetic.main.diary_card.*
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

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
    private val yearMonthDateFormat = SimpleDateFormat("yyyy-MM")

    lateinit var diaryFillCheck: DiaryFillClass
    lateinit var diaryEditCheck: DiaryEditClass

    private var editDiary: Boolean = false

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        var startService = Intent(activity, MyService::class.java)
        activity?.let { ContextCompat.startForegroundService(it, startService) }


        diaryFillCheck = ViewModelProvider(requireActivity()).get(DiaryFillClass::class.java)
        diaryEditCheck = ViewModelProvider(requireActivity()).get(DiaryEditClass::class.java)

        diaryFillCheck.diaryFill.observe(requireActivity(), Observer { value ->
//            binding.diaryBtn.isEnabled = value
            if (value && !editDiary) binding.diaryBtn.isEnabled = true
        })

        diaryEditCheck.diaryEdit.observe(requireActivity(), Observer { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })

        diaryEditCheck.moodEdit.observe(requireActivity(), Observer { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true) {
                binding.diaryBtn.isEnabled = true

                var nowMood = (binding.todayMood.selectedItem as Mood).image
                Log.d("체크", "$nowMood")
            }
        })


        // 일기 작성 여부
        val writeTime = LocalDateTime.now().toString().substring(0, 10)

        diaryDB
            .document("${userId}_${writeTime}")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null) {
                        if (document.exists()) {
                            // 일기 작성을 한 상태

                            editDiary = true
                            binding.diaryBtn.isEnabled = false
                            binding.diaryBtn.text = "일기 수정"

                            // 일기 보여주기
                            var oldDiary = document.data?.getValue("todayDiary").toString()
                            binding.todayDiary.setText(oldDiary)

                            // 마음 보여주기
                            var spinnerAdapter = binding.todayMood.adapter
                            var dbMood =
                                (document.data?.getValue("todayMood") as Map<*, *>)["image"].toString()
                                    .toInt()
                            var selectedPosition = getPositionMood(dbMood).toInt()
                            binding.todayMood.setSelection(selectedPosition)

                            Log.d("체크", "${dbMood} // ${selectedPosition}")

                            binding.todayMood.setOnItemSelectedListener(object :
                                AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    p0: AdapterView<*>?,
                                    p1: View?,
                                    p2: Int,
                                    p3: Long
                                ) {
                                    var nowMood = (binding.todayMood.selectedItem as Mood).image
                                    if (nowMood != dbMood) {
                                        diaryEditCheck.moodEdit.value = true
                                    }
                                }

                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    // null
                                }

                            })



                            binding.todayDiary.addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    p0: CharSequence?,
                                    p1: Int,
                                    p2: Int,
                                    p3: Int
                                ) {
                                    // null
                                }

                                override fun onTextChanged(
                                    char: CharSequence?,
                                    p1: Int,
                                    p2: Int,
                                    p3: Int
                                ) {
                                    if (binding.todayDiary.text.toString() != oldDiary) {
                                        diaryEditCheck.diaryEdit.value = true
                                    }
                                }

                                override fun afterTextChanged(p0: Editable?) {
                                    // null
                                }
                            })


                        } else {
                            // 일기 작성 놉
                        }
                    }
                } else {
                    // 일기 작성 놉
                }
            }

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

        // 매달 일기 작성
        var currentdate = System.currentTimeMillis()
        var currentYearMonth = yearMonthDateFormat.format(currentdate)
        var currentMonth = SimpleDateFormat("MM").format(currentdate)
        var removeZeroCurrentMonth = StringUtils.stripStart(currentMonth, "0");
        var currentMonthDate = MonthDate(currentMonth.toInt()).getDate


        var thisMonthCount = diaryDB
            .whereEqualTo("monthDate", currentYearMonth)
            .whereEqualTo("userId", userId)
            .count()

        thisMonthCount.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val calendarThisMonthCount = "${task.result.count}일"
                var spanText = SpannableStringBuilder()
                    .color(Color.RED) { append("${calendarThisMonthCount}") }
                    .append(" / ${currentMonthDate}일")

                binding.thisMonth.text = "${removeZeroCurrentMonth}월 일기 작성일"
                binding.diaryCount.text = spanText
            }
        }

        // 기분 스니퍼
        binding.todayMood.adapter = activity?.applicationContext?.let {
            MoodArrayAdapter(
                it,
                listOf(
                    Mood(R.drawable.ic_emotion_5, "많이 좋아요"),
                    Mood(R.drawable.ic_emotion_4, "좋아요"),
                    Mood(R.drawable.ic_emotion_3, "평범해요"),
                    Mood(R.drawable.ic_emotion_2, "슬퍼요"),
                    Mood(R.drawable.ic_emotion_1, "많이 슬퍼요"),
                )
            )
        }

        // 음성녹음

        model.initial(textToSpeechEngine, startForResult)

        binding.recordBtn.setOnClickListener {
            model.displaySpeechRecognizer()
//            val text = todayDiary.text?.trim().toString()
//            model.speak(if (text.isNotEmpty()) text else "일기를 써보세요")
        }

        // 다이어리 작성
        binding.todayDiary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // null
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                diaryFillCheck.diaryFill.value = char?.length != 0
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }

        })

        // 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {

            binding.diaryBtn.isEnabled = false

            var currentMilliseconds = System.currentTimeMillis()
            val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
            val writeTime = LocalDateTime.now()
            var diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"

//                "timestamp" to System.currentTimeMillis(),


            val diarySet = hashMapOf(
                "diaryId" to diaryId,
                "userId" to userId.toString(),
                "monthDate" to writeMonthDate,
                "timestamp" to FieldValue.serverTimestamp(),
                "todayMood" to binding.todayMood.selectedItem,
                "todayDiary" to (binding.todayDiary.text.toString()),
                "numLikes" to 0,
                "numComments" to 0,
            )

            diaryDB
                .document(diaryId)
                .set(diarySet, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("결과77", "성공")
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

    private fun getPositionMood(value: Number): Long {
        var result: Long = 0
        when (value) {
            2131230899 -> result = 4
            2131230900 -> result = 3
            2131230901 -> result = 2
            2131230902 -> result = 1
            2131230903 -> result = 0
        }
        return result
    }
}


class DiaryFillClass : ViewModel() {
    val diaryFill by lazy { MutableLiveData<Boolean>(false) }
}

class DiaryEditClass : ViewModel() {
    val diaryEdit by lazy { MutableLiveData<Boolean>(false) }
    val moodEdit by lazy { MutableLiveData<Boolean>(false) }
}